package com.luminis.echochamber.server;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

class Account implements Serializable {
	private final UUID id;
	final Date creationDate;
	transient Client currentClient;

	private String username;
	private String salt;
	private String passwordHash;

	private boolean permanent;
	transient private boolean online;
	Date lastLoginDate;
	Relations relations;

	Account(String username, byte[] pwd) {
		if (pwd != null) {
			byte[] byteSalt = Security.getNewSalt();
			salt = Security.byteArrayToHexString(byteSalt);
			passwordHash = Security.byteArrayToHexString(Security.calculateHash(Security.saltPassword(byteSalt, pwd)));
			permanent = true;
			relations = new Relations(this);
		} else {
			salt = null;
			passwordHash = null;
			permanent = false;
		}

		id = Security.createUUID();

		this.username = username;
		creationDate = new Date();
		currentClient = null;

		online = false;
		lastLoginDate = null;

		if (username != null) {
			Main.logger.info("Created " + (permanent ? "persistent" : "temporary") + " account " + this);
		}
	}

	Account(String username) { // Create non-persistent account that has no friend information
		this(username, null);
	}

	@Override
	public String toString() {
		return id.toString() + "[" + username + "]";
	}

	synchronized void delete() {
		Main.logger.info("Deleted " + (permanent ? "persistent" : "temporary") + " account " + this);

		username = null;
		salt = null;
		passwordHash = null;
		if (permanent) relations.clear();
	}

	String username() {
		return username;
	}

	synchronized void login(Client client){
		if(currentClient == null) {
			currentClient = client;
			online = true;
			lastLoginDate = new Date();
		}
	}

	synchronized void logout() {
		currentClient = null;
		online = false;
	}

	boolean checkPassword(byte[] pwd) {
		byte[] hashedPassword = Security.calculateHash(Security.saltPassword(Security.hexStringToByteArray(salt), pwd));
		byte[] storedPasswordHash = Security.hexStringToByteArray(passwordHash);
		boolean passwordMatch = passwordHash != null && hashedPassword.length == storedPasswordHash.length;
		for(int i=0; i < hashedPassword.length; i++) {
			passwordMatch = passwordMatch && (hashedPassword[i] == storedPasswordHash[i]);
		}
		Main.logger.info((passwordMatch?"SUCCESSFUL":"FAILED") + " authentication attempt for account " + this);
		return passwordMatch;
	}

	synchronized void addRelation(Account account) {
		if (account.permanent && this.permanent) {
			relations.add(account);
		}
	}

	synchronized void removeRelation(Account account) {
		if (account.permanent && this.permanent) {
			relations.remove(account);
		}
	}

	synchronized void makePermanent(byte[] pwd) {
		if (!permanent) {
			byte[] byteSalt = Security.getNewSalt();
			salt = Security.byteArrayToHexString(byteSalt);
			passwordHash = Security.byteArrayToHexString(Security.calculateHash(Security.saltPassword(byteSalt, pwd)));
			permanent = true;
			relations = new Relations(this);

			Main.logger.info("Changed transient account " + this + " to permanent");
		}
		else Main.logger.warn("Account " + this + " is already a permanent account");
	}

	boolean isOnline() {
		return online;
	}

	boolean isPermanent() {
		return permanent;
	}

	String infoString() {
		return "Name: " + this.username() + ", Type: " + (permanent ? "Permanent" : "Transient") + ", Status: "
				+ (online ? "Online" : "Offline") + ", Current channel: " + (currentClient == null ? "none" : currentClient.connectedChannel);
	}
}
