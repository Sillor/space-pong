package myGame;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Manages network connection and ProtocolClient initialization.
 */
public class NetworkingManager {
    private final MyGame game;

    public NetworkingManager(MyGame game) {
        this.game = game;
    }

    public void setupNetworking() {
        try {
            ProtocolClient protocolClient = new ProtocolClient(
                    InetAddress.getByName(game.getServerAddress()),
                    game.getServerPort(),
                    game.getServerProtocol(),
                    game
            );
            game.setProtocolClient(protocolClient);
            game.setMultiplayerMode(true);
            protocolClient.sendJoinMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
