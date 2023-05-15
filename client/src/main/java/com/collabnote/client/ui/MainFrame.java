package com.collabnote.client.ui;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;

import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.client.viewmodel.TextEditorViewModel;

import java.awt.Dimension;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

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
    private JScrollPane stateVisual;

    public MainFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        CRDTDocument document = new CRDTDocument();
        TextEditorViewModel viewModel = new TextEditorViewModel(document);

        Menu menu = new Menu(viewModel, document);

        setJMenuBar(menu);

        gLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(gLayout);

        editorPanel = new EditorPanel(viewModel, document);

        stateVisual = new JScrollPane(new StateVisual(viewModel));
        stateVisual.setPreferredSize(new Dimension(200, 200));

        gLayout.setAutoCreateGaps(true);
        gLayout.setAutoCreateContainerGaps(true);
        gLayout.setHorizontalGroup(
                gLayout.createSequentialGroup()
                        .addGroup(gLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(editorPanel))
                        .addGroup(gLayout.createParallelGroup(Alignment.TRAILING)
                                .addComponent(stateVisual)));

        gLayout.setVerticalGroup(
                gLayout.createSequentialGroup()
                        .addGroup(gLayout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(editorPanel)
                                .addComponent(stateVisual)));

        getContentPane().add(editorPanel);

        pack();
    }

}
