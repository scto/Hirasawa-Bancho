package io.hirasawa.server.chat

import io.hirasawa.server.Hirasawa
import io.hirasawa.server.bancho.packets.ChannelAvailableAutojoinPacket
import io.hirasawa.server.chat.command.ChatCommand
import io.hirasawa.server.chat.command.CommandContext
import io.hirasawa.server.chat.command.CommandSender
import io.hirasawa.server.chat.message.ChatMessage
import io.hirasawa.server.chat.message.GlobalChatMessage
import io.hirasawa.server.chat.message.PrivateChatMessage
import io.hirasawa.server.bancho.packets.ChannelRevokedPacket
import io.hirasawa.server.bancho.packets.SendMessagePacket
import io.hirasawa.server.bancho.user.BanchoUser
import io.hirasawa.server.bancho.user.User
import io.hirasawa.server.chat.enums.ChatChannelVisibility
import io.hirasawa.server.irc.clientcommands.Privmsg
import io.hirasawa.server.irc.objects.IrcUser
import io.hirasawa.server.plugin.HirasawaPlugin
import io.hirasawa.server.plugin.event.chat.UserChatEvent
import kotlin.collections.HashMap

class ChatEngine {
    val chatChannels = HashMap<String, ChatChannel>()
    val privateChatChannels = HashMap<User, HashMap<String, ChatChannel>>()
    val chatCommands = HashMap<String, Pair<ChatCommand, HirasawaPlugin>>()
    val connectedUsers = ArrayList<User>()

    operator fun set(key: String, value: ChatChannel) {
        chatChannels[key] = value
    }

    operator fun get(key: String): ChatChannel? {
        return chatChannels[key]
    }

    operator fun get(user: User, key: String): ChatChannel? {
        return privateChatChannels[user]?.get(key) ?: this[key]
    }

    fun handleChat(user: User, destination: String, message: String) {
        when {
            destination == "!CONSOLE" -> {
                println(message)
            }
            destination.startsWith("#") -> {
                val channel = this[user, destination] ?: return
                handleChat(GlobalChatMessage(user, channel, message))
            }
            else -> {
                if (destination == "BanchoBot" && Hirasawa.banchoBot.username != destination) {
                    // Redirect /bb commands to the "real" BanchoBot
                    val banchoUser = Hirasawa.banchoUsers[user.id]
                    banchoUser?.sendPacket(ChannelRevokedPacket(destination))
                    handleChat(user, Hirasawa.banchoBot.username, message)
                } else {
                    for (connectedUser in connectedUsers) {
                        if (connectedUser.username == destination) {
                            handleChat(PrivateChatMessage(user, connectedUser, message))
                        }
                    }
                }
            }
        }
    }

    fun handleChat(chatMessage: ChatMessage) {
        UserChatEvent(chatMessage).call().then {
            if (chatMessage is GlobalChatMessage) {
                handleGlobalChat(chatMessage)
            } else if (chatMessage is PrivateChatMessage) {
                handlePrivateChat(chatMessage)
            } else if (chatMessage.destinationName == "CONSOLE") {
                println(chatMessage.message)
            }
        }
    }

    private fun handlePrivateChat(chatMessage: PrivateChatMessage) {
        when (chatMessage.destination) {
            is BanchoUser -> {
                if (chatMessage.destination in Hirasawa.banchoUsers) {
                    Hirasawa.banchoUsers[chatMessage.destination]?.sendPacket(SendMessagePacket(chatMessage))
                }
            }
            is IrcUser -> {
                if (chatMessage.destination in Hirasawa.irc.connectedUsers) {
                    Hirasawa.irc.sendToUser(chatMessage.destination, Privmsg(chatMessage))
                }
            }
        }

    }

    private fun handleGlobalChat(chatMessage: GlobalChatMessage) {
        chatMessage.channel.sendMessage(chatMessage)

        if (chatMessage.message.startsWith("!")) {
            // is command
            val chatSegments = chatMessage.message.split(" ")
            handleCommand(chatSegments, chatMessage.source, chatMessage.channel)
        }
    }

    fun handleCommand(chatSegments: List<String>, sender: CommandSender, channel: ChatChannel) {
        val commandName = chatSegments[0].replace("!", "")
        if (commandName in chatCommands) {
            val chatCommand = chatCommands[commandName]?.first
            val context = CommandContext(sender, channel)
            chatCommand?.onCommand(context, chatSegments[0], chatSegments.slice(IntRange(1, chatSegments.size - 1)))
        }
    }


    fun registerCommand(command: ChatCommand, plugin: HirasawaPlugin) {
        chatCommands[command.name] = Pair(command, plugin)
    }

    fun removeCommands(plugin: HirasawaPlugin) {
        val iterator = chatCommands.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next().value
            if (pair.second == plugin) {
                iterator.remove()
            }
        }
    }

    fun addUser(user: User) {
        connectedUsers.add(user)
    }

    fun removeUser(user: User) {
        connectedUsers.remove(user)

        for (channel in chatChannels) {
            channel.value.removeUser(user)
        }

        if (user in privateChatChannels) {
            for (channel in privateChatChannels[user]!!) {
                channel.value.removeUser(user)
            }

            privateChatChannels.remove(user)
        }
    }

    fun removeChannel(chatChannel: ChatChannel) {
        when (chatChannel.visibility) {
            ChatChannelVisibility.PUBLIC -> {
                chatChannels.remove(chatChannel.metadata.name)
            }
            ChatChannelVisibility.PRIVATE -> {
                for (user in privateChatChannels) {
                    if (chatChannel.metadata.name in user.value) {
                        user.value[chatChannel.metadata.name]?.removeUser(user.key)
                    }
                }
            }
        }
    }

    fun addUserToPrivateChannel(user: User, chatChannel: ChatChannel) {
        if (user in privateChatChannels) {
            privateChatChannels[user]?.set(chatChannel.metadata.name, chatChannel)
        } else {
            privateChatChannels[user] = hashMapOf(chatChannel.metadata.name to chatChannel)
        }

        user.addChannel(chatChannel)
    }

    fun removeUserFromPrivateChannel(user: User, chatChannel: ChatChannel) {
        if (user in privateChatChannels) {
            privateChatChannels[user]?.remove(chatChannel.metadata.name)
        }
    }
}