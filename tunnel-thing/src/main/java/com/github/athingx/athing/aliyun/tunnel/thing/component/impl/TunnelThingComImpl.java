package com.github.athingx.athing.aliyun.tunnel.thing.component.impl;

import com.github.athingx.athing.aliyun.thing.runtime.ThingRuntime;
import com.github.athingx.athing.aliyun.thing.runtime.ThingRuntimes;
import com.github.athingx.athing.aliyun.thing.runtime.access.ThingAccess;
import com.github.athingx.athing.aliyun.thing.runtime.mqtt.ThingMqtt;
import com.github.athingx.athing.aliyun.tunnel.core.Tunnel;
import com.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import com.github.athingx.athing.aliyun.tunnel.thing.component.TunnelThingCom;
import com.github.athingx.athing.aliyun.tunnel.thing.component.impl.domain.Debug;
import com.github.athingx.athing.standard.thing.Thing;
import com.github.athingx.athing.standard.thing.boot.Disposable;
import com.github.athingx.athing.standard.thing.boot.Initializing;
import com.github.athingx.athing.standard.thing.op.executor.ThingFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TunnelThingComImpl implements TunnelThingCom, Initializing, Disposable {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new GsonBuilder().create();

    private final TunnelConfig config;
    private Thing thing;
    private ThingMqtt mqtt;
    private volatile Tunnel tunnel;

    public TunnelThingComImpl(TunnelConfig config) {
        this.config = config;
    }

    @Override
    public void onDestroyed() {
        if (null != tunnel) {
            tunnel.destroy();
        }
    }

    @Override
    public void onInitialized(Thing thing) throws Exception {
        final ThingRuntime runtime = ThingRuntimes.getThingRuntime(thing);
        this.thing = thing;
        this.mqtt = runtime.getThingMqtt();

        config.setAccess(toTunnelAccess(runtime.getThingAccess()));
        this.tunnel = new Tunnel(config);

        logger.info("{}/tunnel init completed, " +
                        "remote={};services={};threads={};" +
                        "connect-timeout={}ms;handshake-timeout={}ms;" +
                        "idle-duration={}ms;ping-interval={}ms;reconnect-interval={}ms;",
                thing,
                config.getConnect().getRemote(),
                config.getServices().size(),
                config.getThreads(),
                config.getConnect().getConnectTimeoutMs(),
                config.getConnect().getHandshakeTimeoutMs(),
                config.getConnect().getIdleDurationMs(),
                config.getConnect().getPingIntervalMs(),
                config.getConnect().getReconnectIntervalMs()
        );

        config.getServices().forEach(service -> logger.debug("{}/tunnel load tunnel-service: {}", thing, service));

        // 订阅开关
        subscribeSwitch().sync();

    }

    private TunnelConfig.Access toTunnelAccess(ThingAccess access) {
        return new TunnelConfig.Access(access.getProductId(), access.getThingId(), access.getSecret());
    }

    private ThingFuture<Void> subscribeSwitch() {
        return mqtt.subscribe(String.format("/sys/%s/%s/edge/debug/switch", thing.getProductId(), thing.getThingId()), (topic, message) -> {
            final Debug debug = gson.fromJson(message.getStringData(UTF_8), Debug.class);
            if (debug.isEnable()) {
                tunnel.connect();
            } else {
                tunnel.disconnect();
            }
        });
    }

}
