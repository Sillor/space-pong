package myGame.networking;

import java.io.IOException;
import java.util.*;

import myGame.core.MyGame;
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
		AnimatedShape s = game.getPaddleS_2();
		s.loadAnimation("Bounce", "bounce.rka");

		TextureImage t = game.getGhostTexture();

		playerNumbers.put(id, playerNumber);
		boolean leftSide = (playerNumber == 0);

		Vector3f correctedPosition = new Vector3f(position);
		correctedPosition.x = leftSide ? -5f : 5f;
		correctedPosition.z = -3f;


		GhostAvatar newAvatar = new GhostAvatar(id, s, t, correctedPosition, leftSide);
		newAvatar.setLocalScale(new Matrix4f().scaling(MyGame.PADDLE_SCALE));
		newAvatar.setLocalRotation(game.getAvatarOriginalRotation());
		ghostAvatars.add(newAvatar);

		PhysicsObject ghostPhys = createGhostPhysics(newAvatar);
		newAvatar.setPhysicsObject(ghostPhys);
	}

	private PhysicsObject createGhostPhysics(GhostAvatar ghost) {
		SceneGraph sg = game.getEngine().getSceneGraph();
		Matrix4f ghostTrans = new Matrix4f(ghost.getLocalTranslation());
		double[] ghostTransform = toDoubleArray(ghostTrans.get(vals));

		float mass = 0.0f;
		float[] halfExtents = {0.5f, 2f, 0.5f};

		PhysicsObject physObj = sg.addPhysicsBox(mass, ghostTransform, halfExtents);
		physObj.setBounciness(0.5f);
		physObj.setFriction(0.8f);

		return physObj;
	}

	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
			playerNumbers.remove(id);
		}
	}

	public GhostAvatar findAvatar(UUID id) {
		for (GhostAvatar ghostAvatar : ghostAvatars) {
			if (ghostAvatar.getID().compareTo(id) == 0) return ghostAvatar;
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null && ghostAvatar.getPhysicsObject() != null) {
			double[] transform = ghostAvatar.getPhysicsObject().getTransform();
			transform[13] = position.y;
			ghostAvatar.getPhysicsObject().setTransform(transform);
		}
	}

	public int getPlayerNumber(UUID id) {
		return playerNumbers.getOrDefault(id, 0);
	}

	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	private double[] toDoubleArray(float[] arr) {
		double[] ret = new double[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}
}