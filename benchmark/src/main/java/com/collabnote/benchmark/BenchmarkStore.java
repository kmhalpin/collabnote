package com.collabnote.benchmark;

public class BenchmarkStore {
    public BenchmarkStore() {
    }

    public static void benchmarkTime(String benchmark, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        runnable.run();
        long result = System.currentTimeMillis() - startTime;

    }
}
