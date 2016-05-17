package com.luminis.echochamber.server;

import java.util.ArrayList;
import java.util.Date;

public class Server {
	AccountCollection accounts;
	private ArrayList<Client> clients = new ArrayList<>();
	private volatile ArrayList<Channel> channels = new ArrayList<>();
	private boolean running;

	static Channel defaultChannel = new Channel("Default");

	Server(AccountCollection accounts) {
		this.accounts = accounts;
		channels.add(defaultChannel);
		running = true;
	}

	synchronized void removeAccount(Account account) {
		accounts.remove(account);
	}
	synchronized void addAccount(Account account) {
		accounts.add(account);
	}

	synchronized void add(Client client) {
		clients.add(client);
		client.message(welcomeMessage());
	}
	synchronized void remove(Client client) {
		clients.remove(client);
	}

	public int numberOfClients() {
		return clients.size();
	}

	private String welcomeMessage() {
		return  "--------------------------------------------------\n" +
				"Welcome to the EchoChamber chat server!\n" +
				"Local time is: " + new Date() + "\n" +
				"You are client " + numberOfClients() + " of " + ConnectionManager.maxConnectedClients + ".\n" +
				"Use /help or /help <command> for more information.\n" +
				"--------------------------------------------------";
	}

	public void shutdown() {
		running = false;
		Main.logger.info("Server shutting down...");
		for(Client client : clients) {
			Main.logger.info("Shutting down client " + client.id + "...");
			client.shutdown("Warning: Server shutting down immediately!");
		}
		Main.logger.info("Server stopped");
	}

	public boolean isActive() {
		return running;
	}
}
