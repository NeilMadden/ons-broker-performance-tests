package ons.datadiscovery.broker.perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.HdrHistogram.Recorder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by neil on 02/12/2016.
 */
public class RabbitBroker implements Broker {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConnectionFactory connectionFactory;
    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>(null);

    private final Channel sendChannel;

    public RabbitBroker() throws Exception {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        sendChannel = getChannel();
    }

    private Channel getChannel() throws Exception {
        final Connection connection = connectionFactory.newConnection();
        return connection.createChannel();
    }

    @Override
    public void sendMessage(String message) throws Exception {
        sendChannel.basicPublish("test", "test-key", null, message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Callable<Long> startConsumer(final int expected, final Recorder recorder) {
        latch.compareAndSet(null, new CountDownLatch(expected));
        return () -> {
            final Channel channel = getChannel();
            final RabbitConsumer consumer = new RabbitConsumer(channel, recorder);
            channel.basicConsume("test", true, consumer);
            latch.get().await();
            return consumer.getFirstMessageReceivedTime();
        };
    }

    @Override
    public void close() throws Exception {
        sendChannel.close();
        sendChannel.getConnection().close();
    }

    private class RabbitConsumer extends DefaultConsumer {
        private final Recorder recorder;

        private final AtomicLong firstMessageReceived = new AtomicLong(0L);

        RabbitConsumer(Channel channel, Recorder recorder) {
            super(channel);
            this.recorder = recorder;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            final long processTime = System.currentTimeMillis();
            firstMessageReceived.compareAndSet(0L, processTime);
            latch.get().countDown();
            final long sendTime = objectMapper.readTree(body).get("time").asLong();
            recorder.recordValue(processTime - sendTime);
        }

        long getFirstMessageReceivedTime() {
            return firstMessageReceived.get();
        }
    }
}
