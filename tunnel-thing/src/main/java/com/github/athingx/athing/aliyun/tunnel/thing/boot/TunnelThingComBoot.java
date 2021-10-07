package com.github.athingx.athing.aliyun.tunnel.thing.boot;

import com.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import com.github.athingx.athing.aliyun.tunnel.thing.component.impl.TunnelThingComImpl;
import com.github.athingx.athing.standard.api.ThingCom;
import com.github.athingx.athing.standard.thing.boot.BootArguments;
import com.github.athingx.athing.standard.thing.boot.ThingComBoot;

import java.util.Set;

import static com.github.athingx.athing.standard.thing.boot.BootArguments.Converter.*;
import static java.lang.String.format;

/**
 * 网络隧道设备组件引导程序
 */
public class TunnelThingComBoot implements ThingComBoot {

    @Override
    public ThingCom[] boot(String productId, String thingId, BootArguments arguments) {
        return new ThingCom[]{
                new TunnelThingComImpl(toConfig(arguments))
        };
    }

    private TunnelConfig toConfig(BootArguments arguments) {
        final TunnelConfig config = new TunnelConfig();
        if (arguments.hasArguments("threads")) {
            config.setThreads(arguments.getArgument("threads", cInt));
        }

        // 配置连接参数
        configConnect(config.getConnect(), arguments);

        // 配置隧道服务
        configServiceSet(config.getServices(), arguments);

        return config;
    }

    private void configConnect(TunnelConfig.Connect connect, BootArguments arguments) {
        if (arguments.hasArguments("connect.connect-timeout")) {
            connect.setConnectTimeoutMs(arguments.getArgument("connect.connect-timeout", cLong));
        }
        if (arguments.hasArguments("connect.handshake-timeout")) {
            connect.setHandshakeTimeoutMs(arguments.getArgument("connect.handshake-timeout", cLong));
        }
        if (arguments.hasArguments("connect.reconnect-interval")) {
            connect.setReconnectIntervalMs(arguments.getArgument("connect.reconnect-interval", cLong));
        }
        if (arguments.hasArguments("connect.ping-interval")) {
            connect.setPingIntervalMs(arguments.getArgument("connect.ping-interval", cLong));
        }
        if (arguments.hasArguments("connect.remote")) {
            connect.setRemote(arguments.getArgument("connect.remote", cString));
        }
        if (arguments.hasArguments("connect.idle-duration")) {
            connect.setIdleDurationMs(arguments.getArgument("connect.idle-duration", cLong));
        }
    }

    /**
     * 配置服务集合
     * <pre>
     * service.name=LOCAL_SSH&service-LOCAL_SSH.type=SSH&service-LOCAL_SSH.ip=127.0.0.1&service-LOCAL_SSH.port=22&service-LOCAL_SSH.connect-timeout=3000
     * </pre>
     *
     * @param services  服务列表
     * @param arguments 引导参数
     */
    private void configServiceSet(Set<TunnelConfig.Service> services, BootArguments arguments) {
        if (!arguments.hasArguments("service.name")) {
            return;
        }
        for (final String name : arguments.getArguments("service.name", cString)) {

            if (!arguments.hasArguments(
                    format("service-%s.type", name),
                    format("service-%s.ip", name),
                    format("service-%s.port", name)
            )) {
                continue;
            }

            final TunnelConfig.Service service = new TunnelConfig.Service(
                    name,
                    arguments.getArgument(format("service-%s.type", name), cString),
                    arguments.getArgument(format("service-%s.ip", name), cString),
                    arguments.getArgument(format("service-%s.port", name), cInt)
            );

            if (arguments.hasArguments(format("service-%s.connect-timeout", name))) {
                service.getOption().setConnectTimeoutMs(arguments.getArgument(format("service-%s.connect-timeout", name), cLong));
            }

            services.add(service);

        }
    }

}
