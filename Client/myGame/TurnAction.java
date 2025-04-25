package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private Vector4f oldUp;
	private Matrix4f rotAroundAvatarUp, oldRotation, newRotation;

	public TurnAction(MyGame g)
	{	game = g;
	}

	@Override
	public void performAction(float time, Event e)
	{	float keyValue = e.getValue();
		if (keyValue > -.2 && keyValue < .2) return;  // deadzone

		av = game.getAvatar();
		oldRotation = new Matrix4f(av.getWorldRotation());
		oldUp = new Vector4f(0f,1f,0f,1f).mul(oldRotation);
		rotAroundAvatarUp = new Matrix4f().rotation(-.005f, new Vector3f(oldUp.x(), oldUp.y(), oldUp.z()));
		newRotation = oldRotation;
		newRotation.mul(rotAroundAvatarUp);
		av.setLocalRotation(newRotation);
	}
}


