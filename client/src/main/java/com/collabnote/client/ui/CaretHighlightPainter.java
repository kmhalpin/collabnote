package com.collabnote.client.ui;

import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.FontMetrics;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

public class CaretHighlightPainter extends LayeredHighlighter.LayerPainter {
    private Color color;

    public CaretHighlightPainter(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
    }

    @Override
    public Shape paintLayer(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent editor, View view) {
        g.setColor(color == null ? editor.getSelectionColor() : color);
        Rectangle rectangle = null;

        if (p0 == view.getStartOffset() && p1 == view.getEndOffset()) {
            if (viewBounds instanceof Rectangle) {
                rectangle = (Rectangle) viewBounds;
            } else {
                rectangle = viewBounds.getBounds();
            }
        } else {
            try {
                Shape shape = view.modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, viewBounds);
                rectangle = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
            } catch (BadLocationException e) {
            }
        }

        FontMetrics fm = editor.getFontMetrics(editor.getFont());
        int baseline = rectangle.y + rectangle.height - fm.getDescent() + 1;
        g.drawLine(rectangle.x + 1, baseline, rectangle.x - 1 + rectangle.width, baseline);
        g.drawLine(rectangle.x + 1, baseline + 1, rectangle.x - 1 + rectangle.width, baseline + 1);

        return rectangle;
    }

}
