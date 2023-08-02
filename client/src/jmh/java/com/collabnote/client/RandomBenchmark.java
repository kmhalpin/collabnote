package com.collabnote.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.collabnote.client.ClientBenchmark.DefaultState;
import com.google.gson.GsonBuilder;

class RandomData {
    public final List<InputData> input;

    public RandomData(List<InputData> input) {
        this.input = input;
    }
}

public class RandomBenchmark {
    /**
     * Random Benchmark
     * Perform insertions and deletions based on generated random dataset
     */
    @State(Scope.Benchmark)
    public static class RandomState extends DefaultState {
        public static final int warmUpN = 2000;

        @Override
        public void doSetup() {
            this.rawData = RandomStringUtils.randomAscii(warmUpN / 2);
            String[] dataArray = this.rawData.split("");
            int dataDelete = warmUpN - dataArray.length;

            for (int i = 0; i < dataArray.length; i++) {
                this.data.add(new InputData(i, false, dataArray[i]));
            }

            for (int i = dataDelete - 1; i >= 0; i--) {
                this.data.add(new InputData(i, true, null));
            }

            Reader jsonReader = new InputStreamReader(
                    ClassLoader.getSystemClassLoader().getResourceAsStream("data/random-data.json"));
            RandomData randomData = new GsonBuilder()
                    .create()
                    .fromJson(jsonReader, RandomData.class);

            this.data.addAll(randomData.input);

            this.app.getViewModel().shareDocument("127.0.0.1");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = 11000)
    @Warmup(iterations = RandomState.warmUpN)
    public void randomCharacters(RandomState state, Blackhole blackhole) throws BadLocationException {
        InputData data = state.data.get(state.i);
        if (data.isRemove) {
            state.app.getViewModel().getCurrentReplica().localDelete(data.index, 1);
        } else {
            state.app.getViewModel().getCurrentReplica().localInsert(data.index, data.character);
        }
        state.i += 1;
        blackhole.consume(data);
        if (state.i == RandomState.warmUpN) {
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
