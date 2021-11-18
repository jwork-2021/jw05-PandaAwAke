package com.pandaawake.renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class TileParser {
    
    private String filename;
    private int tileWidth, tileHeight;
    private int fileWidthInTiles, fileHeightInTiles;
    private Texture[] textures;

    public TileParser(String filename, int tileWidth, int tileHeight, int fileWidthInTiles, int fileHeightInTiles) {
        this.filename = filename;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.fileWidthInTiles = fileWidthInTiles;
        this.fileHeightInTiles = fileHeightInTiles;
        textures = new Texture[fileWidthInTiles * fileHeightInTiles];

        loadTileFile();
    }

    public Texture getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= fileWidthInTiles || y >= fileHeightInTiles) {
            throw new IllegalArgumentException("Illegal position!");
        }
        int index = x + y * fileWidthInTiles;
        return textures[index];
    }

    private void loadTileFile() {
        BufferedImage tileFileImg = null;
        try {
            tileFileImg = ImageIO.read(new File(filename));
        } catch (IOException e) {
            System.err.println("loadTileFile(): " + e.getMessage());
        }

        for (int i = 0; i < fileWidthInTiles * fileHeightInTiles; i++) {
            int sx = (i % fileWidthInTiles) * tileWidth;
            int sy = (i / fileWidthInTiles) * tileHeight;
            
            textures[i] = new Texture(new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB));
            textures[i].getImage().getGraphics().drawImage(tileFileImg, 0, 0, tileWidth, tileHeight, sx, sy, sx + tileWidth,
                    sy + tileHeight, null);
        }
    }

}
