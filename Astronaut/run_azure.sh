#!/usr/bin/env bash

SPARK_MASTER_NAME="kkmltradeoff-release-spark-master-0"
SPARK_MASTER_URI="spark://$SPARK_MASTER_NAME.kkmltradeoff-release-spark-headless.default.svc.cluster.local:7077"

kubectl exec -it $SPARK_MASTER_NAME -- spark-submit \
  --class wikalloy.WikalloyRunner \
  --master $SPARK_MASTER_URI \
  --conf spark.jars.ivy=/opt/shared/.ivy \
  "/opt/shared/astronaut.jar" \
  "/opt/shared/specifications/$1"