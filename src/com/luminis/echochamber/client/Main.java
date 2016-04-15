package com.luminis.echochamber.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Main {
    static int serverPort = 4444;
    static String serverAdress = "127.0.0.1";
    public static void main(String[] args) throws Exception {

		System.out.println("Client");
		BufferedReader in = null;
		PrintWriter out = null;
        try {
            Socket kkSocket = new Socket(serverAdress, serverPort);
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(
                    new InputStreamReader(kkSocket.getInputStream()));
		} catch(Exception e) {
			System.err.println("Error");
		}
		BufferedReader stdIn =
				new BufferedReader(new InputStreamReader(System.in));
		String fromServer;
		String fromUser;

		while ((fromServer = in.readLine()) != null) {
			System.out.println("Server: " + fromServer);
			if (fromServer.equals("Bye."))
				break;

			fromUser = stdIn.readLine();
			if (fromUser != null) {
				System.out.println("Client: " + fromUser);
				out.println(fromUser);
			}
		}
    }
}
