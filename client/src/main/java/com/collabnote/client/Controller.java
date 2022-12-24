package com.collabnote.client;

import com.collabnote.crdt.CRDT;

public interface Controller {
    public void newNote();
    public void shareNote(String host);
    public void connectNote(String host, String shareID);

    public void updateCaret(int index);
    public CRDT getCRDT();
    public void insertCRDT(int offset, String changes);
    public void deleteCRDT(int offset);
    public void printCRDT();
}
