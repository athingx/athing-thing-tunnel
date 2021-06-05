package com.github.athingx.athing.aliyun.tunnel.core.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 终端向平台方向传输数据
 */
public class TerminalTranRawDataBody implements TunnelMessage.Body {

    private final byte[] rData;

    public TerminalTranRawDataBody(ByteBuf buffer) {
        this.rData = new byte[buffer.readableBytes()];
        buffer.readBytes(rData);
    }

    @Override
    public byte[] toBytes() {
        return rData;
    }

}