package myGame.core;

import org.joml.Vector3f;
import tage.Camera;

/**
 * Handles updating the game HUD.
 */
public class HUDManager {
    private final MyGame game;

    public HUDManager(MyGame game) {
        this.game = game;
    }

    public void updateHUD() {
        Camera camera = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        double currentTime = System.currentTimeMillis();
        String hud1 = "Time = " + (int) ((currentTime - game.getStartTime()) / 1000);
        String hud2 = String.format("Camera position = %.2f, %.2f, %.2f",
                camera.getLocation().x(),
                camera.getLocation().y(),
                camera.getLocation().z());

        MyGame.getEngine().getHUDmanager().setHUD1(hud1, new Vector3f(1, 0, 0), 15, 15);
        MyGame.getEngine().getHUDmanager().setHUD2(hud2, new Vector3f(1, 1, 1), 500, 15);
    }
}
