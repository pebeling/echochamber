package com.luminis.echochamber.server;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luminis.echochamber.server.ClientState.*;

class InputParser {
	HashMap<String, Command> commands = new HashMap<>();

	void addCommand(Command command) {
		commands.put(command.getName(), command);
	}

	String evaluate(ClientState state, String input) throws Exception {
		String sanitizedInput = input.replaceAll("\\p{C}", ""); // strip non-printable characters by unicode regex
		String commandName, arguments;
		Command command;

		String pattern = "^\\s*(/([^\\s]*)\\s*)?(.*)\\s*";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(sanitizedInput);
		if (matcher.find()) {
			commandName = matcher.group(2);
			arguments = matcher.group(3);
			if (commandName == null || commandName.equals("")) {
				commandName = "no";
			}
		} else {
			commandName = "no";
			arguments = "";
		}

		if (commandName.equals("no")) {
			if (state == ENTRANCE) {
				commandName = "help";
			} else if (state == LOGGED_IN || state == TRANSIENT) {
				commandName	= "shout";
			}
		}

		commandName = commandName.toLowerCase();

		if (commands.containsKey(commandName)) {
			if (state.accepts(commandName) || commandName.equals("no")) {
				command = commands.get(commandName);
			} else {
				throw new Exception("Command not available in this context");
			}
		} else {
			throw new Exception("No such command");
		}

		command.argumentStringParser(arguments);
		return command.execute();
	}
}