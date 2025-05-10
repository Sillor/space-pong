package myGame.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;
import tage.physics.PhysicsObject;
import tage.shapes.*;

public class GameBuilder {
    private final MyGame game;

    private static final float AVATAR_SCALE = 0.1f;
    private static final float TERRAIN_Y_OFFSET = -9f;
    private static final float TERRAIN_SCALE = 60f;
    private static final float GROUND_Y_OFFSET = -4f;
    private static final float CEILING_Y_OFFSET = 4f;
    private static final float PLANE_SCALE = 20f;

    public GameBuilder(MyGame game) {
        this.game = game;
    }

    public void buildScene() {
        buildAvatar();
        buildTorus();
        buildAxes();
        buildTerrain();
        buildGroundPlane();
        buildCeilingPlane();
    }

    private void buildAvatar() {
        GameObject avatar = new GameObject(GameObject.root(), game.getPaddleS(), game.getGhostTexture());
        avatar.setLocalScale(new Matrix4f().scaling(AVATAR_SCALE));
        game.setLockedX(-5f);
        game.setLockedZ(-3f);

        avatar.setLocalRotation(new Matrix4f()
                .rotateY((float) Math.toRadians(90))
                .rotateX((float) Math.toRadians(90)));

        PhysicsObject physics = MyGame.getEngine().getSceneGraph().addPhysicsBox(
                1.0f,
                Utility.toDoubleArray(avatar.getLocalTranslation().get(new float[16])),
                new float[]{0.5f, 2.0f, 0.5f}
        );
        avatar.setPhysicsObject(physics);

        game.setAvatar(avatar);
    }

    private void buildTorus() {
        GameObject torus = new GameObject(GameObject.root(), new Torus(0.5f, 0.2f, 48));
        torus.setLocalTranslation(new Matrix4f().translation(1f, 0f, 0f));
        torus.setLocalScale(new Matrix4f().scaling(0.25f));
    }

    private void buildAxes() {
        createAxis(new Vector3f(3, 0, 0), new Vector3f(1, 0, 0)); // X - Red
        createAxis(new Vector3f(0, 3, 0), new Vector3f(0, 1, 0)); // Y - Green
        createAxis(new Vector3f(0, 0, -3), new Vector3f(0, 0, 1)); // Z - Blue
    }

    private void createAxis(Vector3f end, Vector3f color) {
        GameObject axis = new GameObject(GameObject.root(), new Line(new Vector3f(0, 0, 0), end));
        axis.getRenderStates().setColor(color);
    }

    private void buildTerrain() {
        GameObject terrain = new GameObject(GameObject.root(), new TerrainPlane(512), new TextureImage("rocks.jpg"));
        terrain.setLocalTranslation(new Matrix4f().translation(0f, TERRAIN_Y_OFFSET, 0f));
        terrain.setLocalScale(new Matrix4f().scaling(TERRAIN_SCALE, 6f, TERRAIN_SCALE));
        terrain.setHeightMap(new TextureImage("hills.jpg"));
        terrain.getRenderStates().setColor(new Vector3f(0.5f, 0f, 0f));
        terrain.getRenderStates().setTiling(1);
        terrain.getRenderStates().setTileFactor(10);
    }

    private void buildGroundPlane() {
        addPhysicsPlane(GROUND_Y_OFFSET, new float[]{0f, 1f, 0f}, true);
    }

    private void buildCeilingPlane() {
        addPhysicsPlane(CEILING_Y_OFFSET, new float[]{0f, -1f, 0f}, false);
    }

    private void addPhysicsPlane(float yOffset, float[] normal, boolean invisible) {
        GameObject plane = new GameObject(GameObject.root(), new Plane());
        plane.setLocalTranslation(new Matrix4f().translation(0f, yOffset, 0f));
        plane.setLocalScale(new Matrix4f().scaling(PLANE_SCALE));

        PhysicsObject physics = MyGame.getEngine().getSceneGraph().addPhysicsStaticPlane(
                Utility.toDoubleArray(plane.getLocalTranslation().get(new float[16])),
                normal,
                0f
        );
        physics.setBounciness(0.8f);
        plane.setPhysicsObject(physics);

        if (invisible)
            plane.getRenderStates().disableRendering();
    }
}
