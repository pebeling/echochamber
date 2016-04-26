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
	volatile boolean running;

	Session(Socket socket, Server server) {
		super("Session");
		this.socket = socket;
		this.server = server;
		channel = null;
		account = null;
		id = Security.createUUID();
		super.setName("Session " + id);
		running = true;
	}

	public void run() {
		try {
			server.addSession(this);
			Server.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
			toClient = new PrintWriter(socket.getOutputStream(), true);
			fromClient = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);
			Protocol protocol = new Protocol(this);

			messageClient(TextColors.colorServermessage(protocol.welcomeMessage()));

			String inputLine, outputLine;
			while (running) {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					Server.logger.info("Client in session at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					messageClient("Disconnected by client");
					running = false;
				} else {
					outputLine = protocol.evaluateInput(inputLine.replaceAll("\\p{C}", "")); // strip non-printable characters by unicode regex
					if (outputLine == null) {
						Server.logger.info("Server has closed the connection to client");
						messageClient("Disconnected by server");
						running = false;
					}
					else if (!outputLine.equals("")) {
						messageClient(TextColors.colorServermessage(outputLine));
					}
				}
			}

			protocol.close();
			fromClient.close();
			toClient.close();
			socket.close();
			Server.logger.info("Session terminated");
			server.removeSession(this);
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
			if (!this.account.isPermanent()) {
				this.server.removeAccount(this.account);
			}
			this.account = null;;
		}
		else Server.logger.warn("Session not bound to an account");
	}

	void messageClient(String message){
		if (toClient != null) {
			toClient.println(message);
		} else {
			Server.logger.warn("Message sent to disconnected client");
		}
	}

	void exit() {
		running = false;
	}
}
