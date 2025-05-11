import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID>
{
	private Vector<UUID> players = new Vector<>();

	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
	}

	int getPlayerNumber(UUID clientID) {
		return players.indexOf(clientID);
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String)o;
		String[] messageTokens = message.split(",");

		if (messageTokens.length > 0) {
			if(messageTokens[0].equals("join")) {
				try {
					IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);

					if (players.size() < 2) {
						addClient(ci, clientID);
						players.add(clientID);
						System.out.println("Join request from - " + clientID);
						sendJoinedMessage(clientID, true);
					} else {
						System.out.println("Join request from - " + clientID + " but server is full");
						sendJoinedMessage(clientID, false);
					}
				} catch (IOException e) { e.printStackTrace(); }
			}

			if(messageTokens[0].equals("bye")) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("Exit request from - " + clientID);
				sendByeMessages(clientID);
				removeClient(clientID);
				players.remove(clientID);
			}

			if (messageTokens[0].equals("ball")) {
				try {
					UUID senderId = UUID.fromString(messageTokens[1]);
					forwardPacketToAll(message, senderId);
				} catch (IOException e) { e.printStackTrace(); }
			}

			if(messageTokens[0].equals("create")) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = new String[3];

				int playerIndex = players.indexOf(clientID);
				if (playerIndex == 0) {
					pos[0] = "-4";
					pos[1] = "0";
					pos[2] = "0";
				} else if (playerIndex == 1) {
					pos[0] = "4";
					pos[1] = "0";
					pos[2] = "0";
				} else {
					pos[0] = messageTokens[2];
					pos[1] = messageTokens[3];
					pos[2] = messageTokens[4];
				}

				sendCreateMessages(clientID, pos);
				sendWantsDetailsMessages(clientID);
			}

			if(messageTokens[0].equals("dsfr")) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				sendDetailsForMessage(clientID, remoteID, pos);
			}

			if(messageTokens[0].equals("move")) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				sendMoveMessages(clientID, pos);
			}
		}
	}

	public void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = "join," + (success ? "success" : "failure");
			sendPacket(message, clientID);

			if (success) {
				int number = getPlayerNumber(clientID);
				sendAssignNumber(clientID, number);
			}
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendAssignNumber(UUID clientID, int number) {
		try {
			String message = "assignNumber," + number;
			sendPacket(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendByeMessages(UUID clientID) {
		try {
			String message = "bye," + clientID;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendCreateMessages(UUID clientID, String[] position) {
		try {
			String message = "create," + clientID + "," +
					position[0] + "," +
					position[1] + "," +
					position[2] + "," +
					getPlayerNumber(clientID);
			sendPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position) {
		try {
			String message = "dsfr," + remoteId + "," +
					getPlayerNumber(clientID) + "," +
					position[0] + "," +
					position[1] + "," +
					position[2];
			sendPacket(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendWantsDetailsMessages(UUID clientID) {
		try {
			String message = "wsds," + clientID;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public void sendMoveMessages(UUID clientID, String[] position) {
		try {
			String message = "move," + clientID + "," +
					position[0] + "," +
					position[1] + "," +
					position[2];
			forwardPacketToAll(message, clientID);
		} catch (IOException e) { e.printStackTrace(); }
	}
}