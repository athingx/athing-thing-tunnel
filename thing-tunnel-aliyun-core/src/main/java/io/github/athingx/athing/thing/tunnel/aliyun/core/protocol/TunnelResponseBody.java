package io.github.athingx.athing.thing.tunnel.aliyun.core.protocol;

public class TunnelResponseBody implements TunnelMessage.JsonBody, TunnelResponseCode {

    private final int code;
    private final String message;

    public TunnelResponseBody(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return message;
    }

    public boolean isOk() {
        return code == RESP_OK;
    }

    public static TunnelResponseBody success() {
        return new TunnelResponseBody(RESP_OK, "success");
    }

    public static TunnelResponseBody failure(int code, String reason) {
        return new TunnelResponseBody(code, reason);
    }

}
