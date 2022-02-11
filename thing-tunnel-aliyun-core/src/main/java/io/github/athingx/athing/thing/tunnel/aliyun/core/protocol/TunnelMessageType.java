package io.github.athingx.athing.thing.tunnel.aliyun.core.protocol;

/**
 * 隧道消息类型
 */
public interface TunnelMessageType {

    /**
     * 平台或终端的应答消息
     */
    int MSG_TYPE_RESPONSE = 0;

    /**
     * 终端请求平台握手
     */
    int MSG_TYPE_TERMINAL_HANDSHAKE = 1;

    /**
     * 平台请求端上打开会话
     */
    int MSG_TYPE_PLATFORM_OPEN_SESSION = 4;

    /**
     * 关闭会话
     */
    int MSG_TYPE_CLOSE_SESSION = 5;

    /**
     * 终端向平台传输数据
     */
    int MSG_TYPE_TERMINAL_TRANSMISSION_RAW_DATA = 21;

    /**
     * 平台向终端传输数据
     */
    int MSG_TYPE_PLATFORM_TRANSMISSION_RAW_DATA = 22;

    /**
     * PING
     */
    int MSG_TYPE_PING = 256;

}
