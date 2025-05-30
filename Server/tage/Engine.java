package tage;
import tage.input.*;
import tage.physics.*;
import tage.audio.*;
import com.jogamp.openal.ALFactory;

/**
* The Engine object holds references to the primary game engine components, and
* provides accessors for them.  It also maintains a link to the current game application.
* <p>
* The first thing that a game application should do is instantiate this engine class
* using the three-argument constructor. The arguments are a link back to the game application,
* and the desired window dimensions.
* @author Scott Gordon
*/
public class Engine
{
	private static Engine eng;

	/** returns a reference to the Engine object */
	public static Engine getEngine() { return eng; }

	private RenderSystem rs;
	private SceneGraph sg;
	private HUDmanager hm;
	private LightManager lm;
	private VariableFrameRateGame vfrg;
	private InputManager im;
	private IAudioManager audioMgr;
	private boolean renderGraphicsObjects = true;
	private boolean renderPhysicsObjects = false;
	private boolean glStarted = false;

	/** The game application should first call this constructor, supplying a pointer back to itself. */
	public Engine(VariableFrameRateGame v)
	{	System.out.println("*********************************************************");
		System.out.println("*    Powered by TAGE 2025 - ANOTHER TINY GAME ENGINE    *");
		System.out.println("*********************************************************");
		System.out.println("creating variable frame rate game");
		vfrg = v;
		VariableFrameRateGame.setEngine(this);
		eng = this;

		System.out.println("creating RenderSystem");
		rs = new RenderSystem(this);
		
		System.out.println("creating SceneGraph");
		sg = new SceneGraph(this);
		
		System.out.println("creating HUDmanager");
		hm = new HUDmanager(this);
		
		System.out.println("creating LightManager");
		lm = new LightManager(this);
		Light.setEngine(this);
		
		System.out.println("creating InputManager");
		im = new InputManager();

		System.out.println("creating AudioManager");
		audioMgr = AudioManagerFactory.createAudioManager("tage.audio.joal.JOALAudioManager");
		if (!audioMgr.initialize())
		{	System.out.println("Audio Manager failed to initialize!");
			return;
		}

		System.out.println("setting up OpenGL canvas");
		rs.setUpCanvas();

		System.out.println("building default skybox");
		sg.buildSkyBox();
		
		System.out.println("initializing physics engine");
		String pengine = "tage.physics.JBullet.JBulletPhysicsEngine";
		PhysicsEngine pe = PhysicsEngineFactory.createPhysicsEngine(pengine);
		pe.initSystem();
		sg.setPhysicsEngine(pe);
	}

	// returns boolean flag indicating whether or not the openGL canvas has been created yet.
	protected boolean getGLstarted() { return glStarted; }

	// called by init() when init finishes.
	protected void setGLstarted() { glStarted = true; }

	/** returns the RenderSystem object associated with this Engine */
	public RenderSystem getRenderSystem() { return rs; }

	/** returns the SceneGraph object associated with this Engine */
	public SceneGraph getSceneGraph() { return sg; }

	/** returns the HUDmanager object associated with this Engine */
	public HUDmanager getHUDmanager() { return hm; }

	/** returns the LightManager object associated with this Engine */
	public LightManager getLightManager() { return lm; }

	/** returns the InputManager object associated with this Engine */
	public InputManager getInputManager() { return im; }

	/** returns the AudioManager object associated with this Engine */
	public IAudioManager getAudioManager() { return audioMgr; }

	/** returns a reference to the game application */
	public VariableFrameRateGame getGame() { return vfrg; }
	
	/** enables rendering of the graphics objects */
	public void enableGraphicsWorldRender() { renderGraphicsObjects = true; }
	
	/** disables rendering of the graphics objects */
	public void disableGraphicsWorldRender() { renderGraphicsObjects = false; }
	
	/** enables rendering of the physics objects */
	public void enablePhysicsWorldRender() { renderPhysicsObjects = true; }
	
	/** disables rendering of the physics objects */
	public void disablePhysicsWorldRender() { renderPhysicsObjects = false; }
	
	protected boolean willRenderGraphicsObjects() { return renderGraphicsObjects; }
	protected boolean willRenderPhysicsObjects() { return renderPhysicsObjects; }
}