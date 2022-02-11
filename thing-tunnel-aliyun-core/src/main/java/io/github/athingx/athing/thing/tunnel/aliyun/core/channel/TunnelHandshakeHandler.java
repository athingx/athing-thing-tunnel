package io.github.athingx.athing.thing.tunnel.aliyun.core.channel;

import io.github.athingx.athing.thing.tunnel.aliyun.core.Tunnel;
import io.github.athingx.athing.thing.tunnel.aliyun.core.TunnelConfig;
import io.github.athingx.athing.thing.tunnel.aliyun.core.protocol.TerminalHandshakeRequestBody;
import io.github.athingx.athing.thing.tunnel.aliyun.core.protocol.TunnelMessage;
import io.github.athingx.athing.thing.tunnel.aliyun.core.protocol.TunnelResponseBody;
import io.github.athingx.athing.thing.tunnel.aliyun.core.util.SignUtils;
import io.github.athingx.athing.thing.tunnel.aliyun.core.protocol.TunnelMessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.github.athingx.athing.thing.tunnel.aliyun.core.channel.TunnelHandshakeHandler.TunnelHandshakeStateEvent.TUNNEL_HANDSHAKE_COMPLETE;
import static io.github.athingx.athing.thing.tunnel.aliyun.core.channel.TunnelHandshakeHandler.TunnelHandshakeStateEvent.TUNNEL_HANDSHAKE_TIMEOUT;
import static io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE;

/**
 * 隧道握手处理器
 */
public class TunnelHandshakeHandler extends SimpleChannelInboundHandler<TunnelMessage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Tunnel tunnel;
    private final TunnelConfig config;
    private final Map<String, ResponseCallback> responseCbs = new ConcurrentHashMap<>();

    public TunnelHandshakeHandler(Tunnel tunnel, TunnelConfig config) {
        this.tunnel = tunnel;
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage msg) throws Exception {

        final ResponseCallback responseCb = responseCbs.remove(msg.getHeader().getMessageId());
        if (null != responseCb) {
            responseCb.callback(msg);
        } else {
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // WebSocket握手成功，开始接力隧道握手
        if (evt == HANDSHAKE_COMPLETE) {
            handshakeForTunnel(ctx);
        }

        // 隧道握手超时
        else if (evt == TUNNEL_HANDSHAKE_TIMEOUT) {
            throw new TunnelProtocolHandshakeException("tunnel-handshake timeout!");
        }

        // 隧道握手成功
        else if (evt == TUNNEL_HANDSHAKE_COMPLETE) {
            logger.info("{} handshake success", tunnel);
        }

        // 继续分发事件
        super.userEventTriggered(ctx, evt);

    }

    /**
     * 隧道握手
     *
     * @param ctx ctx
     */
    private void handshakeForTunnel(ChannelHandlerContext ctx) {
        final String productId = config.getAccess().getProductId();
        final String thingId = config.getAccess().getThingId();
        final long timestamp = System.currentTimeMillis();
        final String messageId = UUID.randomUUID().toString();
        final TunnelMessage request = new TunnelMessage.Builder()
                .identity(messageId)
                .type(TunnelMessageType.MSG_TYPE_TERMINAL_HANDSHAKE)
                .timestamp(timestamp)
                .body(new TerminalHandshakeRequestBody(
                        productId,
                        thingId,
                        SignUtils.sign(productId, thingId, config.getAccess().getSecret(), timestamp),
                        config.getServices().stream()
                                .map(service -> new TerminalHandshakeRequestBody.ServiceMeta(
                                        service.getType(),
                                        service.getName(),
                                        service.getIp(),
                                        service.getPort())
                                )
                                .toArray(TerminalHandshakeRequestBody.ServiceMeta[]::new)
                ))
                .build();

        // 注册握手应答回调
        responseCbs.put(messageId, response -> {

            final TunnelResponseBody body = (TunnelResponseBody) response.getBody();

            // 握手失败
            if (!body.isOk()) {
                throw new TunnelProtocolHandshakeException(String.format("tunnel-handshake failure: code=%s;reason=%s;",
                        body.getCode(),
                        body.getReason()
                ));
            }

            // 握手成功
            else {
                logger.info("{} handshake success", tunnel);
                ctx.fireUserEventTriggered(TunnelHandshakeStateEvent.TUNNEL_HANDSHAKE_COMPLETE);
            }

        });

        // 注册握手超时监听
        ctx.executor().schedule(() -> {
                    if (null != responseCbs.remove(messageId)) {
                        ctx.fireUserEventTriggered(TUNNEL_HANDSHAKE_TIMEOUT);
                    }
                },
                config.getConnect().getHandshakeTimeoutMs(),
                TimeUnit.MILLISECONDS
        );

        // 发送握手请求
        ctx.channel().writeAndFlush(request);

    }

    /**
     * 隧道协议握手状态事件
     */
    public enum TunnelHandshakeStateEvent {

        /**
         * 握手超时
         */
        TUNNEL_HANDSHAKE_TIMEOUT,

        /**
         * 握手完成
         */
        TUNNEL_HANDSHAKE_COMPLETE

    }

    /**
     * 握手回调
     */
    private interface ResponseCallback {

        /**
         * 回调
         *
         * @param response 握手应答
         * @throws TunnelProtocolHandshakeException 握手异常
         */
        void callback(TunnelMessage response) throws TunnelProtocolHandshakeException;

    }

    /**
     * 隧道协议握手异常
     */
    private static class TunnelProtocolHandshakeException extends Exception {

        public TunnelProtocolHandshakeException(String message) {
            super(message);
        }

    }


}
