package io.github.athingx.athing.thing.tunnel.aliyun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.athingx.athing.aliyun.thing.runtime.ThingRuntime;
import io.github.athingx.athing.aliyun.thing.runtime.access.ThingAccess;
import io.github.athingx.athing.aliyun.thing.runtime.linker.ThingLinker;
import io.github.athingx.athing.standard.thing.Thing;
import io.github.athingx.athing.standard.thing.ThingComListener;
import io.github.athingx.athing.thing.tunnel.ThingTunnelCom;
import io.github.athingx.athing.thing.tunnel.aliyun.core.Tunnel;
import io.github.athingx.athing.thing.tunnel.aliyun.core.TunnelConfig;
import io.github.athingx.athing.thing.tunnel.aliyun.domain.Debug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class DefaultThingTunnelCom implements ThingTunnelCom, ThingComListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TunnelConfig config;

    public DefaultThingTunnelCom(final TunnelConfig config) {
        this.config = config;
    }

    @Override
    public void onLoaded(Thing thing) throws Exception {

        final Gson gson = new GsonBuilder().create();
        final ThingRuntime runtime = ThingRuntime.getInstance(thing);
        final Tunnel tunnel = new Tunnel("/%s/tunnel".formatted(thing.getPath()), config);

        // 设备销毁时候，同时销毁通道
        thing.getDestroyFuture().onDone(future -> tunnel.destroy());

        final ThingLinker linker = runtime.getThingLinker();

        // 配置设备接入
        if (config.getAccess() == null) {
            config.setAccess(toTunnelAccess(runtime.getThingAccess()));
        }

        logger.info("{}/tunnel init completed, {}", thing, toString(config));

        // 订阅开关
        linker.subscribe("/sys/%s/edge/debug/switch".formatted(thing.getPath()), (topic, json) -> {
            final Debug debug = gson.fromJson(json, Debug.class);
            logger.info("{}/tunnel debug.switch={}", thing, debug.isEnable() ? "OPEN" : "CLOSE");
            if (debug.isEnable()) {
                tunnel.connect();
                logger.info("{}/tunnel is connect.", thing);
            } else {
                tunnel.disconnect();
                logger.info("{}/tunnel is disconnect.", thing);
            }
        }).sync();

    }

    private TunnelConfig.Access toTunnelAccess(ThingAccess access) {
        return new TunnelConfig.Access(access.getProductId(), access.getThingId(), access.getSecret());
    }

    private String toString(TunnelConfig config) {
        return ""
                + ";remote=" + config.getConnect().getRemote()
                + ";services=" + String.join(",", config.getServices().stream().map(TunnelConfig.Service::toString).collect(Collectors.joining()))
                + ";threads=" + config.getThreads()
                + ";connect-timeout=" + config.getConnect().getConnectTimeoutMs() + "ms"
                + ";handshake-timeout" + config.getConnect().getHandshakeTimeoutMs() + "ms"
                + ";idle-duration=" + config.getConnect().getIdleIntervalMs() + "ms"
                + ";ping-interval=" + config.getConnect().getPingIntervalMs() + "ms"
                + ";reconnect-interval=" + config.getConnect().getReconnectIntervalMs() + "ms";
    }

}
