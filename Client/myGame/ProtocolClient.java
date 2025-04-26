package myGame;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import org.joml.Math;
import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private int playerNumber = -1;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }

	@Override
	protected void processPacket(Object message)
	{
		if (message == null) {
			System.out.println("Warning: received null packet");
			return;
		}

		String strMessage = (String) message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");

		if (messageTokens.length > 0)
		{
			if (messageTokens[0].compareTo("join") == 0)
			{
				if (messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if (messageTokens[1].compareTo("failure") == 0)
				{
					System.out.println("join failure confirmed");
					game.setIsConnected(false);
				}
			}

			if (messageTokens[0].compareTo("bye") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}

			if (messageTokens[0].compareTo("create") == 0)
			{
				UUID remoteId = UUID.fromString(messageTokens[1]);
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4])
				);

				if (remoteId.equals(this.id))  // local player
				{
					game.lockedX = pos.x();
					game.lockedZ = pos.z();
				}


				this.playerNumber = Integer.parseInt(messageTokens[5]);
				System.out.println("remoteId: " + remoteId.toString() + " playerNumber: " + playerNumber);

				if (remoteId.equals(this.id))
				{
					System.out.println("Setting local avatar position from server to: " + pos);
					Matrix4f avatarMat = new Matrix4f().translation(pos);
					double[] avatarTransform = toDoubleArray(avatarMat.get(vals));
					game.getAvatar().getPhysicsObject().setTransform(avatarTransform);

					if (playerNumber == 0)
					{
						Matrix4f initialRot = new Matrix4f()
								.rotateY((float) Math.toRadians(90))
								.rotateX((float) Math.toRadians(90));
						game.getAvatar().setLocalRotation(initialRot);
						game.setAvatarOriginalRotation(initialRot);
					}
					else if (playerNumber == 1)
					{
						Matrix4f initialRot = new Matrix4f()
								.rotateY((float) Math.toRadians(90))
								.rotateX((float) Math.toRadians(-90));
						game.getAvatar().setLocalRotation(initialRot);
						game.setAvatarOriginalRotation(initialRot);
					}

				}
				else
				{
					try
					{
						boolean ghostLeftSide = (playerNumber == 0);
						ghostManager.createGhostAvatar(remoteId, pos, ghostLeftSide);
					}
					catch (IOException e)
					{
						System.out.println("error creating ghost avatar");
					}
				}
			}

			if (messageTokens[0].compareTo("dsfr") == 0)
			{
				UUID remoteId = UUID.fromString(messageTokens[1]);
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4])
				);

				if (remoteId.equals(this.id))
				{
					System.out.println("Setting local avatar position from server to: " + pos);
				}
				else
				{
					try
					{
						boolean ghostLeftSide = (pos.x() < 0); // Temporary guess
						ghostManager.createGhostAvatar(remoteId, pos, ghostLeftSide);
					}
					catch (IOException e)
					{
						System.out.println("error creating ghost avatar");
					}
				}
			}

			if (messageTokens[0].compareTo("playerNumber") == 0)
			{
				playerNumber = Integer.parseInt(messageTokens[1]);
				System.out.println("Received player number: " + playerNumber);
			}

			if (messageTokens[0].compareTo("wsds") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}

			if (messageTokens[0].compareTo("move") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4])
				);
				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
			}
		}
	}

	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client s Avatar s position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	private float[] vals = new float[16];
}
