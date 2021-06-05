#!/bin/bash

projects[i++]="com.github.athingx.athing.aliyun.tunnel:tunnel-component"

mvn clean install \
  -f ../pom.xml \
  -pl "$(printf "%s," "${projects[@]}")" -am \
  '-Dmaven.test.skip=true'
