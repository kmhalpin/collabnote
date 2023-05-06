package com.collabnote.client.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.client.viewmodel.TextEditorViewModel;
import com.collabnote.client.viewmodel.TextEditorViewModelCollaborationListener;

public class Menu extends JMenuBar implements TextEditorViewModelCollaborationListener {
    private JMenu fileMenu, editMenu, accountMenu;
    private JMenuItem fileNewItem, fileSaveItem, fileLoadItem, fileShareItem, fileConnectItem, accountPrint,
            accountOfflineToggle;

    public Menu(TextEditorViewModel viewModel, CRDTDocument crdtDocument) {
        viewModel.setCollaborationListener(this);

        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        accountMenu = new JMenu("Account");

        fileNewItem = new JMenuItem("New File");
        fileNewItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewModel.initDocument(crdtDocument);
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
                    viewModel.loadDocument(crdtDocument, file);
                }
            }
        });

        fileShareItem = new JMenuItem("Share File");
        fileShareItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                if (!host.isEmpty())
                    viewModel.shareDocument(host);
            }
        });

        fileConnectItem = new JMenuItem("Connect File");
        fileConnectItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                String shareID = JOptionPane.showInputDialog("Enter collaboration ID");
                if (!host.isEmpty() && !shareID.isEmpty())
                    viewModel.connectDocument(crdtDocument, host, shareID);
            }
        });

        fileMenu.add(fileNewItem);
        fileMenu.add(fileSaveItem);
        fileMenu.add(fileLoadItem);
        fileMenu.add(fileShareItem);
        fileMenu.add(fileConnectItem);

        accountPrint = new JMenuItem("Print");
        accountPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // controller.printCRDT();
            }
        });

        accountOfflineToggle = new JMenuItem("Online Mode");
        accountOfflineToggle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                viewModel.toggleConnection(crdtDocument);
            }

        });

        accountMenu.add(accountPrint);
        accountMenu.add(accountOfflineToggle);

        add(fileMenu);
        add(editMenu);
        add(accountMenu);
    }

    @Override
    public void collaborationStatusListener(boolean status) {
        this.accountOfflineToggle.setText(status ? "Offline Mode" : "Online Mode");
    }
}
