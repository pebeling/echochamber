package com.luminis.echochamber.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

class Account implements Serializable {
	private final UUID id;
	private final Date creationDate;
	transient Session currentSession;

	private String username;
	private byte[] salt;
	private byte[] passwordHash;

	private boolean permanent;
	transient private boolean online;
	Date lastLoginDate;

	ArrayList<Account> friends = null;
	ArrayList<Account> pendingSentFriendRequests = null;
	ArrayList<Account> pendingReceivedFriendRequests = null;

	Account(Server server, String username, byte[] pwd) throws Exception {
		if (pwd != null) {
			salt = Security.getNewSalt();
			passwordHash = Security.calculateHash(Security.saltPassword(salt, pwd));
			permanent = true;

			friends = new ArrayList<>();
			pendingSentFriendRequests = new ArrayList<>();
			pendingReceivedFriendRequests = new ArrayList<>();
		} else {
			salt = null;
			passwordHash = null;
			permanent = false;
		}

		if (!(server.getAccountByName(username) == null)) throw new Exception();

		id = Security.createUUID();

		this.username = username;
		creationDate = new Date();
		currentSession = null;

		online = false;
		lastLoginDate = null;

		Server.logger.info("Created " + (permanent ? "persistent" : "temporary") + " account " + this);
	}

	Account(Server server, String username) throws Exception { // Create non-persistent account that has no friend information
		this(server, username, null);
	}

	@Override
	public String toString() {
		return id.toString() + "[" + username + "]";
	}

	synchronized void delete() {
		Server.logger.info("Deleted " + (permanent ? "persistent" : "temporary") + " account " + this);

		username = null;
		salt = null;
		passwordHash = null;

		if (permanent) {
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
	}

	String getName() {
		return username;
	}

	synchronized void login(Session session){
		if(currentSession == null) {
			currentSession = session;
			online = true;
			lastLoginDate = new Date();
		}
	}

	synchronized void logout() {
		currentSession = null;
		online = false;
	}

	boolean checkPassword(byte[] pwd) {
		byte[] hashedPassword = Security.calculateHash(Security.saltPassword(salt, pwd));
		boolean passwordMatch = passwordHash != null && hashedPassword.length == passwordHash.length;
		for(int i=0; i < hashedPassword.length; i++) {
			passwordMatch = passwordMatch && (hashedPassword[i] == passwordHash[i]);
		}
		Server.logger.info((passwordMatch?"SUCCESSFUL":"FAILED") + " authentication attempt for account " + this);
		return passwordMatch;
	}

	synchronized void sendFriendRequest(Account account) {
		if (!friends.contains(account) && account != this && account.permanent && this.permanent) {
			pendingSentFriendRequests.add(account);
			account.pendingReceivedFriendRequests.add(this);
		}
	}

	synchronized void cancelFriendRequest(Account account) {
		if (account.permanent && this.permanent) {
			pendingSentFriendRequests.remove(account);
			account.pendingReceivedFriendRequests.remove(this);
		}
	}

	synchronized void acceptFriendRequest(Account account) {
		if (account.permanent && this.permanent) {
			boolean pendingHere = pendingReceivedFriendRequests.remove(account);
			boolean pendingThere = account.pendingSentFriendRequests.remove(this);
			if (pendingHere && pendingThere) {
				friends.add(account);
				account.friends.add(this);
			}
		}
	}

	synchronized void refuseFriendRequest(Account account) {
		if (account.permanent && this.permanent) {
			pendingReceivedFriendRequests.remove(account);
			account.pendingSentFriendRequests.remove(this);
		}
	}

	synchronized void unfriend(Account account) {
		if (account.permanent && this.permanent) {
			friends.remove(account);
			account.friends.remove(this);
		}
	}

	synchronized void makePermanent(Server server, byte[] pwd) {
		if (!permanent) {
			salt = Security.getNewSalt();
			passwordHash = Security.calculateHash(Security.saltPassword(salt, pwd));
			permanent = true;

			friends = new ArrayList<>();
			pendingSentFriendRequests = new ArrayList<>();
			pendingReceivedFriendRequests = new ArrayList<>();
			Server.logger.info("Changed transient account " + this + " to permanent");
		}
		else Server.logger.warn("Account " + this + " is already a permanent account");
	}

	boolean isOnline() {
		return online;
	}

	boolean isPermanent() {
		return permanent;
	}

	String infoString() {
		return "Name: " + this.getName() + ", Type: " + (permanent ? "Permanent" : "Transient") + ", Status: "
				+ (online ? "Online" : "Offline") + ", Current channel: " + (currentSession == null ? "none" : currentSession.channel);
	}
}
