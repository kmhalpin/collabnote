package com.collabnote.client;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.annotations.State;

import com.collabnote.crdt.CRDTItem;

public class ClientBenchmark {

    @State(Scope.Benchmark)
    public static class MyState {
        private App app = new App(false);
        public int i = 0;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = 100)
    @Warmup(iterations = 0)
    public List<CRDTItem> insertOperations(MyState state) throws BadLocationException {
        state.app.insertCRDT(state.i, "a");
        state.i += 1;

        return state.app.getCRDT().returnCopy();
    }
}
