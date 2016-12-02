package ons.datadiscovery.broker.perf;

import org.HdrHistogram.Recorder;

import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Created by neil on 02/12/2016.
 */
public class Producer {
    private static final String MESSAGE_TEMPLATE = "{\"data\":\"%s\",\"time\":%d}";

    public static void main(String...args) throws Exception {
        final int numMessages = Integer.parseInt(args[0]);
        try (final Broker broker = Class.forName(args[1]).asSubclass(Broker.class).newInstance()) {
            final Recorder recorder = new Recorder(1L, 1000_000_000L, 3);

            final String data = DatatypeConverter.printHexBinary(new byte[256]);

            final long startTime = System.currentTimeMillis();
            for (int i = 0; i < numMessages; ++i) {
                final String message = String.format(Locale.UK, MESSAGE_TEMPLATE, data, System.currentTimeMillis());
                long start = System.nanoTime();
                broker.sendMessage(message);
                long end = System.nanoTime();
                recorder.recordValue(end - start);
            }
            final long endTime = System.currentTimeMillis();

            System.out.printf("Sent %d messages of 544 bytes in %dms%n", numMessages, endTime - startTime);
            System.out.printf("Total MiB sent: %.2f%n", (544 * numMessages) / (1024d * 1024d));

//            System.out.println("Publish time histogram (microseconds):");
//            recorder.getIntervalHistogram().outputPercentileDistribution(System.out, 1000.0d);

            try (PrintStream out = new PrintStream(new FileOutputStream("/tmp/producer_" + broker.getClass().getSimpleName() + ".hgrm"))) {
                recorder.getIntervalHistogram().outputPercentileDistribution(out, 1000.0d);
            }
        }
    }
}
