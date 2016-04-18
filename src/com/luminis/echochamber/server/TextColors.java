package com.luminis.echochamber.server;

public class TextColors {
	static String colorUserName(String username) {
		return "\033[33m" + username + "\033[0m";
	}

	static String colorServermessage(String message) {
		return "\033[31m" + message + "\033[0m";
	}
}
