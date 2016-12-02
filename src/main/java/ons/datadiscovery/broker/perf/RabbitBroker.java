package ons.datadiscovery.broker.perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.HdrHistogram.Recorder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by neil on 02/12/2016.
 */
public class RabbitBroker implements Broker {
    private static final ObjectMapper objectMapper = new ObjectMapper();


    private final Connection connection;
    private final Channel channel;

    public RabbitBroker() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(String message) throws Exception {
        channel.basicPublish("test", "test-key", null, message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Callable<Long> startConsumer(final int expected, final Recorder recorder) throws Exception {
        return () -> {
            final CountDownLatch latch = new CountDownLatch(expected);
            final RabbitConsumer consumer = new RabbitConsumer(channel, latch, recorder);
            channel.basicConsume("test", true, consumer);
            latch.await();
            return consumer.getFirstMessageReceivedTime();
        };
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }

    private static class RabbitConsumer extends DefaultConsumer {
        private final CountDownLatch latch;
        private final Recorder recorder;

        private final AtomicLong firstMessageReceived = new AtomicLong(0L);

        RabbitConsumer(Channel channel, CountDownLatch latch, Recorder recorder) {
            super(channel);
            this.latch = latch;
            this.recorder = recorder;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            final long processTime = System.currentTimeMillis();
            firstMessageReceived.compareAndSet(0L, processTime);
            final long sendTime = objectMapper.readTree(body).get("time").asLong();
            recorder.recordValue(processTime - sendTime);
            latch.countDown();
        }

        long getFirstMessageReceivedTime() {
            return firstMessageReceived.get();
        }
    }
}
