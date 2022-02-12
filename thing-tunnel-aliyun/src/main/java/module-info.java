module io.github.athingx.athing.thing.tunnel.aliyun {
    requires io.github.athingx.athing.standard.thing;
    requires io.github.athingx.athing.aliyun.thing.runtime;
    requires io.github.athingx.athing.thing.tunnel;
    requires io.github.athingx.athing.thing.tunnel.aliyun.core;
    requires com.google.gson;
    requires org.slf4j;
    requires static metainf.services;

    exports io.github.athingx.athing.thing.tunnel.aliyun;
    opens io.github.athingx.athing.thing.tunnel.aliyun.domain to com.google.gson;

    provides io.github.athingx.athing.standard.thing.boot.ThingBoot with io.github.athingx.athing.thing.tunnel.aliyun.TunnelThingBoot;

}