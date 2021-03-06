package com.mandas.tiled2d.core;

import com.mandas.tiled2d.Config;
import com.mandas.tiled2d.renderer.Renderer;

import javax.swing.*;

public class Application {
    private GameApplication gameApp = null;
    private MainWindow window = null;

    private String windowTitle = "Mandas Java Tiled2D Engine";

    public Application(GameApplication gameApp) {
        this.gameApp = gameApp;
        gameApp.InitRenderer();
    }

    public Application(GameApplication gameApp, String title) {
        Log.mandas().info(">>> Welcome to Mandas Tiled2D Engine! <<<");
        this.gameApp = gameApp;
        this.windowTitle = title;
        gameApp.InitRenderer();
    }

    public void createWindowAndRun() {
        window = new MainWindow(windowTitle, Renderer.getRenderer(), this);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

        Thread updateThread = new Thread(new ApplicationUpdater(this));
        updateThread.setDaemon(true);
        updateThread.start();
    }

    /**
     * This function will be called when the window repaints.
     */
    void OnRender() {
        if (gameApp == null) {
            throw new IllegalStateException("Please setGameApplication first!");
        }
        gameApp.OnRender();
    }

    void OnUpdate(float timestep) {
        if (gameApp == null) {
            throw new IllegalStateException("Please setGameApplication first!");
        }
        gameApp.OnUpdate(timestep);
        window.repaint();
    }

    private static class ApplicationUpdater implements Runnable {

        private long oldTime = 0;
        private final Application app;

        public ApplicationUpdater(Application app) {
            this.app = app;
        }

        @Override
        public void run() {
            while (true) {
                if (oldTime == 0) {
                    oldTime = System.currentTimeMillis();
                }
                else {
                    long time = System.currentTimeMillis();
                    if (time - oldTime >= (1000.0f / Config.MaxFrameRate)) {
                        app.OnUpdate((time - oldTime) / 1000.0f);
                        oldTime = time;
                    }
                }
            }
        }

    }
    
}
