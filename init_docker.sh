#!/usr/bin/env bash

NETWORK_NAME=astronaut-network

MYSQL_NAME=astronaut-mysql
SPARK_NAME=astronaut-spark
SNODE_NAME=astronaut-worker

if docker network inspect $NETWORK_NAME >/dev/null 2>&1; then
  echo "Astronaut network already exists, skipping..."
else
  docker network create $NETWORK_NAME
fi

if docker ps | grep -q "$MYSQL_NAME"; then
  echo "Astronaut database container up, skipping..."
else
  echo "Enter a password for the MySQL database: "
  read -s mysql_password

  # start up the mysql container
  docker run -d --name $MYSQL_NAME \
    --network $NETWORK_NAME \
    -e ALLOW_EMPTY_PASSWORD=yes \
    -e MYSQL_ROOT_PASSWORD=$mysql_password \
    -v data:/bitnami/mysql/data \
    bitnami/mysql:latest
fi
  
if docker ps | grep -q "$SPARK_NAME"; then
  echo "Astronaut Spark cluster up, skipping..."
else
  # start the Spark network
  docker run -d --name $SPARK_NAME \
    --network $NETWORK_NAME \
    -e SPARK_MODE=master \
    -p 8080:8080 \
    -p 7077:7077 \
    -v "$(pwd)/Astronaut/models:/opt/models" \
    bitnami/spark:latest
fi

if docker ps | grep -q "$SNODE_NAME"; then
  echo "Astronaut worker up, skipping..."
else
  # start the Spark worker
  docker run -d --name $SNODE_NAME \
    --network $NETWORK_NAME \
    -e SPARK_MODE=worker \
    -e "SPARK_MASTER_URL=spark://$SPARK_NAME:7077" \
    -p 8081:8081 \
    -v "$(pwd)/Astronaut/models:/opt/models" \
    bitnami/spark:latest
fi

