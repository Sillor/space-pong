package myGame.networking;

import java.util.UUID;

import myGame.core.MyGame;
import org.joml.Math;
import tage.*;
import org.joml.*;
import tage.audio.Sound;
import tage.physics.PhysicsObject;
import tage.shapes.AnimatedShape;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	private UUID uuid;
	private boolean isLeftSide = true; // which side the ghost belongs
	private PhysicsObject physObj;
	private final AnimatedShape animatedShape;

	public GhostAvatar(UUID id, AnimatedShape s, TextureImage t, Vector3f pos, boolean left) {
		super(GameObject.root(), s, t);
		this.animatedShape = s;
		uuid = id;
		isLeftSide = left;
		setPosition(pos);
	}

	public UUID getID() { return uuid; }
	public void setPosition(Vector3f p) { setLocalLocation(p); }
	public Vector3f getPosition() { return getWorldLocation(); }
	public void setLeftSide(boolean side) { isLeftSide = side; }
	public boolean getLeftSide() { return isLeftSide; }

	public void setPhysicsObject(PhysicsObject obj) { physObj = obj; }
	public PhysicsObject getPhysicsObject() { return physObj; }

	public void playBounceAnimation() {
		if (animatedShape != null) {
			animatedShape.playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);

			if (MyGame.getEngine() != null) {
				MyGame game = (MyGame) MyGame.getEngine().getGame();
				Sound bounce = game.getBounceSound();
				if (bounce != null) {
					float randomPitch = 0.9f + (float)(Math.random()) * 0.2f;
					bounce.setPitch(randomPitch);
					bounce.play();
				}
			}
		}
	}


	public void syncToPhysics()
	{
		if (physObj != null)
		{
			double[] transform = physObj.getTransform();
			float y = (float) transform[13]; // only Y changes

			if (isLeftSide)
			{
				setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(90)));
				setLocalTranslation(new Matrix4f().translation(-5f, y, 0f));
			}
			else
			{
				setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(-90)));
				setLocalTranslation(new Matrix4f().translation(5f, y, 0f));
			}
		}
	}
}

