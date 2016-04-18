package com.luminis.echochamber.server;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum Command {
	NICK	("nick",	"Sets the nickname and connects to the default channel"),
	EXIT	("exit",	"Ends the session"),
	LIST	("list",	"Lists all online users"),
	MESSAGE	("message",	"Sends a message to a specific user"),
	SHOUT	("shout",	"Sends a message to all in the channel (default)"),
	HELP	("help",	"Either lists all available commands or gives info on a specific command");

	String commandString, description;
	Command(String command, String description){
		this.commandString = command;
		this.description = description;
	}

	static Command lookUp (String command) {
		for (Command c : Command.values()) {
			if (c.commandString.equals(command)) {
				return c;
			}
		}
		return null;
	}
}


enum SessionState {
	CONFIGURED (new Command[]{Command.EXIT, Command.HELP, Command.MESSAGE, Command.SHOUT, Command.LIST}),
	NOT_CONFIGURED (new Command[]{Command.EXIT, Command.HELP, Command.NICK});
	
	Command[] validCommands;
	SessionState(Command[] availableCommands){
		this.validCommands = availableCommands;
	}
}

public class Protocol {
	private SessionState state = SessionState.NOT_CONFIGURED;
	private ServerThread clientOnServer;

	Protocol (ServerThread clientOnServer) {
		this.clientOnServer = clientOnServer;
	}

	String evaluateInput(String input) {
		String pattern = "^\\s*(/([^\\s]*)\\s*)?(.*)";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(input);

		String enteredCommand = null;
		String argument = null;

		if (matcher.find()) {
			enteredCommand = matcher.group(2);
			argument = matcher.group(3);
		}
		Command command = null;
		for ( Command c : state.validCommands) {
			if (enteredCommand != null && c.commandString.equals(enteredCommand.toLowerCase())) {
				command = c;
				break;
			}
		}
		if (command == null && enteredCommand != null) return "Invalid command: " + enteredCommand.toLowerCase() + ".";
		else if (command == null && state == SessionState.CONFIGURED) command = Command.SHOUT;
		else if (command == null && state == SessionState.NOT_CONFIGURED) {
			command = Command.HELP;
			argument = "";
		}

		switch(command) {
			case NICK :
				if (!argument.equals("")) {
					if (clientOnServer.channel.listSessions().contains(argument)) {
						return "Error: Nickname " + argument + " already in use";
					} else {
						state = SessionState.CONFIGURED;
						clientOnServer.nickName = argument;
						clientOnServer.connectToChannel();
						return "";
					}
				}
				else {
					return "Error: Missing argument" + argument;
				}
			case EXIT :
				if (state == SessionState.CONFIGURED) clientOnServer.disconnectFromChannel();
				return null;
			case LIST :
				String list = "";
				for (String session : clientOnServer.listSessions()){
					if (!list.equals("")) {
						list += ", ";
					}
					list += session;
				}
				return list;
			case SHOUT :
				clientOnServer.broadcastToChannel(argument);
				return "";
			case HELP :
				String availableCommands = "";
				if(argument.equals("")) {
					for (Command c : state.validCommands) {
						if (!availableCommands.equals("")) {
							availableCommands += ", ";
						}
						availableCommands += "/" + c.commandString;
					}
					return "Available commands: " + availableCommands;
				} else {
					Command foundCommand = Command.lookUp(argument);
					if (foundCommand == null) {
						return "Error: No such command \"" + argument + "\"";
					} else {
						return foundCommand.description;
					}
				}
			}
		return "Error: command " + command.commandString + " not implemented";
	}

	public String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + clientOnServer.server.numberOfConnectedClients + " of " + clientOnServer.server.maxConnectedClients + ".",
				"Use /help or /help <command> for more information.",
				"--------------------------------------------------"
		};

		String msg = "";
		for (String s : lines) {
			if (!msg.equals("")) {
				msg += "\n";
			}
			msg += s;
		}
		return TextColors.colorServermessage(msg);
	}
}
