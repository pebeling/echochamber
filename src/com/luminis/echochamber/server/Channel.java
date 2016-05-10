package com.luminis.echochamber.server;

import java.util.ArrayList;

class Channel {
	private volatile ArrayList<Client> connectedClients;
	private String name;

	Channel(String channelName) {
		connectedClients = new ArrayList<>();
		name = channelName;
	}

	@Override
	public String toString() {
		return "[" + name + "]";
	}

	synchronized void subscribe(Client client) {
		if (!connectedClients.contains(client)) {
			connectedClients.add(client);
			broadcast("User " + TextColors.colorUserName(client.connectedAccount.username()) + " joined channel " + this);
		}
	}

	synchronized void unSubscribe(Client client) {
		if (connectedClients.contains(client)) {
			if (client.connectedAccount != null ) broadcast("User " + TextColors.colorUserName(client.connectedAccount.username()) + " left channel " + this);
			connectedClients.remove(client);
		}
	}

	synchronized void shout(String message, Client sender) {
		broadcast(TextColors.colorUserName(sender.connectedAccount.username()) + "> " + message);
	}

//	synchronized private void broadcast(String messageClient, Client sender) {
//		connectedClients.stream().filter(
//				client -> !client.equals(sender)
//		).forEach(
//				client -> client.messageClient(messageClient)
//		);
//	}

	synchronized private void broadcast(String message) {
		connectedClients.stream().forEach(
				client -> client.messageClient(message)
		);
	}

//	synchronized public ArrayList<String> listClients() {
//		return connectedClients.stream().map(client -> client.connectedAccount.username()).collect(Collectors.toCollection(ArrayList::new));
//	}

	ArrayList<Client> getConnectedClients() {
		return connectedClients;
	}
}