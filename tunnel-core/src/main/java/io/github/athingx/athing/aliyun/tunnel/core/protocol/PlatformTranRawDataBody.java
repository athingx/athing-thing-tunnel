package io.github.athingx.athing.aliyun.tunnel.core.protocol;

/**
 * 平台向终端传输数据
 */
public class PlatformTranRawDataBody implements TunnelMessage.Body {

    private final byte[] rData;

    public PlatformTranRawDataBody(byte[] rData) {
        this.rData = rData;
    }

    @Override
    public byte[] toBytes() {
        return rData;
    }

}
