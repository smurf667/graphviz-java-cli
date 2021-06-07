#!/bin/sh
java -Xss16m -DTOTAL_MEMORY=33554432 -jar `dirname $0`/lib/graphviz-java-cli.jar $*
