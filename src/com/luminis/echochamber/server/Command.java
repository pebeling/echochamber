package com.luminis.echochamber.server;

import java.util.*;

abstract class Command {
	private String commandName, description;
	private String[][] usages;
	private boolean greedyLastArgument;

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
	abstract void execute(String arguments) throws Exception;

	Command(String commandName, String description, String[][] usages, boolean greedyLastArgument) {
		this.commandName = commandName;
		this.description = description;
		this.usages = usages;
		this.greedyLastArgument = greedyLastArgument;
	}

	Map<String, String> argumentStringParser(String arguments) throws Exception {
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

		return argumentMapList.get(0);
	}
}

class helpCommand extends Command {
	private Session receiver;

	helpCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.helpCommandImp(argumentStringParser(arguments));
	}
}

class setnameCommand extends Command {
	private Session receiver;

	setnameCommand(Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.setnameCommandImp(argumentStringParser(arguments));
	}
}

class setpwdCommand extends Command {
	private Session receiver;

	setpwdCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.setpwdCommandImp(argumentStringParser(arguments));
	}
}

class loginCommand extends Command {
	private Session receiver;
	
	loginCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.loginCommandImp(argumentStringParser(arguments));
	}
}

class logoutCommand extends Command {
	private Session receiver;

	logoutCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.logoutCommandImp(argumentStringParser(arguments));
	}
}

class accountsCommand extends Command { // TODO: should be admin command only
	private Session receiver;

	accountsCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.accountsCommandImp(argumentStringParser(arguments));
	}
}

class sessionsCommand extends Command {
	private Session receiver;

	sessionsCommand (Session receiver) { // TODO: should be admin command only
		super(
				"sessions", 
				"Lists all sessions.",
				new String[][]{
						{ }
				},
				false
		);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.sessionsCommandImp(argumentStringParser(arguments));
	}
}

class exitCommand extends Command {
	private Session receiver;

	exitCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.exitCommandImp(argumentStringParser(arguments));
	}
}

class usersCommand extends Command {
	private Session receiver;

	usersCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.usersCommandImp(argumentStringParser(arguments));
	}
}

class whisperCommand extends Command {
	private Session receiver;

	whisperCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.whisperCommandImp(argumentStringParser(arguments));
	}
}

class shoutCommand extends Command {
	private Session receiver;

	shoutCommand (Session receiver) { // TODO: should be "all on server" once multiple channel are possible. Command "talk" to speak to all in channel
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

	public void execute(String arguments) throws Exception {
		receiver.shoutCommandImp(argumentStringParser(arguments));
	}
}

class deleteCommand extends Command {
	private Session receiver;

	deleteCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.deleteCommandImp(argumentStringParser(arguments));
	}
}

class cancelCommand extends Command {
	private Session receiver;

	cancelCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.cancelCommandImp(argumentStringParser(arguments));
	}
}

class friendsCommand extends Command {
	private Session receiver;

	friendsCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.friendsCommandImp(argumentStringParser(arguments));
	}
}

class befriendCommand extends Command {
	private Session receiver;

	befriendCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.befriendCommandImp(argumentStringParser(arguments));
	}
}

class unfriendCommand extends Command {
	private Session receiver;

	unfriendCommand (Session receiver) {
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

	public void execute(String arguments) throws Exception {
		receiver.unfriendCommandImp(argumentStringParser(arguments));
	}
}

class noCommand extends Command {
	private Session receiver;

	noCommand (Session receiver) {
		super(
				"no",
				"",
				new String[][]{
					{ },
					{ "arguments" }
				},
				true
		);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.noCommandImp(argumentStringParser(arguments));
	}
}

class invalidCommand extends Command {
	private Session receiver;

	invalidCommand (Session receiver) {
		super(
				"invalid",
				"",
				new String[][]{
						{ },
						{ "command" }
				},
				false
		);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.invalidCommandImp(argumentStringParser(arguments));
	}
}
