package myGame.core;

import org.joml.Vector3f;
import tage.Camera;

public class HUDManager {
    private final MyGame game;
    private boolean gameStarted = false;
    private int playerScore = 0;
    private int opponentScore = 0;

    public HUDManager(MyGame game) {
        this.game = game;
    }

    public void setScores(int player, int opponent) {
        this.playerScore = player;
        this.opponentScore = opponent;
    }

    public void updateHUD() {
        Camera camera = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        double currentTime = System.currentTimeMillis();
        int timeElapsed = (int) ((currentTime - game.getStartTime()) / 1000);

        String scoreLine = String.format("Player: %d   Opponent: %d", playerScore, opponentScore);
        String startLine = gameStarted ? "" : "   [Press P to Start]";
        String hud1 = scoreLine + startLine;

        String hud2 = String.format("Mode: %s   Connection: %s",
                game.isMultiplayerMode() ? "Multiplayer" : "Singleplayer",
                game.isClientConnected() ? "Online" : "Offline");

        MyGame.getEngine().getHUDmanager().setHUD1(hud1, new Vector3f(1, 1, 0), 15, 15);
        MyGame.getEngine().getHUDmanager().setHUD2(hud2, new Vector3f(1, 1, 1), 15, 35);
    }

    public void startGame() {
        gameStarted = true;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void addPlayerScore() {
        playerScore++;
    }

    public void addOpponentScore() {
        opponentScore++;
    }

    public void resetScores() {
        playerScore = 0;
        opponentScore = 0;
    }

    public int getPlayerScore() { return playerScore; }
    public int getOpponentScore() { return opponentScore; }
}
