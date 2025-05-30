package myGame;

import tage.input.action.AbstractInputAction;

public class SendCloseConnectionPacketAction extends AbstractInputAction {
    private final MyGame game;

    public SendCloseConnectionPacketAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, net.java.games.input.Event event) {
        if (game.getProtocolClient() != null && game.isClientConnected())
            game.getProtocolClient().sendByeMessage();
    }
}
