package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class Session extends Thread {
	private Socket socket = null;
	Server server;
	Channel channel;
	PrintWriter toClient;
	Account account;
	private UUID id;

	public Session(Socket socket, Server server) {
		super("EchoChamberSessionThread");
		this.socket = socket;
		this.server = server;
		channel = null;
		id = Security.createUUID();

		server.channelIDs.add(id);
	}

	public void run() {
		try {
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
			server.channelIDs.remove(id);
			toClient.close();
			fromClient.close();
			socket.close();
			server.numberOfConnectedClients--;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void connectToChannel(Channel channel) {
		if (this.channel == null) {
			this.channel = channel;
			channel.subscribe(this);
			server.serverConsole("Session " + id + " bound to channel " + channel.name);
		}
		else server.serverConsole("Warning: Session " + id + " already bound to channel " + this.channel);
	}

	void disconnectFromChannel() {
		if (this.channel != null) {
			server.serverConsole("Session " + id + " unbound from channel " + channel.name);
			channel.unSubscribe(this);
			this.channel = null;
		}
		else server.serverConsole("Warning: Session " + id + " not bound to a channel");
	}

	void broadcastToChannel(String argument) {
		channel.shout(argument, this);
	}

	void message(String message) {
		toClient.println(message);
	}

	ArrayList<Session> sessionsInSameChannel() {
		return channel.connectedSessions;
	}

	ArrayList<Session> sessionsInChannel(Channel channel) {
		return channel.connectedSessions;
	}

	void setAccount(Account account) {
		if (this.account == null) {
			this.account = account;
			account.login(this);
			server.serverConsole("Session " + id + " bound to account " + account.id);
		}
		else server.serverConsole("Warning: Session " + id + " already bound to account " + this.id);
	}

	void unSetAccount() {
		if (this.account != null) {
			server.serverConsole("Session " + id + " unbound from account " + account.id);
			this.account.logout();
			this.account = null;
		}
		else server.serverConsole("Warning: Session " + id + " not bound to an account");
	}
}
