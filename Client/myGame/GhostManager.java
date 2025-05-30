package myGame;

import java.io.IOException;
import java.lang.Math;
import java.util.*;

import org.joml.*;
import tage.*;
import tage.physics.*;
import tage.shapes.AnimatedShape;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<>();
	private Map<UUID, Integer> playerNumbers = new HashMap<>();
	private float[] vals = new float[16];

	public GhostManager(VariableFrameRateGame vfrg) {
		game = (MyGame) vfrg;
	}

	public void createGhostAvatar(UUID id, Vector3f position, int playerNumber) throws IOException {
		AnimatedShape shape = game.getPaddleS_2();
		shape.loadAnimation("Bounce", "bounce.rka");
		TextureImage texture = game.getGhostTexture();

		playerNumbers.put(id, playerNumber);

		Vector3f correctedPosition = new Vector3f(position);
		correctedPosition.x = (playerNumber == 0) ? -5f : 5f;
		correctedPosition.z = -3f;

		GhostAvatar newAvatar = new GhostAvatar(id, shape, texture, correctedPosition, false);
		newAvatar.setLocalScale(new Matrix4f().scaling(MyGame.PADDLE_SCALE));

		float x = correctedPosition.x;
		newAvatar.setLocalRotation(
				x < 0
						? new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(90))
						: new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(-90))
		);

		ghostAvatars.add(newAvatar);

		PhysicsObject ghostPhysics = createGhostPhysics(newAvatar);
		newAvatar.setPhysicsObject(ghostPhysics);
	}

	private PhysicsObject createGhostPhysics(GhostAvatar ghost) {
		SceneGraph sceneGraph = game.getEngine().getSceneGraph();
		Matrix4f ghostTransform = new Matrix4f(ghost.getLocalTranslation());
		double[] transformArray = toDoubleArray(ghostTransform.get(vals));

		float mass = 0.0f;
		float[] halfExtents = {0.5f, 2f, 0.5f};

		PhysicsObject physicsObject = sceneGraph.addPhysicsBox(mass, transformArray, halfExtents);
		physicsObject.setBounciness(0.5f);
		physicsObject.setFriction(0.8f);

		return physicsObject;
	}

	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghost = findAvatar(id);
		if (ghost != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghost);
			ghostAvatars.remove(ghost);
			playerNumbers.remove(id);
		}
	}

	public GhostAvatar findAvatar(UUID id) {
		for (GhostAvatar avatar : ghostAvatars) {
			if (avatar.getID().equals(id)) return avatar;
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghost = findAvatar(id);
		if (ghost != null && ghost.getPhysicsObject() != null) {
			double[] transform = ghost.getPhysicsObject().getTransform();
			transform[13] = position.y;
			ghost.getPhysicsObject().setTransform(transform);

			Vector3f currentPos = ghost.getLocalTranslation().getTranslation(new Vector3f());
			float x = currentPos.x;
			ghost.setLocalRotation(
					x < 0
							? new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(90))
							: new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(-90))
			);
		}
	}

	public int getPlayerNumber(UUID id) {
		return playerNumbers.getOrDefault(id, 0);
	}

	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	private double[] toDoubleArray(float[] array) {
		double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = array[i];
		}
		return result;
	}
}
