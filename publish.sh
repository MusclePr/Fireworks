#!/bin/bash
DST_DIR="/opt/docker/downloads/minecraft/plugins/26.3/"
mkdir -p "$DST_DIR"
cp -auv ./build/libs/Fireworks.jar "$DST_DIR"
