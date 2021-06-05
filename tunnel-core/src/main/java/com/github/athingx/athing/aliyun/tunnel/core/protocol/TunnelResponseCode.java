package com.github.athingx.athing.aliyun.tunnel.core.protocol;

public interface TunnelResponseCode {

    /**
     * 成功
     */
    int RESP_OK = 0;

    /**
     * 服务不存在
     */
    int RESP_SERVICE_NOT_FOUND = 101604;

    /**
     * 服务打开失败
     */
    int RESP_SERVICE_OPEN_FAILURE = 101604;

    /**
     * 会话不存在
     */
    int RESP_SESSION_NOT_AVAILABLE = 101671;

}
