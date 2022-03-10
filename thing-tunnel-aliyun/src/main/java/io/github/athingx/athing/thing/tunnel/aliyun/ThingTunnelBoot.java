package io.github.athingx.athing.thing.tunnel.aliyun;

import io.github.athingx.athing.thing.tunnel.aliyun.core.TunnelConfig;
import io.github.athingx.athing.standard.component.ThingCom;
import io.github.athingx.athing.standard.thing.boot.ThingBoot;
import io.github.athingx.athing.standard.thing.boot.ThingBootArgument;

import java.io.InputStream;
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
        if (arguments.hasArguments("threads")) {
            config.setThreads(arguments.getArgument("threads", cInt));
        }

        // 配置连接参数
        configConnect(config.getConnect(), arguments);

        // 配置隧道服务
        configServiceSet(config.getServices(), arguments);

        return config;
    }

    private void configConnect(TunnelConfig.Connect connect, ThingBootArgument arguments) {
        if (arguments.hasArguments("connect.remote")) {
            connect.setRemote(arguments.getArgument("connect.remote", cString));
        }
        if (arguments.hasArguments("connect.timeout_ms")) {
            connect.setConnectTimeoutMs(arguments.getArgument("connect.timeout_ms", cLong));
        }
        if (arguments.hasArguments("connect.handshake_timeout_ms")) {
            connect.setHandshakeTimeoutMs(arguments.getArgument("connect.handshake_timeout_ms", cLong));
        }
        if (arguments.hasArguments("connect.retry_interval_ms")) {
            connect.setReconnectIntervalMs(arguments.getArgument("connect.retry_interval_ms", cLong));
        }
        if (arguments.hasArguments("connect.ping_interval_ms")) {
            connect.setPingIntervalMs(arguments.getArgument("connect.ping_interval_ms", cLong));
        }
        if (arguments.hasArguments("connect.idle_interval_ms")) {
            connect.setIdleIntervalMs(arguments.getArgument("connect.idle_interval_ms", cLong));
        }
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
        names.forEach(name->{

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
            if(arguments.hasArguments(format("service.%s.option.connect_timeout_ms", name))) {
                service.getOption().setConnectTimeoutMs(arguments.getArgument(format("service.%s.option.connect_timeout_ms", name), cLong));
            }

            // 添加服务
            services.add(service);

        });

    }

    @Override
    public Properties getProperties() {
        final Properties prop = ThingBoot.super.getProperties();
        try (final InputStream in = ThingTunnelBoot.class.getResourceAsStream("/io/github/athingx/athing/thing/tunnel/aliyun/thing-boot.properties")) {
            if (null != in) {
                prop.load(in);
            }
        } catch (Exception cause) {
            // ignore...
        }
        return prop;
    }

}
