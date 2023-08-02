package com.collabnote.client.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.DefaultButtonModel;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;

import com.collabnote.client.viewmodel.TextEditorViewModel;
import com.collabnote.client.viewmodel.TextEditorViewModelCollaborationListener;

public class Menu extends JMenuBar implements TextEditorViewModelCollaborationListener {
    private JMenu fileMenu, editMenu, accountMenu;
    private JMenuItem fileNewItem, fileSaveItem, fileLoadItem, fileShareItem, fileConnectItem, accountPrint,
            accountOfflineToggle;
    private JMenuItem collaboration;

    public Menu(TextEditorViewModel viewModel) {
        viewModel.setCollaborationListener(this);

        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        accountMenu = new JMenu("Debug");

        fileNewItem = new JMenuItem("New File");
        fileNewItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewModel.initDocument();
            }
        });

        fileSaveItem = new JMenuItem("Save File");
        fileSaveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save document");
                fileChooser.setFileFilter(new FileTypeFilter(".txt", "Collaborative Document"));

                int result = fileChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    viewModel.saveDocument(file);
                }
            }
        });

        fileLoadItem = new JMenuItem("Load File");
        fileLoadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Load document");
                fileChooser.setFileFilter(new FileTypeFilter(".txt", "Collaborative Document"));

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    viewModel.loadDocument(file);
                }
            }
        });

        fileShareItem = new JMenuItem("Share File");
        fileShareItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                if (host != null && !host.isEmpty())
                    viewModel.shareDocument(host);
            }
        });

        fileConnectItem = new JMenuItem("Connect File");
        fileConnectItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                String shareID = JOptionPane.showInputDialog("Enter collaboration ID");
                if (host != null && !host.isEmpty() && shareID != null && !shareID.isEmpty())
                    viewModel.connectDocument(host, shareID);
            }
        });

        fileMenu.add(fileNewItem);
        fileMenu.add(fileSaveItem);
        fileMenu.add(fileLoadItem);
        fileMenu.add(fileShareItem);
        fileMenu.add(fileConnectItem);

        accountPrint = new JMenuItem("Toggle Visualize State");
        accountPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // controller.printCRDT();
                try {
                    viewModel.setStateVisualizer();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        accountOfflineToggle = new JMenuItem("Online Mode");
        accountOfflineToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                viewModel.toggleConnection();
            }

        });

        accountMenu.add(accountPrint);
        accountMenu.add(accountOfflineToggle);

        collaboration = new JMenuItem();
        if (collaboration.getModel() instanceof DefaultButtonModel) {
            DefaultButtonModel model = (DefaultButtonModel) collaboration.getModel();
            model.setArmed(false);
            for (ChangeListener cl : model.getChangeListeners()) {
                model.removeChangeListener(cl);
            }
        }
        collaboration.setFocusable(false);

        add(fileMenu);
        // add(editMenu);
        add(accountMenu);
        add(collaboration);
    }

    @Override
    public void collaborationStatusListener(boolean status, String id) {
        if (id != null) {
            id = "Document ID: " + id;
        }
        this.collaboration.setText(id);
        this.accountOfflineToggle.setText(status ? "Offline Mode" : "Online Mode");
    }
}
