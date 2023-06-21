package com.collabnote.client;

import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Warmup;

import com.collabnote.client.ClientBenchmark.DefaultState;

import org.openjdk.jmh.annotations.State;

public class InsertDeleteBenchmark {
    /**
     * Insert Delete Benchmark
     * N = 1000
     * Perform 100 insertions followed by 100 deletions
     */
    @State(Scope.Benchmark)
    public static class InsertDeleteState extends DefaultState {
        static final int N = 6000;
        static final int warmUpN = 1000;
        static final int dataN = 3500; // = (N + warmUpN) / 2

        @Override
        public void doSetup() {
            this.app.getViewModel().shareDocument("127.0.0.1");
            this.rawData = RandomStringUtils.randomAscii(dataN);
            String[] dataArray = this.rawData.split("");

            // prepare warmup data
            int inserted = 0;
            int location = -1;
            for (int i = 0; i < warmUpN; i++) {
                if (Math.floor(i / 100) % 2 == 0) {
                    location++;
                    this.data.add(new InputData(location, false, dataArray[inserted]));
                    inserted++;
                } else {
                    this.data.add(new InputData(99 - location, true, null));
                    location--;
                }
            }

            // prepare benchmark data
            inserted = 0;
            location = -1;
            for (int i = 0; i < N; i++) {
                if (Math.floor(i / 100) % 2 == 0) {
                    location++;
                    this.data.add(new InputData(location, false, dataArray[inserted]));
                    inserted++;
                } else {
                    this.data.add(new InputData(99 - location, true, null));
                    location--;
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = InsertDeleteState.N)
    @Warmup(iterations = InsertDeleteState.warmUpN)
    public void insertDeleteCharacters(InsertDeleteState state) throws BadLocationException {
        InputData data = state.data.get(state.i);
        if (data.isRemove) {
            state.app.getViewModel().getCurrentReplica().localDelete(data.index, 1);
        } else {
            state.app.getViewModel().getCurrentReplica().localInsert(data.index, data.character);
        }
        state.i += 1;
    }
}
