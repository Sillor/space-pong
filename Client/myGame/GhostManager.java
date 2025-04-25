package myGame;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;
import tage.physics.*;

import org.joml.Math;
import tage.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private float[] vals = new float[16];

	public GhostManager(VariableFrameRateGame vfrg)
	{
		game = (MyGame)vfrg;
	}

	public void createGhostAvatar(UUID id, Vector3f position) throws IOException
	{
		System.out.println("adding ghost with ID --> " + id);

		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position);

		// Set visual size
		Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		newAvatar.setLocalScale(initialScale);

		// Add ghost to list
		ghostAvatars.add(newAvatar);
		updateGhostAvatar(id, position);

		PhysicsObject ghostPhys = createGhostPhysics(newAvatar);
		newAvatar.setPhysicsObject(ghostPhys);
	}

	private PhysicsObject createGhostPhysics(GhostAvatar ghost)
	{
		SceneGraph sg = game.getEngine().getSceneGraph();
		Matrix4f ghostTrans = new Matrix4f(ghost.getLocalTranslation());
		double[] ghostTransform = toDoubleArray(ghostTrans.get(vals));

		float mass = 1.0f; // Mass > 0 = dynamic
		float[] halfExtents = {0.25f, 0.25f, 0.25f}; // Size matches scale

		PhysicsObject physObj = sg.addPhysicsBox(
				mass,
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
		if(ghostAvatar != null)
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
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{
			ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{
				return ghostAvatar;
			}
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			float y = position.y;

			double[] transform = ghostAvatar.getPhysicsObject().getTransform();

			if (!game.getLeftSide())
			{
				// --- Lock ghost to left ---
				ghostAvatar.setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(90)));
				ghostAvatar.setLocalTranslation(new Matrix4f().translation(-5f, y, 0f));

				transform[0] = 1; transform[1] = 0; transform[2] = 0;
				transform[4] = 0; transform[5] = 1; transform[6] = 0;
				transform[8] = 0; transform[9] = 0; transform[10] = 1;

				transform[12] = -5.0;
				transform[13] = y;
				transform[14] = 0.0;

				ghostAvatar.getPhysicsObject().setTransform(transform);
			}
			else
			{
				// --- Lock ghost to right ---
				ghostAvatar.setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(-90)));
				ghostAvatar.setLocalTranslation(new Matrix4f().translation(5f, y, 0f));

				transform[0] = 1; transform[1] = 0; transform[2] = 0;
				transform[4] = 0; transform[5] = 1; transform[6] = 0;
				transform[8] = 0; transform[9] = 0; transform[10] = 1;

				transform[12] = 5.0;
				transform[13] = y;
				transform[14] = 0.0;

				ghostAvatar.getPhysicsObject().setTransform(transform);
			}
		}
	}


	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	// Helper to convert float[16] -> double[16]
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

