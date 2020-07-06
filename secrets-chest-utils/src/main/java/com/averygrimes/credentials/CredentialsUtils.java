package com.averygrimes.credentials;

import com.averygrimes.credentials.pojo.RetrieveDataResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

public class CredentialsUtils {

    public <T> CompletableFuture<T> timeoutRetrieveInvocationResponse(CompletableFuture<T> completableFuture,
                                                                                long timeOutDuration, TimeUnit timeUnit){
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        Callable<RetrieveDataResponse> callable = () -> {
            completableFuture.completeExceptionally(new TimeoutException());
            return null;
        };
        scheduledThreadPoolExecutor.schedule(callable, timeOutDuration, timeUnit);
        scheduledThreadPoolExecutor.shutdown();
        return completableFuture;
    }
}
