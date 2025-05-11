package myGame.core;

import myGame.gameplay.Ball;
import myGame.gameplay.NPC;
import myGame.input.InputHandler;
import myGame.networking.GhostManager;
import myGame.networking.ProtocolClient;
import org.joml.*;
import tage.*;
import tage.audio.Sound;
import tage.input.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.*;
import tage.shapes.AnimatedShape;

import java.lang.Math;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;

	private InputManager inputManager;
	private GhostManager ghostManager;
	private ProtocolClient protocolClient;
	private PhysicsEngine physicsEngine;

	private GameObject avatar;
	private AnimatedShape paddleS, paddleS_2, ghostShape;
	private TextureImage paddleTexture;

	private boolean isLeftSide = true;
	private boolean isClientConnected = false;
	// MAKE SURE TO SET THIS TO FALSE FOR LOCAL PLAY
	private boolean isMultiplayerMode = true;

	private Sound bounceSound;
	private Ball ball;
	private NPC npc;

	private HUDManager hudManager;

	private final String serverAddress;
	private final int serverPort;
	private final ProtocolType serverProtocol;

	private double startTime;
	private Matrix4f avatarOriginalRotation;
	private float lockedX, lockedZ;

	public static final float PADDLE_SCALE = 0.1f;

	public MyGame(final String serverAddress, final int serverPort, final String protocol) {
		super();
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.serverProtocol = ProtocolType.UDP;
		this.ghostManager = new GhostManager(this);
	}

	public static void main(final String[] args) {
		final MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes() {
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
		new GameBuilder(this).buildScene();
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.15f, 0.15f, 0.15f);

		Light main = new Light();
		main.setLocation(new Vector3f(0f, 5f, 5f));
		main.setAmbient(0.2f, 0.2f, 0.2f);
		main.setDiffuse(0.7f, 0.7f, 0.7f);
		main.setSpecular(0.8f, 0.8f, 0.8f);
		engine.getSceneGraph().addLight(main);

		Light left = new Light();
		left.setLocation(new Vector3f(-5f, 3f, -3f));
		left.setAmbient(0.1f, 0.1f, 0.5f);
		left.setDiffuse(0.3f, 0.3f, 1.2f);
		left.setSpecular(0.4f, 0.4f, 1.2f);
		engine.getSceneGraph().addLight(left);

		Light right = new Light();
		right.setLocation(new Vector3f(5f, 3f, -3f));
		right.setAmbient(0.5f, 0.1f, 0.1f);
		right.setDiffuse(1.2f, 0.3f, 0.3f);
		right.setSpecular(1.2f, 0.3f, 0.3f);
		engine.getSceneGraph().addLight(right);
	}

	@Override
	public void loadSkyBoxes() {
		int skyboxTexture = engine.getSceneGraph().loadCubeMap("space");
		engine.getSceneGraph().setActiveSkyBoxTexture(skyboxTexture);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame() {
		startTime = System.currentTimeMillis();
		engine.getRenderSystem().setWindowDimensions(1900, 1000);
//		engine.enablePhysicsWorldRender();

		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(new float[]{0f, 0f, 0f});

		hudManager = new HUDManager(this);

		if (!isMultiplayerMode) {
			new GameBuilder(this).buildLocalPlayer(0);
			npc = new NPC(this);
		}

		ball = new Ball(this);
		new NetworkingManager(this).setupNetworking();
		new InputHandler(this).setupInput();
		setupCamera();
		bounceSound = new SoundManager(this).setupSound();
	}

	private void setupCamera() {
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		camera.setLocation(new Vector3f(0f, 1f, 6f));
		camera.setU(new Vector3f(1, 0, 0));
		camera.setV(new Vector3f(0, 1, 0));
		camera.setN(new Vector3f(0, 0, -1));

		Matrix4f pitchDown = new Matrix4f().rotation((float) Math.toRadians(-5), new Vector3f(1, 0, 0));
		camera.setU(AvatarPhysicsSynchronizer.transformVector(camera.getU(), pitchDown));
		camera.setV(AvatarPhysicsSynchronizer.transformVector(camera.getV(), pitchDown));
		camera.setN(AvatarPhysicsSynchronizer.transformVector(camera.getN(), pitchDown));
	}

	@Override
	public void update() {
		double currentTime = System.currentTimeMillis();
		double elapsedTime = currentTime - startTime;
		startTime = currentTime;

		hudManager.updateHUD();

		if (inputManager != null) inputManager.update((float) elapsedTime);
		if (protocolClient != null) protocolClient.processPackets();
		if (physicsEngine != null) physicsEngine.update((float) elapsedTime);
		if (avatar != null && avatar.getPhysicsObject() != null)
			AvatarPhysicsSynchronizer.syncAvatarPhysics(this);

		ghostManager.getGhostAvatars().forEach(ghost ->
				AvatarPhysicsSynchronizer.syncGhostPhysics(
						ghost,
						ghostManager.getPlayerNumber(ghost.getID()) == 0 ? getLockedX() : -getLockedX(),
						getLockedZ()
				)
		);

		if (protocolClient != null && avatar != null)
			protocolClient.sendMoveMessage(avatar.getWorldLocation());

		GameObject opponentPaddle = null;
		if (!isClientConnected && npc != null) {
			opponentPaddle = npc.getNPCPaddle();
		} else if (isClientConnected && !ghostManager.getGhostAvatars().isEmpty()) {
			opponentPaddle = ghostManager.getGhostAvatars().firstElement();
		}

		if (ball != null && hudManager.isGameStarted()) {
			ball.update((float) elapsedTime, opponentPaddle, getPlayerPosition());

			if (isClientConnected && protocolClient != null && protocolClient.getPlayerNumber() == 0) {
				float ballX = ball.getBall().getWorldLocation().x();
				if (Math.abs(ballX) > 7f) {
					if (ballX < 0) {
						hudManager.addOpponentScore();
					} else {
						hudManager.addPlayerScore();
					}
					protocolClient.sendScoreUpdate(hudManager.getPlayerScore(), hudManager.getOpponentScore());
					ball.resetBall();
				}

				protocolClient.sendBallMessage(ball.getBall().getWorldLocation());
			}
		}


		if (!hudManager.isGameStarted() && ball != null) {
			Vector3f ballPos = ball.getBall().getWorldLocation();
			if (ballPos.x() != 0f || ballPos.y() != 0.5f || ballPos.z() != -3f) {
				hudManager.startGame();
			}
		}

		if (!isClientConnected && npc != null)
			npc.update((float) elapsedTime, ball.getBall());

		paddleS.updateAnimation();
		paddleS_2.updateAnimation();
	}

	public static Engine getEngine() { return engine; }
	public InputManager getInputManager() { return inputManager; }
	public void setInputManager(InputManager inputManager) { this.inputManager = inputManager; }
	public GhostManager getGhostManager() { return ghostManager; }
	public ProtocolClient getProtocolClient() { return protocolClient; }
	public void setProtocolClient(ProtocolClient protocolClient) { this.protocolClient = protocolClient; }
	public boolean isClientConnected() { return isClientConnected; }
	public void setClientConnected(boolean clientConnected) { isClientConnected = clientConnected; }
	public GameObject getAvatar() { return avatar; }
	public void setAvatar(GameObject avatar) { this.avatar = avatar; }
	public AnimatedShape getPaddleS() { return paddleS; }
	public AnimatedShape getPaddleS_2() { return paddleS_2; }
	public AnimatedShape getGhostShape() { return ghostShape; }
	public TextureImage getGhostTexture() { return paddleTexture; }
	public Sound getBounceSound() { return bounceSound; }
	public void setLeftSide(boolean leftSide) { isLeftSide = leftSide; }
	public boolean isLeftSide() { return isLeftSide; }
	public HUDManager getHudManager() { return hudManager; }

	public Vector3f getPlayerPosition() {
		return avatar == null ? new Vector3f(0, 0, 0) : avatar.getWorldLocation();
	}

	public Matrix4f getAvatarOriginalRotation() {
		return avatarOriginalRotation == null ?
				new Matrix4f().rotateY((float) Math.toRadians(90)).rotateX((float) Math.toRadians(90)) :
				new Matrix4f(avatarOriginalRotation);
	}

	public void setAvatarOriginalRotation(Matrix4f rotation) {
		this.avatarOriginalRotation = new Matrix4f(rotation);
	}

	public float getLockedX() { return lockedX; }
	public float getLockedZ() { return lockedZ; }
	public void setLockedX(float lockedX) { this.lockedX = lockedX; }
	public void setLockedZ(float lockedZ) { this.lockedZ = lockedZ; }
	public Ball getBall() { return ball; }
	public String getServerAddress() { return serverAddress; }
	public int getServerPort() { return serverPort; }
	public ProtocolType getServerProtocol() { return serverProtocol; }
	public double getStartTime() { return startTime; }

	public Camera getCamera() {
		return MyGame.getEngine()
				.getRenderSystem()
				.getViewport("MAIN")
				.getCamera();
	}

	public void setIsConnected(boolean connected) {
		this.isClientConnected = connected;
		if (npc != null) {
			npc.setIsNPCActive(!connected);
			if (connected) {
				engine.getSceneGraph().removeGameObject(npc.getNPCPaddle());
				npc = null;
			}
		}
	}

	public GameBuilder getGameBuilder() {
		return new GameBuilder(this);
	}

	public void setMultiplayerMode(boolean multiplayer) { isMultiplayerMode = multiplayer; }
	public boolean isMultiplayerMode() { return isMultiplayerMode; }
}
