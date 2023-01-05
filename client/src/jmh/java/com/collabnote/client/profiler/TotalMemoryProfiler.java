package com.collabnote.client.profiler;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.ScalarResult;

public class TotalMemoryProfiler implements InternalProfiler {

    @Override
    public String getDescription() {
        return "Total memory profiler";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
    }

    @Override
    public Collection<ScalarResult> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams,
            IterationResult result) {
        Collection<ScalarResult> results = new ArrayList<>();
        results.add(new ScalarResult("memory.total", getTotalMemory(), "B", AggregationPolicy.MAX));

        return results;
    }

    long getTotalMemory() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() +
                ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
    }
}
