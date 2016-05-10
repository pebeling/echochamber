package com.luminis.echochamber.server;
import java.util.*;

import static com.luminis.echochamber.server.SessionState.*;

enum SessionState {
	ENTRANCE	(new String[]{"exit", "help", "setname", "login"}),
	TRANSIENT	(new String[]{"exit", "help", "logout", "whisper", "shout", "users", "setpwd"}),
	LOGGED_IN	(new String[]{"exit", "help", "logout", "whisper", "shout", "users", "befriend", "unfriend", "friends", "delete", "accounts"}),
	DELETE_CONF	(new String[]{"exit", "help", "cancel", "delete"}),
	EXIT		(new String[]{});

	String[] validCommands;
	SessionState(String[] validCommands){
		this.validCommands = validCommands;
	}

	public boolean isValid(String command) {
		for(String s : validCommands) {
			if (s.equals(command)) return true;
		}
		return false;
	}
}

class Session {
	private Server server;
	private InputParser parser = new InputParser();
	private SessionState state;
	Channel connectedChannel = null;
	Account connectedAccount = null;
	String output; // TODO temporary until interaction with Server class  updated

	Session(Server server) {
		this.server = server;
		this.server.add(this);
		registerCommands();
		state = ENTRANCE;
	}

	public void receive(String input) {
		output = "";
		parser.evaluateInput(input);
		if (parser.command == null) {
			Main.logger.error("Null command received");
		} else if (state.isValid(parser.command.getName()) || parser.command.getName().equals("no") || parser.command.getName().equals("invalid")) {
			try {
				parser.command.execute(parser.arguments);
			} catch (Exception e) {
				messageClient(e.getMessage() + " for command '" + parser.command.getName() + "'. " + parser.command.getUsage());
			}
		} else {
			messageClient("Command not available in this context");
		}
	}

	public void end() {
		disconnectFromChannel();
		unSetAccount();
		server.remove(this);
	}

	private void registerCommands() {
		parser.addCommand(new helpCommand		(this));
		parser.addCommand(new setnameCommand	(this));
		parser.addCommand(new setpwdCommand		(this));
		parser.addCommand(new loginCommand		(this));
		parser.addCommand(new logoutCommand		(this));
		parser.addCommand(new accountsCommand	(this));
		parser.addCommand(new exitCommand		(this));
		parser.addCommand(new usersCommand		(this));
		parser.addCommand(new whisperCommand	(this));
		parser.addCommand(new shoutCommand		(this));
		parser.addCommand(new deleteCommand		(this));
		parser.addCommand(new cancelCommand		(this));
		parser.addCommand(new friendsCommand	(this));
		parser.addCommand(new befriendCommand	(this));
		parser.addCommand(new unfriendCommand	(this));
		parser.addCommand(new noCommand			(this));
		parser.addCommand(new invalidCommand	(this));
	}

	private void connectToChannel(Channel channel) {
		if (this.connectedChannel == null) {
			this.connectedChannel = channel;
			channel.subscribe(this);
			Main.logger.info("Session bound to channel " + channel);
		}
		else Main.logger.warn("Session already bound to channel " + this.connectedChannel);
	}

	private void disconnectFromChannel() {
		if (connectedChannel != null) {
			Main.logger.info("Session unbound from channel " + connectedChannel);
			connectedChannel.unSubscribe(this);
			connectedChannel = null;
		}
		else Main.logger.warn("Session not bound to a channel");
	}

	private void broadcastToChannel(String argument) {
		connectedChannel.shout(argument, this);
	}

	private ArrayList<Session> sessionsInSameChannel() {
		if (connectedChannel == null) {
			Main.logger.warn("Session " + this + " not connected to a channel");
			return new ArrayList<>();
		}
		else {
			return connectedChannel.getConnectedSessions();
		}
	}

	private void setAccount(Account account) {
		if (connectedAccount == null) {
			connectedAccount = account;
			account.login(this);
			Main.logger.info("Session bound to account " + account);
		}
		else Main.logger.warn("Session already bound to account " + account);
	}

	private void unSetAccount() {
		if (connectedAccount != null) {
			Main.logger.info("Session unbound from account " + connectedAccount);
			this.connectedAccount.logout();
			if (!connectedAccount.isPermanent()) {
				server.removeAccount(connectedAccount);
				connectedAccount.delete();
			}
			this.connectedAccount = null;
		}
		else Main.logger.warn("Session not bound to an account");
	}

	void messageClient(String message){ // TODO temporary
		output = String.join("\n", output, message);
	}
	
	void helpCommandImp(Map<String, String> arguments) {
		if (arguments.size() == 0) {
			messageClient("Available commands: " + String.join(", ", state.validCommands));
		} else if (arguments.get("command name") != null) {
			String command = arguments.get("command name");
			if (state.isValid(command)) {
				messageClient(parser.commands.get(command).getDescription() + " " + parser.commands.get(command).getUsage());
			} else {
				messageClient("Error: No such command \"" + command + "\"");
			}
		}
	}

	void setnameCommandImp(Map<String, String> arguments) {
		Account account = new Account(arguments.get("username")); // Create temporary account
		try {
			server.addAccount(account);
			setAccount(account);
		} catch (Exception e) {
			account.delete();
			messageClient("Unable to create temporary account with nickname: " + arguments.get("username"));
		}
		state = TRANSIENT;
		connectToChannel(Server.defaultChannel);
	}

	void setpwdCommandImp(Map<String, String> arguments) {
		connectedAccount.makePermanent(arguments.get("password").getBytes());
		state = LOGGED_IN;
		messageClient("Account now permanent");
	}

	void loginCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account != null && account.checkPassword(arguments.get("password").getBytes())) {
			if (account.isOnline()) {
				messageClient("Account already logged in");
			}
			else {
				String oldLastLoginDate = account.lastLoginDate.toString();
				setAccount(account);
				messageClient("Login successful. Last login: " + oldLastLoginDate);
				connectToChannel(Server.defaultChannel);
				state = LOGGED_IN;
			}
		} else {
			messageClient("Incorrect username or password");
		}
	}

	void logoutCommandImp() {
		disconnectFromChannel();
		unSetAccount();
		state = ENTRANCE;
		messageClient("Returning to Entrance");
	}

	void accountsCommandImp() {
		String list = "";
		for (Account a : server.accounts.getAccounts()) {
			list += a.infoString() + "\n";
		}
		messageClient(list);
	}

	public boolean isActive() {
		return state != EXIT;
	}

	void exitCommandImp() {
		messageClient("Disconnected by server");
		state = EXIT;
	}

	void usersCommandImp(Map<String, String> arguments) {
		if(arguments.size() == 0) {
			String list = "";
			for (Session session : sessionsInSameChannel()) {
				if (!list.equals("")) {
					list += "\n";
				}
				list += session.connectedAccount.username() + " (" + (session.connectedAccount.isPermanent() ? "permanent" : "transient") +")";
			}
			messageClient(list);
		} else messageClient("??"); // TODO: implement once users can create channels
	}

	void whisperCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			messageClient("No account with username " + arguments.get("username") + " found");
		} else if (account.isOnline()) {
			account.currentSession.messageClient(connectedAccount.username() + " whispers: " + arguments.get("message"));
			messageClient("You whispered a message to " + account.username());
		} else {
			messageClient("User " + account.username() + " is not online");
		}
	}

	void shoutCommandImp(Map<String, String> arguments) {
		if (arguments.get("message") != null) {
			broadcastToChannel(arguments.get("message"));
		}
	}

	void deleteCommandImp(Map<String, String> arguments) {
		if(state == LOGGED_IN) {
			state = DELETE_CONF;
			messageClient("This will delete your account!\nType /delete <password> to confirm!");
		} else if (state == DELETE_CONF) {
			Account account = connectedAccount;
			if (arguments.size() == 1 && account.checkPassword(arguments.get("password").getBytes())) {
				disconnectFromChannel();
				unSetAccount();
				server.removeAccount(account);
				account.delete();
				state = ENTRANCE;
				messageClient("Account deleted. Returning to Entrance");
			}
			else {
				state = LOGGED_IN;
				messageClient("Missing or incorrect password. Cancelling deletion.");
			}
		}
	}

	void cancelCommandImp() {
		state = LOGGED_IN;
		messageClient("Delete cancelled");
	}

	void friendsCommandImp() {
		messageClient(connectedAccount.relations.toString());
	}

	void befriendCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			messageClient("No account with username " + arguments.get("username") + " found");
		} else if (!account.isPermanent()) {
			messageClient("You can only send friend requests to permanent accounts");
		} else if (account.equals(connectedAccount)) {
			messageClient("Get a life!");
		} else {
			connectedAccount.addRelation(account);
			messageClient("Friend request sent");
		}
	}

	void unfriendCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			messageClient("No account with username " + arguments.get("username") + " found");
//		} else if (!connectedAccount.friends.contains(account)) {
//			messageClient(account.username() + "is not in your friend list");
		} else {
			connectedAccount.removeRelation(account);
			messageClient("You removed " + account.username() + " from your friend list");
		}
	}

	void noCommandImp(Map<String, String> arguments) {
		if (state == ENTRANCE) {
			helpCommandImp(arguments);
		} else if (state == LOGGED_IN || state == TRANSIENT) {
			arguments.put("message", arguments.get("arguments"));
			shoutCommandImp(arguments);
		}
	}
	
	void invalidCommandImp(Map<String, String> arguments) {
		if ( arguments.get("command") == null) {
			messageClient("Invalid");
		} else {
			messageClient("Invalid command '" + arguments.get("command") + "'");
		}
	}
}
