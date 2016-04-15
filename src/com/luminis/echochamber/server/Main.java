package com.luminis.echochamber.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

class Security {
	final public static SecureRandom random = new SecureRandom();
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	static public byte[] getNewSalt() {
		byte salt[] = new byte[16];
		random.nextBytes(salt);
		random.nextInt();
		return salt;
	}

	static public byte[] saltPassword(byte[] salt, byte[] password){
		byte[] saltedPwd = new byte[salt.length + password.length];
		for (int i = 0; i < salt.length; i++){
			saltedPwd[i] = salt[i];
		}
		for (int i = 0; i < password.length; i++){
			saltedPwd[i + salt.length] = password[i];
			password[i] = 0;
		}
		return saltedPwd;
	}

	static public byte[] calculateHash(byte[] message) throws Exception {
		MessageDigest hash = MessageDigest.getInstance("SHA-256");
		hash.update(message);
		for (int i = 0; i < message.length; i++){
			message[i]=0;
		}
		return hash.digest();
	}

	static String byteArrayToHexString(byte[] bytes){
		char[] hexChars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = hexArray[v >>> 4];
			hexChars[i * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	static byte[] hexStringToByteArray(String hexString){
		int length = hexString.length();
		byte[] bytes = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i+1), 16));
		}
		return bytes;
	}
}

class Account {
	static final Account rootAccount;
	static {
		try {
			rootAccount = new Account("SYSTEM", new byte[]{});
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private String nickname;
	private byte[] salt;
	private byte[] passwordHash;
	final Date creationDate;
	Date lastLoginDate;
	ArrayList<Account> friends;
	ArrayList<Account> pendingSentFriendRequests;
	ArrayList<Account> pendingReceivedFriendRequests;
	boolean active;
	Session session;

	Account(String name, byte[] pwd) throws Exception {
		nickname = name;
		salt = Security.getNewSalt();
		passwordHash = Security.calculateHash(Security.saltPassword(salt, pwd));
		creationDate = new Date();
		lastLoginDate = null;
		friends = new ArrayList<>();
		pendingSentFriendRequests = new ArrayList<>();
		pendingReceivedFriendRequests = new ArrayList<>();
		active = true;
		session = null;
	}

	void delete() {
		stopSession();
		passwordHash = null;
		active = false;
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
	}

	public boolean checkPassword(byte[] pwd) throws Exception {
		byte[] hashedPassword = Security.calculateHash(Security.saltPassword(salt, pwd));

		boolean passwordMatch = passwordHash != null && hashedPassword.length == passwordHash.length;
		for(int i=0; i < hashedPassword.length; i++) {
			passwordMatch = passwordMatch && (hashedPassword[i] == passwordHash[i]);
		}
		return passwordMatch;
	}

	public void sendFriendRequest(Account account) {
		if (!friends.contains(account) && account != this) {
			pendingSentFriendRequests.add(account);
			account.pendingReceivedFriendRequests.add(this);
		}
	}

	public void cancelFriendRequest(Account account) {
		pendingSentFriendRequests.remove(account);
		account.pendingReceivedFriendRequests.remove(this);
	}

	public void acceptFriendRequest(Account account) {
		boolean pendingHere = pendingReceivedFriendRequests.remove(account);
		boolean pendingThere = account.pendingSentFriendRequests.remove(this);
		if (pendingHere && pendingThere) {
			friends.add(account);
			account.friends.add(this);
		}
	}

	public void refuseFriendRequest(Account account) {
		pendingReceivedFriendRequests.remove(account);
		account.pendingSentFriendRequests.remove(this);
	}

	public void unfriend(Account account) {
		friends.remove(account);
		account.friends.remove(this);
	}

//	public void updateLastLoginDate(Date date) {
//		lastLoginDate = date;
//	}

	public String getName() {
		return nickname;
	}

	public void startSession(){
		if(session == null && active) {
			session = new Session(this);
		}
	}

	public void stopSession() {
		session = null;
	}
}

class Session {
	Account account;
	//ArrayList<Message> chat;
	Channel currentChannel;
	boolean active;

	Session(Account account){
		this.account = account;
		//chatLog = new ArrayList<>();
		Channel.defaultChannel.join(this);
		active = true;
	}

//	void addChat(String line) {
//		if(active) {
//			chatLog.add(new Message(account, currentChannel, line));
//		}
//	}

	void createChannel(String newChannel){

	}

	void switchChannel(Channel toChannel) {
		if(active) {
			currentChannel = toChannel;
		}
	}
}

class Message {
	String nickname;
	String channelName;
	Date time;
	String message;

	Message(Account origin, Channel channel, String chat) {
		nickname = origin.getName();
		this.message = chat;
		channelName = channel.getName();
		time = new Date();
	}
	@Override
	public String toString() {
		return "" + time + "#" + channelName + "#" + nickname + "#> " + message;
	}
}

class Channel {
	static final Channel defaultChannel = new Channel("COMMON_ROOM", Account.rootAccount);

	String name;
	String createdBy;
	ArrayList<Account> participants;

	public String getName() {
		return name;
	}

	Channel(String name, Account account) {
		this.name = name;
		participants = new ArrayList<>();
		if(account != null) {
			participants.add(account);
		}
		createdBy = account.getName();
	}

	@Override
	public String toString(){
		return name;
	}

	public void join(Session joiningSession) {
		participants.add(joiningSession.account);
		joiningSession.currentChannel = this;
	}
}

public class Main {
	static ArrayList<UUID> idList = new ArrayList<>();
	static private int port = 4444;
	public static void main(String[] args) {
		//boolean listening = true;

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server started at " + new Date() + ".");
			while (true) {
				new ServerThread(serverSocket.accept()).start();
			}
		} catch (IOException e) {
			System.err.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}
}
