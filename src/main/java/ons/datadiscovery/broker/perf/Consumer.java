package ons.datadiscovery.broker.perf;

import org.HdrHistogram.Recorder;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

/**
 * Created by neil on 02/12/2016.
 */
public class Consumer {

    public static void main(String...args) throws Exception {
        final int numExpected = Integer.parseInt(args[0]);
        try (final Broker broker = Class.forName(args[1]).asSubclass(Broker.class).newInstance()) {
            final Recorder recorder = new Recorder(1L, 100_000L, 3);

            final int numTasks = Runtime.getRuntime().availableProcessors();
            final ExecutorService executorService = Executors.newFixedThreadPool(numTasks);
            final long firstMessageReceived = executorService.invokeAll(
                    generate(() -> broker.startConsumer(numExpected, recorder)).limit(numTasks).collect(toList()))
                    .stream().mapToLong(Utils::awaitLongFuture).min().orElse(Long.MIN_VALUE);

            final long end = System.currentTimeMillis();

            try (PrintWriter log = new PrintWriter(new FileWriter("/tmp/consumer-" + broker.getClass().getSimpleName() + ".log"))) {
                log.printf("Consumed %d messages in %dms%n", numExpected, end - firstMessageReceived);
            }

            executorService.shutdown();

//            System.out.println("Consume time histogram (milliseconds):");
//            recorder.getIntervalHistogram().outputPercentileDistribution(System.out, 1d);

            try (PrintStream out = new PrintStream(new FileOutputStream("/tmp/consumer_" + broker.getClass().getSimpleName() + ".hgrm"))) {
                recorder.getIntervalHistogram().outputPercentileDistribution(out, 1d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
