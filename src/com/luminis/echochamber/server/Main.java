package com.luminis.echochamber.server;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import com.sun.deploy.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
	static final Logger logger = LogManager.getLogger(ConnectionManager.class); // NB: log4j has its own shutdown hook, which we disabled in the config.

	public static void main(String[] args) {
		int port;
		String filename;
		Server server;

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: server <port> [<accounts file>]");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[0]);
			server = new Server();

			if (args.length == 2) {
				filename = args[1];
				List<String> text = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
				AccountCollection accounts = (AccountCollection) JsonReader.jsonToJava(StringUtils.join(text, ""));
				logger.info("Successfully imported " + accounts.size() + " accounts");
				
				server.accounts = accounts;

				Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
					@Override
					public void run() {
						writeAccounts(server, filename);
						shutdown();
					}
				});
			} else {
				Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
					@Override
					public void run() {
						shutdown();
					}
				});
			}

			ConnectionManager connectionManager = new ConnectionManager(port, server);
			connectionManager.start();

		} catch (NumberFormatException e) {
			System.err.println("Argument" + args[0] + " must be an integer.");
			System.exit(1);
		} catch (IOException ex) {
			System.out.println("Can't read from file '" + args[1] + "'.");
			System.exit(1);
		} catch (JsonIoException ex) {
			System.out.println("Can't read from file '" + args[1] + "': wrong format.");
			System.exit(1);
		}
	}

	private static void writeAccounts(Server server, String filename) {
		logger.info("Saving accounts...");
		try (
				PrintWriter out = new PrintWriter(filename)
		) {
			out.print(JsonWriter.formatJson(JsonWriter.objectToJson(server.accounts)));
			out.close();
			logger.info("Accounts saved successfully");
		} catch (IOException ex) {
			logger.error("Cannot write file " + filename);
		}
	}

	private static void shutdown() {
		//shutdown log4j2
		if( LogManager.getContext() instanceof LoggerContext) {
			logger.info("Shutting down log4j2");
			Configurator.shutdown((LoggerContext)LogManager.getContext());
		} else {
			logger.warn("Unable to shutdown log4j2");
		}
	}
}
