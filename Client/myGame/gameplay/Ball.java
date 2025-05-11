package myGame.gameplay;

import myGame.core.MyGame;
import myGame.networking.GhostAvatar;
import org.joml.*;
import tage.*;
import tage.shapes.Sphere;
import tage.shapes.AnimatedShape;

import java.lang.Math;
import java.util.UUID;

public class Ball {
    private static final float BALL_SPEED = 5f;
    private static final float MAX_Y_SPEED = 4f;
    private static final float CEILING_Y = 3f;
    private static final float FLOOR_Y = -3f;
    private static final float PADDLE_HALF_WIDTH = 0.5f;
    private static final float PADDLE_HALF_HEIGHT = 1.0f;
    private static final float BALL_RADIUS = 0.2f;
    private static final float MAX_STEP = 0.005f;

    private final MyGame game;
    private final GameObject ball;
    private final Vector3f ballVelocity = new Vector3f();

    public Ball(MyGame g) {
        game = g;
        ball = createBall();
        resetBall();
    }

    public void setPosition(Vector3f pos) {
        ball.setLocalTranslation(new Matrix4f().translation(pos));
    }

    private GameObject createBall() {
        GameObject b = new GameObject(GameObject.root(), new Sphere(), null);
        b.setLocalScale(new Matrix4f().scaling(0.2f));
        b.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));
        return b;
    }

    public void update(float elapsedTime, GameObject opponentPaddle, Vector3f playerPosition) {
        float delta = elapsedTime / 1000f;
        float d = delta;
        while (d > 0f) {
            float step = Math.min(MAX_STEP, d);
            moveBall(step, opponentPaddle, playerPosition);
            d -= step;
        }
    }

    private void moveBall(float delta, GameObject opponentPaddle, Vector3f playerPosition) {
        Vector3f pos = ball.getLocalTranslation().getTranslation(new Vector3f());
        Vector3f move = new Vector3f(ballVelocity).mul(delta);
        Vector3f nextPos = new Vector3f(pos).add(move);

        boolean playerHit = checkCollision(nextPos, playerPosition, true);
        boolean opponentHit = false;

        if (opponentPaddle != null) {
            Vector3f opponentPos = opponentPaddle.getLocalTranslation().getTranslation(new Vector3f());
            opponentHit = checkCollision(nextPos, opponentPos, false);
        }

        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatars()) {
            Vector3f ghostPos = ghost.getLocalTranslation().getTranslation(new Vector3f());
            if (checkCollision(nextPos, ghostPos, false)) {
                ghost.playBounceAnimation();
                ballVelocity.mul(1.05f);
            }
        }

        if (playerHit || opponentHit) {
            ballVelocity.mul(1.05f);
        }

        if (nextPos.y >= CEILING_Y) {
            ballVelocity.y = -Math.abs(ballVelocity.y);
            nextPos.y = CEILING_Y;
        } else if (nextPos.y <= FLOOR_Y) {
            ballVelocity.y = Math.abs(ballVelocity.y);
            nextPos.y = FLOOR_Y;
        }

        ball.setLocalTranslation(new Matrix4f().translation(nextPos));
    }

    private boolean checkCollision(Vector3f predictedPos, Vector3f paddlePos, boolean isPlayer) {
        float closestX = clamp(predictedPos.x, paddlePos.x - PADDLE_HALF_WIDTH, paddlePos.x + PADDLE_HALF_WIDTH);
        float closestY = clamp(predictedPos.y, paddlePos.y - PADDLE_HALF_HEIGHT, paddlePos.y + PADDLE_HALF_HEIGHT);
        float dx = predictedPos.x - closestX;
        float dy = predictedPos.y - closestY;

        if (dx * dx + dy * dy < BALL_RADIUS * BALL_RADIUS) {
            ballVelocity.x = isPlayer ? Math.abs(ballVelocity.x) : -Math.abs(ballVelocity.x);
            predictedPos.x = isPlayer
                    ? paddlePos.x + PADDLE_HALF_WIDTH + BALL_RADIUS
                    : paddlePos.x - PADDLE_HALF_WIDTH - BALL_RADIUS;

            AnimatedShape paddleShape = isPlayer ? game.getPaddleS() : game.getPaddleS_2();
            paddleShape.playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);

            if (game.getBounceSound() != null) {
                float randomPitch = 0.9f + (float)(Math.random()) * 0.2f;
                game.getBounceSound().setPitch(randomPitch);
                game.getBounceSound().play();
            }

            if (isPlayer && game.getProtocolClient() != null && game.isClientConnected()) {
                game.getProtocolClient().sendPaddleBounceMessage(game.getProtocolClient().getID());
            }

            return true;
        }
        return false;
    }

    public void resetBall() {
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));
        float initialX = (Math.random() > 0.5 ? 1 : -1) * BALL_SPEED;
        float initialY;
        do {
            initialY = (float) (Math.random() * 2f - 1f);
        } while (Math.abs(initialY) < 0.5f);
        ballVelocity.set(initialX, initialY, 0f);
    }

    public GameObject getBall() {
        return ball;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
