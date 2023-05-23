package com.collabnote.client.ui;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;

import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.client.viewmodel.TextEditorViewModel;
import com.collabnote.client.viewmodel.TextEditorViewModelImageListener;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
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
    private JLayeredPane layeredPane;
    private EditorPanel editorPanel;
    private StateVisual stateVisual;
    private JScrollPane stateVisualContainer;

    public MainFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        CRDTDocument document = new CRDTDocument();
        TextEditorViewModel viewModel = new TextEditorViewModel(document);

        Menu menu = new Menu(viewModel);
        setJMenuBar(menu);

        stateVisual = new StateVisual();
        stateVisualContainer = new JScrollPane(stateVisual);
        stateVisualContainer.setVisible(false);
        viewModel.setImageListener(new TextEditorViewModelImageListener() {
            @Override
            public void updateImage(byte[] image) {
                stateVisualContainer.setVisible(image != null);
                stateVisual.updateImage(image);
            }
        });

        editorPanel = new EditorPanel(viewModel, document);

        layeredPane = new JLayeredPane();
        layeredPane.add(stateVisualContainer, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(editorPanel, JLayeredPane.DEFAULT_LAYER);

        gLayout = new GroupLayout(layeredPane);
        gLayout.setHorizontalGroup(
                gLayout.createSequentialGroup()
                        .addContainerGap(60, Short.MAX_VALUE)
                        .addGroup(gLayout.createParallelGroup(Alignment.CENTER)
                                .addComponent(editorPanel))
                        .addContainerGap(60, Short.MAX_VALUE));

        gLayout.setVerticalGroup(
                gLayout.createSequentialGroup()
                        .addComponent(editorPanel));

        layeredPane.setLayout(gLayout);

        setContentPane(layeredPane);

        setPreferredSize(new Dimension(1000, getPreferredSize().height));

        int stateVisualContainerWidth = 180;
        stateVisualContainer
                .setPreferredSize(new Dimension(stateVisualContainerWidth, editorPanel.getPreferredSize().height));
        stateVisualContainer.setBounds(getPreferredSize().width - stateVisualContainerWidth, 0,
                stateVisualContainerWidth, editorPanel.getPreferredSize().height);

        pack();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                stateVisualContainer
                        .setPreferredSize(new Dimension(stateVisualContainerWidth, editorPanel.getHeight()));
                stateVisualContainer.setBounds(getWidth() - stateVisualContainerWidth, 0, stateVisualContainerWidth,
                        editorPanel.getHeight());
            }
        });
    }

}
