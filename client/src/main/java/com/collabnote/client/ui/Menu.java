package com.collabnote.client.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.collabnote.client.Controller;

public class Menu extends JMenuBar {
    JMenu fileMenu, editMenu, accountMenu;
    JMenuItem fileNewItem, fileShareItem, fileConnectItem, accountPrint, accountOfflineToggle;

    public Menu(Controller controller) {
        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        accountMenu = new JMenu("Account");

        fileNewItem = new JMenuItem("New File");
        fileNewItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.newNote();
            }
        });

        fileShareItem = new JMenuItem("Share File");
        fileShareItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                controller.shareNote(host);
            }
        });

        fileConnectItem = new JMenuItem("Connect File");
        fileConnectItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = JOptionPane.showInputDialog("Enter collaboration server host");
                String shareID = JOptionPane.showInputDialog("Enter collaboration ID");
                controller.connectNote(host, shareID);
            }
        });

        fileMenu.add(fileNewItem);
        fileMenu.add(fileShareItem);
        fileMenu.add(fileConnectItem);

        accountPrint = new JMenuItem("Print");
        accountPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                controller.printCRDT();
            }
        });

        accountOfflineToggle = new JMenuItem("Offline Mode");
        accountOfflineToggle.addActionListener(null);

        accountMenu.add(accountPrint);
        accountMenu.add(accountOfflineToggle);

        add(fileMenu);
        add(editMenu);
        add(accountMenu);
    }
}
