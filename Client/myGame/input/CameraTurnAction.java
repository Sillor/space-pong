package myGame.input;

import myGame.core.MyGame;
import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class CameraTurnAction extends AbstractInputAction
{
    private MyGame game;
    private Vector3f rotationAxis; // axis to rotate around (e.g. Y for yaw, X for pitch, Z for roll)
    private float turnSpeed = 0.005f; // radians per input tick

    /**
     * Constructor to set game and axis of rotation.
     * Examples of axis vectors:
     * - new Vector3f(0, 1, 0) for yaw (turn left/right)
     * - new Vector3f(1, 0, 0) for pitch (look up/down)
     * - new Vector3f(0, 0, 1) for roll (tilt head)
     */
    public CameraTurnAction(MyGame g, Vector3f axis)
    {
        game = g;
        rotationAxis = new Vector3f(axis).normalize();
    }

    @Override
    public void performAction(float time, Event e)
    {
        float keyValue = e.getValue();
        if (keyValue > -0.2 && keyValue < 0.2)
            return;

        Camera cam = game.getCamera();

        // Create a rotation matrix to rotate around the given axis in camera's local space
        Matrix4f rotMat = new Matrix4f().rotation(turnSpeed * keyValue, rotationAxis);

        // Get current camera U, V, N vectors
        Vector3f u = cam.getU();
        Vector3f v = cam.getV();
        Vector3f n = cam.getN();

        // Rotate each axis
        Vector4f newU = new Vector4f(u, 0).mul(rotMat);
        Vector4f newV = new Vector4f(v, 0).mul(rotMat);
        Vector4f newN = new Vector4f(n, 0).mul(rotMat);

        // Set the new camera axes
        cam.setU(new Vector3f(newU.x, newU.y, newU.z));
        cam.setV(new Vector3f(newV.x, newV.y, newV.z));
        cam.setN(new Vector3f(newN.x, newN.y, newN.z));
    }
}
