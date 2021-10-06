package com.github.athingx.athing.aliyun.tunnel.core;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class TunnelConfig {

    /**
     * 隧道工作线程数
     */
    private int threads = 1;

    /**
     * 隧道访问
     */
    private Access access;

    /**
     * 隧道连接
     */
    private Connect connect = new Connect();

    /**
     * 隧道服务清单
     */
    private final Set<Service> services = new LinkedHashSet<>();

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Connect getConnect() {
        return connect;
    }

    public void setConnect(Connect connect) {
        this.connect = connect;
    }

    public Set<Service> getServices() {
        return services;
    }

    /**
     * 隧道服务
     */
    public static class Service {

        private final String type;
        private final String name;
        private final String ip;
        private final int port;
        private final Option option = new Option();

        /**
         * 隧道服务
         *
         * @param name 服务名称
         * @param type 服务类型
         * @param ip   服务IP
         * @param port 服务端口
         */
        public Service(String name, String type, String ip, int port) {
            this.type = Objects.requireNonNull(type);
            this.name = Objects.requireNonNull(name);
            this.ip = Objects.requireNonNull(ip);
            this.port = port;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public Option getOption() {
            return option;
        }

        @Override
        public String toString() {
            return String.format("%s://%s:%s/%s?%s", type, ip, port, name, option);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Service)) {
                return false;
            }
            final Service service = (Service) other;
            return port == service.port
                    && Objects.equals(type, service.type)
                    && Objects.equals(name, service.name)
                    && Objects.equals(ip, service.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name, ip, port, option);
        }

        /**
         * 参数
         */
        public static class Option {

            /**
             * 服务连接超时时间，默认10秒
             */
            private long connectTimeoutMs = 10L * 1000;

            public long getConnectTimeoutMs() {
                return connectTimeoutMs;
            }

            public void setConnectTimeoutMs(long connectTimeoutMs) {
                this.connectTimeoutMs = connectTimeoutMs;
            }

            @Override
            public String toString() {
                return "?"
                        + "connect-timeout=" + connectTimeoutMs
                        ;
            }

        }

    }

    /**
     * 隧道访问
     */
    public static class Access {

        private final String productId;
        private final String thingId;
        private final String secret;

        /**
         * 隧道访问
         *
         * @param productId 产品ID
         * @param thingId   设备ID
         * @param secret    设备密码
         */
        public Access(String productId, String thingId, String secret) {
            this.productId = productId;
            this.thingId = thingId;
            this.secret = secret;
        }

        public String getProductId() {
            return productId;
        }

        public String getThingId() {
            return thingId;
        }

        public String getSecret() {
            return secret;
        }

    }

    /**
     * 隧道连接选
     */
    public static class Connect {

        /**
         * 服务地址
         */
        private String remote = "wss://backend-iotx-remote-debug.aliyun.com:443";

        /**
         * 隧道服务器连接超时时间，默认10秒
         */
        private long connectTimeoutMs = 10L * 1000;

        /**
         * 隧道服务器握手超时时间，默认10秒
         */
        private long handshakeTimeoutMs = 10L * 1000;

        /**
         * 隧道服务器ping间隔，默认30秒
         */
        private long pingIntervalMs = 30L * 1000;

        /**
         * 隧道服务器重连间隔，默认30秒
         */
        private long reconnectIntervalMs = 30L * 1000;

        /**
         * 隧道服务器空闲持续时间，默认15分钟
         */
        private long idleDurationMs = 15L * 60 * 1000;

        public String getRemote() {
            return remote;
        }

        public void setRemote(String remote) {
            this.remote = remote;
        }

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public long getHandshakeTimeoutMs() {
            return handshakeTimeoutMs;
        }

        public void setHandshakeTimeoutMs(long handshakeTimeoutMs) {
            this.handshakeTimeoutMs = handshakeTimeoutMs;
        }

        public long getPingIntervalMs() {
            return pingIntervalMs;
        }

        public void setPingIntervalMs(long pingIntervalMs) {
            this.pingIntervalMs = pingIntervalMs;
        }

        public long getReconnectIntervalMs() {
            return reconnectIntervalMs;
        }

        public void setReconnectIntervalMs(long reconnectIntervalMs) {
            this.reconnectIntervalMs = reconnectIntervalMs;
        }

        public long getIdleDurationMs() {
            return idleDurationMs;
        }

        public void setIdleDurationMs(long idleDurationMs) {
            this.idleDurationMs = idleDurationMs;
        }

    }
}
