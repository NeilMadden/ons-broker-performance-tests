#!/usr/bin/env bash

set -m # Enable job control

mvn clean compile

# Start consumer
echo "Kafka 100,000 messages"
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Consumer -Dexec.args="100000 ons.datadiscovery.broker.perf.KafkaBroker" &
sleep 0.1
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Producer -Dexec.args="100000 ons.datadiscovery.broker.perf.KafkaBroker"

echo "Waiting for consumer to finish"
fg

echo "RabbitMQ 100,000 messages"
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Consumer -Dexec.args="100000 ons.datadiscovery.broker.perf.RabbitBroker" &
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Producer -Dexec.args="100000 ons.datadiscovery.broker.perf.RabbitBroker"

echo "Waiting for consumer to finish"
fg