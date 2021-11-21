package com.mandas.tiled2d.core;

import javax.swing.JFrame;

import com.mandas.tiled2d.renderer.Renderer;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MainWindow extends JFrame implements KeyListener {
    
    private Application app;

    public MainWindow(String title, Renderer renderer, Application app) {
        super(title);
        this.app = app;
        add(renderer);
        pack();
        addKeyListener(this);
        repaint();
    }
    
    @Override
    public void repaint() {
        app.OnRender();
        super.repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        app.OnKeyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

}
