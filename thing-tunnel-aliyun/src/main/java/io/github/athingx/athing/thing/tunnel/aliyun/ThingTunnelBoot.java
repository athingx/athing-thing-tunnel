package io.github.athingx.athing.thing.tunnel.aliyun;

import io.github.athingx.athing.standard.component.ThingCom;
import io.github.athingx.athing.standard.thing.boot.ThingBoot;
import io.github.athingx.athing.standard.thing.boot.ThingBootArgument;
import io.github.athingx.athing.thing.tunnel.aliyun.core.TunnelConfig;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.athingx.athing.standard.thing.boot.ThingBootArgument.Converter.*;
import static java.lang.String.format;

/**
 * 网络隧道设备组件引导程序
 * <p>
 * threads=1&connect.remote=wss%3A%2F%2Fbackend-iotx-remote-debug.aliyun.com%3A443&connect.connect_timeout_ms=10000&connect.handshake_timeout_ms=10000&connect.ping_interval_ms=30000&connect.reconnect_interval_ms=30000&connect.idle_duration_ms=900000&service.local_ssh.type=SSH&service.local_ssh.ip=127.0.0.1&service.local_ssh.port=22&service.local_ssh.option.connect_timeout_ms=10000
 * </p>
 */
public class ThingTunnelBoot implements ThingBoot {

    @Override
    public ThingCom[] boot(String productId, String thingId, ThingBootArgument arguments) throws Exception {
        return new ThingCom[]{
                new ThingTunnelComImpl(productId, thingId, toConfig(arguments))
        };
    }

    private TunnelConfig toConfig(ThingBootArgument arguments) {
        final TunnelConfig config = new TunnelConfig();
        if (null != arguments) {
            arguments.optionArgument("threads", cInt, config::setThreads);

            // 配置连接参数
            configConnect(config.getConnect(), arguments);

            // 配置隧道服务
            configServiceSet(config.getServices(), arguments);
        }
        return config;
    }

    private void configConnect(TunnelConfig.Connect connect, ThingBootArgument arguments) {
        arguments.optionArgument("connect.remote", cString, connect::setRemote);
        arguments.optionArgument("connect.timeout_ms", cLong, connect::setConnectTimeoutMs);
        arguments.optionArgument("connect.handshake_timeout_ms", cLong, connect::setHandshakeTimeoutMs);
        arguments.optionArgument("connect.retry_interval_ms", cLong, connect::setReconnectIntervalMs);
        arguments.optionArgument("connect.ping_interval_ms", cLong, connect::setPingIntervalMs);
        arguments.optionArgument("connect.idle_interval_ms", cLong, connect::setIdleIntervalMs);
    }

    /**
     * 配置服务集合
     *
     * @param services  服务列表
     * @param arguments 引导参数
     */
    private void configServiceSet(Set<TunnelConfig.Service> services, ThingBootArgument arguments) {

        // 找出所有的服务名
        final Set<String> names = arguments.names().stream()
                .filter(name -> name.matches("^service\\..*\\.type$"))
                .map(name -> name.substring("service.".length(), name.lastIndexOf(".type")))
                .collect(Collectors.toSet());

        // 组装服务参数
        names.forEach(name -> {

            // 参数完整性检查
            if (!arguments.hasArguments(
                    format("service.%s.type", name),
                    format("service.%s.ip", name),
                    format("service.%s.port", name)
            )) {
                return;
            }

            // 组装服务
            final TunnelConfig.Service service = new TunnelConfig.Service(
                    name,
                    arguments.getArgument(format("service.%s.type", name), cString),
                    arguments.getArgument(format("service.%s.ip", name), cString),
                    arguments.getArgument(format("service.%s.port", name), cInt)
            );

            // 组装服务选项
            arguments.optionArgument(format("service.%s.option.connect_timeout_ms", name), cLong, service.getOption()::setConnectTimeoutMs);

            // 添加服务
            services.add(service);

        });

    }

    @Override
    public Properties getProperties() {
        return new Properties(){{
           put(PROP_GROUP, "io.github.athingx.athing.thing.tunnel");
           put(PROP_ARTIFACT, "thing-tunnel-aliyun");
        }};
    }

}
