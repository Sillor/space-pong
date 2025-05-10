package myGame.core;

import org.joml.*;
import myGame.networking.GhostAvatar;
import tage.GameObject;
import tage.physics.PhysicsObject;

public class AvatarPhysicsSynchronizer {
    private AvatarPhysicsSynchronizer() {}

    public static void syncAvatarPhysics(MyGame game) {
        GameObject avatar = game.getAvatar();
        PhysicsObject physics = avatar.getPhysicsObject();

        double[] transform = physics.getTransform();
        transform[12] = game.getLockedX();
        transform[14] = game.getLockedZ();
        physics.setTransform(transform);

        float[] velocity = physics.getLinearVelocity();
        velocity[0] = velocity[2] = 0f;
        physics.setLinearVelocity(velocity);

        float[] angularVelocity = physics.getAngularVelocity();
        angularVelocity[0] = angularVelocity[1] = angularVelocity[2] = 0f;
        physics.setAngularVelocity(angularVelocity);

        Matrix4f matrix = new Matrix4f().set(Utility.toFloatArray(physics.getTransform()));
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);
        avatar.setLocalTranslation(new Matrix4f().translation(translation));
    }

    public static void syncGhostPhysics(GhostAvatar ghost, float lockedX, float lockedZ) {
        PhysicsObject physics = ghost.getPhysicsObject();
        double[] transform = physics.getTransform();
        transform[12] = lockedX;
        transform[14] = lockedZ;
        physics.setTransform(transform);

        float[] velocity = physics.getLinearVelocity();
        velocity[0] = velocity[2] = 0f;
        physics.setLinearVelocity(velocity);

        float[] angularVelocity = physics.getAngularVelocity();
        angularVelocity[0] = angularVelocity[1] = angularVelocity[2] = 0f;
        physics.setAngularVelocity(angularVelocity);

        Matrix4f matrix = new Matrix4f().set(Utility.toFloatArray(physics.getTransform()));
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);
        ghost.setLocalTranslation(new Matrix4f().translation(translation));
    }

    public static Vector3f transformVector(Vector3f vector, Matrix4f matrix) {
        Vector4f result = new Vector4f(vector, 0).mul(matrix);
        return new Vector3f(result.x, result.y, result.z);
    }
}