package com.collabnote.client;

import java.util.ArrayList;
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
}

public class ClientBenchmark {
    static final int N = 500;
    static final int warmUpN = 5;

    /**
     * Insert Benchmark State
     */
    @State(Scope.Benchmark)
    public static class InsertState extends DefaultState {
        @Override
        public void doSetup() {
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
        state.app.insertCRDT(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void prependCharacters(InsertState state) throws BadLocationException {
        state.app.insertCRDT(0, state.data.get(state.i).character);
        state.i += 1;
    }

    /**
     * Insert Delete Benchmark State
     */
    @State(Scope.Benchmark)
    public static class InsertDeleteState extends DefaultState {
        @Override
        public void doSetup() {
            String[] dataArray = this.rawData.split("");
            for (int i = 0; i < warmUpN; i++) {
                this.data.add(new InputData(i, false, dataArray[i]));
            }
            for (int i = 0; i < warmUpN; i++) {
                this.data.add(new InputData(i, true, null));
            }
            int inserted = 0;
            for (int i = 0; i < N * 2; i++) {
                if(Math.floor(i / 100) % 2 == 0){
                    this.data.add(new InputData(i, false, dataArray[inserted]));
                    inserted++;
                } else {
                    this.data.add(new InputData(i, true, null));
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N * 2)
    @Warmup(iterations = warmUpN * 2)
    public void insertDeleteCharacters(InsertDeleteState state) throws BadLocationException {
        InputData data = state.data.get(state.i);
        if (data.isRemove) {
            state.app.deleteCRDT(0);
        } else {
            state.app.insertCRDT(0, data.character);
        }
        state.i += 1;
    }

    /**
     * Random Position Insert Benchmark State
     */
    @State(Scope.Benchmark)
    public static class RandomDataState extends DefaultState {
        @Override
        public void doSetup() {
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
    public void randomPositionInsertCharacters(RandomDataState state) throws BadLocationException {
        state.app.insertCRDT(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

    /**
     * Random Position Insert and Delete Benchmark State
     */
    @State(Scope.Benchmark)
    public static class RandomInsertDeleteDataState extends DefaultState {
        @Override
        public void doSetup() {
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
    public void randomPositionInsertDeleteCharacters(RandomInsertDeleteDataState state) throws BadLocationException {
        InputData input = state.data.get(state.i);
        if (input.isRemove) {
            state.app.deleteCRDT(input.index);
        } else {
            state.app.insertCRDT(input.index, input.character);
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

            this.rawData = RandomStringUtils.randomAscii(N + warmUpN);
            doSetup();
            System.out.println("data:\n" + rawData);
        }
    }

}
