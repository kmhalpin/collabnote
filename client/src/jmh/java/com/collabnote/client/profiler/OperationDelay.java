package com.collabnote.client.profiler;

import java.util.ArrayList;
import java.util.Collection;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.ScalarResult;

public class OperationDelay implements InternalProfiler {
    @Override
    public String getDescription() {
        return "Delay";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        };
    }

    @Override
    public Collection<ScalarResult> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams,
            IterationResult result) {
                Collection<ScalarResult> results = new ArrayList<>();
                results.add(new ScalarResult("-", 0, "ms", AggregationPolicy.MAX));
                return results;
    }

}
