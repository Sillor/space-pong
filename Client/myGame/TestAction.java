package myGame;

import tage.input.action.AbstractInputAction;
import tage.shapes.AnimatedShape;

public class TestAction extends AbstractInputAction {
    private final MyGame game;

    public TestAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, net.java.games.input.Event event) {
        game.getPaddleS().stopAnimation();
        game.getPaddleS().playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);
        if (game.getBounceSound() != null) {
            float randomPitch = 0.9f + (float) Math.random() * 0.2f;
            game.getBounceSound().setPitch(randomPitch);
            game.getBounceSound().play();
        }
    }
}
