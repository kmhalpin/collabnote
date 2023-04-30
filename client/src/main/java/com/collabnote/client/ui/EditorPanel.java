package com.collabnote.client.ui;

import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.client.viewmodel.TextEditorViewModel;
import com.collabnote.client.viewmodel.TextEditorViewModelListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Color;

public class EditorPanel extends JPanel implements TextEditorViewModelListener {
    private JTextArea textArea;
    private CaretHighlighter highlighter;
    private CRDTDocument document;

    public EditorPanel(TextEditorViewModel viewModel, CRDTDocument crdtDocument) {
        viewModel.setListener(this);

        this.document = crdtDocument;
        this.textArea = new JTextArea(document, null, 30, 40);
        this.highlighter = new CaretHighlighter(Color.RED);
        this.highlighter.setDrawsLayeredHighlights(true);
        this.textArea.setHighlighter(highlighter);

        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                viewModel.updateCaret(e.getDot());
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public Object addCaretListener(int index) throws BadLocationException {
        if (index == 0 || document.getText(index - 1, 1).equals("\n"))
            return highlighter.addHighlight(index, index + 1);
        return highlighter.addHighlight(index - 1, index);
    }

    @Override
    public void removeCaretListener(Object tag) {
        this.highlighter.removeHighlight(tag);
    }
}
