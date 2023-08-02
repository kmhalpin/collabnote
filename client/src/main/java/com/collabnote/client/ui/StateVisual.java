package com.collabnote.client.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import com.collabnote.client.viewmodel.TextEditorViewModelImageListener;

public class StateVisual extends JPanel implements TextEditorViewModelImageListener {
    BufferedImage image;

    public StateVisual() {
        this.image = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image == null)
            return;

        g.drawImage(this.image, 0, 0, null);
    }

    @Override
    public void updateImage(byte[] image) {
        if (image == null) {
            this.image = null;
            setPreferredSize(new Dimension(0, 0));
        } else
            try {
                this.image = ImageIO.read(new ByteArrayInputStream(image));
                int w = this.image.getWidth();
                int h = this.image.getHeight();
                setPreferredSize(new Dimension(w, h));
            } catch (IOException e) {
                e.printStackTrace();
            }
        revalidate();
        repaint();
    }
}
