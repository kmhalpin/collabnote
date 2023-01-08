package com.collabnote.client.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.ScalarResult;

public class MemoryUsageProfiler implements InternalProfiler {

    @Override
    public String getDescription() {
        return "Memory usage profiler";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
    }

    @Override
    public Collection<ScalarResult> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams,
            IterationResult result) {
        Collection<ScalarResult> results = new ArrayList<>();
        results.add(new ScalarResult("memory.usage", getReallyUsedMemory(), "B", AggregationPolicy.MAX));

        return results;
    }

    long getCurrentlyUsedMemory() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() +
                ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
    }

    long getGcCount() {
        long sum = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = b.getCollectionCount();
            if (count != -1) {
                sum += count;
            }
        }
        return sum;
    }

    long getReallyUsedMemory() {
        long before = getGcCount();
        System.gc();
        while (getGcCount() == before)
            ;
        return getCurrentlyUsedMemory();
    }

    long getSettledUsedMemory() {
        long m;
        long m2 = getReallyUsedMemory();
        do {
            try {
                Thread.sleep(567);
            } catch (InterruptedException e) {
            }
            m = m2;
            m2 = getReallyUsedMemory();
        } while (m2 < getReallyUsedMemory());
        return m;
    }
}
