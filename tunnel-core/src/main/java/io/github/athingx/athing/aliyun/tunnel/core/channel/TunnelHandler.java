package io.github.athingx.athing.aliyun.tunnel.core.channel;

import io.github.athingx.athing.aliyun.tunnel.core.Tunnel;
import io.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.UUID;

import static io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory.newHandshaker;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.V13;

/**
 * 隧道处理器
 */
public class TunnelHandler extends ChannelInboundHandlerAdapter {

    private final Tunnel tunnel;
    private final TunnelConfig config;

    public TunnelHandler(Tunnel tunnel, TunnelConfig config) {
        this.tunnel = tunnel;
        this.config = config;
    }

    private static SSLEngine initSSLEngine() throws SSLException {
        final char[] secret = UUID.randomUUID().toString().toCharArray();
        try (final InputStream in = Tunnel.class.getResourceAsStream("/cret/ali-ca.crt")) {

            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, secret);
            keyStore.setCertificateEntry("root", CertificateFactory.getInstance("X.509").generateCertificate(in));

            final KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmFactory.init(keyStore, secret);

            final TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStore);

            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), null);

            final SSLEngine engine = ctx.createSSLEngine();
            engine.setUseClientMode(true);

            return engine;
        } catch (Exception cause) {
            throw new SSLException("create TLS/SSL failure!", cause);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addLast(
                new SslHandler(initSSLEngine()),
                new HttpClientCodec(),
                new HttpObjectAggregator(512 * 1024),
                new ChunkedWriteHandler(),
                new WebSocketClientProtocolHandler(newHandshaker(new URI(config.getConnect().getRemote()), V13, null, true, new DefaultHttpHeaders())),
                new TunnelProtocolCodec(),
                new TunnelLoggingHandler(),
                new TunnelHandshakeHandler(tunnel, config),
                new TunnelPingHandler(config),
                new IdleStateHandler(0, 0, (int) (config.getConnect().getIdleDurationMs() / 1000)),
                new TunnelProtocolHandler(tunnel, config)
        );
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {

    }

}
