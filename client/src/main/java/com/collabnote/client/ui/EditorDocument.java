package com.collabnote.client.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;

import com.collabnote.client.Controller;

public class EditorDocument extends PlainDocument implements DocumentListener {
    private Controller controller;

    public EditorDocument(Controller controller) {
        super();
        this.controller = controller;
    }

    public EditorDocument(Content c, Controller controller) {
        super(c);
        this.controller = controller;
        addDocumentListener(this);
    }

    public void asyncInsert(int offset, String text) throws BadLocationException {
        this.writeLock();

        this.getContent().insertString(offset, text);
        DefaultDocumentEvent e = new EditorDocumentEvent(offset, text.length(), DocumentEvent.EventType.INSERT);
        this.insertUpdate(e, new SimpleAttributeSet());
        e.end();

        this.fireInsertUpdate(e);

        this.writeUnlock();
    }

    public void asyncDelete(int offset, int length) throws BadLocationException {
        this.writeLock();

        DefaultDocumentEvent e = new EditorDocumentEvent(offset, length, DocumentEvent.EventType.REMOVE);
        this.removeUpdate(e);
        this.getContent().remove(offset, length);
        this.postRemoveUpdate(e);
        e.end();

        this.fireRemoveUpdate(e);

        this.writeUnlock();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (e instanceof EditorDocumentEvent) {
            return;
        }

        String changes = "";
        int offset = e.getOffset();

        try {
            changes = this.getText(offset, e.getLength());
        } catch (BadLocationException e2) {
            changes = "";
        }

        controller.insertCRDT(offset, changes);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        if (e instanceof EditorDocumentEvent) {
            return;
        }

        int offset = e.getOffset();

        controller.deleteCRDT(offset);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    class EditorDocumentEvent extends DefaultDocumentEvent {
        public EditorDocumentEvent(int offs, int len, EventType type) {
            super(offs, len, type);
        }
    }
}