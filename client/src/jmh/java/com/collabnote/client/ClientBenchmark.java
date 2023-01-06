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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

class InputData {
    public int index;
    public String character;

    public InputData(int index, String character) {
        this.index = index;
        this.character = character;
    }
}

public class ClientBenchmark {
    static final int N = 100;
    static final int warmUpN = 5;

    @State(Scope.Benchmark)
    public static class DefaultStateImpl extends DefaultState {
        @Override
        public void doSetup() {
            String[] dataArray = this.rawData.split("");
            for (int i = 0; i < N + warmUpN; i++) {
                this.data.add(new InputData(i, dataArray[i]));
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void appendCharacters(DefaultStateImpl state) throws BadLocationException {
        state.app.insertCRDT(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void prependCharacters(DefaultStateImpl state) throws BadLocationException {
        state.app.insertCRDT(0, state.data.get(state.i).character);
        state.i += 1;
    }

    @State(Scope.Benchmark)
    public static class RandomDataState extends DefaultState {
        @Override
        public void doSetup() {
            String[] dataArray = this.rawData.split("");
            for (int i = 0; i < N + warmUpN; i++) {
                this.data.add(new InputData(ThreadLocalRandom.current().nextInt(0, i + 1), dataArray[i]));
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void randomPositionCharacters(RandomDataState state) throws BadLocationException {
        state.app.insertCRDT(state.data.get(state.i).index, state.data.get(state.i).character);
        state.i += 1;
    }

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
            System.out.println("data:\n" + rawData);
            doSetup();
        }
    }

}
