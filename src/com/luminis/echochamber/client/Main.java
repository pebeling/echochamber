package com.luminis.echochamber.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static java.lang.System.exit;

public class Main {
    static int serverPort = 4444;

    public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println(
					"Usage: java EchoChamber <host name>");
			System.exit(1);
		}

		String hostName = args[0];

		System.out.println("Client started!");
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			Socket socket = new Socket(hostName, serverPort);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
		} catch(Exception e) {
			System.err.println("Error connecting to server.");
			exit(0);
		}

		InputReader input = new InputReader(out);
		String fromServer;

		while ((fromServer = in.readLine()) != null) {
			System.out.println(fromServer);
		}
		input.stop();
    }

	static void inputHandler(String input, PrintWriter out) {
		out.println(input);
	}
}

class InputReader implements Runnable {
	private BufferedReader stdIn;
	private boolean stopped = false;
	private PrintWriter out;

	InputReader(PrintWriter out) {
		this.out = out;
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		String lastInput;
		while (!stopped) {
			try {
				lastInput = stdIn.readLine();
				Main.inputHandler(lastInput, out);
			} catch(Exception e) {
				System.err.println("Error: can't read from keyboard.");
				exit(0);
			}
		}
	}

	void stop() {
		stopped = true;
	}
}