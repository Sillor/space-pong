package myGame;

import org.joml.Math;
import tage.*;
import tage.input.*;
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;
import tage.shapes.*;

import net.java.games.input.Component;
import org.joml.*;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import tage.audio.*;

/**
 * Main game class. Handles initialization, updates, networking, and input.
 */
public class MyGame extends VariableFrameRateGame {
	private static Engine engine;
	private InputManager inputManager;
	private GhostManager ghostManager;
	private ProtocolClient protocolClient;

	private PhysicsEngine physicsEngine;

	private GameObject avatar;
	private TextureImage paddleTexture;
	private ObjShape ghostShape, paddleShape;
	private AnimatedShape paddleS;

	private boolean isLeftSide = true;
	private boolean isClientConnected = false;

	private int skyboxTexture;
	public float lockedX, lockedZ;

	private final String serverAddress;
	private final int serverPort;
	private final ProtocolType serverProtocol;

	private final float[] matrixValues = new float[16];

	private double startTime;

	private Matrix4f avatarOriginalRotation;

	private Sound bounceSound;

	private PongNPCandBall pong;

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.serverProtocol = protocol.equalsIgnoreCase("TCP") ? ProtocolType.TCP : ProtocolType.UDP;
		this.ghostManager = new GhostManager(this);
	}

	public static void main(String[] args) {
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes() {
		ghostShape = new ImportedModel("paddle.obj");
//		paddleShape = new ImportedModel("paddle.obj");
		paddleS = new AnimatedShape("pong.rkm", "pong.rks");
		paddleS.loadAnimation("Bounce", "bounce.rka");
	}

	@Override
	public void loadTextures() {
		paddleTexture = new TextureImage("paddle1.png");
	}

	@Override
	public void buildObjects() {
		buildAvatar();
		buildTorus();
		buildAxes();
		buildTerrain();
		buildGroundPlane();
		buildCeilingPlane();
	}

	private void buildAvatar() {
		avatar = new GameObject(GameObject.root(), paddleS, paddleTexture);
		avatar.setLocalScale(new Matrix4f().scaling(0.25f));
		setLockedX(-5f);
		setLockedZ(-3f);
		Matrix4f initialRot = new Matrix4f()
				.rotateY((float) Math.toRadians(90))
				.rotateX((float) Math.toRadians(90));
		avatar.setLocalRotation(initialRot);

		Matrix4f avatarTranslation = new Matrix4f(avatar.getLocalTranslation());
		double[] avatarTransform = toDoubleArray(avatarTranslation.get(matrixValues));
		PhysicsObject avatarPhysics = engine.getSceneGraph().addPhysicsBox(1.0f, avatarTransform, new float[]{0.5f, 2.0f, 0.5f});
		avatar.setPhysicsObject(avatarPhysics);
	}

	private void buildTorus() {
		GameObject torus = new GameObject(GameObject.root(), new Torus(0.5f, 0.2f, 48));
		torus.setLocalTranslation(new Matrix4f().translation(1f, 0f, 0f));
		torus.setLocalScale(new Matrix4f().scaling(0.25f));
	}

	private void buildAxes() {
		GameObject xAxis = new GameObject(GameObject.root(), new Line(new Vector3f(0, 0, 0), new Vector3f(3, 0, 0)));
		GameObject yAxis = new GameObject(GameObject.root(), new Line(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0)));
		GameObject zAxis = new GameObject(GameObject.root(), new Line(new Vector3f(0, 0, 0), new Vector3f(0, 0, -3)));

		xAxis.getRenderStates().setColor(new Vector3f(1, 0, 0)); // Red
		yAxis.getRenderStates().setColor(new Vector3f(0, 1, 0)); // Green
		zAxis.getRenderStates().setColor(new Vector3f(0, 0, 1)); // Blue
	}

	private void buildTerrain() {
		GameObject terrain = new GameObject(GameObject.root(), new TerrainPlane(512), new TextureImage("rocks.jpg"));
		terrain.setLocalTranslation(new Matrix4f().translation(0f, -9f, 0f));
		terrain.setLocalScale(new Matrix4f().scaling(60f, 6f, 60f));
		terrain.setHeightMap(new TextureImage("hills.jpg"));

		terrain.getRenderStates().setColor(new Vector3f(0.5f, 0f, 0f));
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);
	}

	private void buildGroundPlane() {
		GameObject groundPlane = new GameObject(GameObject.root(), new Plane());
		groundPlane.setLocalTranslation(new Matrix4f().translation(0f, -4f, 0f));
		groundPlane.setLocalScale(new Matrix4f().scaling(20f));

		double[] groundTransform = toDoubleArray(new Matrix4f(groundPlane.getLocalTranslation()).get(matrixValues));
		PhysicsObject groundPhysics = engine.getSceneGraph().addPhysicsStaticPlane(groundTransform, new float[]{0f, 1f, 0f}, 0f);
		groundPhysics.setBounciness(0.8f);

		groundPlane.setPhysicsObject(groundPhysics);
		groundPlane.getRenderStates().disableRendering(); // Make it invisible
	}

	private void buildCeilingPlane() {
		GameObject ceilingPlane = new GameObject(GameObject.root(), new Plane());
		ceilingPlane.setLocalTranslation(new Matrix4f().translation(0f, 4f, 0f));
		ceilingPlane.setLocalScale(new Matrix4f().scaling(20f));

		double[] ceilingTransform = toDoubleArray(new Matrix4f(ceilingPlane.getLocalTranslation()).get(matrixValues));
		PhysicsObject ceilingPhysics = engine.getSceneGraph().addPhysicsStaticPlane(ceilingTransform, new float[]{0f, -1f, 0f}, 0f);
		ceilingPhysics.setBounciness(0.8f);

		ceilingPlane.setPhysicsObject(ceilingPhysics);
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.5f, 0.2f, 0.5f);
		Light light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 5f));
		engine.getSceneGraph().addLight(light);
	}

	@Override
	public void loadSkyBoxes() {
		skyboxTexture = engine.getSceneGraph().loadCubeMap("space");
		engine.getSceneGraph().setActiveSkyBoxTexture(skyboxTexture);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame() {
		startTime = System.currentTimeMillis();
		engine.getRenderSystem().setWindowDimensions(1900, 1000);
		engine.enablePhysicsWorldRender();
		pong = new PongNPCandBall(this);

		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(new float[]{0f, -9.8f, 0f});

		setupNetworking();
		setupInput();
		setupCamera();
		setupSound();
	}

	private void setupSound() {
		IAudioManager audioMgr = engine.getAudioManager();
		AudioResource bounceResource = audioMgr.createAudioResource("bounce.wav", AudioResourceType.AUDIO_SAMPLE);
		bounceSound = new Sound(bounceResource, SoundType.SOUND_EFFECT, 25, false);
		bounceSound.initialize(audioMgr);

	}

	private void setupNetworking() {
		try {
			protocolClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
			protocolClient.sendJoinMessage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupInput() {
		inputManager = engine.getInputManager();

		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.W, new vAction(this, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.S, new vAction(this, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.LEFT, new CameraTurnAction(this, new Vector3f(0, 1, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.RIGHT, new CameraTurnAction(this, new Vector3f(0, -1, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.UP, new CameraTurnAction(this, new Vector3f(1, 0, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.DOWN, new CameraTurnAction(this, new Vector3f(-1, 0, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.ESCAPE, new SendCloseConnectionPacketAction(), InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.HOME, new TestAction(), IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		inputManager.associateActionWithAllGamepads(Component.Identifier.Button._1, new vAction(this, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllGamepads(Component.Identifier.Axis.X, new TurnAction(this), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	private class TestAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event event) {
			paddleS.stopAnimation();
			paddleS.playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);
			if (bounceSound != null) {
				float randomPitch = 0.9f + (float)(java.lang.Math.random()); // random between 0.9 and 1.1
				bounceSound.setPitch(randomPitch);
				bounceSound.play();
			}
		}
	}

	private void setupCamera() {
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		camera.setLocation(new Vector3f(0f, 1f, 6f));
		camera.setU(new Vector3f(1, 0, 0));
		camera.setV(new Vector3f(0, 1, 0));
		camera.setN(new Vector3f(0, 0, -1));

		Matrix4f pitchDown = new Matrix4f().rotation((float)Math.toRadians(-5), new Vector3f(1, 0, 0));

		Vector3f u = camera.getU();
		Vector3f v = camera.getV();
		Vector3f n = camera.getN();

		Vector4f newU = new Vector4f(u, 0).mul(pitchDown);
		Vector4f newV = new Vector4f(v, 0).mul(pitchDown);
		Vector4f newN = new Vector4f(n, 0).mul(pitchDown);

		camera.setU(new Vector3f(newU.x, newU.y, newU.z));
		camera.setV(new Vector3f(newV.x, newV.y, newV.z));
		camera.setN(new Vector3f(newN.x, newN.y, newN.z));
	}

	@Override
	public void update() {
		double elapsedTime = System.currentTimeMillis() - startTime;
		startTime = System.currentTimeMillis();

		updateHUD();

		inputManager.update((float) elapsedTime);
		if (protocolClient != null) protocolClient.processPackets();
		if (physicsEngine != null) physicsEngine.update((float) elapsedTime);

		if (avatar != null && avatar.getPhysicsObject() != null) {
			syncAvatarPhysics();
		}

		if (protocolClient != null) {
			protocolClient.sendMoveMessage(avatar.getWorldLocation());
		}

		for (GhostAvatar ghost : ghostManager.getGhostAvatars()) {
			ghost.syncToPhysics();
		}

		if (pong != null) {
			pong.update((float)elapsedTime);
		}

		paddleS.updateAnimation();
	}

	private void updateHUD() {
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();

		String hud1 = "Time = " + (int) ((System.currentTimeMillis() - startTime) / 1000);
		String hud2 = String.format("Camera position = %.2f, %.2f, %.2f",
				camera.getLocation().x(),
				camera.getLocation().y(),
				camera.getLocation().z());

		engine.getHUDmanager().setHUD1(hud1, new Vector3f(1, 0, 0), 15, 15);
		engine.getHUDmanager().setHUD2(hud2, new Vector3f(1, 1, 1), 500, 15);
	}

	private void syncAvatarPhysics() {
		double[] transform = avatar.getPhysicsObject().getTransform();
		transform[12] = lockedX;
		transform[14] = lockedZ;
		avatar.getPhysicsObject().setTransform(transform);

		float[] velocity = avatar.getPhysicsObject().getLinearVelocity();
		velocity[0] = velocity[2] = 0f;
		avatar.getPhysicsObject().setLinearVelocity(velocity);

		float[] angularVelocity = avatar.getPhysicsObject().getAngularVelocity();
		angularVelocity[0] = angularVelocity[1] = angularVelocity[2] = 0f;
		avatar.getPhysicsObject().setAngularVelocity(angularVelocity);

		Matrix4f fullMatrix = new Matrix4f().set(toFloatArray(avatar.getPhysicsObject().getTransform()));
		Vector3f translation = new Vector3f();
		fullMatrix.getTranslation(translation);

		avatar.setLocalTranslation(new Matrix4f().translation(translation));
	}


	public GameObject getAvatar() { return avatar; }
	public AnimatedShape getPaddleS() { return paddleS; }
	public Camera getCamera() { return engine.getRenderSystem().getViewport("MAIN").getCamera(); }
	public ObjShape getGhostShape() { return ghostShape; }
	public TextureImage getGhostTexture() { return paddleTexture; }
	public GhostManager getGhostManager() { return ghostManager; }
	public Engine getEngine() { return engine; }
	public float getLockedX() { return lockedX; }
	public float getLockedZ() { return lockedZ; }
	public void setLockedX(float x) { lockedX = x; }
	public void setLockedZ(float z) { lockedZ = z;	}
	public void setAvatarOriginalRotation(Matrix4f rotation) { this.avatarOriginalRotation = new Matrix4f(rotation); }
	public Matrix4f getAvatarOriginalRotation() { return new Matrix4f(avatarOriginalRotation); }


	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }
	public void setIsConnected(boolean connected) {
		isClientConnected = connected;
		if (pong != null) {
			pong.setIsNPCActive(!connected);
		}
	}
	public void setLeftSide(boolean side) { isLeftSide = side; }
	public boolean getLeftSide() { return isLeftSide; }

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event event) {
			if (protocolClient != null && isClientConnected) {
				protocolClient.sendByeMessage();
			}
		}
	}

	private double[] toDoubleArray(float[] array) {
		if (array == null) return null;
		double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	private float[] toFloatArray(double[] array) {
		if (array == null) return null;
		float[] result = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = (float) array[i];
		}
		return result;
	}
}
