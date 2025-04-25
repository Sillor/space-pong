package tage;
import java.util.*;
import tage.shapes.*;
import tage.nodeControllers.*;
import tage.physics.*;
import org.joml.*;

/**
* Tools for building a scene graph tree, and building and applying the associated node controllers.
* A game application should use the tools here for adding game objects, lights, and node controllers.
* The game objects and node controllers are stored in ArrayLists.
* The renderer also uses methods here for applying node controllers at each frame.
* <p>
* The functions here that are useful for the game application are:
* <ul>
* <li> addLight()
* <li> removeLight()
* <li> addNodeController()
* <li> getRoot()
* <li> loadCubeMap()
* <li> removeGameObject()
* <li> getNumGameObjects()
* <li> addPhysicsXXX() - where XXX is the desired physics object shape
* <li> removePhysicsObject()
* </ul>
* <p>
* It is important to understand that adding a game object doesn't require calling addGameObject().
* That function is called by the engine.  All that is necessary is to use one of the GameObject constructors.
* They will call addGameObject().  Similarly it isn't necessary to call buildSkyBox(), that happens automatically.
* However, adding a Light or a Node Controller does necessitate calling addLight() and addNodeController().
* <p>
* Loading a skybox texture into an OpenGL CubeMap using loadCubeMap() is done as follows.
* The game application needs to supply the name of a folder containing the 6 textures.
* The folder is assumed to be in the assets/skyboxes folder.
* The resulting OpenGL CubeMap is referenced by the returned integer.
* The game application should store that integer to refer to the cubemap if it wishes to swap
* between multiple cubemaps.  That can be done by calling setActiveSkyBoxTexture() with the integer
* skybox reference provided as a parameter. All cubemaps should be loaded before starting the game loop.
* @author Scott Gordon
*/

public class SceneGraph
{	private static GameObject root;
	private ArrayList<GameObject> gameObjects = new ArrayList<GameObject>();
	private ArrayList<NodeController> nodeControllers = new ArrayList<NodeController>();
	private Vector<GameObject> physicsRenderables = new Vector<GameObject>();
	private Engine engine;

	private RenderSystem rs;
	private PhysicsEngine pe;
	private PhysicsObject po;
	private GameObject go;
	private NodeController nc, nci;
	private GameObject skybox;
	private boolean skyboxEnabled = false;
	private int activeSkyBoxTexture;
	
	private GameObject physicsRoot;
	private ObjShape physicsBox, physicsSphere, physicsCylinder, physicsCone, physicsCapsule, physicsPlane;
	private float halfExtents[] = new float[3];

	protected SceneGraph(Engine e)
	{	engine = e;
		root = GameObject.createRoot();
		preparePhysicsDisplayObjects();
	}

	// -------------- LIGHT SECTION ------------------------

	/** adds the specified Light object to the LightManager for rendering. */
	public void addLight(Light light) { (engine.getLightManager()).addLight(light); }
	
	/** removes the specified Light object, using the LightManager, if the light is installed, but doesn't delete the Light object. */
	public void removeLight(Light light) { (engine.getLightManager()).removeLight(light); }

	// -------------- NODE CONTROLLER SECTION -------------------

	/** adds the specified node controller for use in the game. */
	public void addNodeController(NodeController nc) { nodeControllers.add(nc); }

	// Apply the node controllers to their attached objects - for engine use only.
	// Called by RenderSystem, should not be called by the game application directly.

	protected void applyNodeControllers()
	{	for (int i = 0; i < nodeControllers.size(); i++)
		{	nci = nodeControllers.get(i);
			if (nci.isEnabled()) nci.applyController();
		}
	}

	// -------------- GAME OBJECT SECTION ---------------------

	/** returns the current number of GameObjects. */
	public int getNumGameObjects() { return gameObjects.size(); }

	/** returns a reference to the entire ArrayList of GameObjects - not likely to be useful to the game application. */
	public ArrayList<GameObject> getGameObjects() { return gameObjects; }

	protected GameObject getGameObject(int i) { return gameObjects.get(i); }
	protected GameObject getRoot() { return root; }
	protected void updateAllObjectTransforms() { root.update(); }

	/** removes the specified GameObject from the scenegraph. */
	public void removeGameObject(GameObject go)
	{	if (go.hasChildren())
		{	System.out.println("attempted deletion of game object with children");
		}
		else
		{	// first delete any NodeController references to the game object
			for (int i = 0; i < nodeControllers.size(); i++)
			{	nci = nodeControllers.get(i);
				if (nci.hasTarget(go)) nci.removeTarget(go);
			}
			// then remove the object, also removing the parent reference
			if (go.getParent() != null) (go.getParent()).removeChild(go);
			if (gameObjects.contains(go)) gameObjects.remove(go);
		}
	}

	protected void addGameObject(GameObject g) { gameObjects.add(g); }

	//------------- SKYBOX SECTION ---------------------

	/** loads a set of six skybox images into an OpenGL cubemap so that it can be used in an OpenGL skybox. */
	public int loadCubeMap(String foldername)
	{	int skyboxTexture = Utils.loadCubeMap("assets/skyboxes/"+foldername);
		return skyboxTexture;
	}

	/** returns a boolean that is true if skybox rendering has been enabled */
	public boolean isSkyboxEnabled() { return skyboxEnabled; }

	/** sets whether or not to render a skybox */
	public void setSkyBoxEnabled(boolean sbe) { skyboxEnabled = sbe; }

	/** specifies which loaded skybox should be rendered */
	public void setActiveSkyBoxTexture(int tex) { activeSkyBoxTexture = tex; }

	/** returns an integer reference to the current active skybox */
	public int getActiveSkyBoxTexture() { return activeSkyBoxTexture; }

	protected GameObject getSkyBoxObject() { return skybox; }

	protected void buildSkyBox()
	{	skybox = new GameObject(new SkyBoxShape());
	}

	//------------------- PHYSICS SECTION -----------------------------

	protected void setPhysicsEngine(PhysicsEngine peng) { pe = peng; }

	/** returns the physics engine object */
	public PhysicsEngine getPhysicsEngine() { return pe; }
	
	// returns the GameObjects that are physics renderables - used by the renderer.
	protected Vector<GameObject> getPhysicsRenderables() { return physicsRenderables; }
	
	// prepares the physics objects and shapes for displaying the physics world if enabled
	protected void preparePhysicsDisplayObjects()
	{	physicsRoot = new GameObject();
		physicsBox = new Cube();
		physicsSphere = new Sphere(8);
		physicsCone = new ImportedModel("cone.obj", "assets/defaultAssets/");
		physicsCylinder = new ImportedModel("cylinder.obj", "assets/defaultAssets/");
		physicsCapsule = new ImportedModel("capsule.obj", "assets/defaultAssets/");
		physicsPlane = new ImportedModel("plane.obj", "assets/defaultAssets/");
	}		
	
	// sets relevant parameters for physics object and corresponding renderable (if physics visualization enabled).
	protected void setPhysicsObjectParams(ObjShape shape, Matrix4f m)
	{	go = new GameObject();
		go.setShape(shape);
		go.setFirstParent(physicsRoot);
		go.setLocalScale(m);
		go.getRenderStates().setWireframe(true);
		go.setPhysicsObject(po);
		physicsRenderables.add(go);
	}

	/** Adds a box physics object in the physics world, with specified mass, matrix transform, and size.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*  The size is specified using a 3-element array of type double, with desired dimensions in X, Y, and Z.<br>
	*/
	public PhysicsObject addPhysicsBox(float mass, double[] transform, float[] size)
	{	po = pe.addBoxObject(pe.nextUID(), mass, transform, size);
		setPhysicsObjectParams(physicsBox, (new Matrix4f()).scaling(size[0]/2f, size[1]/2f, size[2]/2f));
		return po;
	}
	
	/** Adds a sphere physics object in the physics world, with specified mass, matrix transform, and radius.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsSphere(float mass, double[] transform, float radius)
	{	po = pe.addSphereObject(pe.nextUID(), mass, transform, radius);
		setPhysicsObjectParams(physicsSphere, (new Matrix4f()).scaling(radius));
		return po;
	}

	/** Adds a cone physics object (pointing upward in +Y direction) in the physics world, with specified mass, matrix transform, and radius.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCone(float mass, double[] transform, float radius, float height)
	{	po = pe.addConeObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCone, (new Matrix4f()).scaling(radius, height/2f, radius));
		return po;
	}
	
	/** Adds a cone physics object (pointing right in +X direction) in the physics world, with specified mass, matrix transform, and radius.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsConeX(float mass, double[] transform, float radius, float height)
	{	po = pe.addConeXObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCone, (new Matrix4f()).scaling(radius, height/2f, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		return po;
	}
	
	/** Adds a cone physics object (pointing forward in +Z direction) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsConeZ(float mass, double[] transform, float radius, float height)
	{	po = pe.addConeZObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCone, (new Matrix4f()).scaling(radius, height/2f, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationX((float)java.lang.Math.toRadians(90.0f)));
		return po;
	}

	/** Adds a capsule physics object (oriented vertically on Y axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  A capsule has cylindrical sides, but with rounded semi-spherical ends.<br>
	*  The dimensions of a capsule are specified with radius and height, where radius is for the semi-spheres on each end, and height is the connecting cylinder.<br>
	*  The total length of the capsule is height + 2 * radius.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCapsule(float mass, double[] transform, float radius, float height)
	{	po = pe.addCapsuleObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCapsule, (new Matrix4f()).scaling(radius, (height+radius*2f)/4f, radius));
		return po;
	}

	/** Adds a capsule physics object (oriented horizontally on X axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  A capsule has cylindrical sides, but with rounded semi-spherical ends.<br>
	*  The dimensions of a capsule are specified with radius and height, where radius is for the semi-spheres on each end, and height is the connecting cylinder.<br>
	*  The total length of the capsule is height + 2 * radius.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCapsuleX(float mass, double[] transform, float radius, float height)
	{	po = pe.addCapsuleXObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCapsule, (new Matrix4f()).scaling(radius, (height+radius*2f)/4f, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		return po;
	}

	/** Adds a capsule physics object (oriented horizontally on Z axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  A capsule has cylindrical sides, but with rounded semi-spherical ends.<br>
	*  The dimensions of a capsule are specified with radius and height, where radius is for the semi-spheres on each end, and height is the connecting cylinder.<br>
	*  The total length of the capsule is height + 2 * radius.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCapsuleZ(float mass, double[] transform, float radius, float height)
	{	po = pe.addCapsuleZObject(pe.nextUID(), mass, transform, radius, height);
		setPhysicsObjectParams(physicsCapsule, (new Matrix4f()).scaling(radius, (height+radius*2f)/4f, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationX((float)java.lang.Math.toRadians(90.0f)));
		return po;
	}

	/** Adds a cylinder physics object (oriented vertically on Y axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  The dimensions of a cylinder are specified with radius and height.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCylinder(float mass, double[] transform, float radius, float height)
	{	halfExtents[0] = radius; halfExtents[1] = height; halfExtents[2] = radius;
		po = pe.addCylinderObject(pe.nextUID(), mass, transform, halfExtents);
		setPhysicsObjectParams(physicsCylinder, (new Matrix4f()).scaling(radius, height, radius));
		return po;
	}

	/** Adds a cylinder physics object (oriented horizontally on X axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  The dimensions of a cylinder are specified with radius and height.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCylinderX(float mass, double[] transform, float radius, float height)
	{	halfExtents[0] = height; halfExtents[1] = radius; halfExtents[2] = radius;
		po = pe.addCylinderXObject(pe.nextUID(), mass, transform, halfExtents);
		setPhysicsObjectParams(physicsCylinder, (new Matrix4f()).scaling(radius, height, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationZ((float)java.lang.Math.toRadians(270.0f)));
		return po;
	}

	/** Adds a cylinder physics object (oriented horizontally on Z axis) in the physics world, with specified mass, matrix transform, radius, and height.
	*  <br>
	*  The dimensions of a cylinder are specified with radius and height.<br>
	*  The transform is a 4x4 homogeneous matrix, stored in an array of type double, which should only contain translation and/or rotation.<br>
	*/
	public PhysicsObject addPhysicsCylinderZ(float mass, double[] transform, float radius, float height)
	{	halfExtents[0] = radius; halfExtents[1] = radius; halfExtents[2] = height;
		po = pe.addCylinderZObject(pe.nextUID(), mass, transform, halfExtents);
		setPhysicsObjectParams(physicsCylinder, (new Matrix4f()).scaling(radius, height, radius));
		go.getRenderStates().setModelOrientationCorrection(
		   (new Matrix4f()).rotationX((float)java.lang.Math.toRadians(90.0f)));
		return po;
	}

	/** Adds a a static plane object in the physics world, with specified up vector and location offset.
	*  <br>
	*  The up vector, usually set to (0,1,0), defines a normal to the plane, and is opposite the pull of gravity.<br>
	*  The plane_constant, usually set to 0, is an offset (positive or negative) for moving the plane up and down along its up vector.<br>
	*  Also adds a corresponding renderable object if displaying the physics world has been enabled.<br>
	*  The renderable object is a 100x100 rectangle, although the actual physics object is infinite.
	*/
	public PhysicsObject addPhysicsStaticPlane(double[] transform, float[] up_vector, float plane_constant)
	{	po = pe.addStaticPlaneObject(pe.nextUID(), transform, up_vector, plane_constant);
		setPhysicsObjectParams(physicsPlane, (new Matrix4f()).scaling(100));
		return po;
	}
	
	/** removes the specified PhysicsObject from the physics world, and its associated renderable, if one exists. */
	public void removePhysicsObject(PhysicsObject po)
	{	GameObject toRemove = null;
	
		// look for it in the renderables queue
		for (int i = 0; i < physicsRenderables.size(); i++)
		{	GameObject go = physicsRenderables.get(i);
			if (go.getPhysicsObject() == po) toRemove = go;
		}
		// if it is found in the renderables queue, remove it from the root list and the renderables list
		if (toRemove != null)
		{	if (toRemove.getParent() != null) (toRemove.getParent()).removeChild(toRemove);
			physicsRenderables.remove(toRemove);
		}
		// finally, tell the physics engine to remove the physics object itself
		pe.removeObject(po.getUID());
	}
}









