package myGame;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tage.*;
import tage.audio.*;
import tage.input.*;
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;
import tage.shapes.*;

import net.java.games.input.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main game class. Handles initialization, updates, networking, and input.
 */
public class MyGame extends VariableFrameRateGame {
	private static final float AVATAR_SCALE = 0.1f;
	private static final float TORUS_SCALE = 0.25f;
	private static final float TERRAIN_Y_OFFSET = -9f;
	private static final float TERRAIN_SCALE = 60f;
	private static final float GROUND_Y_OFFSET = -4f;
	private static final float CEILING_Y_OFFSET = 4f;
	private static final float PLANE_SCALE = 20f;

	private static Engine engine;
	private InputManager inputManager;
	private GhostManager ghostManager;
	private ProtocolClient protocolClient;

	private Ball ball;
	private NPC npc;
	private PhysicsEngine physicsEngine;

	private GameObject avatar;
	private TextureImage paddleTexture;
	private ObjShape ghostShape;
	private AnimatedShape paddleS, paddleS_2;

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
		paddleS = new AnimatedShape("pong.rkm", "pong.rks");
		paddleS.loadAnimation("Bounce", "bounce.rka");
		paddleS_2 = new AnimatedShape("pong.rkm", "pong.rks");
		paddleS_2.loadAnimation("Bounce", "bounce.rka");
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
		avatar.setLocalScale(new Matrix4f().scaling(AVATAR_SCALE));
		setLockedX(-5f);
		setLockedZ(-3f);

		avatar.setLocalRotation(new Matrix4f()
				.rotateY(Math.toRadians(90))
				.rotateX(Math.toRadians(90)));

		PhysicsObject avatarPhysics = engine.getSceneGraph().addPhysicsBox(
				1.0f,
				toDoubleArray(avatar.getLocalTranslation().get(matrixValues)),
				new float[]{0.5f, 2.0f, 0.5f}
		);
		avatar.setPhysicsObject(avatarPhysics);
	}

	private void buildTorus() {
		GameObject torus = new GameObject(GameObject.root(), new Torus(0.5f, 0.2f, 48));
		torus.setLocalTranslation(new Matrix4f().translation(1f, 0f, 0f));
		torus.setLocalScale(new Matrix4f().scaling(TORUS_SCALE));
	}

	private void buildAxes() {
		createAxis(new Vector3f(3, 0, 0), new Vector3f(1, 0, 0)); // X - Red
		createAxis(new Vector3f(0, 3, 0), new Vector3f(0, 1, 0)); // Y - Green
		createAxis(new Vector3f(0, 0, -3), new Vector3f(0, 0, 1)); // Z - Blue
	}

	private void createAxis(Vector3f end, Vector3f color) {
		GameObject axis = new GameObject(GameObject.root(), new Line(new Vector3f(0, 0, 0), end));
		axis.getRenderStates().setColor(color);
	}

	private void buildTerrain() {
		GameObject terrain = new GameObject(GameObject.root(), new TerrainPlane(512), new TextureImage("rocks.jpg"));
		terrain.setLocalTranslation(new Matrix4f().translation(0f, TERRAIN_Y_OFFSET, 0f));
		terrain.setLocalScale(new Matrix4f().scaling(TERRAIN_SCALE, 6f, TERRAIN_SCALE));
		terrain.setHeightMap(new TextureImage("hills.jpg"));
		terrain.getRenderStates().setColor(new Vector3f(0.5f, 0f, 0f));
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);
	}

	private void buildGroundPlane() {
		addPhysicsPlane(GROUND_Y_OFFSET, new float[]{0f, 1f, 0f}, true);
	}

	private void buildCeilingPlane() {
		addPhysicsPlane(CEILING_Y_OFFSET, new float[]{0f, -1f, 0f}, false);
	}

	private void addPhysicsPlane(float yOffset, float[] normal, boolean invisible) {
		GameObject plane = new GameObject(GameObject.root(), new Plane());
		plane.setLocalTranslation(new Matrix4f().translation(0f, yOffset, 0f));
		plane.setLocalScale(new Matrix4f().scaling(PLANE_SCALE));

		PhysicsObject physics = engine.getSceneGraph().addPhysicsStaticPlane(
				toDoubleArray(plane.getLocalTranslation().get(matrixValues)),
				normal,
				0f
		);
		physics.setBounciness(0.8f);
		plane.setPhysicsObject(physics);

		if (invisible) plane.getRenderStates().disableRendering();
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

		ball = new Ball(this);
		npc = new NPC(this);

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

	private void setupCamera() {
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		camera.setLocation(new Vector3f(0f, 1f, 6f));
		camera.setU(new Vector3f(1, 0, 0));
		camera.setV(new Vector3f(0, 1, 0));
		camera.setN(new Vector3f(0, 0, -1));

		Matrix4f pitchDown = new Matrix4f().rotation((float) Math.toRadians(-5), new Vector3f(1, 0, 0));
		camera.setU(transformVector(camera.getU(), pitchDown));
		camera.setV(transformVector(camera.getV(), pitchDown));
		camera.setN(transformVector(camera.getN(), pitchDown));
	}

	private Vector3f transformVector(Vector3f vector, Matrix4f matrix) {
		Vector4f result = new Vector4f(vector, 0).mul(matrix);
		return new Vector3f(result.x, result.y, result.z);
	}

	@Override
	public void update() {
		double currentTime = System.currentTimeMillis();
		double elapsedTime = currentTime - startTime;
		startTime = currentTime;

		updateHUD();
		inputManager.update((float) elapsedTime);
		if (protocolClient != null) protocolClient.processPackets();
		if (physicsEngine != null) physicsEngine.update((float) elapsedTime);

		if (avatar != null && avatar.getPhysicsObject() != null) syncAvatarPhysics();
		if (protocolClient != null) protocolClient.sendMoveMessage(avatar.getWorldLocation());

		ghostManager.getGhostAvatars().forEach(GhostAvatar::syncToPhysics);

		if (ball != null && npc != null) {
			ball.update((float) elapsedTime, npc.getNPCPaddle(), getPlayerPosition());
			npc.update((float) elapsedTime, ball.getBall());
		}

		paddleS.updateAnimation();
		paddleS_2.updateAnimation();
	}

	private void updateHUD() {
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		String hud1 = "Time = " + (int) ((System.currentTimeMillis() - startTime) / 1000);
		String hud2 = String.format("Camera position = %.2f, %.2f, %.2f",
				camera.getLocation().x(), camera.getLocation().y(), camera.getLocation().z());

		engine.getHUDmanager().setHUD1(hud1, new Vector3f(1, 0, 0), 15, 15);
		engine.getHUDmanager().setHUD2(hud2, new Vector3f(1, 1, 1), 500, 15);
	}

	private void syncAvatarPhysics() {
		PhysicsObject physics = avatar.getPhysicsObject();
		double[] transform = physics.getTransform();
		transform[12] = lockedX;
		transform[14] = lockedZ;
		physics.setTransform(transform);

		float[] velocity = physics.getLinearVelocity();
		velocity[0] = velocity[2] = 0f;
		physics.setLinearVelocity(velocity);

		float[] angularVelocity = physics.getAngularVelocity();
		angularVelocity[0] = angularVelocity[1] = angularVelocity[2] = 0f;
		physics.setAngularVelocity(angularVelocity);

		Matrix4f matrix = new Matrix4f().set(toFloatArray(physics.getTransform()));
		Vector3f translation = new Vector3f();
		matrix.getTranslation(translation);
		avatar.setLocalTranslation(new Matrix4f().translation(translation));
	}

	// ------------------------
	// Public API Methods
	// ------------------------

	public GameObject getAvatar() { return avatar; }
	public AnimatedShape getPaddleS() { return paddleS; }
	public AnimatedShape getPaddleS_2() { return paddleS_2; }
	public Camera getCamera() { return engine.getRenderSystem().getViewport("MAIN").getCamera(); }
	public ObjShape getGhostShape() { return ghostShape; }
	public TextureImage getGhostTexture() { return paddleTexture; }
	public GhostManager getGhostManager() { return ghostManager; }
	public Engine getEngine() { return engine; }
	public float getLockedX() { return lockedX; }
	public float getLockedZ() { return lockedZ; }

	public void setLockedX(float x) { lockedX = x; }
	public void setLockedZ(float z) { lockedZ = z; }

	public void setAvatarOriginalRotation(Matrix4f rotation) {
		avatarOriginalRotation = new Matrix4f(rotation);
	}

	public Matrix4f getAvatarOriginalRotation() {
		return new Matrix4f(avatarOriginalRotation);
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean connected) {
		isClientConnected = connected;
		if (npc != null) npc.setIsNPCActive(!connected);
	}

	public Sound getBounceSound() { return bounceSound; }
	public void setLeftSide(boolean side) { isLeftSide = side; }
	public boolean getLeftSide() { return isLeftSide; }
	public Ball getBall() { return ball; }

	// ------------------------
	// Private Helper Classes
	// ------------------------

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event event) {
			if (protocolClient != null && isClientConnected) protocolClient.sendByeMessage();
		}
	}

	private class TestAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event event) {
			paddleS.stopAnimation();
			paddleS.playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);
			if (bounceSound != null) {
				float randomPitch = 0.9f + (float) Math.random() * 0.2f; // between 0.9 and 1.1
				bounceSound.setPitch(randomPitch);
				bounceSound.play();
			}
		}
	}

	// ------------------------
	// Private Utility Methods
	// ------------------------

	private double[] toDoubleArray(float[] array) {
		if (array == null) return null;
		double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++) result[i] = array[i];
		return result;
	}

	private float[] toFloatArray(double[] array) {
		if (array == null) return null;
		float[] result = new float[array.length];
		for (int i = 0; i < array.length; i++) result[i] = (float) array[i];
		return result;
	}
}
