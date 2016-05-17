package com.luminis.echochamber.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;

class ConnectionManager {
	private int port;
	private Server server;
	public static int maxConnectedClients = 3;

	public ConnectionManager(int port, Server server) {
		this.port = port;
		this.server = server;
	}

	private class Connection implements Runnable {
		private Socket socket;
		private Client client;
		private Thread connectionThread;

		Connection(Socket socket, UUID id) {
			this.socket = socket;

			client = new Client(server, id);
			connectionThread = new Thread(this, "Client " + id);
			connectionThread.start();
		}

		@Override
		public void run() {
			Main.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
			try (
					PrintWriter toRemote = new PrintWriter(socket.getOutputStream(), true);
					BufferedReader fromRemote = new BufferedReader(
							new InputStreamReader(socket.getInputStream())
					)
			) {
				Main.logger.info("Server has opened a connection to client");

				while (client.isActive()) {
					try {
						socket.setSoTimeout(100); // socket timeout to prevent readLine() from blocking
						if (client.outputForRemoteAvailable()) {
							toRemote.println(client.outputForRemote());
						}

						String input = fromRemote.readLine();
						if (input == null) {
							Main.logger.info("Client has unexpectedly disconnected from the server");
							break;
						} else {
							client.inputFromRemote(input);
						}
					} catch (SocketTimeoutException e) {

					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
				client.cleanup();
				fromRemote.close();
				toRemote.close();
				socket.close();

				Main.logger.info("Server has closed the connection to client");
			} catch (IOException e) {
				e.printStackTrace();
			}
			Main.logger.info("Session terminated");
		}
	}

	void start() {
		Main.logger.info("Listening for connections.");
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			serverSocket.setSoTimeout(100); // When we shutdown the ConnectionManager, we want it to end the blocking call to accept() and terminate the loop
			while (server.isActive()) {
				try {
					Socket socket = serverSocket.accept();
					if (!server.isActive()) break; // to prevent it from accepting a connection while the rest of the system is shutting down

					if (server.numberOfClients() >= maxConnectedClients) {
						PrintWriter toRemote = new PrintWriter(socket.getOutputStream(), true);
						toRemote.println("Too many connections. Closing connection");
						toRemote.close();
						socket.close();
						Main.logger.warn("Maximum number of simultaneous connections reached");
					} else {
						new Connection(socket, Security.createUUID());
					}
				} catch (SocketTimeoutException s) {

				}
			}
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
		Main.logger.info("Stopped listening for connections");
	}
}