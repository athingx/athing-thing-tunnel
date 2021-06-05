package com.github.athingx.athing.aliyun.tunnel.boot;

import com.github.athingx.athing.aliyun.tunnel.component.TunnelThingComImpl;
import com.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import com.github.athingx.athing.standard.component.ThingCom;
import com.github.athingx.athing.standard.thing.boot.BootArguments;
import com.github.athingx.athing.standard.thing.boot.ThingComBoot;
import org.kohsuke.MetaInfServices;

import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static com.github.athingx.athing.standard.thing.boot.BootArguments.Converter.*;
import static java.lang.String.format;

/**
 * 隧道组件启动器
 */
@MetaInfServices
public class BootImpl implements ThingComBoot {

    private static final String OPT_REMOTE = "remote";
    private static final String OPT_SERVICE = "service";
    private static final String OPT_SUFFIX_SERVICE_TYPE = "_type";
    private static final String OPT_SUFFIX_SERVICE_IP = "_ip";
    private static final String OPT_SUFFIX_SERVICE_PORT = "_port";

    private static final String OPT_CONNECT_TIMEOUT = "connect_timeout";
    private static final String OPT_HANDSHAKE_TIMEOUT = "handshake_timeout";
    private static final String OPT_PING_INTERVAL = "ping_interval";
    private static final String OPT_RECONNECT_INTERVAL = "reconnect_interval";
    private static final String OPT_IDLE_DURATION = "idle_duration";
    private static final String OPT_THREADS = "threads";

    @Override
    public Specifications getSpecifications() {
        return () -> new Properties() {{
            put("TUNNEL-PROTOCOL", "V2.0");
            put("AUTHOR", "oldmanpushcart@gmail.com");
            put("VERSION", "1.0.0");
        }};
    }

    @Override
    public ThingCom bootUp(String productId, String thingId, BootArguments arguments) {

        final TunnelConfig config = new TunnelConfig();

        // 参数设置
        config.getConnect().setRemote(arguments.getArgument(OPT_REMOTE, cString, "wss://backend-iotx-remote-debug.aliyun.com:443"));
        config.getConnect().setConnectTimeoutMs(arguments.getArgument(OPT_CONNECT_TIMEOUT, cLong, 10L * 1000));
        config.getConnect().setHandshakeTimeoutMs(arguments.getArgument(OPT_HANDSHAKE_TIMEOUT, cLong, 10L * 1000));
        config.getConnect().setPingIntervalMs(arguments.getArgument(OPT_PING_INTERVAL, cLong, 30L * 1000));
        config.getConnect().setReconnectIntervalMs(arguments.getArgument(OPT_RECONNECT_INTERVAL, cLong, 30L * 1000));
        config.getConnect().setIdleDurationMs(arguments.getArgument(OPT_IDLE_DURATION, cLong, 15L * 60 * 1000));
        config.setThreads(arguments.getArgument(OPT_THREADS, cInt, 1));

        // 服务列表设置
        final Set<TunnelConfig.Service> services = config.getServices();
        for (final String name : arguments.getArguments(OPT_SERVICE, cString)) {
            services.add(new TunnelConfig.Service(
                    name,
                    Objects.requireNonNull(arguments.getArgument(name + OPT_SUFFIX_SERVICE_TYPE, cString), format("service:%s type is required!", name)),
                    Objects.requireNonNull(arguments.getArgument(name + OPT_SUFFIX_SERVICE_IP, cString), format("service:%s ip is required!", name)),
                    Objects.requireNonNull(arguments.getArgument(name + OPT_SUFFIX_SERVICE_PORT, cInt), format("service:%s port is required!", name))
            ));
        }

        return new TunnelThingComImpl(config);
    }

}
