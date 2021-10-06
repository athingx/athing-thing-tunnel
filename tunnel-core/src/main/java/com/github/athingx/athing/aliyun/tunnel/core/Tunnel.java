package com.github.athingx.athing.aliyun.tunnel.core;

import com.github.athingx.athing.aliyun.tunnel.core.channel.TunnelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * 网络隧道
 */
public class Tunnel {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TunnelConfig.Access access;
    private final TunnelConfig config;
    private final URI remoteURI;
    private final EventLoopGroup loop;
    private final Bootstrap boot;

    private volatile boolean isConnect;
    private volatile boolean isDestroy;
    private volatile Channel channel;

    /**
     * 网络隧道
     *
     * @param config 隧道配置
     * @throws URISyntaxException remote地址解析失败
     */
    public Tunnel(TunnelConfig config) throws URISyntaxException {
        this.access = config.getAccess();
        this.config = config;
        this.remoteURI = new URI(config.getConnect().getRemote());
        this.loop = new NioEventLoopGroup(config.getThreads());
        this.boot = bootstrap();
    }

    @Override
    public String toString() {
        return String.format("/%s/%s/tunnel", access.getProductId(), access.getThingId());
    }

    // 启动
    private Bootstrap bootstrap() {
        return new Bootstrap()
                .group(loop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnect().getConnectTimeoutMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new TunnelHandler(this, config));
    }

    /**
     * 建立隧道连接
     */
    public synchronized void connect() {

        // 如果隧道已经连接，则直接返回
        if (isConnect) {
            return;
        }

        // 标记隧道连接
        isConnect = true;

        _connect().awaitUninterruptibly();
    }

    private ChannelFuture _connect() {
        return boot.connect(remoteURI.getHost(), remoteURI.getPort())
                .addListener((ChannelFutureListener) connectF -> {

                    // 连接成功
                    if (connectF.isSuccess()) {
                        logger.info("{} connect success, remote={}", Tunnel.this, remoteURI);
                        channel = connectF.channel();

                        // 连接掉线
                        channel.closeFuture().addListener(closeFuture -> {
                            logger.info("{} connection is lost.", Tunnel.this);
                            tryReconnect();
                        });

                    }

                    // 连接失败
                    else {
                        logger.warn("{} connect failure!", Tunnel.this, connectF.cause());
                        tryReconnect();
                    }

                });
    }


    /**
     * 断开隧道连接
     */
    public synchronized void disconnect() {

        // 标记隧道为断开
        isConnect = false;

        // 断开连接
        if (null != channel && channel.isOpen()) {
            channel.close().awaitUninterruptibly();
            logger.info("{} disconnect!", this);
        }

    }

    /**
     * 尝试重连，直到{@link #disconnect()}被调用为止
     */
    private void tryReconnect() {
        if (!isConnect) {
            logger.debug("{} is closed, give up reconnect.", this);
            return;
        }

        logger.info("{} is try reconnect after {}ms, remote={}", this, config.getConnect().getReconnectIntervalMs(), remoteURI);
        loop.schedule(this::_connect, config.getConnect().getReconnectIntervalMs(), TimeUnit.MILLISECONDS);

    }

    /**
     * 销毁隧道
     */
    public synchronized void destroy() {

        // 如果已被标记为销毁，则返回
        if (isDestroy) {
            return;
        }

        // 先标记关闭
        isDestroy = true;

        // 隧道断开连接
        disconnect();

        // 关闭loop
        if (!loop.isShutdown()) {
            loop.shutdownGracefully().syncUninterruptibly();
        }

        logger.info("{} destroyed!", this);
    }

}
