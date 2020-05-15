package io.hirasawa.server

import io.hirasawa.server.bancho.packethandler.SendIrcMessagePacket
import io.hirasawa.server.bancho.packets.BanchoPacketType
import io.hirasawa.server.routes.BanchoRoute
import io.hirasawa.server.routes.test.TestGetRoute
import io.hirasawa.server.routes.test.TestPostRoute
import io.hirasawa.server.webserver.enums.HttpMethod
import io.hirasawa.server.webserver.Webserver
import io.hirasawa.server.webserver.routes.TestRoute
import java.io.File


fun main() {
    Hirasawa.packetRouter[BanchoPacketType.OSU_SEND_IRC_MESSAGE] = SendIrcMessagePacket()

    val webserver = Hirasawa.webserver

    webserver.addRoute("/", HttpMethod.GET, TestRoute())
    webserver.addRoute("/", HttpMethod.POST, BanchoRoute())

    webserver.addRoute("/test/get", HttpMethod.GET, TestGetRoute())
    webserver.addRoute("/test/post", HttpMethod.POST, TestPostRoute())

    Hirasawa.pluginManager.loadPluginsFromDirectory(File("plugins"))

    webserver.start()
}