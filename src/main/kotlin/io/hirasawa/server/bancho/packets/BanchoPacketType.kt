package io.hirasawa.server.bancho.packets


enum class BanchoPacketType(val id: Short) {
    UNKNOWN(-1),
    OSU_SEND_USER_STATS(0),
    OSU_SEND_IRC_MESSAGE(1),
    OSU_EXIT(2),
    OSU_REQUEST_STATUS_UPDATE(3),
    OSU_PONG(4),
    BANCHO_LOGIN_REPLY(5),
    BANCHO_COMMAND_ERROR(6),
    BANCHO_SEND_MESSAGE(7),

    BANCHO_HANDLE_OSU_UPDATE(11),
    BANCHO_HANDLE_USER_QUIT(12),

    OSU_CHANNEL_JOIN(63),
    BANCHO_CHANNEL_JOIN_SUCCESS(64),
    BANCHO_CHANNEL_AVAILABLE(65),
    BANCHO_CHANNEL_REVOKED(66),
    BANCHO_CHANNEL_AVAILABLE_AUTOJOIN(67),

    BANCHO_LOGIN_PERMISSIONS(71),
    BANCHO_FRIENDS_LIST(72),

    BANCHO_PROTOCOL_NEGOTIATION(75),

    OSU_CHANNEL_LEAVE(78),

    BANCHO_USER_PRESENCE(83),

    OSU_USER_STATS_REQUEST(85),
    BANCHO_RESTART(86),

    BANCHO_CHANNEL_LISTING_COMPLETE(89),

    BANCHO_USER_PRESENCE_SINGLE(95),
    BANCHO_USER_PRESENCE_BUNDLE(96),
    OSU_USER_PRESENCE_REQUEST(97),

    BANCHO_RTX(105);

    companion object {
        private val map = BanchoPacketType.values().associateBy(BanchoPacketType::id)
        fun fromId(type: Short): BanchoPacketType = map[type] ?: UNKNOWN
    }
}