#!/usr/bin/env bash

javac -d mods --source-path "./src/*" \
$(find . -name "*.java")

sleep 2

cd mods/

jar cvmf ../manifest.txt jollama.jar .

