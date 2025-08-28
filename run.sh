#!/bin/bash

docker compose down && ./gradlew jar && cp -auv build/libs/Fireworks.jar ./plugins/

trap 'docker compose down' INT TERM
docker compose up && docker compose down
