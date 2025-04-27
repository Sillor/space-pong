package myGame;

import org.joml.*;
import tage.*;
import tage.shapes.Sphere;

import java.lang.Math;

public class PongNPCandBall {
    private static final float BALL_SPEED = 5f;
    private static final float MAX_Y_SPEED = 4f;
    private static final float CEILING_Y = 3f;
    private static final float FLOOR_Y = -3f;
    private static final float PADDLE_HALF_WIDTH = 0.5f;
    private static final float PADDLE_HALF_HEIGHT = 1.0f;
    private static final float BALL_RADIUS = 0.2f;
    private static final float MAX_STEP = 0.005f; // smaller step for precise collision

    private final MyGame game;
    private final GameObject npcPaddle;
    private final GameObject ball;
    private final Vector3f ballVelocity = new Vector3f();
    private boolean isNPCActive = true;
    private final float npcSpeed = 3f;

    public PongNPCandBall(MyGame g) {
        game = g;
        ball = createBall();
        npcPaddle = createNPCPaddle();
        resetBall();
    }

    private GameObject createBall() {
        var b = new GameObject(GameObject.root(), new Sphere(), null);
        b.setLocalScale(new Matrix4f().scaling(0.4f));
        b.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));
        return b;
    }

    private GameObject createNPCPaddle() {
        var paddle = new GameObject(GameObject.root(), game.getPaddleS(), game.getGhostTexture());
        paddle.setLocalScale(new Matrix4f().scaling(0.25f));
        paddle.setLocalRotation(new Matrix4f().rotateY((float) Math.toRadians(-90)).rotateX((float) Math.toRadians(90)));
        Vector3f playerPos = game.getPlayerPosition();
        paddle.setLocalTranslation(new Matrix4f().translation(-playerPos.x, playerPos.y, -3f));
        return paddle;
    }

    public void update(float elapsedTime) {
        if (!isNPCActive) return;
        float delta = elapsedTime / 1000f;
        while (delta > 0f) {
            float step = Math.min(MAX_STEP, delta);
            moveBall(step);
            delta -= step;
        }
        moveNPC(elapsedTime / 1000f);
    }

    private void moveBall(float delta) {
        Vector3f pos = ball.getLocalTranslation().getTranslation(new Vector3f());
        Vector3f move = new Vector3f(ballVelocity).mul(delta);
        Vector3f nextPos = new Vector3f(pos).add(move);

        if (checkCollision(nextPos, game.getPlayerPosition(), true) ||
                checkCollision(nextPos, npcPaddle.getLocalTranslation().getTranslation(new Vector3f()), false)) {
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

        if (nextPos.x > 7f || nextPos.x < -7f) {
            resetBall();
        }
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
            return true;
        }
        return false;
    }

    private void moveNPC(float delta) {
        Vector3f playerPos = game.getPlayerPosition();
        Vector3f npcPos = npcPaddle.getLocalTranslation().getTranslation(new Vector3f());
        float targetX = -playerPos.x;
        float dx = targetX - npcPos.x;
        float move = npcSpeed * delta;

        if (Math.abs(dx) > move) {
            npcPos.x += move * Math.signum(dx);
        } else {
            npcPos.x = targetX;
        }
        npcPaddle.setLocalTranslation(new Matrix4f().translation(npcPos.x, playerPos.y, -3f));
    }

    private void resetBall() {
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));
        float initialX = (Math.random() > 0.5 ? 1 : -1) * BALL_SPEED;
        float initialY;
        do {
            initialY = (float) (Math.random() * 2f - 1f);
        } while (Math.abs(initialY) < 0.5f);
        ballVelocity.set(initialX, initialY, 0f);
    }

    public void setIsNPCActive(boolean active) {
        isNPCActive = active;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
