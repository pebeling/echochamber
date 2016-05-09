package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
//import java.lang.reflect.Constructor;
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
	private PrintWriter toClient;
	private BufferedReader fromClient;
	private InputParser parser = new InputParser();
	private SessionState state;
	Channel connectedChannel;
	Account connectedAccount;

	Session(Server server, PrintWriter toClient, BufferedReader fromClient) {
		this.toClient = toClient;
		this.fromClient = fromClient;
		this.server = server;
		connectedChannel = null;
		connectedAccount = null;
		state = ENTRANCE;
	}

	public void run() {
		registerCommands();
		String inputLine;

		while (!(state == EXIT)) {
			try {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					ConnectionManager.logger.info("Client has disconnected from server");
					state = EXIT;
				} else {
					parser.evaluateInput(inputLine);
					if (parser.command == null) {
						ConnectionManager.logger.error("Null command received");
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ConnectionManager.logger.info("Server has closed the connection to client");

		if (connectedChannel != null) disconnectFromChannel();
		if (connectedAccount != null) unSetAccount();
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
			ConnectionManager.logger.info("Session bound to channel " + channel);
		}
		else ConnectionManager.logger.warn("Session already bound to channel " + this.connectedChannel);
	}

	private void disconnectFromChannel() {
		if (connectedChannel != null) {
			ConnectionManager.logger.info("Session unbound from channel " + connectedChannel);
			connectedChannel.unSubscribe(this);
			connectedChannel = null;
		}
		else ConnectionManager.logger.warn("Session not bound to a channel");
	}

	private void broadcastToChannel(String argument) {
		connectedChannel.shout(argument, this);
	}

	private ArrayList<Session> sessionsInSameChannel() {
		if (connectedChannel == null) {
			ConnectionManager.logger.warn("Session " + this + " not connected to a channel");
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
			ConnectionManager.logger.info("Session bound to account " + account);
		}
		else ConnectionManager.logger.warn("Session already bound to account " + account);
	}

	private void unSetAccount() {
		if (connectedAccount != null) {
			ConnectionManager.logger.info("Session unbound from account " + connectedAccount);
			this.connectedAccount.logout();
			if (!connectedAccount.isPermanent()) {
				server.removeAccount(this.connectedAccount);
			}
			this.connectedAccount = null;;
		}
		else ConnectionManager.logger.warn("Session not bound to an account");
	}

	void messageClient(String message){
		if (toClient != null) {
			toClient.println(message);
		} else {
			ConnectionManager.logger.warn("Message sent to disconnected client");
		}
	}

	void exit() {
		state = EXIT;
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
				connectToChannel(Server.defaultChannel);
				state = LOGGED_IN;
				messageClient("Login successful. Last login: " + oldLastLoginDate);
			}
		} else {
			messageClient("Incorrect username or password");
		}
	}

	void logoutCommandImp(Map<String, String> arguments) {
		disconnectFromChannel();
		unSetAccount();
		state = ENTRANCE;
		messageClient("Returning to Entrance");
	}

	void accountsCommandImp(Map<String, String> arguments) {
		String list = "";
		for (Account a : server.accounts.getAccounts()) {
			list += a.infoString() + "\n";
		}
		messageClient(list);
	}

	void exitCommandImp(Map<String, String> arguments) {
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
				state = ENTRANCE;
				messageClient("Account deleted. Returning to Entrance");
			}
			else {
				state = LOGGED_IN;
				messageClient("Missing or incorrect password. Cancelling deletion.");
			}
		}
	}

	void cancelCommandImp(Map<String, String> arguments) {
		state = LOGGED_IN;
		messageClient("Delete cancelled");
	}

	void friendsCommandImp(Map<String, String> arguments) {
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
