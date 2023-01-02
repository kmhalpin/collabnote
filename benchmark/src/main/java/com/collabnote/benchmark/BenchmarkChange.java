package com.collabnote.benchmark;

public interface BenchmarkChange {
    void run(BenchmarkAbstractCRDT crdt, int index, String text);
}
