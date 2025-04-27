package myGame;

import org.joml.*;
import tage.*;
import tage.physics.*;
import tage.shapes.AnimatedShape;
import tage.shapes.Sphere;

import java.lang.Math;

public class PongNPCandBall {
    private MyGame game;
    private GameObject npcPaddle;
    private GameObject ball;
    private float npcSpeed = 3.0f; // units per second
    private boolean isNPCActive = true;
    private PhysicsObject ballPhys;

    public PongNPCandBall(MyGame g) {
        this.game = g;
        createBall();
        createNPCPaddle();
    }

    private void createBall() {
        ball = new GameObject(GameObject.root(), new Sphere(), null);
        ball.setLocalScale(new Matrix4f().scaling(0.4f));
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));

        double[] ballTransform = toDoubleArray(new Matrix4f(ball.getLocalTranslation()).get(new float[16]));
        ballPhys = game.getEngine().getSceneGraph().addPhysicsSphere(0.05f, ballTransform, 0.4f);
        ballPhys.setBounciness(1.4f);

        float initialX = (Math.random() > 0.5 ? 3f : -3f);
        float initialY = (float)(Math.random() * 2f - 1f);
        if (Math.abs(initialY) < 0.3f) initialY = 0.3f * Math.signum(initialY != 0 ? initialY : 1);

        ballPhys.setLinearVelocity(new float[]{initialX, initialY, 0f});

        ball.setPhysicsObject(ballPhys);
    }

    private void createNPCPaddle() {
        npcPaddle = new GameObject(GameObject.root(), game.getPaddleS(), game.getGhostTexture());
        npcPaddle.setLocalScale(new Matrix4f().scaling(0.25f));
        Matrix4f initialRot = new Matrix4f()
                .rotateY((float) Math.toRadians(-90))
                .rotateX((float) Math.toRadians(90));
        npcPaddle.setLocalRotation(initialRot);

        Vector3f playerPos = game.getPlayerPosition();
        npcPaddle.setLocalTranslation(new Matrix4f().translation(-playerPos.x(), playerPos.y(), playerPos.z()));

        double[] npcTransform = toDoubleArray(new Matrix4f(npcPaddle.getLocalTranslation()).get(new float[16]));
        PhysicsObject npcPhys = game.getEngine().getSceneGraph().addPhysicsBox(0, npcTransform, new float[]{0.5f, 2.0f, 0.5f});
        npcPaddle.setPhysicsObject(npcPhys);
    }

    public void update(float elapsedTime) {
        if (!isNPCActive) return;
        if (ball == null || npcPaddle == null) return;

        Vector3f playerPos = game.getPlayerPosition();
        Vector3f npcPos = npcPaddle.getWorldLocation();

        // Smoothly move NPC toward mirrored player X
        float npcX = npcPos.x();
        float targetX = -playerPos.x();
        float dx = targetX - npcX;
        float move = npcSpeed * elapsedTime / 1000.0f;

        if (Math.abs(dx) > move) {
            npcX += move * Math.signum(dx);
        } else {
            npcX = targetX;
        }

        npcPaddle.setLocalTranslation(new Matrix4f().translation(npcX, playerPos.y(), -3f));

        if (npcPaddle.getPhysicsObject() != null) {
            double[] npcTransform = toDoubleArray(new Matrix4f(npcPaddle.getLocalTranslation()).get(new float[16]));
            npcPaddle.getPhysicsObject().setTransform(npcTransform);

            float[] vel = npcPaddle.getPhysicsObject().getLinearVelocity();
            vel[0] = vel[1] = vel[2] = 0f;
            npcPaddle.getPhysicsObject().setLinearVelocity(vel);
        }

        // Ball reset if out of bounds
        float ballX = ball.getWorldLocation().x();
        System.out.println("ballX: " + ballX);
        if (ballX > 7f || ballX < -7f) {
            resetBall();
        }

        // Always sync ball physics every frame
        syncBallPhysics();
    }

    private void resetBall() {
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, -3f));

        ballPhys = ball.getPhysicsObject();
        if (ballPhys != null) {
            double[] ballResetTransform = toDoubleArray(new Matrix4f().translation(0f, 0.5f, -3f).get(new float[16]));
            ballPhys.setTransform(ballResetTransform);

            float resetX = (Math.random() > 0.5 ? 3f : -3f);
            float resetY;
            do {
                resetY = (float)(Math.random() * 2f - 1f);
            } while (Math.abs(resetY) < 0.3f);

            ballPhys.setLinearVelocity(new float[]{resetX, resetY, 0f});
        }
    }

    private void syncBallPhysics() {
        if (ball == null || ball.getPhysicsObject() == null) return;

        double[] transform = ball.getPhysicsObject().getTransform();

        ball.getPhysicsObject().setTransform(transform);

        float[] velocity = ball.getPhysicsObject().getLinearVelocity();
        ball.getPhysicsObject().setLinearVelocity(velocity);

        float[] angularVelocity = ball.getPhysicsObject().getAngularVelocity();
        angularVelocity[0] = angularVelocity[1] = angularVelocity[2] = 0f;
        ball.getPhysicsObject().setAngularVelocity(angularVelocity);

        Matrix4f fullMatrix = new Matrix4f().set(toFloatArray(transform));
        Vector3f translation = new Vector3f();
        fullMatrix.getTranslation(translation);

        ball.setLocalTranslation(new Matrix4f().translation(translation));
    }

    public void setIsNPCActive(boolean active) {
        isNPCActive = active;
    }

    private double[] toDoubleArray(float[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    private float[] toFloatArray(double[] array) {
        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (float) array[i];
        }
        return result;
    }

    public GameObject getBall() { return ball; }
}
