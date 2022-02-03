#!/bin/bash

projects[i++]="io.github.athingx.athing.aliyun.tunnel:tunnel-thing"
projects[i++]="io.github.athingx.athing.aliyun.tunnel:tunnel-thing-impl"

mvn clean install \
  -f ../pom.xml \
  -pl "$(printf "%s," "${projects[@]}")" -am \
  '-Dmaven.test.skip=true'
