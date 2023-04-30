package com.collabnote.client.ui;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;

import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.client.viewmodel.TextEditorViewModel;

import javax.swing.GroupLayout;
import javax.swing.JFrame;

import mdlaf.MaterialLookAndFeel;
import mdlaf.themes.JMarsDarkTheme;

public class MainFrame extends JFrame {
    static {
        try {
            UIManager.setLookAndFeel(new MaterialLookAndFeel(new JMarsDarkTheme()));
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private GroupLayout gLayout;
    private EditorPanel editorPanel;

    public MainFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        CRDTDocument document = new CRDTDocument();
        TextEditorViewModel viewModel = new TextEditorViewModel(document);

        setJMenuBar(new Menu(viewModel, document));

        gLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(gLayout);

        editorPanel = new EditorPanel(viewModel, document);

        gLayout.setHorizontalGroup(
                gLayout.createSequentialGroup()
                        .addContainerGap(60, Short.MAX_VALUE)
                        .addGroup(gLayout.createParallelGroup(Alignment.CENTER)
                                .addComponent(editorPanel))
                        .addContainerGap(60, Short.MAX_VALUE));

        gLayout.setVerticalGroup(
                gLayout.createSequentialGroup()
                        .addComponent(editorPanel));

        getContentPane().add(editorPanel);

        pack();
    }

}
