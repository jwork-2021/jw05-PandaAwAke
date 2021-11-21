package com.pandaawake.render;

import com.mandas.tiled2d.renderer.Camera;
import com.mandas.tiled2d.renderer.Renderer;
import com.mandas.tiled2d.renderer.Texture;
import com.mandas.tiled2d.scene.Scene;
import com.pandaawake.Config;
import com.pandaawake.scene.GameMap;
import com.pandaawake.sprites.Sprite;
import com.pandaawake.tiles.Thing;
import com.pandaawake.tiles.Tile;
import com.mandas.tiled2d.utils.FloatPair;
import com.mandas.tiled2d.utils.IntPair;

import java.util.Set;

public class RenderCommand {

    private static Renderer renderer;
    private static Texture emptyTexture;
    private static Camera camera;

    public static void Init(Scene scene) {
        emptyTexture = Config.TileParser.getEmptyTexture();
        camera = new Camera(Config.MapWidth, Config.MapHeight);
        camera.setScale(1.2, 1.2);
        camera.setTranslation(0, 0);

        Renderer.Init(Config.MapWidth, Config.MapHeight, Config.TileSize, Config.TileSize,
                Config.ScoreBoardWidth, emptyTexture, scene, camera);

        renderer = Renderer.getRenderer();
    }

    public static void drawGameMap(GameMap gameMap) {
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile<Thing> tile = gameMap.getTile(x, y);
                Thing thing = tile.getThing();
                if (thing == null) {
                    renderer.setTexture(emptyTexture, x, y);
                } else {
                    int glyphIndex = thing.getTiles().indexOf(tile);
                    renderer.setTexture(thing.getTextures().get(glyphIndex), x, y);
                }
            }
        }
    }

    public static void repaintPosition(Set<IntPair> position) {
        renderer.addRepaintTilePositions(position);
    }

    public static void clear() {
        renderer.clear();
    }



    public static void clearScoreboard() {
        renderer.clearScoreboard();
    }

    /**
     * This function draws some string in scoreboard.
     * @param startX x in scoreboard
     * @param startY y in scoreboard
     * @param string string to draw
     */
    public static void drawScoreboardString(int startX, int startY, String string) {
        renderer.drawScoreboardString(startX, startY, string);
    }

    /**
     * This function draws some string in scoreboard.
     * @param startX    x in scoreboard
     * @param startY    y in scoreboard
     * @param texture   texture to draw
     */
    public static void drawScoreboardTile(int startX, int startY, Texture texture) {
        renderer.drawScoreboardTile(startX, startY, texture);
    }

}
