package com.mandas.tiled2d.renderer;

import com.mandas.tiled2d.Config;
import com.mandas.tiled2d.scene.*;
import com.mandas.tiled2d.scene.Component;
import com.mandas.tiled2d.utils.FloatPair;
import com.mandas.tiled2d.utils.IntPair;
import com.mandas.tiled2d.utils.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

/**
 * 2D Renderer for the game.
 * 
 * @author Ma Yingshuo
 */
public class Renderer extends JPanel {

    private static Renderer globalRenderer = null;
    /**
     * Initialize the rendering configuration
     *
     * @param widthInTiles The width of map (in tiles count).
     * @param heightInTiles The height of map (in tiles count).
     * @param tileWidth The width of a tile.
     * @param tileHeight The width of a tile.
     * @param scoreboardWidth The width of scoreboard, set to 0 if you do not need this.
     * @param emptyTextureColor If there are unset tiles in the map, set to this pure color.
     */
    public static void Init(int widthInTiles, int heightInTiles, int tileWidth, int tileHeight,
                            int scoreboardWidth, Color emptyTextureColor, Scene scene, Camera camera) {
        globalRenderer = new Renderer(widthInTiles, heightInTiles, tileWidth, tileHeight,
                scoreboardWidth, Texture.getPureColorTexture(tileWidth, tileHeight, emptyTextureColor), scene, camera);
        globalRenderer.Init();
    }
    /**
     * Initialize the rendering configuration
     *
     * @param widthInTiles The width of map (in tiles count).
     * @param heightInTiles The height of map (in tiles count).
     * @param tileWidth The width of a tile.
     * @param tileHeight The width of a tile.
     * @param scoreboardWidth The width of scoreboard, set to 0 if you do not need this.
     * @param emptyTexture If there are unset tiles in the map, set to this texture.
     */
    public static void Init(int widthInTiles, int heightInTiles, int tileWidth, int tileHeight,
                            int scoreboardWidth, Texture emptyTexture, Scene scene, Camera camera) {
        globalRenderer = new Renderer(widthInTiles, heightInTiles, tileWidth, tileHeight,
                scoreboardWidth, emptyTexture, scene, camera);
        globalRenderer.Init();
    }
    public static Renderer getRenderer() {
        if (globalRenderer == null) {
            throw new NullPointerException("Renderer is not initialized!");
        }
        return globalRenderer;
    }

    private BufferedImage offscreenBuffer, scoreboardBuffer;
    private Graphics2D offscreenGraphics, scoreboardGraphics;
    private int mapWidthInTiles;
    private int mapHeightInTiles;
    private int tileWidth;
    private int tileHeight;

    private Texture[][] tiles;
    private Texture[][] oldTiles;
    private Texture emptyTexture;
    private int scoreboardWidth;
    private Scene scene;
    private Camera camera;

    // Before every rendering, set this variable to clear tiles inside.
    private Set<IntPair> repaintTilePositions;
    // Additional things to render
    private ArrayList<Pair<FloatPair, Texture>> flotingTiles;

    Renderer(int mapWidthInTiles, int mapHeightInTiles, int tileWidth, int tileHeight, int scoreboardWidth,
             Texture emptyTexture, Scene scene, Camera camera) {
        super();
        if (emptyTexture == null) {
            throw new IllegalArgumentException("emptyTexture cannot be null!");
        }
        if (mapWidthInTiles < 1) {
            throw new IllegalArgumentException("width " + mapWidthInTiles + " must be greater than 0.");
        }
        if (mapHeightInTiles < 1) {
            throw new IllegalArgumentException("height " + mapHeightInTiles + " must be greater than 0.");
        }

        this.mapWidthInTiles = mapWidthInTiles;
        this.mapHeightInTiles = mapHeightInTiles;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.emptyTexture = emptyTexture;
        this.scoreboardWidth = scoreboardWidth;
        this.scene = scene;
        this.camera = camera;
        
        Dimension panelSize = new Dimension(tileWidth * (int)(camera.getWidthInTiles()) + scoreboardWidth,
                tileHeight * (int)(camera.getHeightInTiles()));
        setPreferredSize(panelSize);
        
        offscreenBuffer = new BufferedImage(tileWidth * mapWidthInTiles, tileHeight * mapHeightInTiles, BufferedImage.TYPE_INT_ARGB);
        offscreenGraphics = offscreenBuffer.createGraphics();
        scoreboardBuffer = new BufferedImage(scoreboardWidth, tileHeight * (int)(camera.getHeightInTiles()), BufferedImage.TYPE_INT_ARGB);
        scoreboardGraphics = scoreboardBuffer.createGraphics();

        tiles = new Texture[mapWidthInTiles][mapHeightInTiles];
        oldTiles = new Texture[mapWidthInTiles][mapHeightInTiles];

        repaintTilePositions = new TreeSet<>();
        flotingTiles = new ArrayList<>();

        Init();
    }

    private void Init() {
        synchronized (this) {
            // Set Clear Color First
            offscreenGraphics.setColor(Config.DefaultBackgroundColor);
            offscreenGraphics.fillRect(0, 0, mapWidthInTiles * tileWidth + scoreboardWidth, mapHeightInTiles * tileHeight);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    /**
     * Clear the entire screen to emptyTile.
     *
     */
    public void clear() {
        synchronized (this) {
            for (int i = 0; i < mapWidthInTiles; i++) {
                for (int j = 0; j < mapHeightInTiles; j++) {
                    tiles[i][j] = emptyTexture;
                    oldTiles[i][j] = emptyTexture;
                }
            }
            repaintTilePositions.clear();
            flotingTiles.clear();
            offscreenGraphics.fillRect(0, 0, tileWidth * mapWidthInTiles + scoreboardWidth, tileHeight * mapHeightInTiles);
        }
    }

    /**
     * Write a tile to the specified position. This updates the cursor's
     * position.
     *
     * @param texture   the tile to write
     * @param x         the distance from the left to begin writing from
     * @param y         the distance from the top to begin writing from
     */
    public void setTexture(Texture texture, int x, int y) {
        synchronized (this) {
            if (x < 0 || x >= mapWidthInTiles)
                throw new IllegalArgumentException("x " + x + " must be within range [0," + mapWidthInTiles + ")");

            if (y < 0 || y >= mapHeightInTiles)
                throw new IllegalArgumentException("y " + y + " must be within range [0," + mapHeightInTiles + ")");

            tiles[x][y] = texture;
        }
    }

    /**
     * This function is used for set some positions to be repainted next time, even through tile does not change.
     * @param repaintPositions Positions of tiles to repaint
     */
    public void addRepaintTilePositions(Collection<IntPair> repaintPositions) {
        synchronized (this) {
            this.repaintTilePositions.addAll(repaintPositions);
        }
    }


    /**
     * This function is used for set some additional render tiles.
     * They should be set before every rendering.
     * @param position          Position to render.
     * @param texture           Texture to show.
     */
    public void addFloatingTile(FloatPair position, Texture texture) {
        synchronized (this) {
            this.flotingTiles.add(new Pair<>(position, texture));
        }
    }


    public void clearScoreboard() {
        scoreboardGraphics.setColor(Config.DefaultBackgroundColor);
        scoreboardGraphics.fillRect(0, 0, scoreboardWidth, mapHeightInTiles * tileHeight);
    }

    /**
     * This function draws some string in scoreboard.
     * @param startX x in scoreboard
     * @param startY y in scoreboard
     * @param string string to draw
     */
    public void drawScoreboardString(int startX, int startY, String string) {
        drawScoreboardString(startX, startY, string, Config.ScoreboardTextFont);
    }

    /**
     * This function draws some string in scoreboard.
     * @param startX x in scoreboard
     * @param startY y in scoreboard
     * @param string string to draw
     * @param font Font for drawing
     */
    public void drawScoreboardString(int startX, int startY, String string, Font font) {
        scoreboardGraphics.setColor(Config.FontColor);
        scoreboardGraphics.setFont(font);
        scoreboardGraphics.drawString(string, startX, startY);
    }

    /**
     * This function draws some string in scoreboard.
     * @param startX x in scoreboard
     * @param startY y in scoreboard
     * @param texture texture to draw
     */
    public void drawScoreboardTile(int startX, int startY, Texture texture) {
        scoreboardGraphics.drawImage(texture.getImage(tileWidth, tileHeight), startX, startY, null);
    }



    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        synchronized (this) {
            if (g == null)
                throw new NullPointerException();

            // 1: Repaint specified positions
            paint_repaintTiles();

            // 2: Drawing tiles
            paint_tiles();

            // 3: Paint all entities in Scene
            paint_scene();

            // 4: Drawing floting tiles
            paint_floatingTiles();

            // 5: Camera
            BufferedImage cameraBufferedImage = paint_camera();

            int scoreboardLeft = mapWidthInTiles * tileWidth;
            g.drawImage(cameraBufferedImage, 0, 0, cameraBufferedImage.getWidth(), cameraBufferedImage.getHeight(), this);
            g.drawImage(scoreboardBuffer, scoreboardLeft, 0, this);
        }
    }

    
    private void paint_repaintTiles() {
        synchronized (this) {
            for (IntPair pos : repaintTilePositions) {
                int x = pos.first, y = pos.second;
                offscreenGraphics.setColor(Config.DefaultBackgroundColor);
                offscreenGraphics.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);

                if (tiles[x][y] == null) {
                    tiles[x][y] = emptyTexture;
                }
                BufferedImage img = tiles[x][y].getImage(tileWidth, tileHeight);
                offscreenGraphics.drawImage(img, x * tileWidth, y * tileHeight, null);
            }
            this.repaintTilePositions.clear();
        }
    }

    private void paint_tiles() {
        synchronized (this) {
            for (int x = 0; x < mapWidthInTiles; x++) {
                for (int y = 0; y < mapHeightInTiles; y++) {
                    if (oldTiles[x][y] == tiles[x][y]) {
                        continue;
                    }
                    if (tiles[x][y] == null) {
                        tiles[x][y] = emptyTexture;
                    }
                    offscreenGraphics.setColor(Config.DefaultBackgroundColor);
                    offscreenGraphics.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
                    BufferedImage img = tiles[x][y].getImage(tileWidth, tileHeight);
                    offscreenGraphics.drawImage(img, x * tileWidth, y * tileHeight, null);

                    oldTiles[x][y] = tiles[x][y];
                }
            }
        }
    }

    private void paint_scene() {
        synchronized (this) {
            for (Entity entity : scene.getEntities()) {
                TransformComponent transComponent = (TransformComponent) entity.getComponent(Component.TransformComponentId);
                double translationX = 0, translationY = 0;
                if (transComponent != null) {
                    translationX = transComponent.getTranslateX();
                    translationY = transComponent.getTranslateY();
                }

                TileTextureRenderComponent ttrComponent = (TileTextureRenderComponent) entity.getComponent(Component.TileTextureRenderComponentId);
                if (ttrComponent != null) {
                    for (Pair<FloatPair, Texture> positionAndTex : ttrComponent.getPositionsAndTextures()) {
                        BufferedImage img = positionAndTex.second.getImage(tileWidth, tileHeight);

                        double leftTileIndex = positionAndTex.first.first + translationX;
                        double topTileIndex = positionAndTex.first.second + translationY;

                        int leftPixel = (int) Math.round(leftTileIndex * tileWidth);
                        int topPixel = (int) Math.round(topTileIndex * tileHeight);

                        offscreenGraphics.drawImage(img, leftPixel, topPixel, null);

                        // Repaint nearby area next time
                        int left = (int) Math.round(Math.floor(leftTileIndex));
                        int right = (int) Math.round(Math.ceil(leftTileIndex));
                        int top = (int) Math.round(Math.floor(topTileIndex));
                        int bottom = (int) Math.round(Math.ceil(topTileIndex));
                        for (int x = Math.max(left, 0); x <= right && x < mapWidthInTiles; x++) {
                            for (int y = Math.max(top, 0); y <= bottom && y < mapHeightInTiles; y++) {
                                this.repaintTilePositions.add(new IntPair(x, y));
                            }
                        }
                    }
                }
            }
        }
    }

    private void paint_floatingTiles() {
        synchronized (this) {
            for (Pair<FloatPair, Texture> flotingTile : flotingTiles) {
                FloatPair position = flotingTile.first;
                Texture texture = flotingTile.second;
                if (texture != null) {
                    BufferedImage img = texture.getImage(tileWidth, tileHeight);
                    int leftPixel = Math.round(position.first * tileWidth);
                    int topPixel = Math.round(position.second * tileHeight);
                    offscreenGraphics.drawImage(img, leftPixel, topPixel, null);
                }

                // Repaint nearby area next time
                int left = (int) Math.round(Math.floor(position.first));
                int right = (int) Math.round(Math.ceil(position.first));
                int top = (int) Math.round(Math.floor(position.second));
                int bottom = (int) Math.round(Math.ceil(position.second));

                for (int x = Math.max(left, 0); x <= right && x < mapWidthInTiles; x++) {
                    for (int y = Math.max(top, 0); y <= bottom && y < mapHeightInTiles; y++) {
                        this.repaintTilePositions.add(new IntPair(x, y));
                    }
                }
            }
            flotingTiles.clear();
        }
    }

    private BufferedImage paint_camera() {
        synchronized (this) {
            int cameraWidth = Math.max((int) (camera.getWidthInTiles() * tileWidth), 0);
            int cameraHeight = Math.max((int) (camera.getHeightInTiles() * tileHeight), 0);
            int cameraTranslateX = Math.max((int) (camera.getTranslateX() * tileWidth), 0);
            int cameraTranslateY = Math.max((int) (camera.getTranslateY() * tileHeight), 0);
            int cameraScaledWidth = Math.max((int) (cameraWidth * camera.getScaleX()), 0);
            int cameraScaledHeight = Math.max((int) (cameraHeight * camera.getScaleY()), 0);

            Graphics2D graphics2D;

            // Get targeted area
            BufferedImage offScreenSubImage = offscreenBuffer.getSubimage(cameraTranslateX, cameraTranslateY,
                    Math.max(Math.min(cameraWidth, offscreenBuffer.getWidth() - cameraTranslateX), 0),
                    Math.max(Math.min(cameraHeight, offscreenBuffer.getHeight() - cameraTranslateY), 0));
            // Get scaled image
            Image scaledOffScreenSubImage = offScreenSubImage.getScaledInstance(
                    (int) (offScreenSubImage.getWidth() * camera.getScaleX()),
                    (int) (offScreenSubImage.getHeight() * camera.getScaleY()),
                    Image.SCALE_DEFAULT);
            // Paint scaled image into a BufferedImage
            BufferedImage scaledOffScreenSubBuffedImage = new BufferedImage(cameraScaledWidth, cameraScaledHeight, BufferedImage.TYPE_INT_ARGB);
            graphics2D = scaledOffScreenSubBuffedImage.createGraphics();
            graphics2D.drawImage(scaledOffScreenSubImage, 0, 0, null);
            graphics2D.dispose();

            // Paint subBufferedImage of it
            BufferedImage cameraBufferedImage = new BufferedImage(cameraWidth, cameraHeight, BufferedImage.TYPE_INT_ARGB);
            graphics2D = cameraBufferedImage.createGraphics();
            graphics2D.setColor(Config.DefaultBackgroundColor);
            graphics2D.fillRect(0, 0, cameraWidth, cameraHeight);
            graphics2D.drawImage(
                    scaledOffScreenSubBuffedImage.getSubimage(0, 0,
                            Math.min(cameraWidth, scaledOffScreenSubBuffedImage.getWidth()),
                            Math.min(cameraHeight, scaledOffScreenSubBuffedImage.getHeight())),
                    0, 0, null);
            graphics2D.dispose();

            return cameraBufferedImage;
        }
    }

}