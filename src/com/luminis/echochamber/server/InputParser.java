package com.luminis.echochamber.server;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InputParser {
	HashMap<String, Command> commands = new HashMap<>();

	Command command;
	String arguments;

	void addCommand(String name, Command command) {
		commands.put(name, command);
	}

	void evaluateInput(String input) {
		String sanitizedInput = input.replaceAll("\\p{C}", ""); // strip non-printable characters by unicode regex
		String inputCommand, inputArguments;

		String pattern = "^\\s*(/([^\\s]*)\\s*)?(.*)\\s*";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(sanitizedInput);
		if (matcher.find()) {
			inputCommand = matcher.group(2);
			inputArguments = matcher.group(3);
			if (inputCommand == null || inputCommand.equals("")) {
				inputCommand = "nop";
			}
		} else {
			inputCommand = "nop";
			inputArguments = "";
		}

		inputCommand = inputCommand.toLowerCase();

		if (commands.containsKey(inputCommand)) {
			command = commands.get(inputCommand);
			arguments = inputArguments;
		} else {
			command = commands.get("invalid");
			arguments = inputCommand;
		}
	}
}
