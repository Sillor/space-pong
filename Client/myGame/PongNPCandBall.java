package myGame;

import org.joml.*;
import tage.*;
import tage.shapes.Sphere;

import java.lang.Math;

public class PongNPCandBall {
    private MyGame game;
    private GameObject npcPaddle;
    private GameObject ball;
    private Vector3f ballVelocity;
    private float npcSpeed = 3.0f; // units per second
    private boolean isNPCActive = true;

    private static final float BALL_SPEED = 5.0f; // starting speed
    private static final float MAX_Y_SPEED = 4.0f; // limit bounce angle
    private static final float CEILING_Y = 3.0f; // ceiling height
    private static final float FLOOR_Y = -3.0f; // floor height

    public PongNPCandBall(MyGame g) {
        this.game = g;
        createBall();
        createNPCPaddle();
    }

    private void createBall() {
        ball = new GameObject(GameObject.root(), new Sphere(), null);
        ball.setLocalScale(new Matrix4f().scaling(0.4f));
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));

        float initialX = (Math.random() > 0.5 ? 1f : -1f) * BALL_SPEED;
        float initialY = (float)(Math.random() * 2f - 1f);
        if (Math.abs(initialY) < 0.5f) initialY = 0.5f * Math.signum(initialY != 0 ? initialY : 1);

        ballVelocity = new Vector3f(initialX, initialY, 0f);
    }

    private void createNPCPaddle() {
        npcPaddle = new GameObject(GameObject.root(), game.getPaddleS(), game.getGhostTexture());
        npcPaddle.setLocalScale(new Matrix4f().scaling(0.25f));
        Matrix4f initialRot = new Matrix4f()
                .rotateY((float) Math.toRadians(-90))
                .rotateX((float) Math.toRadians(90));
        npcPaddle.setLocalRotation(initialRot);

        Vector3f playerPos = game.getPlayerPosition();
        npcPaddle.setLocalTranslation(new Matrix4f().translation(-playerPos.x(), playerPos.y(), -3f));
    }

    public void update(float elapsedTime) {
        if (ball == null || npcPaddle == null) return;

        float delta = elapsedTime / 1000f;

        moveBall(delta);
        moveNPC(delta);
        checkCollisions();
    }

    private void moveBall(float delta) {
        Vector3f pos = ball.getLocalTranslation().getTranslation(new Vector3f());
        pos.add(new Vector3f(ballVelocity).mul(delta));
        ball.setLocalTranslation(new Matrix4f().translation(pos));

        // Bounce off ceiling and floor
        if (pos.y >= CEILING_Y) {
            ballVelocity.y *= -1;
            ballVelocity.y = Math.max(Math.min(ballVelocity.y, MAX_Y_SPEED), -MAX_Y_SPEED);
        }
        if (pos.y <= FLOOR_Y) {
            ballVelocity.y *= -1;
            ballVelocity.y = Math.max(Math.min(ballVelocity.y, MAX_Y_SPEED), -MAX_Y_SPEED);
        }

        // Ball out of bounds (missed paddle)
        if (pos.x > 7f || pos.x < -7f) {
            resetBall();
        }
    }

    private void moveNPC(float delta) {
        Vector3f playerPos = game.getPlayerPosition();
        Vector3f npcPos = npcPaddle.getLocalTranslation().getTranslation(new Vector3f());

        float targetX = -playerPos.x();
        float dx = targetX - npcPos.x;
        float move = npcSpeed * delta;

        if (Math.abs(dx) > move) {
            npcPos.x += move * Math.signum(dx);
        } else {
            npcPos.x = targetX;
        }

        npcPaddle.setLocalTranslation(new Matrix4f().translation(npcPos.x, playerPos.y(), -3f));
    }

    private void checkCollisions() {
        Vector3f ballPos = ball.getLocalTranslation().getTranslation(new Vector3f());
        Vector3f playerPos = game.getPlayerPosition();
        Vector3f npcPos = npcPaddle.getLocalTranslation().getTranslation(new Vector3f());

        float paddleWidth = 0.5f;
        float paddleHeight = 2.0f;
        float ballRadius = 0.2f;

        // Check collision with player paddle
        if (ballPos.x > playerPos.x - paddleWidth && ballPos.x < playerPos.x + paddleWidth &&
                ballPos.y > playerPos.y - paddleHeight / 2 && ballPos.y < playerPos.y + paddleHeight / 2) {
            ballVelocity.x *= -1.05f; // reflect and speed up
            ballPos.x = playerPos.x - paddleWidth; // prevent sticking
        }

        // Check collision with NPC paddle
        if (ballPos.x > npcPos.x - paddleWidth && ballPos.x < npcPos.x + paddleWidth &&
                ballPos.y > npcPos.y - paddleHeight / 2 && ballPos.y < npcPos.y + paddleHeight / 2) {
            ballVelocity.x *= -1.05f; // reflect and speed up
            ballPos.x = npcPos.x + paddleWidth; // prevent sticking
        }
    }

    private void resetBall() {
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));

        float initialX = (Math.random() > 0.5 ? 1f : -1f) * BALL_SPEED;
        float initialY = (float)(Math.random() * 2f - 1f);
        if (Math.abs(initialY) < 0.5f) initialY = 0.5f * Math.signum(initialY != 0 ? initialY : 1);

        ballVelocity = new Vector3f(initialX, initialY, 0f);
    }

    public void setIsNPCActive(boolean active) {
        isNPCActive = active;
    }
}
