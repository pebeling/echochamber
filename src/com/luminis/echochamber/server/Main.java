package com.luminis.echochamber.server;

public class Main {
	static private int port = 4444;

	public static void main(String[] args) {
		ConnectionManager connectionManager = new ConnectionManager(port, "accounts.json");
		connectionManager.start();
	}
}
