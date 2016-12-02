#!/usr/bin/env bash

mvn clean compile

# Start consumer
echo "Kafka 100,000 messages"
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Consumer -Dexec.args="100000 ons.datadiscovery.broker.perf.KafkaBroker" -l /tmp/consumer-kafka.log &
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Producer -Dexec.args="100000 ons.datadiscovery.broker.perf.KafkaBroker" -l /tmp/producer-kafka.log

sleep 5

echo "RabbitMQ 100,000 messages"
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Consumer -Dexec.args="100000 ons.datadiscovery.broker.perf.RabbitBroker" -l /tmp/consumer-rabbit.log &
mvn exec:java -Dexec.mainClass=ons.datadiscovery.broker.perf.Producer -Dexec.args="100000 ons.datadiscovery.broker.perf.RabbitBroker" -l /tmp/producer-rabbit.log

sleep 5