package com.luminis.echochamber.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.UUID;

class ConnectionManager {
	private int port;
	private Server server;
	private static int maxConnectedClients = 3;

	ConnectionManager(int port, Server server) {
		this.port = port;
		this.server = server;
	}

	void start() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Main.logger.info("Listening for connections.");
			boolean running = true; // TODO: set to false by exit command on server.
			while (running) {
				Socket socket = serverSocket.accept();
				if (!running) break;
				if (numberOfConnectedClients() + 1 > maxConnectedClients) {
					PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
					toClient.println("Too many connections. Closing connection");
					toClient.close();
					socket.close();
					Main.logger.warn("Maximum number of simultaneous connections reached");
				} else {
					UUID id = Security.createUUID();
					new Thread("Session " + id) {
						public void run() {
							try {
								Main.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());

								PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
								BufferedReader fromClient = new BufferedReader(
										new InputStreamReader(socket.getInputStream())
								);

								Session session = new Session(server);
								toClient.println(TextColors.colorServermessage(welcomeMessage()));

								String input;

								while (session.isActive()) {
									try {
										input = fromClient.readLine();
										if (input == null) {
											Main.logger.info("Client has disconnected from server");
											break;
										} else {
											session.receive(input);
											toClient.println(session.output); // TODO: temporary, should go via Server
										}
									} catch (IOException e) {
										e.printStackTrace();
										break;
									}
								}
								Main.logger.info("Server has closed the connection to client");

								session.end();

								fromClient.close();
								toClient.close();
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							Main.logger.info("Session terminated");
						}
					}.start();
				}
			}
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}

	private int numberOfConnectedClients() {
		return server.sessions.size();
	}

	private String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + numberOfConnectedClients() + " of " + maxConnectedClients + ".",
				"Use /help or /help <command> for more information.",
				"--------------------------------------------------"
		};
		return String.join("\n", lines);
	}
}
