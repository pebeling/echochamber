package com.luminis.echochamber.server;

public class Main {
	static private int port = 4444; // TODO read from command line

	public static void main(String[] args) {
		Server server = new Server();
		ConnectionManager connectionManager = new ConnectionManager(port, "accounts.json", server);
		connectionManager.start();
	}
}
