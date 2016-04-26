package com.luminis.echochamber.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

class Channel {
	private volatile ArrayList<Session> connectedSessions;
	private String name;

	Channel(String channelName) {
		connectedSessions = new ArrayList<>();
		name = channelName;
	}

	@Override
	public String toString() {
		return "[" + name + "]";
	}

	synchronized void subscribe(Session session) {
		if (!connectedSessions.contains(session)) {
			connectedSessions.add(session);
			broadcast("User " + TextColors.colorUserName(session.account.getName()) + " joined channel " + this);
		}
	}

	synchronized void unSubscribe(Session session) {
		if (connectedSessions.contains(session)) {
			broadcast("User " + TextColors.colorUserName(session.account.getName()) + " left channel " + this);
			connectedSessions.remove(session);
		}
	}

	synchronized void shout(String message, Session sender) {
		broadcast(TextColors.colorUserName(sender.account.getName()) + "> " + message);
	}

//	synchronized private void broadcast(String messageClient, Session sender) {
//		connectedSessions.stream().filter(
//				session -> !session.equals(sender)
//		).forEach(
//				session -> session.messageClient(messageClient)
//		);
//	}

	synchronized private void broadcast(String message) {
		connectedSessions.stream().forEach(
				session -> session.messageClient(message)
		);
	}

//	synchronized public ArrayList<String> listSessions() {
//		return connectedSessions.stream().map(session -> session.account.getName()).collect(Collectors.toCollection(ArrayList::new));
//	}

	ArrayList<Session> getConnectedSessions() {
		return connectedSessions;
	}
}

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

		try (
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer)
		) {
			accounts = (ArrayList<Account>)input.readObject();
		}
		catch(ClassNotFoundException ex){
			System.out.println("Can't read from file '" + filename + "': Class not found");
		}
		catch(IOException ex){
			System.out.println("Can't read from file '" + filename + "'.");
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
				}
				else {
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

	synchronized void addAccount(Account account) {
		accounts.add(account);
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
			OutputStream file = new FileOutputStream("accounts.ser");
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer)
		) {
			output.writeObject(accounts);
			Server.logger.info("Accounts saved successfully");
		}
		catch(NotSerializableException ex) {
			Server.logger.error("Class not serializable");
		}
		catch(IOException ex){
			Server.logger.error("Cannot write file");
		}
		Server.logger.info("Server stopped");
		LogManager.shutdown();
	}

	synchronized void exit() {
		Server.logger.info("Logging everyone out");
		sessions.forEach(Session::exit);
		running = false;
	}

//	Channel getChannelByName(String channelname) {
//		for (Channel channel : channels) {
//			if (channel.name.equals(channelname)) {
//				return channel;
//			}
//		}
//		return null;
//	}
}
