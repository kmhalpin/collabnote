// package com.collabnote.client;

// import java.util.List;

// import javax.swing.text.BadLocationException;

// import org.openjdk.jmh.annotations.Benchmark;
// import org.openjdk.jmh.annotations.BenchmarkMode;
// import org.openjdk.jmh.annotations.Level;
// import org.openjdk.jmh.annotations.Mode;
// import org.openjdk.jmh.annotations.Param;
// import org.openjdk.jmh.annotations.Scope;
// import org.openjdk.jmh.annotations.Setup;
// import org.openjdk.jmh.annotations.Warmup;
// import org.openjdk.jmh.infra.Blackhole;

// import com.collabnote.crdt.CRDTItem;
// import java.util.concurrent.TimeUnit;

// import org.openjdk.jmh.annotations.OutputTimeUnit;

// import org.openjdk.jmh.annotations.State;

// @State(Scope.Benchmark)
// public class TimeBenchmark {
//     @Param({ "10", "100", "1000" })
//     private int operations;

//     private App app;

//     @Setup(Level.Trial)
//     public void setup() {
//         this.app = new App(false);
//     }

//     @Benchmark
//     @BenchmarkMode(Mode.AverageTime)
//     @OutputTimeUnit(TimeUnit.MILLISECONDS)
//     @Warmup(iterations = 0)
//     public List<CRDTItem> insertTime() throws BadLocationException {
//         for (int i = 0; i < operations; i++) {
//             this.app.insertCRDT(i, "a");
//         }
//         return this.app.getCRDT().returnCopy();
//     }
// }