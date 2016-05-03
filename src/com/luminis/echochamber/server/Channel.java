package com.luminis.echochamber.server;

import java.util.ArrayList;

class Channel {
	private volatile ArrayList<Session> connectedSessions;
	private String name;

	Channel(String channelName) {
		connectedSessions = new ArrayList<>();
		name = channelName;
	}

	@Override
	public String toString() {
		return "[" + name + "]";
	}

	synchronized void subscribe(Session session) {
		if (!connectedSessions.contains(session)) {
			connectedSessions.add(session);
			broadcast("User " + TextColors.colorUserName(session.connectedAccount.getName()) + " joined channel " + this);
		}
	}

	synchronized void unSubscribe(Session session) {
		if (connectedSessions.contains(session)) {
			broadcast("User " + TextColors.colorUserName(session.connectedAccount.getName()) + " left channel " + this);
			connectedSessions.remove(session);
		}
	}

	synchronized void shout(String message, Session sender) {
		broadcast(TextColors.colorUserName(sender.connectedAccount.getName()) + "> " + message);
	}

//	synchronized private void broadcast(String messageClient, Session sender) {
//		connectedSessions.stream().filter(
//				session -> !session.equals(sender)
//		).forEach(
//				session -> session.messageClient(messageClient)
//		);
//	}

	synchronized private void broadcast(String message) {
		connectedSessions.stream().forEach(
				session -> session.messageClient(message)
		);
	}

//	synchronized public ArrayList<String> listSessions() {
//		return connectedSessions.stream().map(session -> session.connectedAccount.getName()).collect(Collectors.toCollection(ArrayList::new));
//	}

	ArrayList<Session> getConnectedSessions() {
		return connectedSessions;
	}
}