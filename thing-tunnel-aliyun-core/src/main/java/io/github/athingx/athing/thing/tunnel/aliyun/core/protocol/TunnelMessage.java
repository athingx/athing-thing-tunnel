package io.github.athingx.athing.thing.tunnel.aliyun.core.protocol;

import io.github.athingx.athing.thing.tunnel.aliyun.core.util.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 隧道消息
 */
public class TunnelMessage {

    /**
     * 分隔边界
     */
    public static final byte[] boundary = {'\r', '\n', '\r', '\n'};

    /**
     * 分割边界字符串
     */
    public static final String boundaryString = new String(boundary);
    private static final Gson gson = GsonFactory.getGson();
    private final Header header;
    private final Body body;

    public TunnelMessage(Header header, Body body) {
        this.header = header;
        this.body = body;
    }

    public Header getHeader() {
        return header;
    }

    public Body getBody() {
        return body;
    }


    /**
     * 序列化消息
     *
     * @return 消息序列化数据
     */
    public byte[] toBytes() {

        // 序列化消息体
        final byte[] payload = body.toBytes();

        // 计算载荷长度，并修正消息载荷长度
        header.length = payload.length;

        // 序列化消息头
        final byte[] hBytes = gson.toJson(header).getBytes(UTF_8);

        // 序列化整个消息
        final ByteBuffer buffer = ByteBuffer.allocate(hBytes.length + boundary.length + header.length)
                .put(hBytes)
                .put(boundary)
                .put(payload);
        buffer.flip();
        return buffer.array();

    }


    /**
     * 消息体
     */
    public interface Body {

        /**
         * 空消息体
         */
        Body empty = () -> new byte[0];

        /**
         * 序列化消息体
         *
         * @return 消息体序列化数据
         */
        byte[] toBytes();

    }


    /**
     * Json消息体
     */
    public interface JsonBody extends Body {

        @Override
        default byte[] toBytes() {
            return (GsonFactory.getGson().toJson(this) + boundaryString).getBytes(UTF_8);
        }

    }

    /**
     * 消息构造器
     */
    public static class Builder {

        private String messageId;
        private int messageType;
        private String token;
        private long timestamp = -1;
        private Body body;

        public Builder identity(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder type(int messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder response(TunnelMessage request) {
            identity(request.getHeader().getMessageId());
            token(request.getHeader().token);
            type(TunnelMessageType.MSG_TYPE_RESPONSE);
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder session(String session) {
            this.token = session;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        /**
         * 构建消息
         *
         * @return 隧道消息
         */
        public TunnelMessage build() {

            if (null == messageId) {
                messageId = UUID.randomUUID().toString();
            }

            if (-1 == timestamp) {
                timestamp = System.currentTimeMillis();
            }

            if (-1 == messageType) {
                throw new IllegalArgumentException("MESSAGE-TYPE is required!");
            }

            if (null == body) {
                body = Body.empty;
            }

            return new TunnelMessage(new Header(messageId, messageType, timestamp, token), body);
        }

    }

    /**
     * 消息头
     */
    public static class Header {

        @SerializedName("msg_id")
        private final String messageId;

        @SerializedName("msg_type")
        private final int messageType;

        @SerializedName("timestamp")
        private final long timestamp;

        @SerializedName("token")
        private final String token;

        @SerializedName("payload_len")
        private int length = -1;

        public Header(String messageId, int messageType, long timestamp, String token) {
            this.messageId = messageId;
            this.messageType = messageType;
            this.timestamp = timestamp;
            this.token = token;
        }

        public String getMessageId() {
            return messageId;
        }

        public int getMessageType() {
            return messageType;
        }

        public int getLength() {
            if (-1 == length) {
                throw new UnsupportedOperationException();
            }
            return length;
        }

        public boolean hasBody() {
            return getLength() > 0;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getToken() {
            return token;
        }

        public String getSessionId() {
            return token;
        }

        @Override
        public String toString() {
            return String.format("tunnel-message[id=%s;type=%s;length=%s;]", messageId, messageType, length);
        }

    }

}
