package com.collabnote.benchmark;

import java.util.HashMap;

public class BenchmarkStore {
    private HashMap<String, HashMap<String, String>> store;

    public BenchmarkStore() {
    }

    public void setBenchmarkResult(String benchmark, String id, String result) {
        HashMap<String, String> bench = store.get(benchmark);
        if (bench == null) {
            bench = new HashMap<>();
            store.put(benchmark, bench);
        }
        bench.put(id, result);
    }

    public void benchmarkTime(String benchmark, String id, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        runnable.run();
        long result = System.currentTimeMillis() - startTime;
        setBenchmarkResult(benchmark, id, result + " (ms)");
    }
}
