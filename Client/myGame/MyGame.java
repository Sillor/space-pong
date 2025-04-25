package myGame;

import java.awt.event.*;
import java.io.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.java.games.input.Component;
import org.joml.*;
import tage.*;
import tage.input.*;
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;
import tage.shapes.*;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private int counter = 0;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z, terr, groundPlane, ceilingPlane;
	private ObjShape torS, ghostS, linxS, linyS, linzS, terrS, paddleS;
	private TextureImage ghostT, paddleT, hills, rocks;
	private Light light;

	private int skybox;
	private boolean isLeftSide = true;

	private PhysicsEngine physicsEngine;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args) {
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes() {
		torS = new Torus(0.5f, 0.2f, 48);
		ghostS = new ImportedModel("paddle.obj");
		paddleS = new ImportedModel("paddle.obj");
		terrS = new TerrainPlane(512);
		linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
		linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
		linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
	}

	@Override
	public void loadTextures() {
		hills = new TextureImage("hills.jpg");
		rocks = new TextureImage("rocks.jpg");
		paddleT = new TextureImage("paddle1.png");
		ghostT = new TextureImage("paddle1.png");
	}

	@Override
	public void buildObjects() {
		Matrix4f initialTranslation, initialScale;

		// build player avatar
		avatar = new GameObject(GameObject.root(), paddleS, paddleT);
		initialScale = (new Matrix4f()).scaling(0.25f);
		avatar.setLocalScale(initialScale);

		// build torus along X axis
		tor = new GameObject(GameObject.root(), torS);
		initialTranslation = (new Matrix4f()).translation(1, 0, 0);
		tor.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.25f);
		tor.setLocalScale(initialScale);

		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
		(y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
		(z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));

		// build terrain object
		terr = new GameObject(GameObject.root(), terrS, rocks);
		initialTranslation = (new Matrix4f()).translation(0f, -9f, 0f);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(60.0f, 6.0f, 60.0f);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(hills);
//		terr.getRenderStates().setWireframe(true);
//		terr.getRenderStates().setHasSolidColor(true);
		terr.getRenderStates().setColor(new Vector3f(0.5f,0,0));

		// set tiling for terrain texture
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(10);

		// build ground plane
		groundPlane = new GameObject(GameObject.root(), new Plane());
		groundPlane.setLocalTranslation(new Matrix4f().translation(0f, -2f, 0f));
		groundPlane.setLocalScale(new Matrix4f().scaling(20f));
		double[] groundTransform = toDoubleArray(
				(new Matrix4f(groundPlane.getLocalTranslation())).get(vals)
		);
		float[] up = {0f, 1f, 0f}; // normal facing up
		PhysicsObject groundPhys = engine.getSceneGraph().addPhysicsStaticPlane(groundTransform, up, 0f);

		groundPhys.setBounciness(0.8f);
		groundPlane.setPhysicsObject(groundPhys);
		groundPlane.getRenderStates().disableRendering();

		// build ceiling plane
		ceilingPlane = new GameObject(GameObject.root(), new Plane());
		ceilingPlane.setLocalTranslation(new Matrix4f().translation(0f, 2f, 0f)); // above the heads
		ceilingPlane.setLocalScale(new Matrix4f().scaling(20f));

		double[] ceilingTransform = toDoubleArray(
				(new Matrix4f(ceilingPlane.getLocalTranslation())).get(vals)
		);
		float[] down = {0f, -1f, 0f}; // normal facing downward
		PhysicsObject ceilingPhys = engine.getSceneGraph().addPhysicsStaticPlane(ceilingTransform, down, 0f);

		ceilingPhys.setBounciness(0.8f);
		ceilingPlane.setPhysicsObject(ceilingPhys);

	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(.5f, .2f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 5f));
		(engine.getSceneGraph()).addLight(light);
	}

	@Override
	public void loadSkyBoxes() {
		skybox = (engine.getSceneGraph()).loadCubeMap("space");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(skybox);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame() {
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);

		engine.enablePhysicsWorldRender();

		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(new float[]{0f, -9.8f, 0f});

		Matrix4f avatarTranslation = new Matrix4f(avatar.getLocalTranslation());
		double[] avatarTransforms = toDoubleArray(avatarTranslation.get(vals));

		PhysicsObject paddlePhysics = (engine.getSceneGraph()).addPhysicsBox(1.0f, avatarTransforms, new float[]{0.5f, 0.5f, 0.5f});
		avatar.setPhysicsObject(paddlePhysics);
		// ----------------- initialize camera ----------------

		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		c.setLocation(new Vector3f(0f, 1f, 6f));
		c.setU(new Vector3f(1, 0, 0));
		c.setV(new Vector3f(0, 1, 0));
		c.setN(new Vector3f(0, 0, -1));

		setupNetworking();

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

		// build some action objects for doing things in response to user input
		vAction upAction = new vAction(this, true);
		vAction downAction = new vAction(this, false);
		TurnAction turnAction = new TurnAction(this);

		CameraTurnAction camUpAction = new CameraTurnAction(this,
				new Vector3f(1, 0, 0));
		CameraTurnAction camDownAction = new CameraTurnAction(this,
				new Vector3f(-1, 0, 0));
		CameraTurnAction camLeftAction = new CameraTurnAction(this,
				new Vector3f(0, 1, 0));
		CameraTurnAction camRightAction = new CameraTurnAction(this,
				new Vector3f(0, -1, 0));

		// attach the action objects to keyboard and gamepad components
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Button._1, upAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.X, turnAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.W, upAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.S, downAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.LEFT, camLeftAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.RIGHT, camRightAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.UP, camUpAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.DOWN, camDownAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
				Component.Identifier.Key.ESCAPE,
				new SendCloseConnectionPacketAction(),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	public GameObject getAvatar() {
		return avatar;
	}

	public Camera getCamera() {
		return (engine.getRenderSystem()).getViewport("MAIN").getCamera();
	}

	@Override
	public void update() {
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();

		// build and set HUD
		int elapsTimeSec = Math.round(
				(float) (System.currentTimeMillis() - startTime) / 1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "camera position = " + (c.getLocation()).x() + ", "
				+ (c.getLocation()).y() + ", " + (c.getLocation()).z();
		Vector3f hud1Color = new Vector3f(1, 0, 0);
		Vector3f hud2Color = new Vector3f(1, 1, 1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		// update inputs and camera
		im.update((float) elapsedTime);
		processNetworking((float) elapsedTime);
		physicsEngine.update((float)elapsedTime);

		if (avatar.getPhysicsObject() != null) {
			Matrix4f mat = new Matrix4f();
			Matrix4f translationMatrix = new Matrix4f().identity();
			AxisAngle4f rotationAxis = new AxisAngle4f();

			mat.set(toFloatArray(avatar.getPhysicsObject().getTransform()));

			// Set the translation part
			translationMatrix.set(3, 0, mat.m30());
			translationMatrix.set(3, 1, mat.m31());
			translationMatrix.set(3, 2, mat.m32());
			avatar.setLocalTranslation(translationMatrix);

			// Set the rotation part
			mat.getRotation(rotationAxis);
			Matrix4f rotationMatrix = new Matrix4f().identity().rotation(rotationAxis);
			avatar.setLocalRotation(rotationMatrix);
		}

		protClient.sendMoveMessage(avatar.getWorldLocation());

		if (avatar != null && avatar.getPhysicsObject() != null)
		{
			float y = avatar.getLocalLocation().y;

			double[] transform = avatar.getPhysicsObject().getTransform();

			if (avatar.getLocalLocation().x < 0)
			{
				// --- VISUAL LOCK ---
				avatar.setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(90)));
				avatar.setLocalTranslation(new Matrix4f().translation(-5f, y, 0f));

				// --- PHYSICS LOCK ---
				transform[0] = 1; transform[1] = 0; transform[2] = 0;
				transform[4] = 0; transform[5] = 1; transform[6] = 0;
				transform[8] = 0; transform[9] = 0; transform[10] = 1;

				transform[12] = -5.0; // X position
				transform[13] = y;    // Y position
				transform[14] = 0.0;  // Z position

				avatar.getPhysicsObject().setTransform(transform);
			}
			else
			{
				// --- VISUAL LOCK ---
				avatar.setLocalRotation(new Matrix4f()
						.rotateY((float) Math.toRadians(90))
						.rotateX((float) Math.toRadians(-90)));
				avatar.setLocalTranslation(new Matrix4f().translation(5f, y, 0f));

				// --- PHYSICS LOCK ---
				transform[0] = 1; transform[1] = 0; transform[2] = 0;
				transform[4] = 0; transform[5] = 1; transform[6] = 0;
				transform[8] = 0; transform[9] = 0; transform[10] = 1;

				transform[12] = 5.0; // X position
				transform[13] = y;   // Y position
				transform[14] = 0.0; // Z position

				avatar.getPhysicsObject().setTransform(transform);
			}
		}
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() {
		return ghostS;
	}
	public TextureImage getGhostTexture() {
		return ghostT;
	}
	public GhostManager getGhostManager() {
		return gm;
	}
	public Engine getEngine() {
		return engine;
	}

	private void setupNetworking() {
		isClientConnected = false;
		try {
			protClient =
					new ProtocolClient(InetAddress.getByName(serverAddress),
							serverPort, serverProtocol, this);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (protClient == null) {
			System.out.println("missing protocol host");
		} else { // Send the initial join message with a unique identifier for
			// this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}

	protected void processNetworking(
			float elapsTime) { // Process packets received by the client from the
		// server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		this.isClientConnected = value;
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			if (protClient != null && isClientConnected == true) {
				protClient.sendByeMessage();
			}
		}
	}

	private float vals[] = new float[16];

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}

	private float[] toFloatArray(double[] arr)
	{ if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{ ret[i] = (float)arr[i];
		}
		return ret;
	}

	public void setLeftSide(boolean side) { isLeftSide = side; }
	public boolean getLeftSide() { return isLeftSide; }
}