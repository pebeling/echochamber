package com.luminis.echochamber.server;

import java.util.ArrayList;
import java.util.Date;

public class Server {
	AccountCollection accounts = new AccountCollection();
	private ArrayList<Client> clients = new ArrayList<>();
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
		client.messageClient(welcomeMessage());
	}
	synchronized void remove(Client client) {
		clients.remove(client);
	}

	public int numberOfClients() {
		return clients.size();
	}

	private String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + numberOfClients() + " of " + ConnectionManager.maxConnectedClients + ".",
				"Use /help or /help <command> for more information.",
				"--------------------------------------------------"
		};
		return String.join("\n", lines);
	}
}
