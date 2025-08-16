#!/usr/bin/env bash

docker run -it \
  --network astronaut_spark-network \
  -v "$(pwd)/src/main/resources/log4j2.properties:/opt/bitnami/spark/conf/log4j2.properties" \
  -v "$(pwd)/target/scala-2.13/astronaut.jar:/opt/shared/astronaut.jar" \
  -v "$(pwd)/out:/opt/shared" \
  -p 4040:4040 \
  bitnami/spark:latest \
  spark-submit \
  --class wikalloy.WikalloyRunner \
  --master spark://spark-master:7077 \
  "/opt/shared/astronaut.jar" \
  "/opt/shared/specifications/$1"
