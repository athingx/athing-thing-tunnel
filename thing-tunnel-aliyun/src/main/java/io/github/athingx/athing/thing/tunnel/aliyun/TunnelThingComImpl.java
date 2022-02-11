package io.github.athingx.athing.thing.tunnel.aliyun;

import io.github.athingx.athing.thing.tunnel.aliyun.core.Tunnel;
import io.github.athingx.athing.thing.tunnel.aliyun.core.TunnelConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.athingx.athing.aliyun.thing.runtime.ThingRuntime;
import io.github.athingx.athing.aliyun.thing.runtime.access.ThingAccess;
import io.github.athingx.athing.aliyun.thing.runtime.linker.ThingLinker;
import io.github.athingx.athing.thing.tunnel.TunnelThingCom;
import io.github.athingx.athing.thing.tunnel.aliyun.domain.Debug;
import io.github.athingx.athing.standard.thing.Thing;
import io.github.athingx.athing.standard.thing.ThingLifeCycle;
import io.github.athingx.athing.standard.thing.boot.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.stream.Collectors;

class TunnelThingComImpl implements TunnelThingCom, ThingLifeCycle {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new GsonBuilder().create();

    private final TunnelConfig config;
    private final Tunnel tunnel;

    @Inject
    private ThingRuntime runtime;

    public TunnelThingComImpl(String productId, String thingId, TunnelConfig config) throws URISyntaxException {
        this.config = config;
        this.tunnel = new Tunnel(String.format("/%s/%s/tunnel", productId, thingId), config);
    }

    @Override
    public void onDestroyed() {
        if (null != tunnel) {
            tunnel.destroy();
        }
    }

    @Override
    public void onLoaded() throws Exception {

        final Thing thing = runtime.getThing();
        final ThingLinker linker = runtime.getThingLinker();

        // 配置设备接入
        if (config.getAccess() == null) {
            config.setAccess(toTunnelAccess(runtime.getThingAccess()));
        }

        logger.info("{}/tunnel init completed, {}", thing, toString(config));

        // 订阅开关
        linker.subscribe(String.format("/sys/%s/edge/debug/switch", thing.path()), (topic, json) -> {
            final Debug debug = gson.fromJson(json, Debug.class);
            logger.info("{}/tunnel debug.switch={}", thing, debug.isEnable()?"OPEN":"CLOSE");
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
                + ";idle-duration=" + config.getConnect().getIdleDurationMs() + "ms"
                + ";ping-interval=" + config.getConnect().getPingIntervalMs() + "ms"
                + ";reconnect-interval=" + config.getConnect().getReconnectIntervalMs() + "ms";
    }

}
