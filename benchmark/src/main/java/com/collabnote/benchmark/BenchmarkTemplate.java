package com.collabnote.benchmark;

public abstract class BenchmarkTemplate {
    protected String name;

    public BenchmarkTemplate(String name) {
        this.name = name;
    }

    abstract protected void run(BenchmarkCheck check, String[] inputData, BenchmarkChange ...changeDoc);
}
