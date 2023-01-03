package com.collabnote.benchmark;

public interface BenchmarkAbstractCRDT {
    void insertText(int index, String text, Object... args);
    void applyUpdate(CRDTOperation operation);
}
