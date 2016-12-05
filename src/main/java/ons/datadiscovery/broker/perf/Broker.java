package ons.datadiscovery.broker.perf;

import org.HdrHistogram.Recorder;

import java.util.concurrent.Callable;

/**
 * Created by neil on 02/12/2016.
 */
public interface Broker extends AutoCloseable {

    void sendMessage(String message) throws Exception;

    Callable<Long> startConsumer(final int expected, final Recorder recorder);
}
