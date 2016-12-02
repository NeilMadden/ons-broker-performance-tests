# Message Broker Performance Tests

This is some *very* basic performance test rig for comparing the performance of
message brokers. The test setup is very basic, just shovelling small messages over
the loopback interface to a local consumer. It is therefore just a test of absolute
best-case raw throughput and latency figures for one particular use-case.

## Running the tests

```bash
bash src/main/resources/run-tests.sh
```

## Results

See raw data in the `./data` directory. Headline figures:

### Producers
```
Kafka:  Sent 100000 messages of 544 bytes in 7678ms. Total MiB sent: 51.88
Rabbit: Sent 100000 messages of 544 bytes in 9755ms. Total MiB sent: 51.88
```

![](data/Producer-100000-99.99.png)

### Consumers

```
Kafka:  Consumed 100000 messages in 7300ms
Rabbit: Consumed 100000 messages in 9475ms
```

![](data/Consumer-100000.png)

### Summary

Kafka is generally faster than Rabbit for producers and consumers in these single-broker tests - about 2 seconds
faster (~20%) in the end-to-end timing. This is a significant performance win for our use-cases. For producers,
it provides per-message latencies that are consistently lower across the entire percentile range. For consumers, Rabbit
has a slightly better median latency, but this is completely dominated by significantly worse latencies from around the
95th percentile.

Given that Kafka is designed for very high scalability, it is likely that these numbers will widen even favour in Kafka's
favour in larger scale tests, but these remain to be performed.