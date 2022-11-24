package com.collabnote.client.ui;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.awt.Color;

public class CaretHighlighter extends DefaultHighlighter {
    protected static final Highlighter.HighlightPainter sharedPainter = new CaretHighlightPainter(
            null);
    protected Highlighter.HighlightPainter painter;

    public CaretHighlighter(Color c) {
        painter = (c == null ? sharedPainter : new CaretHighlightPainter(c));
    }

    public Object addHighlight(int p0, int p1) throws BadLocationException {
        return addHighlight(p0, p1, painter);
    }

    @Override
    public void setDrawsLayeredHighlights(boolean newValue) {
        if (newValue == false) {
            throw new IllegalArgumentException(
                    "CaretHighlighter only draws layered highlights");
        }
        super.setDrawsLayeredHighlights(true);
    }

}
