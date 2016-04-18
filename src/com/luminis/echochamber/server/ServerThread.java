package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class ServerThread extends Thread {
	private Socket socket = null;
	private Server server;

	public ServerThread(Socket socket, Server server) {
		super("EchoChamberServerThread");
		this.socket = socket;
		this.server = server;
	}

	public void run() {
		try {
			UUID id = UUID.randomUUID();
			server.idList.add(id);

			System.out.println("Client " + id + " at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has connected to server.");

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);
			String inputLine, outputLine;
			Protocol protocol = new Protocol();

			out.println(protocol.welcomeMessage());

			while (true) {
				inputLine = in.readLine();
				if (inputLine == null) {
					System.out.println("Client " + id + " at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					out.println("Disconnected by client");
					break;
				} else {
					outputLine = protocol.evaluateInput(inputLine);
					if (outputLine == null) {
						System.out.println("Server has disconnected from client " + id + ".");
						out.println("Disconnected by server");
						break;
					}
					else
						out.println(outputLine);
				}
			}
			server.idList.remove(id);
			out.close();
			in.close();
			socket.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
