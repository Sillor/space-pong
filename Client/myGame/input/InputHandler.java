package myGame.input;

import myGame.core.MyGame;
import net.java.games.input.Component;
import org.joml.Vector3f;
import tage.input.*;

/**
 * Handles all input and action bindings.
 */
public class InputHandler {
    private final MyGame game;

    public InputHandler(MyGame game) {
        this.game = game;
    }

    public void setupInput() {
        InputManager inputManager = MyGame.getEngine().getInputManager();
        game.setInputManager(inputManager);

        // Keyboard movement
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.W,
                new vAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.S,
                new vAction(game, false), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // Keyboard camera turn
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.LEFT,
                new CameraTurnAction(game, new Vector3f(0, 1, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.RIGHT,
                new CameraTurnAction(game, new Vector3f(0, -1, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.UP,
                new CameraTurnAction(game, new Vector3f(1, 0, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.DOWN,
                new CameraTurnAction(game, new Vector3f(-1, 0, 0)), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // Special keyboard actions
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.ESCAPE,
                new SendCloseConnectionPacketAction(game), InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.HOME,
                new TestAction(game), IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        // Gamepad controls
        inputManager.associateActionWithAllGamepads(Component.Identifier.Button._1,
                new vAction(game, true), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllGamepads(Component.Identifier.Axis.X,
                new TurnAction(game), InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }
}
