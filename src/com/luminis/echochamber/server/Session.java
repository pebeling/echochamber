package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

class Session extends Thread {
	Server server;
	Channel channel;
	Account account;
	private Socket socket = null;
	private PrintWriter toClient;
	private BufferedReader fromClient;
	private UUID id;

	Session(Socket socket, Server server) {
		super("EchoChamberSessionThread");
		this.socket = socket;
		this.server = server;
		channel = null;
		account = null;
		id = Security.createUUID();
	}

	public void run() {
		try {
			server.numberOfConnectedClients++;
			server.channelIDs.add(id);
			server.serverConsole("Session " + id + " started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
			toClient = new PrintWriter(socket.getOutputStream(), true);
			fromClient = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);
			Protocol protocol = new Protocol(this);

			messageToClient(protocol.welcomeMessage());

			String inputLine, outputLine;
			while (true) {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					server.serverConsole("Client in session " + id + " at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					messageToClient("Disconnected by client");
					break;
				} else {
					outputLine = protocol.evaluateInput(inputLine);
					if (outputLine == null) {
						server.serverConsole("Server has closed the connection to client in session " + id + ".");
						messageToClient("Disconnected by server");
						break;
					}
					else if (!outputLine.equals("")) {
						messageToClient(TextColors.colorServermessage(outputLine));
					}
				}
			}

			protocol.close();
			fromClient.close();
			toClient.close();
			socket.close();
			server.serverConsole("Session " + id + " terminated");
			server.channelIDs.remove(id);
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
		messageToClient(message);
	}

	ArrayList<Session> sessionsInSameChannel() {
		return channel.connectedSessions;
	}

//	ArrayList<Session> sessionsInChannel(Channel channel) {
//		return channel.connectedSessions;
//	}

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

	private void messageToClient(String message){
		toClient.println(message);
	}
}
