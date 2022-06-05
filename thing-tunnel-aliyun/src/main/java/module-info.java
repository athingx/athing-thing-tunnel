module io.github.athingx.athing.thing.tunnel.aliyun {
    requires transitive io.github.athingx.athing.thing.tunnel;
    requires transitive io.github.athingx.athing.thing.tunnel.aliyun.core;
    requires io.github.athingx.athing.standard.thing;
    requires io.github.athingx.athing.aliyun.thing.runtime;

    requires com.google.gson;
    requires org.slf4j;

    exports io.github.athingx.athing.thing.tunnel.aliyun;

    opens io.github.athingx.athing.thing.tunnel.aliyun.domain to com.google.gson;

}