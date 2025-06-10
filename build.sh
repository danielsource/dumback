#!/bin/sh

set -e

mkdir -p out

javac -Xlint -d out -classpath out -sourcepath src src/cli/App.java src/gui/App.java

cp src/icon.png src/*.properties out

jar cfm dumback.jar src/MANIFEST.MF -C out .
