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
import org.openjdk.jmh.infra.Blackhole;

import com.collabnote.client.ClientBenchmark.DefaultState;

import org.openjdk.jmh.annotations.State;

public class InsertDeleteBenchmark {
    /**
     * Insert Delete Benchmark
     * N = 11000
     * Perform 1000 insertions followed by 1000 deletions
     */
    @State(Scope.Benchmark)
    public static class InsertDeleteState extends DefaultState {
        static final int N = 11000;
        static final int warmUpN = 2000;
        static final int switchN = 1000;
        static final int dataN = 7000; // data used both in warm up and benchmark when insert switch

        @Override
        public void doSetup() {
            this.rawData = RandomStringUtils.randomAscii(dataN);
            String[] dataArray = this.rawData.split("");

            // prepare warmup data
            int inserted = 0;
            int location = 0;
            for (int i = 0; i < warmUpN; i++) {
                if (Math.floor(i / switchN) % 2 == 0) {
                    this.data.add(new InputData(location, false, dataArray[inserted]));
                    location++;
                    inserted++;
                } else {
                    this.data.add(new InputData(0, true, null));
                    if (location != 0)
                        location = 0;
                }
            }

            // prepare benchmark data
            inserted = 0;
            location = 0;
            for (int i = 0; i < N; i++) {
                if (Math.floor(i / switchN) % 2 == 0) {
                    this.data.add(new InputData(location, false, dataArray[inserted]));
                    location++;
                    inserted++;
                } else {
                    this.data.add(new InputData(0, true, null));
                    if (location != 0)
                        location = 0;
                }
            }

            this.app.getViewModel().shareDocument("127.0.0.1");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = InsertDeleteState.N)
    @Warmup(iterations = InsertDeleteState.warmUpN)
    public void insertDeleteCharacters(InsertDeleteState state, Blackhole blackhole) throws BadLocationException {
        InputData data = state.data.get(state.i);
        if (data.isRemove) {
            state.app.getViewModel().getCurrentReplica().localDelete(data.index, 1);
        } else {
            state.app.getViewModel().getCurrentReplica().localInsert(data.index, data.character);
        }
        state.i += 1;
        blackhole.consume(data);
        if (state.i == InsertDeleteState.warmUpN) {
            state.app.getViewModel().initDocument();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            state.trackVersionVector();
            state.app.getViewModel().shareDocument("127.0.0.1");
            System.out.println("Warmup finished");
        }
    }
}
