#!/bin/bash

projects[i++]="io.github.athingx.athing.thing.tunnel:thing-tunnel"
projects[i++]="io.github.athingx.athing.thing.tunnel:thing-tunnel-aliyun"

# maven package boot projects
mvn clean package \
  -f ../pom.xml \
  -pl "$(printf "%s," "${projects[@]}")" -am \
  '-Dmaven.test.skip=true'
