package com.luminis.echochamber.server;

public class Protocol {
	String evaluateInput(String input) {
		if (input.equals("Bye")){
			return null;
		}
		else return "Server received: " + input;
	}

	public String welcomeMessage() {
		return "Welcome to the Echo Chamber chat server!";
	}
}
