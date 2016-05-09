package com.luminis.echochamber.server;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.sun.deploy.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ConnectionManager {
	private int port;
	private volatile ArrayList<Channel> channels = new ArrayList<>();;
	static final Logger logger = LogManager.getLogger(ConnectionManager.class); // NB: log4j has its own shutdown hook, which we disabled in the config.
	static int maxConnectedClients = 3;
	volatile ArrayList<Session> sessions = new ArrayList<>();
	volatile AccountCollection accounts = new AccountCollection();
	static Channel defaultChannel = new Channel("Default");
	private boolean running = true;

	ConnectionManager(int port) {
		this.port = port;
		channels.add(defaultChannel);
	}

	ConnectionManager(int port, String filename) {
		this(port);
		try {
			List<String> text = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
			accounts = (AccountCollection) JsonReader.jsonToJava(StringUtils.join(text, ""));
		} catch (IOException ex) {
			System.out.println("Can't read from file '" + filename + "'.");
		} catch (JsonIoException ex) {
			System.out.println("Can't read from file '" + filename + "': Account format doesn't match.");
		}
		if (accounts == null) accounts = new AccountCollection();
		ConnectionManager.logger.info("Successfully imported " + accounts.size() + " accounts");
	}

	void start() {
		Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
			@Override
			public void run() {
				shutdown();
			}
		});

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			ConnectionManager.logger.info("Server started.");
			while (running) {
				Socket socket = serverSocket.accept();
				ConnectionManager connectionManager = this;
				if (!running) break;
				if (numberOfConnectedClients() + 1 > maxConnectedClients) {
					PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
					toClient.println("Too many connections. Closing connection");
					toClient.close();
					socket.close();
					ConnectionManager.logger.warn("Maximum number of simultaneous connections reached");
				} else {
					UUID id = Security.createUUID();
					new Thread("Session " + id) {
						public void run() {
							try {
								ConnectionManager.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());

								PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
								BufferedReader fromClient = new BufferedReader(
										new InputStreamReader(socket.getInputStream())
								);

								Session session = new Session(connectionManager, id, toClient, fromClient);

								sessions.add(session);
								session.run();
								sessions.remove(session);

								fromClient.close();
								toClient.close();
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							logger.info("Session terminated");
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

	synchronized void removeAccount(Account account) {
		accounts.remove(account);
		account.delete();
	}

	int numberOfConnectedClients() {
		return sessions.size();
	}

	private void shutdown() {
		ConnectionManager.logger.info("Shutting down...");
		ConnectionManager.logger.info("Saving accounts...");
		try (
				PrintWriter out = new PrintWriter("accounts.json")
		) {
			out.print(JsonWriter.formatJson(JsonWriter.objectToJson(accounts)));
			out.close();
			ConnectionManager.logger.info("Accounts saved successfully");
		} catch (IOException ex) {
			ConnectionManager.logger.error("Cannot write file");
		}
		ConnectionManager.logger.info("Server stopped");
		//shutdown log4j2
		if( LogManager.getContext() instanceof LoggerContext ) {
			logger.info("Shutting down log4j2");
			Configurator.shutdown((LoggerContext)LogManager.getContext());
		} else
			logger.warn("Unable to shutdown log4j2");
	}
}
