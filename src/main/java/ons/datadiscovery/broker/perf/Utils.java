package ons.datadiscovery.broker.perf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class Utils {

    static void ignoringExceptions(Exceptional task) {
        try {
            task.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static long awaitLongFuture(Future<Long> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return Long.MAX_VALUE;
        }
    }

    interface Exceptional {
        void run() throws Exception;
    }

}
