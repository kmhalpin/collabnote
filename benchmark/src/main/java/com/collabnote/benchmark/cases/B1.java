package com.collabnote.benchmark.cases;

import java.util.ArrayList;

import com.collabnote.benchmark.BenchmarkAbstractCRDT;
import com.collabnote.benchmark.BenchmarkChange;
import com.collabnote.benchmark.BenchmarkCheck;
import com.collabnote.benchmark.BenchmarkTemplate;
import com.collabnote.benchmark.BenchmarkStore;
import com.collabnote.benchmark.CRDTFactory;
import com.collabnote.benchmark.CRDTOperation;
import com.collabnote.benchmark.CRDTOperation.Operation;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTListener;

public class B1 {
    private CRDTFactory crdtFactory;

    public B1(CRDTFactory crdtFactory) {
        this.crdtFactory = crdtFactory;
    }

    public void runBenchmarks() {
        new B1BenchmarkTemplate("[B1.1] Append N characters")
                .run(new BenchmarkCheck() {
                    @Override
                    public boolean check(BenchmarkAbstractCRDT... docs) {
                        // TODO Auto-generated method stub
                        return false;
                    }
                }, new String[] {

                }, new BenchmarkChange() {
                    @Override
                    public void run(BenchmarkAbstractCRDT crdt, int index, String text) {
                        crdt.insertText(index, text);
                    }
                });
    }

    class B1BenchmarkTemplate extends BenchmarkTemplate {

        public B1BenchmarkTemplate(String name) {
            super(name);
        }

        @Override
        protected void run(BenchmarkCheck benchmarkCheck, String[] inputData, BenchmarkChange... changeDocs) {
            ArrayList<CRDTOperation> operations = new ArrayList<>();
            BenchmarkAbstractCRDT doc1 = crdtFactory.create(new CRDTListener() {
                @Override
                public void onCRDTInsert(CRDTItem item) {
                    operations.add(new CRDTOperation(item, Operation.INSERT));
                }

                @Override
                public void onCRDTDelete(CRDTItem item) {
                    operations.add(new CRDTOperation(item, Operation.DELETE));
                }
            });
            BenchmarkAbstractCRDT doc2 = crdtFactory.create();

            BenchmarkStore.benchmarkTime(name + " (time)", new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < inputData.length; i++) {
                        changeDocs[0].run(doc1, i, inputData[i]);
                    }
                }
            });

            for (CRDTOperation operation : operations) {
                doc2.applyUpdate(operation);
            }

            benchmarkCheck.check(doc1, doc2);
        }
    }
}
