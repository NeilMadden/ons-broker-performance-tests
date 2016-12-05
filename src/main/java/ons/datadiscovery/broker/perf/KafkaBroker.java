package ons.datadiscovery.broker.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.HdrHistogram.Recorder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by neil on 02/12/2016.
 */
public class KafkaBroker implements Broker {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Properties props;
    private final KafkaProducer<String, String> producer;

    private final LongAdder totalReceived = new LongAdder();

    public KafkaBroker() {
        props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("group.id", UUID.randomUUID().toString());
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void sendMessage(String message) throws Exception {
        producer.send(new ProducerRecord<>("test", message));
    }

    @Override
    public Callable<Long> startConsumer(final int expected, final Recorder recorder) {
        return () -> {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singleton("test"));

                long firstMessage = Long.MAX_VALUE;
                while (totalReceived.longValue() < expected) {
                    final ConsumerRecords<String, String> records = consumer.poll(100);
                    if (records.isEmpty()) {
                        continue;
                    }
                    long processTime = System.currentTimeMillis();
                    totalReceived.add(records.count());

//                    System.out.printf("Received %d of %d (%dms)%n", totalReceived.longValue(), expected, processTime - firstMessage);

                    for (ConsumerRecord<String, String> record : records) {
                        if (firstMessage == Long.MAX_VALUE) {
                            firstMessage = processTime;
                        }
                        try {
                            JsonNode json = objectMapper.readTree(record.value());
                            long startTime = json.get("time").asLong();
                            recorder.recordValue(processTime - startTime);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return firstMessage;
            }
        };
    }

    @Override
    public void close() throws Exception {
        producer.close();
    }
}
