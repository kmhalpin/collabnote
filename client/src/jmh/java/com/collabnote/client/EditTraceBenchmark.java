package com.collabnote.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

class EditTraceTransactionPatchesDeserializer implements JsonDeserializer<InputData> {
    @Override
    public InputData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        return new InputData(array.get(0).getAsInt(),
                array.get(1).getAsInt() == 1,
                array.get(2).getAsString());
    }
}

class EditTraceTransactions {
    public final String time;
    public final List<InputData> patches;

    public EditTraceTransactions(String time, List<InputData> patches) {
        this.time = time;
        this.patches = patches;
    }
}

class EditTraceData {
    public final String startContent;
    public final String endContent;
    public final List<EditTraceTransactions> txns;

    public EditTraceData(String startContent, String endContent, List<EditTraceTransactions> txns) {
        this.startContent = startContent;
        this.endContent = endContent;
        this.txns = txns;
    }
}

public class EditTraceBenchmark {
    /**
     * Edit Trace Benchmark
     * Perform insertions and deletions based on edit trace dataset
     */
    @State(Scope.Benchmark)
    public static class EditTraceState extends DefaultState {
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
                    ClassLoader.getSystemClassLoader().getResourceAsStream("data/automerge-paper.json"));
            EditTraceData editTraceData = new GsonBuilder()
                    .registerTypeAdapter(InputData.class,
                            new EditTraceTransactionPatchesDeserializer())
                    .create()
                    .fromJson(jsonReader, EditTraceData.class);

            for (EditTraceTransactions data : editTraceData.txns) {
                this.data.addAll(data.patches);
            }

            this.app.getViewModel().shareDocument("127.0.0.1");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Measurement(batchSize = 1, iterations = 259778)
    @Warmup(iterations = EditTraceState.warmUpN)
    public void editTraceCharacters(EditTraceState state, Blackhole blackhole) throws BadLocationException {
        InputData data = state.data.get(state.i);
        if (data.isRemove) {
            state.app.getViewModel().getCurrentReplica().localDelete(data.index, 1);
        } else {
            state.app.getViewModel().getCurrentReplica().localInsert(data.index, data.character);
        }
        state.i += 1;
        blackhole.consume(data);
        if (state.i == EditTraceState.warmUpN) {
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
