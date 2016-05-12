package com.luminis.echochamber.server;

import java.util.*;

abstract class Command {
	private String commandName, description;
	private String[][] usages;
	private boolean greedyLastArgument;
	Map<String, String> arguments = new HashMap<>();

	String getName() {
		return commandName;
	}

	String getUsage() {
		String output = (usages.length > 1) ? "Usages:" : "Usage:";
		for (String[] usage : usages) {
			output += "\n";
			output += "\t/" + commandName + " ";
			for (String s : usage) {
				output += "<" + s + "> ";
			}
		}
		return output;
	}

	String getDescription() {
		return description;
	}
	abstract String execute();

	Command(String commandName, String description, String[][] usages, boolean greedyLastArgument) {
		this.commandName = commandName;
		this.description = description;
		this.usages = usages;
		this.greedyLastArgument = greedyLastArgument;
	}

	public void argumentStringParser(String arguments) throws Exception {
		List< Map<String, String> > argumentMapList = new ArrayList<>();

		// try to match the string to the entries in usages
		for (String[] usage : usages) {
			Map<String, String> map = new HashMap<>();

			int l = usage.length;

			String[] asplit = arguments.split("\\s", greedyLastArgument ? l : 0);
			if (asplit.length == 1 && asplit[0].equals("")) { asplit = new String[]{}; } // this needed because split of "" results in [""] instead of []

			if (l == 0 && asplit.length == 0) {
				argumentMapList.add(map);
			} else if (l > 0) {
				if ( asplit.length == l ) {
					for (int i = 0; i < l; i++) {
						map.put(usage[i], asplit[i]);
					}
					argumentMapList.add(map);
				}
			}
		}
		if (argumentMapList.isEmpty()) {throw new Exception("Wrong number of arguments");}
		if (argumentMapList.size() > 1 ) {throw new Exception("Ambiguous arguments");} // indicates an error in usages array for this command

		this.arguments = argumentMapList.get(0);
	}
}

class helpCommand extends Command {
	private Client receiver;

	helpCommand (Client receiver) {
		super(
				"help",
				"Either lists all available commands or gives info on a specific command.",
				new String[][]{
						{ },
						{ "command name" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.helpCommandImp(arguments);
	}
}

class setnameCommand extends Command {
	private Client receiver;

	setnameCommand(Client receiver) {
		super(
				"setname",
				"Sets a username and connects to the default channel as a temporary account.",
				new String[][]{
						{ "username" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.setnameCommandImp(arguments);
	}
}

class setpwdCommand extends Command {
	private Client receiver;

	setpwdCommand (Client receiver) {
		super(
				"setpwd",
				"creates new account.",
				new String[][]{
						{ "password" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.setpwdCommandImp(arguments);
	}
}

class loginCommand extends Command {
	private Client receiver;
	
	loginCommand (Client receiver) {
		super(
				"login", 
				"Log in to your account.",
				new String[][]{
						{ "username", "password" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.loginCommandImp(arguments);
	}
}

class logoutCommand extends Command {
	private Client receiver;

	logoutCommand (Client receiver) {
		super(
				"logout", 
				"Logs out.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.logoutCommandImp();
	}
}

class accountsCommand extends Command { // TODO: should be admin command only
	private Client receiver;

	accountsCommand (Client receiver) {
		super(
				"accounts", 
				"Lists all accounts.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.accountsCommandImp();
	}
}

class exitCommand extends Command {
	private Client receiver;

	exitCommand (Client receiver) {
		super(
				"exit", 
				"Ends the current session.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.exitCommandImp();
	}
}

class usersCommand extends Command {
	private Client receiver;

	usersCommand (Client receiver) {
		super(
				"users",
				"Lists online users.",
				new String[][]{
						{ },
						{ "channel" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.usersCommandImp(arguments);
	}
}

class whisperCommand extends Command {
	private Client receiver;

	whisperCommand (Client receiver) {
		super(
				"whisper",
				"Sends a message to a specific user.",
				new String[][]{
						{ "username", "message" }
				},
				true
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.whisperCommandImp(arguments);
	}
}

class shoutCommand extends Command {
	private Client receiver;

	shoutCommand (Client receiver) { // TODO: should message "everyone on server" once multiple channel are possible. Command "talk" to speak to all in channel
		super(
				"shout",
				"Sends a message to all in the channel (default).",
				new String[][]{
						{ },
						{ "message" }
				},
				true
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.shoutCommandImp(arguments);
	}
}

class deleteCommand extends Command {
	private Client receiver;

	deleteCommand (Client receiver) {
		super(
				"delete",
				"Deletes your account.",
				new String[][]{
						{ },
						{ "password" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.deleteCommandImp(arguments);
	}
}

class cancelCommand extends Command {
	private Client receiver;

	cancelCommand (Client receiver) {
		super(
				"cancel", 
				"Cancels delete.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.cancelCommandImp();
	}
}

class friendsCommand extends Command {
	private Client receiver;

	friendsCommand (Client receiver) {
		super(
				"friends", 
				"List all friends and friend request statuses.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.friendsCommandImp();
	}
}

class befriendCommand extends Command {
	private Client receiver;

	befriendCommand (Client receiver) {
		super(
				"befriend",
				"Sends someone a friend request or accepts a request.",
				new String[][]{
						{ "username" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.befriendCommandImp(arguments);
	}
}

class unfriendCommand extends Command {
	private Client receiver;

	unfriendCommand (Client receiver) {
		super(
				"unfriend", 
				"Removes someone from your friend list or cancels a friend request.",
				new String[][]{
						{ "username" }
				},
				false
		);
		this.receiver = receiver;
	}

	public String execute() {
		return receiver.unfriendCommandImp(arguments);
	}
}

class noCommand extends Command {
	noCommand () {
		super(
				"no",
				"",
				new String[][]{{}},
				true
		);
	}

	public String execute() {
		return null;
	}
}
