package com.luminis.echochamber.server;

import java.util.ArrayList;

public class Server {
	AccountCollection accounts = new AccountCollection();
	ArrayList<Client> clients = new ArrayList<>();
	private volatile ArrayList<Channel> channels = new ArrayList<>();

	static Channel defaultChannel = new Channel("Default");

	Server() {
		channels.add(defaultChannel);
	}

	synchronized void removeAccount(Account account) {
		accounts.remove(account);
	}
	synchronized void addAccount(Account account) {
		accounts.add(account);
	}

	synchronized void add(Client client) {
		clients.add(client);
	}
	synchronized void remove(Client client) {
		clients.remove(client);
	}
}
