package com.luminis.echochamber.server;

import java.util.Arrays;
import java.util.List;

abstract class Command {
	private String commandString, usage, description;
	private int minArgs, maxArgs;
	private boolean greedyLastArgument;

	String getName() {
		return commandString;
	}
	String getUsage() {
		return "Usage: /" + commandString + " " + usage;
	}
	String getDescription() {
		return description;
	}
	abstract void execute(String arguments) throws Exception;

	Command(String commandString, String usage, String description, int minArgs, int maxArgs, boolean greedyLastArgument) {
		this.commandString = commandString;
		this.usage = usage;
		this.description = description;
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.greedyLastArgument = greedyLastArgument;
	}

	List<String> argumentStringParser(String arguments) throws Exception {
		String[] argumentList;
		if (this.greedyLastArgument) { // signifies that the last argument contains the rest of the input un-split.
			argumentList = arguments.split("\\s+", this.minArgs);
		} else {
			argumentList = arguments.split("\\s+");
		}
		if (argumentList.length == 1 && argumentList[0].equals("")) argumentList = new String[]{}; // this because split of "" results in [""]
		if (argumentList.length > this.maxArgs) throw new Exception("Too many arguments");
		if (argumentList.length < this.minArgs) throw new Exception("Too few arguments");

		return Arrays.asList(argumentList);
	}
}

class helpCommand extends Command {
	private Session receiver;

	helpCommand (Session receiver) {
		super("help", "[<command>]", "Either lists all available commands or gives info on a specific command", 0, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.helpCommandImp(argumentStringParser(arguments));
	}
}

class setnameCommand extends Command {
	private Session receiver;

	setnameCommand(Session receiver) {
		super("setname", "<name>", "Sets a nickname and connects to the default channel as a temporary account", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.setnameCommandImp(argumentStringParser(arguments));
	}
}

class setpwdCommand extends Command {
	private Session receiver;

	setpwdCommand (Session receiver) {
		super("setpwd", "<password>", "creates new account", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.setpwdCommandImp(argumentStringParser(arguments));
	}
}

class loginCommand extends Command {
	private Session receiver;

	loginCommand (Session receiver) {
		super("login", "<name> <password>", "Log in to your account", 2, 2, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.loginCommandImp(argumentStringParser(arguments));
	}
}

class logoutCommand extends Command {
	private Session receiver;

	logoutCommand (Session receiver) {
		super("logout", "", "Logs out", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.logoutCommandImp(argumentStringParser(arguments));
	}
}

class accountsCommand extends Command {
	private Session receiver;

	accountsCommand (Session receiver) {
		super("accounts", "", "Lists all accounts", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.accountsCommandImp(argumentStringParser(arguments));
	}
}

class sessionsCommand extends Command {
	private Session receiver;

	sessionsCommand (Session receiver) {
		super("sessions", "", "Lists all sessions", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.sessionsCommandImp(argumentStringParser(arguments));
	}
}

class exitCommand extends Command {
	private Session receiver;

	exitCommand (Session receiver) {
		super("exit", "", "Ends the current session", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.exitCommandImp(argumentStringParser(arguments));
	}
}

class usersCommand extends Command {
	private Session receiver;

	usersCommand (Session receiver) {
		super("users", "[<channel>]", "Lists online users", 0, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.usersCommandImp(argumentStringParser(arguments));
	}
}

class whisperCommand extends Command {
	private Session receiver;

	whisperCommand (Session receiver) {
		super("whisper", "<user> <message>", "Sends a message to a specific user", 2, 2, true);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.whisperCommandImp(argumentStringParser(arguments));
	}
}

class shoutCommand extends Command {
	private Session receiver;

	shoutCommand (Session receiver) {
		super("shout", "<message>", "Sends a message to all in the channel (default)", 1, 1, true);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.shoutCommandImp(argumentStringParser(arguments));
	}
}

class deleteCommand extends Command {
	private Session receiver;

	deleteCommand (Session receiver) {
		super("delete", "", "Deletes your account", 0, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.deleteCommandImp(argumentStringParser(arguments));
	}
}

class cancelCommand extends Command {
	private Session receiver;

	cancelCommand (Session receiver) {
		super("cancel", "", "Cancels delete", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.cancelCommandImp(argumentStringParser(arguments));
	}
}

class friendsCommand extends Command {
	private Session receiver;

	friendsCommand (Session receiver) {
		super("friends", "", "List friends and friend request statuses", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.friendsCommandImp(argumentStringParser(arguments));
	}
}

class befriendCommand extends Command {
	private Session receiver;

	befriendCommand (Session receiver) {
		super("befriend", "<user>", "Sends someone a friend request", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.befriendCommandImp(argumentStringParser(arguments));
	}
}

class unfriendCommand extends Command {
	private Session receiver;

	unfriendCommand (Session receiver) {
		super("unfriend", "<user>", "Removes someone from your friend list", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.unfriendCommandImp(argumentStringParser(arguments));
	}
}

class acceptCommand extends Command {
	private Session receiver;

	acceptCommand (Session receiver) {
		super("accept", "<user>", "Accept a friend request", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.acceptCommandImp(argumentStringParser(arguments));
	}
}

class refuseCommand extends Command {
	private Session receiver;

	refuseCommand (Session receiver) {
		super("refuse", "<user>", "Refuse a friend request", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.refuseCommandImp(argumentStringParser(arguments));
	}
}

class forgetCommand extends Command {
	private Session receiver;

	forgetCommand (Session receiver) {
		super("forget", "<user>", "Forgets a sent friend request", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.forgetCommandImp(argumentStringParser(arguments));
	}
}

class noCommand extends Command {
	private Session receiver;

	noCommand (Session receiver) {
		super("", "", "", 0, 0, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.noCommandImp(argumentStringParser(arguments));
	}
}

class invalidCommand extends Command {
	private Session receiver;

	invalidCommand (Session receiver) {
		super("", "", "", 1, 1, false);
		this.receiver = receiver;
	}

	public void execute(String arguments) throws Exception {
		receiver.invalidCommandImp(argumentStringParser(arguments));
	}
}
