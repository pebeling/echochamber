package com.luminis.echochamber.server;

import java.util.ArrayList;

public class Server {
	AccountCollection accounts = new AccountCollection();
	private volatile ArrayList<Channel> channels = new ArrayList<>();

	static Channel defaultChannel = new Channel("Default");

	Server() {
		channels.add(defaultChannel);
	}

	synchronized void removeAccount(Account account) {
		accounts.remove(account);
		account.delete();
	}
	synchronized void addAccount(Account account) {
		accounts.add(account);
	}
}
