package myGame;

import java.io.IOException;
import java.lang.Math;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.*;
import tage.networking.client.GameConnectionClient;
import tage.shapes.AnimatedShape;

public class ProtocolClient extends GameConnectionClient {
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private UUID localPlayerId;
	private int playerNumber = -1;

	private float[] vals = new float[16];

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException {
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		this.localPlayerId = this.id;
		this.ghostManager = game.getGhostManager();
	}

	public UUID getID() {
		return id;
	}

	public UUID getLocalPlayerId() {
		return localPlayerId;
	}

	public int getPlayerNumber() {
		return playerNumber;
	}

	@Override
	protected void processPacket(Object message) {
		if (message == null) return;
		String strMessage = (String) message;
		String[] tokens = strMessage.split(",");

		if (tokens.length > 0) {
			switch (tokens[0]) {
				case "join":
					if (tokens[1].equals("success")) {
						game.setIsConnected(true);
						sendCreateMessage();
					} else {
						game.setIsConnected(false);
					}
					break;

				case "assignNumber":
					playerNumber = Integer.parseInt(tokens[1]);
					System.out.println("[Client] Assigned player number = " + playerNumber);
					break;

				case "bye":
					ghostManager.removeGhostAvatar(UUID.fromString(tokens[1]));
					break;

				case "create":
					UUID remoteId = UUID.fromString(tokens[1]);
					Vector3f pos = new Vector3f(
							Float.parseFloat(tokens[2]),
							Float.parseFloat(tokens[3]),
							Float.parseFloat(tokens[4])
					);
					int number = Integer.parseInt(tokens[5]);

					if (remoteId.equals(this.id)) {
						boolean left = (number == 0);
						game.setLockedX(left ? -5f : 5f);
						game.setLockedZ(-3f);
						game.setLeftSide(left);
						game.getGameBuilder().buildLocalPlayer(number);
					} else {
						try {
							ghostManager.createGhostAvatar(remoteId, pos, number);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;

				case "dsfr":
					UUID remoteIdDsfr = UUID.fromString(tokens[1]);
					if (!remoteIdDsfr.equals(this.id)) {
						int remotePlayerNumber = Integer.parseInt(tokens[2]);
						Vector3f posDsfr = new Vector3f(
								Float.parseFloat(tokens[3]),
								Float.parseFloat(tokens[4]),
								Float.parseFloat(tokens[5])
						);
						try {
							ghostManager.createGhostAvatar(remoteIdDsfr, posDsfr, remotePlayerNumber);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;

				case "bounce":
					UUID bounceId = UUID.fromString(tokens[1]);
					if (bounceId.equals(this.id)) {
						game.getPaddleS().playAnimation("Bounce", 0.25f, AnimatedShape.EndType.PAUSE, 0);
						if (game.getBounceSound() != null) {
							float pitch = 0.9f + (float) Math.random() * 0.2f;
							game.getBounceSound().setPitch(pitch);
							game.getBounceSound().play();
						}
					} else {
						GhostAvatar ghost = ghostManager.findAvatar(bounceId);
						if (ghost != null) ghost.playBounceAnimation();
					}
					break;

				case "playerNumber":
					playerNumber = Integer.parseInt(tokens[1]);
					break;

				case "wsds":
					sendDetailsForMessage(UUID.fromString(tokens[1]), game.getPlayerPosition());
					break;

				case "move":
					ghostManager.updateGhostAvatar(
							UUID.fromString(tokens[1]),
							new Vector3f(
									Float.parseFloat(tokens[2]),
									Float.parseFloat(tokens[3]),
									Float.parseFloat(tokens[4])
							)
					);
					break;

				case "ball":
					Vector3f ballPos = new Vector3f(
							Float.parseFloat(tokens[2]),
							Float.parseFloat(tokens[3]),
							Float.parseFloat(tokens[4])
					);
					game.getBall().setPosition(ballPos);
					break;

				case "score":
					int player = Integer.parseInt(tokens[2]);
					int opponent = Integer.parseInt(tokens[3]);
					game.getHudManager().setScores(player, opponent);
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

	public void sendScoreUpdate(int playerScore, int opponentScore) {
		try {
			sendPacket(String.format("score,%s,%d,%d", id.toString(), playerScore, opponentScore));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBallMessage(Vector3f ballPosition) {
		try {
			sendPacket(String.format("ball,%s,%.3f,%.3f,%.3f",
					id.toString(), ballPosition.x(), ballPosition.y(), ballPosition.z()));
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
			sendPacket(String.format("dsfr,%s,%s,%.3f,%.3f,%.3f",
					remoteId, id, position.x(), position.y(), position.z()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPaddleBounceMessage(UUID paddleOwnerId) {
		try {
			sendPacket("bounce," + paddleOwnerId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMoveMessage(Vector3f position) {
		try {
			sendPacket(String.format("move,%s,%.3f,%.3f,%.3f",
					id, position.x(), position.y(), position.z()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double[] toDoubleArray(float[] arr) {
		double[] result = new double[arr.length];
		for (int i = 0; i < arr.length; i++) result[i] = arr[i];
		return result;
	}
}
