package com.luminis.server;

import java.util.*;

class Account {
	private String nickname;
	private String password;
	private static Date creationDate;
	private Date lastLoginDate;
	private ArrayList<Account> friends;
	ChatSession currentSession;

	Account(String name, String pwd){
		nickname = name;
		password = pwd;
		creationDate = new Date();
		lastLoginDate = null;
		friends = new ArrayList<Account>();
		currentSession = null;
	}

	public void addFriend(Account account) {
		friends.add(account);
	}

	public boolean removeFriend(Account account) {
		return friends.remove(account);
	}

	public void updateLastLoginDate(Date date) {
		lastLoginDate = date;
	}

	public String getName() {
		return nickname;
	}

	public void startSession(){
		currentSession = new ChatSession(this);
		//return currentSession;
	}

	public void stopSession() {
		currentSession = null;
	}
}

class ChatSession {
	private Account account;
	ArrayList<Chat> chat;
	Channel currentChannel;
	boolean active;

	ChatSession(Account account){
		this.account = account;
		chat = new ArrayList<>();
		currentChannel = Main.defaultChannel;
		active = true;
	}

	void addChat(String line) {
		if(active) {
			chat.add(new Chat (account, currentChannel, line));
		}
	}

	void switchChannel(Channel newChannel) {
		if(active) {
			currentChannel = newChannel;
		}
	}
}

class Chat {
	String nickname;
	String channelName;
	Date time;
	String chat;

	Chat(Account origin, Channel channel, String chat) {
		nickname = origin.getName();
		this.chat = chat;
		channelName = channel.getName();
		time = new Date();
	}
	@Override
	public String toString() {
		return "" + time + "#" + channelName + "#" + nickname + "#> " + chat;
	}
}
class Channel {
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
}

class ChatServer {

}

public class Main {
	static Account rootAccount = new Account("SYSTEM", "");
	static Channel defaultChannel = new Channel("COMMON_ROOM", rootAccount);

	public static void main(String[] args) {
		Account account = new Account("Paul", "foo");
		account.startSession();
		System.out.println(account.currentSession.currentChannel);

		account.stopSession();
	}
}
