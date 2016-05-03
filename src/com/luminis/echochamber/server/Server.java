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

class Server {
	static final Logger logger = LogManager.getLogger(Server.class); // NB: log4j has its own shutdown hook, which we disabled in the config.
	private int port;
	static int maxConnectedClients = 3;
	private volatile int numberOfConnectedClients = 0;
	volatile ArrayList<Session> sessions;
	volatile ArrayList<Account> accounts;
	private volatile ArrayList<Channel> channels;
	static Channel defaultChannel = new Channel("Default");
	private volatile boolean running;

	Server(int port) {
		this.port = port;
		sessions = new ArrayList<>();
		accounts = new ArrayList<>();
		channels = new ArrayList<>();
		channels.add(defaultChannel);
		running = true;
	}

	Server(int port, String filename) {
		this(port);
		try {
			List<String> text = Files.readAllLines(Paths.get("accounts.json"), StandardCharsets.UTF_8);
			accounts = (ArrayList<Account>) JsonReader.jsonToJava(StringUtils.join(text, ""));
		} catch (IOException ex) {
			System.out.println("Can't read from file '" + filename + "'.");
		} catch (JsonIoException ex) {
			System.out.println("Can't read from file '" + filename + "': Account format doesn't match.");
		}
		if (accounts == null) accounts = new ArrayList<>();
		Server.logger.info("Successfully imported " + accounts.size() + " accounts");
	}

	void start() {
		Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
			@Override
			public void run() {
				shutdown();
			}
		});

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Server.logger.info("Server started.");
			while (running) {
				Socket socket = serverSocket.accept();
				if (!running) break;
				if (numberOfConnectedClients + 1 > maxConnectedClients) {
					PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
					toClient.println("Too many connections. Closing connection");
					toClient.close();
					socket.close();
					Server.logger.warn("Maximum number of simultaneous connections reached");
				} else {
					new Session(socket, this).start();
				}
			}
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}

	synchronized Account getAccountByName(String username) {
		for (Account account : accounts) {
			if (account.getName().equals(username)) {
				return account;
			}
		}
		return null;
	}

	synchronized void addSession(Session session) {
		numberOfConnectedClients++;
		sessions.add(session);
	}

	synchronized void removeSession(Session session) {
		sessions.remove(session);
		numberOfConnectedClients--;
	}

	synchronized void addAccount(Account account) throws Exception {
		Account accountExists = getAccountByName(account.getName());
		if (accountExists == null) {
			accounts.add(account);
		} else {
			logger.warn("Can't add account " + account + " to server because another account with the same username exists: " + accountExists);
			throw new Exception();
		}
	}

	synchronized void removeAccount(Account account) {
		accounts.remove(account);
		account.delete();
	}

	int numberOfConnectedClients() {
		return numberOfConnectedClients;
	}

	private void shutdown() {
		Server.logger.info("Shutting down...");
		Server.logger.info("Saving accounts...");
		try (
				PrintWriter out = new PrintWriter("accounts.json")
		) {
			out.print(JsonWriter.formatJson(JsonWriter.objectToJson(accounts)));
			out.close();
			Server.logger.info("Accounts saved successfully");
		} catch (IOException ex) {
			Server.logger.error("Cannot write file");
		}
		Server.logger.info("Server stopped");
		//shutdown log4j2
		if( LogManager.getContext() instanceof LoggerContext ) {
			logger.info("Shutting down log4j2");
			Configurator.shutdown((LoggerContext)LogManager.getContext());
		} else
			logger.warn("Unable to shutdown log4j2");
	}
}

//	Channel getChannelByName(String channelname) {
//		for (Channel channel : channels) {
//			if (channel.name.equals(channelname)) {
//				return channel;
//			}
//		}
//		return null;
//	}

