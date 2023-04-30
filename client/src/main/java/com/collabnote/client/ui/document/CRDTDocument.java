package com.collabnote.client.ui.document;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;

import org.apache.commons.math3.util.Pair;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTRemoteTransaction;
import com.collabnote.crdt.Transaction;

public class CRDTDocument extends PlainDocument implements DocumentListener, CRDTRemoteTransaction {
    private CRDT crdt;

    public void setCrdt(CRDT crdt) {
        this.crdt = crdt;
    }

    public CRDTDocument() {
        super();
        addDocumentListener(this);
    }

    public CRDTDocument(Content c) {
        super(c);
        addDocumentListener(this);
    }

    @Override
    public void onRemoteCRDTInsert(Transaction transaction) {
        try {
            this.writeLock();

            Pair<Integer, CRDTItem> result = transaction.execute();
            if (result == null || result.getSecond().isDeleted()) {
                return;
            }

            String text = result.getSecond().content;
            this.getContent().insertString(result.getFirst(), text);
            DefaultDocumentEvent e = new CRDTDocumentEvent(result.getFirst(), text.length(),
                    DocumentEvent.EventType.INSERT);
            this.insertUpdate(e, new SimpleAttributeSet());
            e.end();

            this.fireInsertUpdate(e);
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            this.writeUnlock();
        }
    }

    @Override
    public void onRemoteCRDTDelete(Transaction transaction) {
        try {
            this.writeLock();

            Pair<Integer, CRDTItem> result = transaction.execute();
            if (result == null) {
                return;
            }

            int length = result.getSecond().content.length();
            DefaultDocumentEvent e = new CRDTDocumentEvent(result.getFirst(), length, DocumentEvent.EventType.REMOVE);
            this.removeUpdate(e);
            this.getContent().remove(result.getFirst(), length);
            this.postRemoveUpdate(e);
            e.end();

            this.fireRemoveUpdate(e);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        } finally {
            this.writeUnlock();
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (e instanceof CRDTDocumentEvent) {
            return;
        }

        String changes = "";
        int offset = e.getOffset();

        try {
            changes = this.getText(offset, e.getLength());
        } catch (BadLocationException e2) {
            changes = "";
        }

        crdt.localInsert(offset, changes);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (e instanceof CRDTDocumentEvent) {
            return;
        }

        int offset = e.getOffset();

        crdt.localDelete(offset, e.getLength());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    class CRDTDocumentEvent extends DefaultDocumentEvent {
        public CRDTDocumentEvent(int offs, int len, EventType type) {
            super(offs, len, type);
        }
    }
}
