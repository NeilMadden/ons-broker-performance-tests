package ons.datadiscovery.broker.perf;

import org.HdrHistogram.Recorder;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by neil on 02/12/2016.
 */
public class Consumer {

    public static void main(String...args) throws Exception {
        final int numExpected = Integer.parseInt(args[0]);
        try (final Broker broker = Class.forName(args[1]).asSubclass(Broker.class).newInstance()) {
            final Recorder recorder = new Recorder(1L, 100_000L, 3);

            final ExecutorService executorService = Executors.newSingleThreadExecutor();

            final long firstMessageReceived = executorService.submit(broker.startConsumer(numExpected, recorder)).get();
            final long end = System.currentTimeMillis();

            executorService.shutdown();

            System.out.printf("Consumed %d messages in %dms%n", numExpected, end - firstMessageReceived);
//            System.out.println("Consume time histogram (milliseconds):");
//            recorder.getIntervalHistogram().outputPercentileDistribution(System.out, 1d);

            try (PrintStream out = new PrintStream(new FileOutputStream("/tmp/consumer_" + broker.getClass().getSimpleName() + ".hgrm"))) {
                recorder.getIntervalHistogram().outputPercentileDistribution(out, 1d);
            }
        }
    }
}
