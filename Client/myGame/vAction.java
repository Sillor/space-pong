package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class vAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private float directionMultiplier;

	public vAction(MyGame g, boolean up)
	{
		game = g;
		directionMultiplier = up ? 1.0f : -1.0f; // +1 for up, -1 for down
	}

	@Override
	public void performAction(float time, Event e)
	{
		av = game.getAvatar();

		if (av != null && av.getPhysicsObject() != null)
		{
			// Apply a small force upward or downward along Y-axis
			float forceAmount = 50.0f * directionMultiplier;
			av.getPhysicsObject().applyForce(
					0f, forceAmount, 0f,
					0f, 0f, 0f
			);
		}
	}
}
