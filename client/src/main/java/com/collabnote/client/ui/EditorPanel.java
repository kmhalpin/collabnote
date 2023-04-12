package com.collabnote.client.ui;

import com.collabnote.client.Controller;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;

public class EditorPanel extends JPanel {
    private HashMap<Integer, Object> carets;
    private JTextArea textArea;
    private CaretHighlighter highlighter;

    public EditorPanel(Controller controller) {
        carets = new HashMap<>();
        textArea = new JTextArea(controller.getDocument(), null, 30, 40);
        highlighter = new CaretHighlighter(Color.RED);
        highlighter.setDrawsLayeredHighlights(true);
        textArea.setHighlighter(highlighter);

        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                controller.updateCaret(e.getDot());
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateCaret(int agent, int index) {
        if (index == -1) {
            Object tag = carets.remove(agent);
            if (tag != null)
                highlighter.removeHighlight(tag);
            return;
        }

        Object tag = carets.get(agent);
        if (tag != null)
            highlighter.removeHighlight(tag);
        try {
            if (index == 0 || textArea.getDocument().getText(index - 1, 1).equals("\n")) {
                carets.put(agent, highlighter.addHighlight(index, index + 1));
            } else {
                carets.put(agent, highlighter.addHighlight(index - 1, index));
            }
        } catch (BadLocationException e1) {
        }
    }
}
