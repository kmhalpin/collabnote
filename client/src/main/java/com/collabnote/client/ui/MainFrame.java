package com.collabnote.client.ui;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;

import com.collabnote.client.Controller;

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

    public MainFrame(Controller controller) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setJMenuBar(new Menu(controller));

        gLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(gLayout);

        newEditorPanel(controller);

        pack();
    }

    public void newEditorPanel(Controller controller) {
        if (editorPanel != null)
            getContentPane().remove(editorPanel);

        editorPanel = new EditorPanel(controller);

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
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

}
