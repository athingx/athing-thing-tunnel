package io.github.athingx.athing.aliyun.tunnel.core.channel;

import io.github.athingx.athing.aliyun.tunnel.core.protocol.TunnelMessage;
import io.github.athingx.athing.aliyun.tunnel.core.util.GsonFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 隧道消息日志处理器
 */
public class TunnelLoggingHandler extends ChannelDuplexHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (msg instanceof TunnelMessage) {
            loggingForWrite(ctx.channel(), (TunnelMessage) msg);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TunnelMessage) {
            loggingForRead(ctx.channel(), (TunnelMessage) msg);
        }
        super.channelRead(ctx, msg);
    }

    private void loggingForRead(Channel channel, TunnelMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug("CHANNEL:{} -> {}", channel, message.getHeader());
        }
        if (logger.isTraceEnabled()) {
            logger.debug("CHANNEL:{} -> DATA:\n{}", channel, toString(message));
        }
    }

    private void loggingForWrite(Channel channel, TunnelMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug("CHANNEL:{} <- {}", channel, message.getHeader());
        }
        if (logger.isTraceEnabled()) {
            logger.debug("CHANNEL:{} <- DATA:\n{}", channel, toString(message));
        }
    }

    private String toString(TunnelMessage message) {
        if (message.getBody() instanceof TunnelMessage.JsonBody) {
            return new String(message.toBytes(), UTF_8);
        }
        final StringBuilder toStringSB = new StringBuilder();
        toStringSB.append("{");
        toStringSB.append(String.format("\tHEADER:[%s]\n", GsonFactory.getGson().toJson(message.getHeader())));
        if (message.getHeader().getLength() > 0) {
            toStringSB.append(String.format("\tBODY:[size=%s]\n", message.getBody().toBytes().length));
        }
        toStringSB.append("}");
        return toStringSB.toString();
    }


}
