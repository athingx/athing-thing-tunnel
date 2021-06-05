package com.github.athingx.athing.aliyun.tunnel.core.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * 平台请求打开会话
 */
public class PlatformOpenSessionRequestBody implements TunnelMessage.JsonBody {

    @SerializedName("tenant_token")
    private String token;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("product_key")
    private String productId;

    @SerializedName("device_name")
    private String thingId;

    @SerializedName("service_ip")
    private String serviceIp;

    @SerializedName("service_port")
    private int servicePort;

    @SerializedName("service_name")
    private String serviceName;

    @SerializedName("service_type")
    private String serviceType;

    public String getToken() {
        return token;
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

    public String getServiceIp() {
        return serviceIp;
    }

    public int getServicePort() {
        return servicePort;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

}
