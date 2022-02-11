package io.github.athingx.athing.thing.tunnel.aliyun.core.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 签名工具类
 */
public class SignUtils {

    /**
     * 字节数组转16进制字符串
     *
     * @param bArray 目标字节数组
     * @return 16进制字符串
     */
    public static String bytesToHexString(final byte[] bArray) {
        final StringBuilder sb = new StringBuilder(bArray.length * 2);
        for (byte b : bArray)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Ra签名
     *
     * @param productId 产品ID
     * @param thingId   设备ID
     * @param secret    设备密码
     * @return Ra签名
     */
    public static String sign(String productId, String thingId, String secret, long timestamp) {
        final String content = String.format("clientId%sdeviceName%sproductKey%stimestamp%s",
                "alibaba_iot",
                thingId,
                productId,
                timestamp
        );
        try {
            final Mac mac = Mac.getInstance("hmacsha256");
            mac.init(new SecretKeySpec(secret.getBytes(UTF_8), mac.getAlgorithm()));
            return bytesToHexString(mac.doFinal(content.getBytes(UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
