package com.luminis.echochamber.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

class Account {
	UUID id;
	final Date creationDate;
	Server server;
	Session currentSession;

	private String username;
	private byte[] salt;
	private byte[] passwordHash;

	boolean temporary;
	boolean online;
	Date lastLoginDate;

	ArrayList<Account> friends = null;
	ArrayList<Account> pendingSentFriendRequests = null;
	ArrayList<Account> pendingReceivedFriendRequests = null;

	Account(Server server, String username, byte[] pwd) throws Exception {
		if (pwd != null) {
			salt = Security.getNewSalt();
			passwordHash = Security.calculateHash(Security.saltPassword(salt, pwd));
			System.out.println(passwordHash);
			temporary = false;

			friends = new ArrayList<>();
			pendingSentFriendRequests = new ArrayList<>();
			pendingReceivedFriendRequests = new ArrayList<>();
		} else {
			salt = null;
			passwordHash = null;
			temporary = true;
		}

		for (Account account : server.accounts) {
			if (username.equals(account.username)) throw new Exception();
		}

		id = Security.createUUID();

		this.username = username;
		creationDate = new Date();
		currentSession = null;

		this.server = server;

		online = false;
		lastLoginDate = null;

		this.server.accounts.add(this);
		this.server.serverConsole("Created " + (temporary ? "temporary" : "persistent") + " account " + id + " for user " + this.username);
	}

	Account(Server server, String username) throws Exception { // Create temporary account that is non-persistent and has no friend information
		this(server, username, null);
	}

	void delete() {
		server.serverConsole("Deleted " + (temporary ? "temporary" : "persistent") + " account " + id + " for user " + username);

		username = null;
		salt = null;
		passwordHash = null;

		if (!temporary) {
			// can't use friends.forEach((friend) -> unfriend(friend)); because of ConcurrentModificationException
			Iterator<Account> friendIterator = friends.iterator();
			while (friendIterator.hasNext()) {
				Account friend = friendIterator.next();
				friendIterator.remove();
				friend.friends.remove(this);
			}
			friendIterator = pendingSentFriendRequests.iterator();
			while (friendIterator.hasNext()) {
				Account friend = friendIterator.next();
				friendIterator.remove();
				friend.pendingReceivedFriendRequests.remove(this);
			}
			friendIterator = pendingReceivedFriendRequests.iterator();
			while (friendIterator.hasNext()) {
				Account friend = friendIterator.next();
				friendIterator.remove();
				friend.pendingSentFriendRequests.remove(this);
			}

			friends = null;
			pendingSentFriendRequests = null;
			pendingReceivedFriendRequests = null;
		}
		server.accounts.remove(this);
	}

	public String getName() {
		return username;
	}

	public void login(Session session){
		if(currentSession == null) {
			currentSession = session;
			online = true;
			lastLoginDate = new Date();
		}
	}

	public void logout() {
		if (currentSession != null) {
			if (temporary) delete();
			else {
				currentSession = null;
				online = false;
			}
		}
	}

	public boolean checkPassword(byte[] pwd) {
		byte[] hashedPassword = Security.calculateHash(Security.saltPassword(salt, pwd));
		boolean passwordMatch = passwordHash != null && hashedPassword.length == passwordHash.length;
		for(int i=0; i < hashedPassword.length; i++) {
			passwordMatch = passwordMatch && (hashedPassword[i] == passwordHash[i]);
		}
		server.serverConsole((passwordMatch?"SUCCESSFUL":"FAILED") + " authentication attempt for account " + id + "(" + username + ")");
		return passwordMatch;
	}

	public void sendFriendRequest(Account account) {
		if (!friends.contains(account) && account != this && !account.temporary && !this.temporary) {
			pendingSentFriendRequests.add(account);
			account.pendingReceivedFriendRequests.add(this);
		}
	}

	public void cancelFriendRequest(Account account) {
		if (!account.temporary && !this.temporary) {
			pendingSentFriendRequests.remove(account);
			account.pendingReceivedFriendRequests.remove(this);
		}
	}

	public void acceptFriendRequest(Account account) {
		if (!account.temporary && !this.temporary) {
			boolean pendingHere = pendingReceivedFriendRequests.remove(account);
			boolean pendingThere = account.pendingSentFriendRequests.remove(this);
			if (pendingHere && pendingThere) {
				friends.add(account);
				account.friends.add(this);
			}
		}
	}

	public void refuseFriendRequest(Account account) {
		if (!account.temporary && !this.temporary) {
			pendingReceivedFriendRequests.remove(account);
			account.pendingSentFriendRequests.remove(this);
		}
	}

	public void unfriend(Account account) {
		if (!account.temporary && !this.temporary) {
			friends.remove(account);
			account.friends.remove(this);
		}
	}

	public void updateLastLoginDate(Date date) {
		lastLoginDate = date;
	}

	public void makePermanent(Server server, byte[] pwd) {
		if (temporary) {
			salt = Security.getNewSalt();
			passwordHash = Security.calculateHash(Security.saltPassword(salt, pwd));
			temporary = false;

			friends = new ArrayList<>();
			pendingSentFriendRequests = new ArrayList<>();
			pendingReceivedFriendRequests = new ArrayList<>();
			server.serverConsole("Changed temporary account " + id + " for user " + username + " to permanent");
		}
		else server.serverConsole("Warning: account " + id + " for user " + username + " is already a permanent account");
	}
}

class Channel {
	volatile ArrayList<Session> connectedSessions;
	public String name;

	Channel(String channelName) {
		connectedSessions = new ArrayList<>();
		name = channelName;
	}

	public void subscribe(Session session) {
		if (connectedSessions.contains(session)) {

		}
		connectedSessions.add(session);
		broadcast("User " + TextColors.colorUserName(session.account.getName()) + " joined channel " + name);
	}

	public void unSubscribe(Session session) {
		if (connectedSessions.contains(session)) {
			broadcast("User " + TextColors.colorUserName(session.account.getName()) + " left channel " + name);
			connectedSessions.remove(session);
		}
	}

	public void shout(String message, Session sender) {
		broadcast(TextColors.colorUserName(sender.account.getName()) + "> " + message, sender);
	}

	private void broadcast(String message, Session sender) {
		connectedSessions.stream().filter(
				session -> !session.equals(sender)
		).forEach(
				session -> session.message(message)
		);
	}

	private void broadcast(String message) {
		connectedSessions.stream().forEach(
				session -> session.message(message)
		);
	}

	public ArrayList<String> listSessions() {
		return connectedSessions.stream().map(session -> session.account.getName()).collect(Collectors.toCollection(ArrayList::new));
	}
}

public class Server {
	private int port;
	static int maxConnectedClients = 3;
	volatile int numberOfConnectedClients = 0;
	volatile ArrayList<UUID> channelIDs;
	volatile ArrayList<Account> accounts;
	volatile ArrayList<Channel> channels;
	static Channel defaultChannel;

	Server(int port) {
		this.port = port;
		channelIDs = new ArrayList<>();
		accounts = new ArrayList<>();
		channels = new ArrayList<>();
		defaultChannel = new Channel("Default");
		channels.add(defaultChannel);
	}

	void start() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			serverConsole("Server started.");
			while (true) {
				Socket socket = serverSocket.accept();
				numberOfConnectedClients++;
				if (numberOfConnectedClients > maxConnectedClients) {
					PrintWriter toClient = new PrintWriter(socket.getOutputStream(), true);
					toClient.println("Too many connections. Closing connection");
					toClient.close();
					socket.close();
					serverConsole("Maximum number of simultaneous connections reached");
					numberOfConnectedClients--;
				}
				else {
					new Session(socket, this).start();
				}
			}
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}

	void serverConsole(String output) {
		System.out.println(new Date() + ": " + output);
	}

	Account getAccountByName(String username) {
		for (Account account : accounts) {
			if (account.getName().equals(username)) {
				return account;
			}
		}
		return null;
	}
	Channel getChannelByName(String channelname) {
		for (Channel channel : channels) {
			if (channel.name.equals(channelname)) {
				return channel;
			}
		}
		return null;
	}
}
