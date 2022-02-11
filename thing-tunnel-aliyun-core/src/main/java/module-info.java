module io.github.athingx.athing.thing.tunnel.aliyun.core {
    requires io.netty.all;
    requires org.slf4j;
    requires com.google.gson;
    exports io.github.athingx.athing.thing.tunnel.aliyun.core to io.github.athingx.athing.thing.tunnel.aliyun;
    opens io.github.athingx.athing.thing.tunnel.aliyun.core.protocol to com.google.gson;
}