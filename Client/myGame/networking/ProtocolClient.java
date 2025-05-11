package myGame.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import myGame.core.MyGame;
import org.joml.*;
import org.joml.Math;
import tage.networking.client.GameConnectionClient;
import tage.shapes.AnimatedShape;

public class ProtocolClient extends GameConnectionClient {
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;

	private UUID localPlayerId = id;   // set local player ID

	public UUID getLocalPlayerId() {
		return localPlayerId;
	}

	private int playerNumber = -1;

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException {
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}

	public UUID getID() {
		return id;
	}

	@Override
	protected void processPacket(Object message) {
		if (message == null) return;
		String strMessage = (String) message;
		String[] messageTokens = strMessage.split(",");

		if (messageTokens.length > 0) {
			switch (messageTokens[0]) {
				case "join":
					if (messageTokens[1].equals("success")) {
						game.setIsConnected(true);
						sendCreateMessage();
					} else {
						game.setIsConnected(false);
					}
					break;

				case "assignNumber":
					playerNumber = Integer.parseInt(messageTokens[1]);
					System.out.println("[Client] Assigned player number = " + playerNumber);
					break;

				case "bye":
					ghostManager.removeGhostAvatar(UUID.fromString(messageTokens[1]));
					break;

				case "create":
					System.out.println("Assigned player number: " + playerNumber);
					UUID remoteId = UUID.fromString(messageTokens[1]);
					Vector3f pos = new Vector3f(
							Float.parseFloat(messageTokens[2]),
							Float.parseFloat(messageTokens[3]),
							Float.parseFloat(messageTokens[4])
					);
					int number = Integer.parseInt(messageTokens[5]);

					if (remoteId.equals(this.id)) {
						// ✅ Local player: assign side, lock positions
						boolean left = (number == 0);
						game.setLockedX(left ? -5f : 5f);
						game.setLockedZ(-3f);
						game.setLeftSide(left);

						game.getGameBuilder().buildLocalPlayer(number);
					} else {
						// ghost player
						try {
							ghostManager.createGhostAvatar(remoteId, pos, number);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;

				case "dsfr":
					UUID remoteIdDsfr = UUID.fromString(messageTokens[1]);
					if (!remoteIdDsfr.equals(this.id)) {
						int remotePlayerNumber = Integer.parseInt(messageTokens[2]);          // ✅ from server now
						Vector3f posDsfr = new Vector3f(
								Float.parseFloat(messageTokens[3]),
								Float.parseFloat(messageTokens[4]),
								Float.parseFloat(messageTokens[5])
						);
						try {
							ghostManager.createGhostAvatar(remoteIdDsfr, posDsfr, remotePlayerNumber);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;

				case "bounce":
					UUID bounceId = UUID.fromString(messageTokens[1]);
					if (bounceId.equals(this.id)) {
						game.getPaddleS().playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);

						if (game.getBounceSound() != null) {
							float randomPitch = 0.9f + (float)(Math.random()) * 0.2f;
							game.getBounceSound().setPitch(randomPitch);
							game.getBounceSound().play();
						}
					} else {
						GhostAvatar ghost = ghostManager.findAvatar(bounceId);
						if (ghost != null)
							ghost.playBounceAnimation();
					}
					break;




				case "playerNumber":
					playerNumber = Integer.parseInt(messageTokens[1]);
					break;

				case "wsds":
					sendDetailsForMessage(UUID.fromString(messageTokens[1]), game.getPlayerPosition());
					break;

				case "move":
					ghostManager.updateGhostAvatar(
							UUID.fromString(messageTokens[1]),
							new Vector3f(
									Float.parseFloat(messageTokens[2]),
									Float.parseFloat(messageTokens[3]),
									Float.parseFloat(messageTokens[4])
							)
					);
					break;

				case "ball":
					UUID senderId = UUID.fromString(messageTokens[1]);
					Vector3f ballPos = new Vector3f(
							Float.parseFloat(messageTokens[2]),
							Float.parseFloat(messageTokens[3]),
							Float.parseFloat(messageTokens[4])
					);
					game.getBall().setPosition(ballPos);
					break;
			}
		}
	}

	public void sendJoinMessage() {
		try {
			sendPacket("join," + id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBallMessage(Vector3f ballPosition) {
		try {
			sendPacket(String.format("ball,%s,%.3f,%.3f,%.3f",
					id.toString(),
					ballPosition.x(), ballPosition.y(), ballPosition.z()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendByeMessage() {
		try {
			sendPacket("bye," + id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCreateMessage() {
		try {
			sendPacket("create," + id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendDetailsForMessage(UUID remoteId, Vector3f position) {
		try {
			sendPacket(String.format("dsfr,%s,%s,%.3f,%.3f,%.3f", remoteId, id, position.x(), position.y(), position.z()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPaddleBounceMessage(UUID paddleOwnerId) {
		try {
			sendPacket("bounce," + paddleOwnerId.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMoveMessage(Vector3f position) {
		try {
			sendPacket(String.format("move,%s,%.3f,%.3f,%.3f", id, position.x(), position.y(), position.z()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double[] toDoubleArray(float[] arr) {
		double[] ret = new double[arr.length];
		for (int i = 0; i < arr.length; i++) ret[i] = arr[i];
		return ret;
	}

	private float[] vals = new float[16];

	public int getPlayerNumber() {
		return playerNumber;
	}
}
