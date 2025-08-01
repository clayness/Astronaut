#!/usr/bin/env bash

docker run -it \
  --network astronaut-network \
  -v "$(pwd)/Astronaut/target/scala-2.13:/opt/spark-apps" \
  -v "$(pwd)/Astronaut/models:/opt/models" \
  bitnami/spark:latest \
  spark-submit \
  --class edu.virginia.cs.Main \
  --master spark://astronaut-spark:7077 \
  /opt/spark-apps/astronaut.jar \
  mysql "/opt/models/$1"
