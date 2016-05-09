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
import java.util.Date;
import java.util.List;
import java.util.UUID;

class ConnectionManager {
	private int port;
	private Server server;
	private static int maxConnectedClients = 3;
	private ArrayList<Session> sessions = new ArrayList<>();

	static final Logger logger = LogManager.getLogger(ConnectionManager.class); // NB: log4j has its own shutdown hook, which we disabled in the config.

	private ConnectionManager(int port, Server server) {
		this.port = port;
		this.server = server;
	}

	ConnectionManager(int port, String filename, Server server) {
		this(port, server);
		try {
			List<String> text = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
			server.accounts = (AccountCollection) JsonReader.jsonToJava(StringUtils.join(text, ""));
		} catch (IOException ex) {
			System.out.println("Can't read from file '" + filename + "'.");
		} catch (JsonIoException ex) {
			System.out.println("Can't read from file '" + filename + "': Account format doesn't match.");
		}
		if (server.accounts == null) server.accounts = new AccountCollection();
		ConnectionManager.logger.info("Successfully imported " + server.accounts.size() + " accounts");
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
			boolean running = true; // TODO: set to false by exit command on server.
			while (running) {
				Socket socket = serverSocket.accept();
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
								toClient.println(TextColors.colorServermessage(welcomeMessage()));

								Session session = new Session(server, toClient, fromClient);

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

	private int numberOfConnectedClients() {
		return sessions.size();
	}

	private void shutdown() {
		ConnectionManager.logger.info("Shutting down...");
		ConnectionManager.logger.info("Saving accounts...");
		try (
				PrintWriter out = new PrintWriter("accounts.json")
		) {
			out.print(JsonWriter.formatJson(JsonWriter.objectToJson(server.accounts)));
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
