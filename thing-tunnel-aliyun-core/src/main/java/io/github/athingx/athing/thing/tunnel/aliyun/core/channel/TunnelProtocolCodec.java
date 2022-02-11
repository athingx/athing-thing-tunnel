package io.github.athingx.athing.thing.tunnel.aliyun.core.channel;

import io.github.athingx.athing.thing.tunnel.aliyun.core.protocol.*;
import io.github.athingx.athing.thing.tunnel.aliyun.core.util.GsonFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 隧道协议编解码器
 */
public class TunnelProtocolCodec extends MessageToMessageCodec<WebSocketFrame, TunnelMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelMessage tunnelMessage, List<Object> out) {
        try {
            out.add(new BinaryWebSocketFrame(Unpooled.buffer().writeBytes(tunnelMessage.toBytes())));
        } catch (Throwable cause) {
            ctx.fireExceptionCaught(new TunnelProtocolCodecException("encode ra-protocol failure!", cause));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) {
        try {
            final ByteBuf buf = frame.content();
            final TunnelMessage.Header header = decodeHeader(buf);
            out.add(new TunnelMessage(header, decodeBody(header, buf)));
        } catch (TunnelProtocolCodecException rpCause) {
            ctx.fireExceptionCaught(rpCause);
        } catch (Throwable cause) {
            ctx.fireExceptionCaught(new TunnelProtocolCodecException("decode ra-protocol failure!", cause));
        }

    }

    /**
     * 解码消息头
     *
     * @param buf 数据
     * @return 消息头
     * @throws TunnelProtocolCodecException 解码失败
     */
    private TunnelMessage.Header decodeHeader(ByteBuf buf) throws TunnelProtocolCodecException {
        return GsonFactory.getGson()
                .fromJson(new String(bounds(buf, TunnelMessage.boundary), UTF_8), TunnelMessage.Header.class);
    }

    /**
     * 解码消息体
     *
     * @param header 消息头
     * @param buf    数据
     * @return 消息体
     * @throws TunnelProtocolCodecException 解码失败
     */
    private TunnelMessage.Body decodeBody(TunnelMessage.Header header, ByteBuf buf) throws TunnelProtocolCodecException {

        // 检查消息载荷长度是否超出数据预期
        if (header.getLength() > buf.readableBytes()) {
            throw new TunnelProtocolCodecException(String.format("too large PAYLOAD-LENGTH: %s for BODY! message-id=%s",
                    header.getLength(),
                    header.getMessageId()
            ));
        }

        // 读取有效消息载荷
        final byte[] data = new byte[header.getLength()];
        buf.readBytes(data);

        switch (header.getMessageType()) {
            case TunnelMessageType.MSG_TYPE_RESPONSE -> {
                return GsonFactory.getGson().fromJson(new String(data, UTF_8), TunnelResponseBody.class);
            }
            case TunnelMessageType.MSG_TYPE_PLATFORM_OPEN_SESSION -> {
                return GsonFactory.getGson().fromJson(new String(data, UTF_8), PlatformOpenSessionRequestBody.class);
            }
            case TunnelMessageType.MSG_TYPE_PLATFORM_TRANSMISSION_RAW_DATA -> {
                return new PlatformTranRawDataBody(data);
            }
            case TunnelMessageType.MSG_TYPE_CLOSE_SESSION -> {
                return TunnelMessage.Body.empty;
            }
            default -> throw new TunnelProtocolCodecException(format("unsupported MESSAGE-TYPE: %s for BODY! message-id=%s",
                    header.getMessageType(),
                    header.getMessageId()
            ));
        }
    }

    private byte[] bounds(ByteBuf buf, byte... boundary) throws TunnelProtocolCodecException {

        final int limit = buf.readableBytes();
        final byte[] match = new byte[boundary.length];
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(limit)) {

            int pos = 0;
            for (int index = 0; index < limit; index++) {

                final byte data = buf.readByte();

                if (data == boundary[pos]) {
                    match[pos++] = data;
                } else {
                    out.write(match, 0, pos);
                    out.write(data);
                    pos = 0;
                }

                if (pos == match.length) {
                    break;
                }

            }

            return out.toByteArray();
        } catch (IOException cause) {
            throw new TunnelProtocolCodecException("decode occur error at bounds", cause);
        }

    }


    /**
     * Ra协议编解码异常
     */
    private static class TunnelProtocolCodecException extends Exception {

        public TunnelProtocolCodecException(String message) {
            super(message);
        }

        public TunnelProtocolCodecException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
