package io.github.athingx.athing.thing.tunnel.aliyun.core.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * 终端请求握手
 */
public class TerminalHandshakeRequestBody implements TunnelMessage.JsonBody {

    @SerializedName("auth_type")
    private final String authType = "device";

    @SerializedName("uuid")
    private final String uuid;

    @SerializedName("product_key")
    private final String productId;

    @SerializedName("device_name")
    private final String thingId;

    @SerializedName("version")
    private final String version;

    @SerializedName("signmethod")
    private final String signMethod;

    @SerializedName("sign")
    private final String sign;

    @SerializedName("service_meta")
    private final ServiceMeta[] serviceMetas;

    public TerminalHandshakeRequestBody(String productId, String thingId, String sign, ServiceMeta[] serviceMetas) {
        this.uuid = "alibaba_iot";
        this.version = "2.0";
        this.signMethod = "hmacsha256";
        this.productId = productId;
        this.thingId = thingId;
        this.sign = sign;
        this.serviceMetas = serviceMetas;
    }

    public String getUuid() {
        return uuid;
    }

    public String getProductId() {
        return productId;
    }

    public String getThingId() {
        return thingId;
    }

    public String getVersion() {
        return version;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public String getSign() {
        return sign;
    }

    public ServiceMeta[] getServiceMetas() {
        return serviceMetas;
    }

    /**
     * 服务元数据
     */
    public static class ServiceMeta {

        @SerializedName("service_type")
        private final String type;

        @SerializedName("service_name")
        private final String name;

        @SerializedName("service_ip")
        private final String ip;

        @SerializedName("service_port")
        private final int port;

        public ServiceMeta(String type, String name, String ip, int port) {
            this.type = type;
            this.name = name;
            this.ip = ip;
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

    }

}
