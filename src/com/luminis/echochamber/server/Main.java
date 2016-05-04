package com.luminis.echochamber.server;

public class Main {
	static private int port = 4444;

	public static void main(String[] args) {
		Server server = new Server(port, "accounts.json");
		server.start();
	}
}
