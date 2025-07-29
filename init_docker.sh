#!/usr/bin/env bash

echo "Enter a password for the MySQL database: "
read -s mysql_password

# start the Spark network
docker network create astronaut-network

# start up the mysql container
docker run -d --name astronaut-mysql \
  --network astronaut-network \
  -e ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_ROOT_PASSWORD=$mysql_password \
  -v data:/bitnami/mysql/data \
  bitnami/mysql:latest

# start the Spark network
docker run -d --name astronaut-spark \
  --network astronaut-network \
  -e SPARK_MODE=master \
  -p 8080:8080 \
  -p 7077:7077 \
  bitnami/spark:latest

# start the Spark worker
docker run -d --name astronaut-worker \
  --network astronaut-network \
  -e SPARK_MODE=worker \
  -e SPARK_MASTER_URL=spark://astronaut-spark:7077 \
  -p 8081:8081 \
  bitnami/spark:latest

