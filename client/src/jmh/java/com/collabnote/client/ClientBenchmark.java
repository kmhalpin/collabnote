package com.collabnote.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;

import com.collabnote.crdt.CRDTItem;

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
        state.app.getViewModel().getCurrentReplica().localInsert(state.data.get(state.i).index,
                state.data.get(state.i).character);
        state.i += 1;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = N)
    @Warmup(iterations = warmUpN)
    public void prependCharacters(InsertState state) throws BadLocationException {
        state.app.getViewModel().getCurrentReplica().localInsert(0, state.data.get(state.i).character);
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
        state.app.getViewModel().getCurrentReplica().localInsert(state.data.get(state.i).index,
                state.data.get(state.i).character);
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
            state.app.getViewModel().getCurrentReplica().localDelete(input.index, 1);
        } else {
            state.app.getViewModel().getCurrentReplica().localInsert(input.index, input.character);
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
        ArrayList<CRDTItem> versionVector;
        FileWriter fWriter;

        @TearDown
        public void tearDown() {
            try {
                // demo
                Thread.sleep(3000);
                // Thread.sleep(35000);
                doTearDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                fWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // try {
            // OpsResultRender.render(this.app.getViewModel().getCurrentReplica(), new File(
            // "/Users/kemasmhuseinalviansyah/Documents/Code/Java/collabnote/client/build/results/jmh/render.png"));
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
        }

        @TearDown(Level.Iteration)
        public void doTearDown() {
            try {
                fWriter.write(versionVector.size() + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public abstract void doSetup();

        @Setup
        public void setup() {
            try {
                this.fWriter = new FileWriter(
                        "/Users/kemasmhuseinalviansyah/Documents/Code/Java/collabnote/client/build/results/jmh/ops.csv");
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.app = new App(false);
            this.i = 0;
            this.data = new ArrayList<>();
            trackVersionVector();

            doSetup();
            System.out.println("data:\n" + Arrays.toString(data.toArray()));
        }

        public void trackVersionVector() {
            this.versionVector = new ArrayList<>();
            app.getViewModel().getCurrentReplica().getVersionVector().versionVector
                    .put(app.getViewModel().agent, this.versionVector);
        }
    }

}
