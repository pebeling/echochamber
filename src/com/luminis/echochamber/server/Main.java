package com.luminis.echochamber.server;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
	// NB: log4j has its own shutdown hook, which we've disabled in the config. We shut log4j down using our method shutdownLog4j2()
	static final Logger logger = LogManager.getLogger(ConnectionManager.class);

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: server <port> <accounts file>");
			System.exit(1);
		}
		try {
			int port = Integer.parseInt(args[0]);
			Path file = Paths.get(args[1]);

			AccountCollection accounts = readAccounts(file);
			Server server = new Server(accounts);
			ConnectionManager connectionManager = new ConnectionManager(port, server);

			Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
				@Override
				public void run() {
					shutdownServer(server);
					writeAccounts(accounts, file);
					shutdownLog4j2();
				}
			});

			connectionManager.start();

		} catch (NumberFormatException e) {
			System.err.println("Argument" + args[0] + " must be an integer.");
			System.exit(1);
		} catch (JsonIoException ex) {
			System.out.println("Can't read from file '" + args[1] + "': wrong format.");
			System.exit(1);
		} catch (IOException ex) {
			System.out.println("Can't read from file '" + args[1] + "'.");
		}
	}

	private static void shutdownServer(Server server) {
		if (server.isActive()) {
			Main.logger.warn("Forced server shutdown initiated");
			server.shutdown();
		}
	}

	private static AccountCollection readAccounts(Path file) throws IOException, JsonIoException {
		logger.info("Reading accounts...");
		AccountCollection accounts = (AccountCollection) JsonReader.jsonToJava(
				String.join("", Files.readAllLines(file, StandardCharsets.UTF_8))
		);
		logger.info("Successfully imported " + accounts.size() + " accounts");
		return accounts;
	}

	private static void writeAccounts(AccountCollection accounts, Path file) {
		logger.info("Saving accounts...");
		try {
			BufferedWriter out = Files.newBufferedWriter(file);
			out.write(JsonWriter.formatJson(JsonWriter.objectToJson(accounts)));
			out.close();
			logger.info("Accounts saved successfully");
		} catch (IOException ex) {
			logger.error("Cannot write to file " + file.getFileName());
		}
	}

	private static void shutdownLog4j2() {
		if( LogManager.getContext() instanceof LoggerContext) {
			logger.info("Shutting down logger");
			Configurator.shutdown((LoggerContext)LogManager.getContext());
		} else {
			logger.warn("Unable to shutdown log4j2");
		}
	}
}
