#!/usr/bin/env bash

docker run -it \
  --network astronaut_spark-network \
  -v "$(pwd)/Astronaut/target/scala-2.13:/opt/spark-apps" \
  -v "$(pwd)/Astronaut/models:/opt/models" \
  -v "$(pwd)/Astronaut/out/models:/opt/solutions" \
  -v "$(pwd)/Astronaut/out:/opt/output" \
  -v "$(pwd)/Astronaut/src/main/resources/log4j2.properties:/opt/bitnami/spark/conf/log4j2.properties" \
  -p 4040:4040 \
  bitnami/spark:latest \
  spark-submit \
  --class wikalloy.WikalloyRunner \
  --master spark://spark-master:7077 \
  /opt/spark-apps/astronaut.jar \
  "/opt/models/$1"
