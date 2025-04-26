package myGame;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;
import tage.*;
import tage.physics.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private float[] vals = new float[16];

	public GhostManager(VariableFrameRateGame vfrg)
	{
		game = (MyGame)vfrg;
	}

	public void createGhostAvatar(UUID id, Vector3f position, boolean leftSide) throws IOException
	{
		System.out.println("adding ghost with ID --> " + id);

		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position, leftSide);

		// Set visual size
		Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		newAvatar.setLocalScale(initialScale);

		// Add ghost to list
		ghostAvatars.add(newAvatar);

		// --- Create PhysicsObject now ---
		PhysicsObject ghostPhys = createGhostPhysics(newAvatar);
		newAvatar.setPhysicsObject(ghostPhys);
	}

	private PhysicsObject createGhostPhysics(GhostAvatar ghost)
	{
		SceneGraph sg = game.getEngine().getSceneGraph();
		Matrix4f ghostTrans = new Matrix4f(ghost.getLocalTranslation());
		double[] ghostTransform = toDoubleArray(ghostTrans.get(vals));

		float mass = 0.0f; // Mass 0 = STATIC / KINEMATIC

		float[] halfExtents = {0.5f, 2f, 0.5f}; // Size matches ghost

		PhysicsObject physObj = sg.addPhysicsBox(
				mass,          // <-- 0 mass means not affected by gravity
				ghostTransform,
				halfExtents
		);

		physObj.setBounciness(0.5f);
		physObj.setFriction(0.8f);

		return physObj;
	}

	public void removeGhostAvatar(UUID id)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{
			System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{
		for (GhostAvatar ghostAvatar : ghostAvatars)
		{
			if (ghostAvatar.getID().compareTo(id) == 0)
			{
				return ghostAvatar;
			}
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null && ghostAvatar.getPhysicsObject() != null)
		{
			// Set the new Y position (keep X and Z locked depending on side)
			double[] transform = ghostAvatar.getPhysicsObject().getTransform();
			transform[13] = position.y; // only update Y
			ghostAvatar.getPhysicsObject().setTransform(transform);
		}
	}

	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
}
