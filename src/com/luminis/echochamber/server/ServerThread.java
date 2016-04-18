package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class ServerThread extends Thread {
	private Socket socket = null;
	Server server;
	SimpleChannel channel;
	private PrintWriter toClient;
	public String nickName;

	public ServerThread(Socket socket, Server server) {
		super("EchoChamberServerThread");
		this.socket = socket;
		this.server = server;
		channel = server.channel;
	}

	public void run() {
		try {
			UUID id = UUID.randomUUID();
			server.idList.add(id);

			server.serverConsole("Client " + id + " at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has connected to server.");

			toClient = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader fromClient = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);
			String inputLine, outputLine;
			Protocol protocol = new Protocol(this);

			toClient.println(protocol.welcomeMessage());

			while (true) {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					server.serverConsole("Client " + id + " at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					toClient.println("Disconnected by client");
					disconnectFromChannel();
					break;
				} else {
					outputLine = protocol.evaluateInput(inputLine);
					if (outputLine == null) {
						server.serverConsole("Server has closed the connection to client " + id + ".");
						toClient.println("Disconnected by server");
						break;
					}
					else if (!outputLine.equals("")) {
						toClient.println(TextColors.colorServermessage(outputLine));
					}
				}
			}
			server.idList.remove(id);
			toClient.close();
			fromClient.close();
			socket.close();
			server.numberOfConnectedClients--;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void connectToChannel() {
		channel.subscribeToChannel(this);
	}

	void disconnectFromChannel() {
		channel.unSubscribeToChannel(this);
	}

	void broadcastToChannel(String argument) {
		channel.shout(argument, this);
	}

	void message(String message) {
		toClient.println(message);
	}

	ArrayList<String> listSessions() {
		return channel.listSessions();
	}
}
