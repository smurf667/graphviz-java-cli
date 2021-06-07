@echo off
java -Xss16m -DTOTAL_MEMORY=33554432 -jar %~dp0\lib\graphviz-java-cli.jar %*
