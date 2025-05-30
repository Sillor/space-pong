package tage;
import tage.Light.*;
import java.nio.*;
import java.util.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

/**
* Manages storage of Lights to facilitate sending them to shaders.
* <p>
* Handling an arbitrary number of lights is complicated because all lights need to be sent to
* each shader invocation, each time an object is rendered.  This is facilitated using an OpenGL SSBO
* (Shader Storage Buffer Object). Converting the array of light objects to an SSBO requires
* first converting the array of Light objects to a float array, then to a java direct FloatBuffer,
* and then finally to an SSBO.
* <p>
* This class manages all four of those storages.
* Each time a light is modified, added, or removed, the FloatBuffer needs to be rebuilt.
* At each frame, the FloatBuffer is reloaded into the SSBO.
* <p>
* Although a few accessors have been made public, there shouldn't be any reason for a game
* application to interact with the LightManager.  A game application should instatiate and
* modify Light objects directly, and insert them into the game via the SceneGraph addLight() method.
* @author Scott Gordon
*/
public class LightManager
{	private GLCanvas myCanvas;
	private Engine engine;

	private ArrayList<Light> lights = new ArrayList<Light>();
	private float[] lightArray;
	private FloatBuffer lightBuf;
	private int[] lightSSBO = new int[1];
	private boolean hasChanged = false;

	private int fieldsPerLight = 23;

	protected LightManager(Engine e)
	{	engine = e;
	}
	
	// accessors for the flag that indicates if any light has been added, removed, or changed.
	
	protected boolean getHasChanged() { return hasChanged; }
	protected void setHasChanged() { hasChanged = true; }

	// Adds a light object to the scene.
	// Checks whether the light object has already been added.
	
	protected void addLight(Light light)
	{	if (light.getIndex() != -1)
			System.out.println("This light is already installed.");
		else
		{	light.setIndex(lights.size());
			lights.add(light);
			hasChanged = true;
	}	}
	
	// Removes a light object from the scene.
	// Does not delete the Light object itself.
	// Checks whether the light is in the scene, if it is not, it will not try to delete it.
	// Also does not delete the light if it is the only light in the scene.
	
	protected void removeLight(Light light)
	{	int lightRef = light.getIndex();
		if ((lights.size() == 1) && ((lights.get(0)) == light))
			System.out.println("There must be at least one light object. Consider disabling the light.");
		else
		{	if (lightRef == -1)
				System.out.println("This light object has not been installed, so cannot delete it.");
			else
			{	lights.set(lightRef, lights.get(lights.size()-1));
				(lights.get(lightRef)).setIndex(lightRef);
				lights.remove(lights.size()-1);
				light.setIndex(-1);
				hasChanged = true;
	}	}	}

	/** returns a reference to the ith Light - not likely to be useful in the game application. */
	public Light getLight(int i) { return lights.get(i); }

	/** returns the number of lights currently in the game */
	public int getNumLights() { return lights.size(); }

	/** Used by the renderer - not likely to be useful in the game application. */
	public int getFieldsPerLight() { return fieldsPerLight; }

	protected FloatBuffer getLightBuffer() { return lightBuf; }
	protected float[] getLightArray() { return lightArray; }

	/** for engine use only, returns a reference to the SSBO containing the data for all of the lights  */
	public int getLightSSBO() { return lightSSBO[0]; }

	// Updates the SSBO for each frame.
	// Called from display() and init().

	protected void updateSSBO()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, lightSSBO[0]);
		gl.glBufferData(GL_SHADER_STORAGE_BUFFER, lightBuf.limit()*4, lightBuf, GL_STATIC_DRAW);
	}

	// Loads the elements of the ArrayList of lights into the float lightArray.
	// It then loads the corresponding float buffer.
	// It is called once at the beginning (from init), then whenever lights are added, removed, or changed.
	// During rendering, updating the SSBO is done from display().

	protected void loadLightArraySSBO()
	{	int numLights = getNumLights();
		lightArray = new float[numLights*fieldsPerLight];
		for (int i=0; i < numLights; i++)
		{	lightArray[i*fieldsPerLight + 0] = (getLight(i).getLocation())[0];
			lightArray[i*fieldsPerLight + 1] = (getLight(i).getLocation())[1];
			lightArray[i*fieldsPerLight + 2] = (getLight(i).getLocation())[2];
			lightArray[i*fieldsPerLight + 3] = (getLight(i).getAmbient())[0];
			lightArray[i*fieldsPerLight + 4] = (getLight(i).getAmbient())[1];
			lightArray[i*fieldsPerLight + 5] = (getLight(i).getAmbient())[2];
			lightArray[i*fieldsPerLight + 6] = (getLight(i).getDiffuse())[0];
			lightArray[i*fieldsPerLight + 7] = (getLight(i).getDiffuse())[1];
			lightArray[i*fieldsPerLight + 8] = (getLight(i).getDiffuse())[2];
			lightArray[i*fieldsPerLight + 9] = (getLight(i).getSpecular())[0];
			lightArray[i*fieldsPerLight + 10] = (getLight(i).getSpecular())[1];
			lightArray[i*fieldsPerLight + 11] = (getLight(i).getSpecular())[2];
			lightArray[i*fieldsPerLight + 12] = (getLight(i).getConstantAttenuation());
			lightArray[i*fieldsPerLight + 13] = (getLight(i).getLinearAttenuation());
			lightArray[i*fieldsPerLight + 14] = (getLight(i).getQuadraticAttenuation());
			lightArray[i*fieldsPerLight + 15] = (getLight(i).getRange());
			lightArray[i*fieldsPerLight + 16] = (getLight(i).getDirection())[0];
			lightArray[i*fieldsPerLight + 17] = (getLight(i).getDirection())[1];
			lightArray[i*fieldsPerLight + 18] = (getLight(i).getDirection())[2];
			lightArray[i*fieldsPerLight + 19] = (getLight(i).getCutoffAngle());
			lightArray[i*fieldsPerLight + 20] = (getLight(i).getOffAxisExponent());
			float type;
			LightType lightType = getLight(i).getLightType();
			if (lightType == LightType.POSITIONAL) type = 0.0f; else type = 1.0f;
			lightArray[i*fieldsPerLight + 21] = type;
			boolean enabled = getLight(i).isEnabled();
			float e = 1.0f; if (!enabled) e = 0.0f;
			lightArray[i*fieldsPerLight + 22] = e;
		}
		lightBuf = Buffers.newDirectFloatBuffer(lightArray);
		updateSSBO();
		hasChanged = false;
	}

	// This function is called once, from init() in the renderer.
	// It starts by generating the SSBO that will contain the lights.
	// It then calls functions that load information from the Light array into the FloatArray,
	// then from the FloatArray into the FloatBuffer, and finally into the SSBO.

	protected void loadLightsSSBOinitial()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glGenBuffers(1, lightSSBO, 0);
		loadLightArraySSBO();
	}
}