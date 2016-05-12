package com.luminis.echochamber.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

class ConnectionManager {
	private int port;
	private Server server;
	public static int maxConnectedClients = 3;

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
				if (server.numberOfClients() >= maxConnectedClients) {
					PrintWriter toRemote = new PrintWriter(socket.getOutputStream(), true);
					toRemote.println("Too many connections. Closing connection");
					toRemote.close();
					socket.close();
					Main.logger.warn("Maximum number of simultaneous connections reached");
				} else {
					UUID id = Security.createUUID();
					new Thread("Client " + id) {
						public void run() {
							Main.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
							try (
								PrintWriter toRemote = new PrintWriter(socket.getOutputStream(), true);
								BufferedReader fromRemote = new BufferedReader(
										new InputStreamReader(socket.getInputStream())
								)
							) {
								Client client = new Client(server);

								while (client.isActive()) {
									try {
										if (socket.getInputStream().available() > 0) { // TODO: to make read non blocking. Broke check for disconnect however
											String input = fromRemote.readLine();
											if (input == null) {
												Main.logger.info("Client has disconnected from server");
												break;
											} else {
												client.receive(input);
											}
										}
										if (client.outputAvailable()) {
											toRemote.println(client.emit());
										}
									} catch (IOException e) {
										e.printStackTrace();
										break;
									}
								}
								Main.logger.info("Server has closed the connection to client");

								client.end();

								fromRemote.close();
								toRemote.close();
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
}
