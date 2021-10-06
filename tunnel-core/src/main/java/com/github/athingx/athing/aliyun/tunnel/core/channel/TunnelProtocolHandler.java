package com.github.athingx.athing.aliyun.tunnel.core.channel;

import com.github.athingx.athing.aliyun.tunnel.core.Tunnel;
import com.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import com.github.athingx.athing.aliyun.tunnel.core.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * 隧道协议处理器
 */
public class TunnelProtocolHandler extends SimpleChannelInboundHandler<TunnelMessage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Tunnel tunnel;
    private final TunnelConfig config;
    private final Map<String, Channel> sessionChannels = new ConcurrentHashMap<>();

    public TunnelProtocolHandler(Tunnel tunnel, TunnelConfig config) {
        this.tunnel = tunnel;
        this.config = config;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        // 如果Ra终端闲置了一段时间内，没有读写请求的触发，将会主动的被关闭
        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
            logger.warn("{} is idle duration {}ms, connection will be close", tunnel, config.getConnect().getIdleDurationMs());
            final Thread hook = new Thread(tunnel::destroy, "ra-terminal-shutdown-hook");
            hook.setDaemon(true);
            hook.start();
        }

        // 其他事件继续通知
        else {
            super.userEventTriggered(ctx, evt);
        }

    }

    /**
     * 写入应答
     *
     * @param channel 通道
     * @param request 请求
     * @param code    应答码
     * @param reason  应答原因
     */
    private void writeResponse(Channel channel, TunnelMessage request, int code, String reason) {
        channel.writeAndFlush(
                new TunnelMessage.Builder()
                        .response(request)
                        .body(new TunnelResponseBody(code, reason))
                        .build()
        );
    }

    /**
     * 写入应答成功
     *
     * @param channel 渠道
     * @param request 请求
     */
    private void writeResponseSuccess(Channel channel, TunnelMessage request) {
        channel.writeAndFlush(
                new TunnelMessage.Builder()
                        .response(request)
                        .body(TunnelResponseBody.success())
                        .build()
        );
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelMessage message) throws Exception {

        final Channel channel = ctx.channel();
        final TunnelMessage.Header header = message.getHeader();

        switch (header.getMessageType()) {

            case TunnelMessageType.MSG_TYPE_RESPONSE: {
                onPlatformResponse(message);
                break;
            }

            case TunnelMessageType.MSG_TYPE_PLATFORM_OPEN_SESSION: {
                onPlatformOpenSession(channel, message);
                break;
            }

            case TunnelMessageType.MSG_TYPE_PLATFORM_TRANSMISSION_RAW_DATA: {
                onPlatformTranRawData(channel, message);
                break;
            }

            case TunnelMessageType.MSG_TYPE_CLOSE_SESSION: {
                onPlatformCloseSession(channel, message);
                break;
            }

            default: {
                throw new TunnelMessageHandleException(
                        header,
                        format("unsupported MESSAGE-TYPE: %s!", header.getMessageType())
                );
            }

        }

    }

    // 处理应答
    private void onPlatformResponse(TunnelMessage message) {
        final TunnelResponseBody body = (TunnelResponseBody) message.getBody();

        // 平台应答失败
        if (!body.isOk()) {
            logger.warn("platform response failure, code={};reason={};",
                    body.getCode(),
                    body.getReason()
            );
        }

        // 平台应答成功
        else {
            logger.debug("platform response success, message-id={};code={};reason={};",
                    message.getHeader().getMessageId(),
                    body.getCode(),
                    body.getReason()
            );
        }
    }

    // 搜索隧道服务
    private TunnelConfig.Service searchTunnelService(PlatformOpenSessionRequestBody body) {

        /*
         * 这里需要对阿里云WEB版控制台进行兼容，这个家伙不讲武德，只传递了一个"service_port=0"过来，
         * 但他想表达的意思是：要进行本地的SSH登录
         */
        if (body.getServicePort() == 0) {
            return config.getServices().stream()
                    .filter(service -> service.getPort() == 22)
                    .filter(service -> "localhost".equalsIgnoreCase(service.getIp())
                            || "127.0.0.1".equals(service.getIp()))
                    .findFirst()
                    .orElse(null);
        }

        // 其他情况则需要严格匹配
        else {
            return config.getServices().stream()
                    .filter(service -> service.getPort() == body.getServicePort())
                    .filter(service -> Objects.equals(service.getIp(), body.getServiceIp()))
                    .filter(service -> Objects.equals(service.getType(), body.getServiceType()))
                    .filter(service -> Objects.equals(service.getName(), body.getServiceName()))
                    .findFirst()
                    .orElse(null);
        }

    }

    // 请求打开会话
    private void onPlatformOpenSession(Channel channel, TunnelMessage request) {

        final PlatformOpenSessionRequestBody body = (PlatformOpenSessionRequestBody) request.getBody();

        // 找到符合的服务
        final TunnelConfig.Service service = searchTunnelService(body);

        // 服务未找到
        if (null == service) {
            logger.warn("request tunnel-service not found! message-id={};", request.getHeader().getMessageId());
            writeResponse(channel, request, TunnelResponseCode.RESP_SERVICE_NOT_FOUND, "service not found");
            return;
        }

        // 打开会话
        openTunnelSession(channel, request, service);

    }

    // 启动会话
    private Bootstrap bootstrapRaSession(EventLoop loop, TunnelConfig.Service service, SimpleChannelInboundHandler<ByteBuf> handler) {
        return new Bootstrap()
                .group(loop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) service.getOption().getConnectTimeoutMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(handler);
    }

    // 打开会话
    private void openTunnelSession(Channel channel, TunnelMessage request, TunnelConfig.Service service) {

        // 生成会话ID
        final String sessionId = UUID.randomUUID().toString();

        // 启动会话
        final Bootstrap sessionBootstrap = bootstrapRaSession(channel.eventLoop(), service, new SimpleChannelInboundHandler<ByteBuf>() {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf sessionBuf) {
                channel.writeAndFlush(
                        new TunnelMessage.Builder()
                                .type(TunnelMessageType.MSG_TYPE_TERMINAL_TRANSMISSION_RAW_DATA)
                                .session(sessionId)
                                .body(new TerminalTranRawDataBody(sessionBuf))
                                .build()
                );
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) {

                logger.info("tunnel-session[{}] open success, of {}", sessionId, service);

                // 注册会话通道
                sessionChannels.put(sessionId, ctx.channel());

                // 应答会话通道打开成功
                channel.writeAndFlush(
                        new TunnelMessage.Builder()
                                .response(request)
                                .session(sessionId)
                                .body(TunnelResponseBody.success())
                                .build());
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) {

                logger.info("tunnel-session[{}] closed", sessionId);

                // 移除会话通道
                if (null != sessionChannels.remove(sessionId)) {
                    final TunnelMessage request = new TunnelMessage.Builder()
                            .type(TunnelMessageType.MSG_TYPE_CLOSE_SESSION)
                            .session(sessionId)
                            .build();

                    // 请求关闭会话
                    channel.writeAndFlush(request);
                }

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                logger.warn("tunnel-session[{}] occur error, session connection will be close!", sessionId, cause);
                ctx.channel().close();
            }

        });

        // 连接会话
        sessionBootstrap.connect(service.getIp(), service.getPort())
                .addListener((ChannelFutureListener) sessionConnectFuture -> {

                    // 会话连接失败
                    if (!sessionConnectFuture.isSuccess()) {
                        logger.warn("tunnel-session[{}] connect failure!", sessionId, sessionConnectFuture.cause());
                        final TunnelMessage response = new TunnelMessage.Builder()
                                .response(request)
                                .session(sessionId)
                                .body(new TunnelResponseBody(TunnelResponseCode.RESP_SERVICE_OPEN_FAILURE, format("connect failure: %s", sessionConnectFuture.cause().getLocalizedMessage())))
                                .build();
                        channel.writeAndFlush(response);
                    }

                });
    }

    // 平台向终端传输数据
    private void onPlatformTranRawData(Channel channel, TunnelMessage request) {
        final PlatformTranRawDataBody body = (PlatformTranRawDataBody) request.getBody();
        final Channel sessionChannel = sessionChannels.get(request.getHeader().getSessionId());

        // 找不到会话
        if (null == sessionChannel) {
            writeResponse(channel, request, TunnelResponseCode.RESP_SESSION_NOT_AVAILABLE, "session not available");
            return;
        }

        // 找到会话，向会话中写入数据
        sessionChannel.writeAndFlush(Unpooled.wrappedBuffer(body.toBytes()));
    }

    // 平台关闭会话
    private void onPlatformCloseSession(Channel channel, TunnelMessage request) {
        final Channel sessionChannel = sessionChannels.get(request.getHeader().getSessionId());
        if (null != sessionChannel) {
            sessionChannel.close();
        }
        writeResponseSuccess(channel, request);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 通道关闭时，需要主动关闭通道所打开的所有会话通道
        sessionChannels.forEach((sessionId, sessionChannel) -> sessionChannel.close());

        // 继续通知通道关闭
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("{} occur error, connection will be close!", tunnel, cause);
        ctx.channel().close();
    }

    /**
     * 消息处理异常
     */
    private static class TunnelMessageHandleException extends Exception {

        public TunnelMessageHandleException(TunnelMessage.Header header, String message) {
            super(format("%s handle occur error: %s", header, message));
        }

    }


}
