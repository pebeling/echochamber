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
		super("Session");
		this.socket = socket;
		this.server = server;
		channel = null;
		account = null;
		id = Security.createUUID();
		super.setName("Session " + id);
	}

	public void run() {
		try {
			server.addSession(id);
			Server.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
			toClient = new PrintWriter(socket.getOutputStream(), true);
			fromClient = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);
			Protocol protocol = new Protocol(this);

			messageToClient(TextColors.colorServermessage(protocol.welcomeMessage()));

			String inputLine, outputLine;
			while (true) {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					Server.logger.info("Client in session at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					messageToClient("Disconnected by client");
					break;
				} else {
					outputLine = protocol.evaluateInput(inputLine);
					if (outputLine == null) {
						Server.logger.info("Server has closed the connection to client");
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
			Server.logger.info("Session terminated");
			server.removeSession(id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return id.toString();
	}

	void connectToChannel(Channel channel) {
		if (this.channel == null) {
			this.channel = channel;
			channel.subscribe(this);
			Server.logger.info("Session bound to channel " + channel);
		}
		else Server.logger.warn("Session already bound to channel " + this.channel);
	}

	void disconnectFromChannel() {
		if (this.channel != null) {
			Server.logger.info("Session unbound from channel " + channel);
			channel.unSubscribe(this);
			channel = null;
		}
		else Server.logger.warn("Session not bound to a channel");
	}

	void broadcastToChannel(String argument) {
		channel.shout(argument, this);
	}

	void message(String message) {
		messageToClient(message);
	}

	ArrayList<Session> sessionsInSameChannel() {
		return channel.getConnectedSessions();
	}

//	ArrayList<Session> sessionsInChannel(Channel channel) {
//		return channel.connectedSessions;
//	}

	void setAccount(Account account) {
		if (this.account == null) {
			this.account = account;
			account.login(this);
			Server.logger.info("Session bound to account " + account);
		}
		else Server.logger.warn("Session already bound to account " + account);
	}

	void unSetAccount() {
		if (this.account != null) {
			Server.logger.info("Session unbound from account " + account);
			this.account.logout();
			this.account = null;
		}
		else Server.logger.warn("Session not bound to an account");
	}

	private void messageToClient(String message){
		toClient.println(message);
	}
}
