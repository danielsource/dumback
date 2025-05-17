#!/bin/sh

set -e

javac -Xlint -d out -sourcepath src src/cli/App.java

#jar cfm dumback.jar src/MANIFEST.MF -C out .
