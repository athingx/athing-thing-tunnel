#!/bin/bash

projects[i++]="com.github.athingx.athing.aliyun.tunnel:tunnel-boot"
projects[i++]="com.github.athingx.athing.aliyun.tunnel:tunnel-agent"

# maven package boot projects
mvn clean package \
  -f ../pom.xml \
  -pl "$(printf "%s," "${projects[@]}")" -am \
  '-Dmaven.test.skip=true' ||
  exit $?

# package agent
TUNNEL_AGENT_TARGET=../tunnel-agent/target
TUNNEL_AGENT_DIST=${TUNNEL_AGENT_TARGET}/dist
mkdir -p ${TUNNEL_AGENT_DIST} &&
  mkdir -p ${TUNNEL_AGENT_DIST}/cfg &&
  mkdir -p ${TUNNEL_AGENT_DIST}/logs &&
  cp ${TUNNEL_AGENT_TARGET}/tunnel-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar ${TUNNEL_AGENT_DIST}/tunnel-agent.jar &&
  cp ${TUNNEL_AGENT_TARGET}/../config/* ${TUNNEL_AGENT_DIST}/cfg/ ||
  exit $?
