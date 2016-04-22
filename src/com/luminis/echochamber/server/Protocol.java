package com.luminis.echochamber.server;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luminis.echochamber.server.Command.*;
import static com.luminis.echochamber.server.SessionState.*;

enum Command {
	SETNAME		("setname",	1, 1, "<name>", 			"Sets a nickname and connects to the default channel as a temporary account"),
	EXIT		("exit",	0, 0, "", 					"Logs out or ends the current session"										),
	USERS		("users",	0, 1, "[<channel>]", 		"Lists online users"														),
	WHISPER		("whisper",	2, 0, "<user> <message>",	"Sends a message to a specific user"										),
	SHOUT		("shout",	1, 0, "<message>", 			"Sends a message to all in the channel (default)"							),
	HELP		("help",	0, 1, "[<command>]", 		"Either lists all available commands or gives info on a specific command"	),
	LOGIN		("login",	2, 2, "<name> <password>", 	"Log in to your account"													),
	BEFRIEND	("befriend",1, 1, "<user>", 			"Sends someone a friend request"											),
	UNFRIEND	("unfriend",1, 1, "<user>", 			"Removes someone from your friend list"										),
	FRIENDS		("friends", 0, 0, "", 					"List friends and friend request statuses"									),
	ACCEPT		("accept", 	1, 1, "<user>", 			"Accept a friend request"													),
	REFUSE		("refuse", 	1, 1, "<user>", 			"Refuse a friend request"													),
	CANCEL		("cancel", 	1, 1, "<user>", 			"Cancels a pending friend request"											),
	DELETE		("delete", 	0, 1, "", 					"Deletes your account"														),
	SETPWD		("setpwd",	1, 1, "<password>", 		"creates new account"														);

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
	TRANSIENT	(new Command[]{EXIT, HELP, WHISPER, SHOUT, USERS, SETPWD}),
	LOGGED_IN	(new Command[]{EXIT, HELP, WHISPER, SHOUT, USERS, BEFRIEND, UNFRIEND, FRIENDS, ACCEPT, REFUSE, CANCEL, DELETE}),
	DELETE_CONF	(new Command[]{EXIT, HELP, DELETE});
	
	Command[] validCommands;
	SessionState(Command[] validCommands){
		this.validCommands = validCommands;
	}
}

public class Protocol {
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

		if (input.equals("")) return "";

		String pattern = "^\\s*(/([^\\s]*)\\s*)?(.*)";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(input);
		if (matcher.find()) {
			inputCommand = matcher.group(2);
			inputArguments = matcher.group(3);
		}

		if (inputCommand == null) {
			if (state == TRANSIENT || state == LOGGED_IN) {
				command = SHOUT;
			} else if (state == ENTRANCE) {
				command = HELP;
				inputArguments = "";
			}
		} else command = lookUp(inputCommand);

		if (command == null || !command.containedIn(state.validCommands)) {
			return "Invalid command '" + inputCommand.toLowerCase() + "'";
		}

		String[] inputArgumentList;
		if (command.maxArgs < command.minArgs) { // signifies that the last argument contains the rest of the input un-split.
			inputArgumentList = inputArguments.split("\\s+", command.maxArgs);
		} else {
			inputArgumentList = inputArguments.split("\\s+");
		}
		if (inputArgumentList.length == 1 && inputArgumentList[0].equals("") ) inputArgumentList = new String[]{}; // this because split of "" results in [""]
		if (inputArgumentList.length > Math.max(command.maxArgs, command.minArgs)) return "Error: Too many arguments\n" + command.getUsage();
		if (inputArgumentList.length < command.minArgs) return "Error: Missing arguments\n" + command.getUsage();

		return executeCommand(command, inputArgumentList);
	}

	public String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + clientSession.server.numberOfConnectedClients + " of " + Server.maxConnectedClients + ".",
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

	private String executeCommand(Command command, String[] inputArgumentList) {
		Account account;

		switch(command) {
			case SETNAME:
				try {
					clientSession.setAccount(new Account(clientSession.server, inputArgumentList[0])); // Create temporary account
				} catch (Exception e){
					return "Unable to create temporary account with nickname: " + inputArgumentList[0];
				}
				state = TRANSIENT;
				clientSession.connectToChannel(Server.defaultChannel);
				return "";

			case EXIT :
				if (state == TRANSIENT || state == LOGGED_IN) {
					clientSession.disconnectFromChannel();
					clientSession.unSetAccount();
					state = ENTRANCE;
					return "Returning to Entrance";
				}  else return null;

			case USERS:
				if(inputArgumentList.length == 0) {
					String list = "";
					for (Session session : clientSession.sessionsInSameChannel()) {
						if (!list.equals("")) {
							list += "\n";
						}
						list += session.account.getName() + " (" + (session.account.temporary ? "transient" : "permanent") +")";
					}
					return list;
				} else return "??"; // TODO: implement once users can create channels

			case WHISPER:
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (account.online) {
					account.currentSession.toClient.println(clientSession.account.getName() + " whispers: " + inputArgumentList[1]);
					return "You whispered a message to " + account.getName();
				} else {
					return "User " + account.getName() + " is not online";
				}

			case SHOUT :
				clientSession.broadcastToChannel(inputArgumentList[0]);
				return "";

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

			case DELETE:
				if(state == LOGGED_IN) {
					state = DELETE_CONF;
					return "This will delete your account!\nType /delete <password> to confirm!";
				} else if (state == DELETE_CONF) {
					account = clientSession.account;
					if (inputArgumentList.length == 1 && account.checkPassword(inputArgumentList[0].getBytes())) {
						clientSession.disconnectFromChannel();
						clientSession.unSetAccount();
						account.delete();
						state = ENTRANCE;
						return "Account deleted. Returning to Entrance";
					}
					else {
						state = LOGGED_IN;
						return "Missing or incorrect password. Cancelling deletion.";
					}
				}
				break;
			case SETPWD:
				clientSession.account.makePermanent(clientSession.server, inputArgumentList[0].getBytes());
				state = LOGGED_IN;
				return "Account now permanent";

			case LOGIN :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account != null && account.checkPassword(inputArgumentList[1].getBytes())) {
					if (account.online) {
						return "Account already logged in";
					}
					else {
						clientSession.setAccount(account);
						clientSession.connectToChannel(Server.defaultChannel);
						state = LOGGED_IN;
						account.lastLoginDate = new Date();
						return "Login successful. Last login: " + account.lastLoginDate;
					}
				} else {
					return "Incorrect username or password";
				}

			case BEFRIEND :
				account = clientSession.server.getAccountByName(inputArgumentList[0]);
				if (account == null){
					return "No account with username " + inputArgumentList[0] + " found";
				} else if (account.temporary) {
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
			case FRIENDS :
				String friendStatus = "";
				friendStatus += "Current friends:\n";
				for (Account friend : clientSession.account.friends) {
					friendStatus += "\t" + friend.getName() + " (" + (friend.online ? friend.currentSession.channel.name : "OFFLINE") + ") \n";
				}
				friendStatus += "\n";
				friendStatus += "Pending sent friend requests: \n";
				for (Account friend : clientSession.account.pendingSentFriendRequests) {
					friendStatus += "\t" + friend.getName() + "\n";
				}
				friendStatus += "\n";
				friendStatus += "Pending received friend requests: \n";
				for (Account friend : clientSession.account.pendingReceivedFriendRequests) {
					friendStatus += "\t" + friend.getName() + "\n";
				}
				return friendStatus;
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
			case CANCEL :
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
}
