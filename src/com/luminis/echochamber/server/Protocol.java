package com.luminis.echochamber.server;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luminis.echochamber.server.Command.*;
import static com.luminis.echochamber.server.SessionState.*;

enum Command {
	HELP		("help",	0, 1, "[<command>]", 		"Either lists all available commands or gives info on a specific command"	),
	SETNAME		("setname",	1, 1, "<name>", 			"Sets a nickname and connects to the default channel as a temporary account"),
	SETPWD		("setpwd",	1, 1, "<password>", 		"creates new account"														),
	LOGIN		("login",	2, 2, "<name> <password>", 	"Log in to your account"													),
	LOGOUT		("logout",	0, 0, "", 					"Logs out"																	),
	ACCOUNTS	("accounts",0, 0, "", 					"Lists all accounts"														),
	SESSIONS	("sessions",0, 0, "", 					"Lists all sessions"														),
	EXIT		("exit",	0, 0, "", 					"Ends the current session"													),
	USERS		("users",	0, 1, "[<channel>]", 		"Lists online users"														),
	WHISPER		("whisper",	2, 0, "<user> <message>",	"Sends a message to a specific user"										),
	SHOUT		("shout",	1, 0, "<message>", 			"Sends a message to all in the channel (default)"							),
	DELETE		("delete", 	0, 1, "", 					"Deletes your account"														),
	CANCEL		("cancel", 	0, 0, "", 					"Cancels delete"															),
	FRIENDS		("friends", 0, 0, "", 					"List friends and friend request statuses"									),
	BEFRIEND	("befriend",1, 1, "<user>", 			"Sends someone a friend request"											),
	UNFRIEND	("unfriend",1, 1, "<user>", 			"Removes someone from your friend list"										),
	ACCEPT		("accept", 	1, 1, "<user>", 			"Accept a friend request"													),
	REFUSE		("refuse", 	1, 1, "<user>", 			"Refuse a friend request"													),
	FORGET		("forget", 	1, 1, "<user>", 			"Forgets a sent friend request"												),;

	String commandString, usage, description;
	int minArgs, maxArgs;
	Command(String commandString, int minArgs, int maxArgs, String usage, String description){
		this.commandString = commandString;
		this.usage = usage;
		this.description = description;
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
	}

	static Command lookUp (String command) {
		if (command == null) return null;
		for (Command c : Command.values()) {
			if (c.commandString.equals(command.toLowerCase())) {
				return c;
			}
		}
		return null;
	}

	public boolean containedIn(Command[] commandList) {
		for (Command c : commandList) {
			if (c == this) {
				return true;
			}
		}
		return false;
	}

	String getUsage () {
		return "Usage: /" + commandString + " " + usage;
	}

	String getDescription() {
		return description;
	}
}


enum SessionState {
	ENTRANCE	(new Command[]{EXIT, HELP, SETNAME, LOGIN}),
	TRANSIENT	(new Command[]{EXIT, HELP, LOGOUT, WHISPER, SHOUT, USERS, SETPWD}),
	LOGGED_IN	(new Command[]{EXIT, HELP, LOGOUT, WHISPER, SHOUT, USERS, BEFRIEND, UNFRIEND, FRIENDS, ACCEPT, REFUSE, FORGET, DELETE, ACCOUNTS, SESSIONS}),
	DELETE_CONF	(new Command[]{EXIT, HELP, CANCEL, DELETE});

	Command[] validCommands;
	SessionState(Command[] validCommands){
		this.validCommands = validCommands;
	}
}

class Protocol {
	private Session clientSession;
	private SessionState state;

	Protocol (Session session) {
		clientSession = session;
		state = ENTRANCE;
	}

	String evaluateInput(String input) {
		String inputCommand = null;
		String inputArguments = null;
		Command command = null;

		String pattern = "^\\s*(/([^\\s]*)\\s*)?(.*)";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(input);
		if (matcher.find()) {
			inputCommand = matcher.group(2);
			inputArguments = matcher.group(3);
		}

		if (inputCommand == null || inputCommand.equals("")) {
			if (state == TRANSIENT || state == LOGGED_IN) {
				if (inputArguments.equals("")) {
					return "";
				}
				else {
					command = SHOUT;
				}
			} else if (state == ENTRANCE || state == DELETE_CONF) {
				command = HELP;
				inputArguments = "";
			} else {
				return "Response to empty command undefined";
			}
		} else command = lookUp(inputCommand);

		if (command == null || !command.containedIn(state.validCommands)) {
			return "Invalid command '" + inputCommand.toLowerCase() + "'";
		}

		String[] inputArgumentList;
		if (command.maxArgs < command.minArgs) { // signifies that the last argument contains the rest of the input un-split.
			inputArgumentList = inputArguments.split("\\s+", command.minArgs);
		} else {
			inputArgumentList = inputArguments.split("\\s+");
		}
		if (inputArgumentList.length == 1 && inputArgumentList[0].equals("") ) inputArgumentList = new String[]{}; // this because split of "" results in [""]
		if (inputArgumentList.length > Math.max(command.maxArgs, command.minArgs)) return "Error: Too many arguments\n" + command.getUsage();
		if (inputArgumentList.length < command.minArgs) return "Error: Missing arguments\n" + command.getUsage();

		return executeCommand(command, inputArgumentList);
	}

	String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + clientSession.server.numberOfConnectedClients() + " of " + Server.maxConnectedClients + ".",
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
		return msg;
	}

	private String executeCommand(Command command, String[] inputArgumentList) {
		Account account;
		String list;

		switch(command) {
			case HELP :
				String availableCommands = "";
				if(inputArgumentList.length == 0) {
					for (Command c : state.validCommands) {
						if (!availableCommands.equals("")) {
							availableCommands += ", ";
						}
						availableCommands += "'" + c.commandString + "'";
					}
					return "Available commands: " + availableCommands;
				} else if (inputArgumentList.length == 1) {
					Command foundCommand = Command.lookUp(inputArgumentList[0]);
					if (foundCommand == null) {
						return "Error: No such command \"" + inputArgumentList[0] + "\"";
					} else {
						return foundCommand.getDescription();
					}
				}

			case SETNAME:
				account = new Account(inputArgumentList[0]); // Create temporary account
				try {
					clientSession.server.addAccount(account);
					clientSession.setAccount(account);
				} catch (Exception e) {
					account.delete();
					return "Unable to create temporary account with nickname: " + inputArgumentList[0];
				}
				state = TRANSIENT;
				clientSession.connectToChannel(Server.defaultChannel);
				return "";

			case SETPWD:
				clientSession.account.makePermanent(inputArgumentList[0].getBytes());
				state = LOGGED_IN;
				return "Account now permanent";

			case LOGIN :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account != null && account.checkPassword(inputArgumentList[1].getBytes())) {
					if (account.isOnline()) {
						return "Account already logged in";
					}
					else {
						String oldLastLoginDate = account.lastLoginDate.toString();
						clientSession.setAccount(account);
						clientSession.connectToChannel(Server.defaultChannel);
						state = LOGGED_IN;
						return "Login successful. Last login: " + oldLastLoginDate;
					}
				} else {
					return "Incorrect username or password";
				}

			case LOGOUT:
				clientSession.disconnectFromChannel();
				clientSession.unSetAccount();
				state = ENTRANCE;
				return "Returning to Entrance";

			case ACCOUNTS:
				list = "";
				for (Account a : clientSession.server.accounts) {
					list += a.infoString() + "\n";
				}
				return list;

			case SESSIONS:
				list = "";
				for (Session s : clientSession.server.sessions) {
					list += s.toString() + "\n";
				}
				return list;

			case EXIT :
				return null;

			case USERS:
				if(inputArgumentList.length == 0) {
					list = "";
					for (Session session : clientSession.sessionsInSameChannel()) {
						if (!list.equals("")) {
							list += "\n";
						}
						list += session.account.getName() + " (" + (session.account.isPermanent() ? "permanent" : "transient") +")";
					}
					return list;
				} else return "??"; // TODO: implement once users can create channels

			case WHISPER:
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (account.isOnline()) {
					account.currentSession.messageClient(clientSession.account.getName() + " whispers: " + inputArgumentList[1]);
					return "You whispered a message to " + account.getName();
				} else {
					return "User " + account.getName() + " is not online";
				}

			case SHOUT :
				clientSession.broadcastToChannel(inputArgumentList[0]);
				return "";

			case DELETE:
				if(state == LOGGED_IN) {
					state = DELETE_CONF;
					return "This will delete your account!\nType /delete <password> to confirm!";
				} else if (state == DELETE_CONF) {
					account = clientSession.account;
					if (inputArgumentList.length == 1 && account.checkPassword(inputArgumentList[0].getBytes())) {
						clientSession.disconnectFromChannel();
						clientSession.unSetAccount();
						clientSession.server.removeAccount(account);
						state = ENTRANCE;
						return "Account deleted. Returning to Entrance";
					}
					else {
						state = LOGGED_IN;
						return "Missing or incorrect password. Cancelling deletion.";
					}
				}
				break;

			case CANCEL:
				state = LOGGED_IN;
				return "Delete cancelled";

			case FRIENDS :
				String friendStatus = "";
				friendStatus += "Current friends:\n";
				for (Account friend : clientSession.account.friends) {
					friendStatus += "\t" + friend.getName() + " " + (friend.isOnline() ? friend.currentSession.channel : "[OFFLINE]") + " \n";
				}
				friendStatus += "Pending sent friend requests: \n";
				for (Account friend : clientSession.account.pendingSentFriendRequests) {
					friendStatus += "\t" + friend.getName() + "\n";
				}
				friendStatus += "Pending received friend requests: \n";
				for (Account friend : clientSession.account.pendingReceivedFriendRequests) {
					friendStatus += "\t" + friend.getName() + "\n";
				}
				return friendStatus;

			case BEFRIEND :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (!account.isPermanent()) {
					return "You can only send friend requests to permanent accounts";
				} else if (account.equals(clientSession.account)) {
					return "Get a life!";
				} else {
					clientSession.account.sendFriendRequest(account);
					return "Friend request sent";
				}

			case UNFRIEND :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (!clientSession.account.friends.contains(account)) {
					return account.getName() + "is not in your friend list";
				} else {
					clientSession.account.unfriend(account);
					return "You removed " + account.getName() + " from your friend list";
				}

			case ACCEPT :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (!clientSession.account.pendingReceivedFriendRequests.contains(account)) {
					return "No pending friend request from " + account.getName();
				} else {
					clientSession.account.acceptFriendRequest(account);
					return "You and " + account.getName() + " are now friends";
				}

			case REFUSE :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (!clientSession.account.pendingReceivedFriendRequests.contains(account)) {
					return "No pending friend request from " + account.getName();
				} else {
					clientSession.account.refuseFriendRequest(account);
					return "You refused " + account.getName() + "'s friend request";
				}

			case FORGET:
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (!clientSession.account.pendingSentFriendRequests.contains(account)) {
					return "No outstanding friend request to " + account.getName();
				} else {
					clientSession.account.refuseFriendRequest(account);
					return "You cancelled friend request to " + account.getName();
				}
		}
		return "Error: command '" + command.commandString + "' not implemented";
	}

	void close() {
		if (clientSession.channel != null) clientSession.disconnectFromChannel();
		if (clientSession.account != null) clientSession.unSetAccount();
	}
}
