package com.collabnote.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

class InputData {
    public int index;
    public boolean isRemove;
    public String character;

    public InputData(int index, boolean isRemove, String character) {
        this.index = index;
        this.isRemove = isRemove;
        this.character = character;
    }

    @Override
    public String toString() {
        return "{" + this.index + ", " + (this.isRemove ? "D" : "I") + ", " + this.character + "}";
    }
}

public class ClientBenchmark {
    static final int N = 6000;
    static final int warmUpN = 1000;

    /**
     * Insert Benchmark State
     */
    @State(Scope.Benchmark)
    public static class InsertState extends DefaultState {
        @Override
        public void doSetup() {
            this.rawData = RandomStringUtils.randomAscii(N + warmUpN);
            String[] dataArray = this.rawData.split("");
            for (int i = 0; i < N + warmUpN; i++) {
                this.data.add(new InputData(i, false, dataArray[i]));
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void appendCharacters(InsertState state) throws BadLocationException {
        state.app.getCRDT().localInsert(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void prependCharacters(InsertState state) throws BadLocationException {
        state.app.getCRDT().localInsert(0, state.data.get(state.i).character);
        state.i += 1;
    }

    /**
     * Random Position Insert Benchmark State
     */
    @State(Scope.Benchmark)
    public static class RandomInsertState extends DefaultState {
        @Override
        public void doSetup() {
            this.rawData = RandomStringUtils.randomAscii(N + warmUpN);
            String[] dataArray = this.rawData.split("");
            for (int i = 0; i < N + warmUpN; i++) {
                this.data.add(new InputData(ThreadLocalRandom.current().nextInt(0, i + 1),
                        false,
                        dataArray[i]));
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void randomPositionInsertCharacters(RandomInsertState state) throws BadLocationException {
        state.app.getCRDT().localInsert(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

    /**
     * Random Position Insert and Delete Benchmark State
     */
    @State(Scope.Benchmark)
    public static class RandomInsertDeleteState extends DefaultState {
        @Override
        public void doSetup() {
            this.rawData = RandomStringUtils.randomAscii(N + warmUpN);
            String[] dataArray = this.rawData.split("");
            int inserted = 0;
            for (int i = 0; i < N + warmUpN; i++) {
                if (inserted != 0 && ThreadLocalRandom.current().nextBoolean()) {
                    this.data.add(new InputData(ThreadLocalRandom.current().nextInt(0, inserted + 1),
                            true, null));
                    inserted--;
                } else {
                    this.data.add(new InputData(ThreadLocalRandom.current().nextInt(0, inserted + 1),
                            false,
                            dataArray[i]));
                    inserted++;
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void randomPositionInsertDeleteCharacters(RandomInsertDeleteState state) throws BadLocationException {
        InputData input = state.data.get(state.i);
        if (input.isRemove) {
            state.app.getCRDT().localDelete(input.index, 1);
        } else {
            state.app.getCRDT().localInsert(input.index, input.character);
        }
        state.i += 1;
    }

    /**
     * Abstract Benchmark State
     */
    @State(Scope.Benchmark)
    public static abstract class DefaultState {
        App app;
        List<InputData> data;
        String rawData;
        int i;

        @TearDown
        public void doTearDown() {
            System.out.println("result:\n" + this.app.getCRDT().toString());
        }

        public abstract void doSetup();

        @Setup
        public void setup() {
            this.app = new App(false);
            this.i = 0;
            this.data = new ArrayList<>();

            doSetup();
            System.out.println("data:\n" + Arrays.toString(data.toArray()));
        }
    }

}
