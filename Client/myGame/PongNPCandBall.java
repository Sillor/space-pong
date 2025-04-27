package myGame;

import org.joml.*;
import tage.*;
import tage.input.action.*;
import tage.physics.*;
import tage.shapes.AnimatedShape;
import tage.shapes.Sphere;

import java.lang.Math;

public class PongNPCandBall {
    private MyGame game;
    private GameObject npcPaddle;
    private GameObject ball;
    private float npcSpeed = 3.0f;
    private boolean isNPCActive = true;

    public PongNPCandBall(MyGame g) {
        this.game = g;
        createBall();
        createNPCPaddle();
    }

    private void createBall() {
        ball = new GameObject(GameObject.root(), new Sphere(), null);
        ball.setLocalScale(new Matrix4f().scaling(0.1f));
        ball.setLocalTranslation(new Matrix4f().translation(0f, 0.5f, 0f));

        double[] ballTransform = toDoubleArray(new Matrix4f(ball.getLocalTranslation()).get(new float[16]));
        PhysicsObject ballPhys = game.getEngine().getSceneGraph().addPhysicsSphere(0.1f, ballTransform, 0.1f);
        ballPhys.setBounciness(0.95f);
        ballPhys.setLinearVelocity(new float[]{3f, 0f, 2f});

        ball.setPhysicsObject(ballPhys);
    }

    private void createNPCPaddle() {
        npcPaddle = new GameObject(GameObject.root(), new AnimatedShape("pong.rkm", "pong.rks"), game.getGhostTexture());
        npcPaddle.setLocalScale(new Matrix4f().scaling(0.25f));
        Matrix4f initialRot = new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(90));
        npcPaddle.setLocalRotation(initialRot);
        npcPaddle.setLocalTranslation(new Matrix4f().translation(5f, 0f, -3f));

        double[] npcTransform = toDoubleArray(new Matrix4f(npcPaddle.getLocalTranslation()).get(new float[16]));
        PhysicsObject npcPhys = game.getEngine().getSceneGraph().addPhysicsBox(0, npcTransform, new float[]{0.5f, 2.0f, 0.5f});
        npcPaddle.setPhysicsObject(npcPhys);
    }

    public void update(float elapsedTime) {
        if (!isNPCActive) return;

        if (ball == null || npcPaddle == null) return;

        float ballZ = ball.getWorldLocation().z();
        float npcZ = npcPaddle.getWorldLocation().z();

        float move = 0f;

        if (ballZ > npcZ + 0.2f) move = npcSpeed * elapsedTime;
        else if (ballZ < npcZ - 0.2f) move = -npcSpeed * elapsedTime;

        Vector3f npcPos = npcPaddle.getWorldLocation();
        npcPaddle.setLocalTranslation(new Matrix4f().translation(npcPos.x, npcPos.y, npcPos.z + move));
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

    public GameObject getBall() { return ball; }
}
