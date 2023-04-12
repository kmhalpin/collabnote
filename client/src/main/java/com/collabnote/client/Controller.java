package com.collabnote.client;

import com.collabnote.documentcrdt.CRDTDocument;

public interface Controller {
    public void newNote();
    public void shareNote(String host);
    public void connectNote(String host, String shareID);

    public void updateCaret(int index);
    public CRDTDocument getDocument();
    public void printCRDT();
}
