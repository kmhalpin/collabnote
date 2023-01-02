package com.collabnote.benchmark.cases;

import java.util.ArrayList;

import com.collabnote.benchmark.BenchmarkAbstractCRDT;
import com.collabnote.benchmark.BenchmarkChange;
import com.collabnote.benchmark.BenchmarkCheck;
import com.collabnote.benchmark.BenchmarkTemplate;
import com.collabnote.benchmark.CRDTFactory;
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
        protected void run(BenchmarkCheck check, String[] inputData, BenchmarkChange... changeDocs) {
            ArrayList<CRDTItem> operations = new ArrayList<>();
            BenchmarkAbstractCRDT doc1 = crdtFactory.create(new CRDTListener() {
                @Override
                public void onCRDTInsert(CRDTItem item) {
                    operations.add(item);
                }

                @Override
                public void onCRDTDelete(CRDTItem item) {
                    operations.add(item);
                }
            });
            BenchmarkAbstractCRDT doc2 = crdtFactory.create();

            for (int i = 0; i < inputData.length; i++) {
                changeDocs[0].run(doc1, i, inputData[i]);
            }

            for (CRDTItem operation : operations) {
                doc2.insertText(0, name);
            }
        }
    }
}
