package com.luminis.echochamber.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import static com.luminis.echochamber.server.SessionState.*;

enum SessionState {
	ENTRANCE	(new String[]{"exit", "help", "setname", "login"}),
	TRANSIENT	(new String[]{"exit", "help", "logout", "whisper", "shout", "users", "setpwd"}),
	LOGGED_IN	(new String[]{"exit", "help", "logout", "whisper", "shout", "users", "befriend", "unfriend", "friends", "accept", "refuse", "forget", "delete", "accounts", "sessions"}),
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

class Session extends Thread {
	private Server server;
	private Socket socket = null;
	private PrintWriter toClient;
	private BufferedReader fromClient;
	private UUID id;
	private InputParser parser = new InputParser();
	private SessionState state;
	Channel connectedChannel;
	Account connectedAccount;

	Session(Socket socket, Server server) {
		super("Session");
		this.socket = socket;
		this.server = server;
		connectedChannel = null;
		connectedAccount = null;
		id = Security.createUUID();
		super.setName("Session " + id);
		state = ENTRANCE;
	}

	public void run() {
		registerCommands();
		try {
			server.addSession(this);
			Server.logger.info("Session started for client at " + socket.getInetAddress() + ":" + socket.getLocalPort());
			toClient = new PrintWriter(socket.getOutputStream(), true);
			fromClient = new BufferedReader(
					new InputStreamReader(socket.getInputStream())
			);

			messageClient(TextColors.colorServermessage(welcomeMessage()));

			String inputLine;

			while (!(state == EXIT)) {
				inputLine = fromClient.readLine();
				if (inputLine == null) {
					Server.logger.info("Client in session at " + socket.getInetAddress() + ":" + socket.getLocalPort() + " has disconnected from server");
					state = EXIT;
				} else {
					parser.evaluateInput(inputLine);
					if (state.isValid(parser.command.getName()) || parser.command.getName().equals("") ) {
						try {
							parser.command.execute(parser.arguments);
						} catch (Exception e) {
							messageClient("Invalid arguments for command " + parser.command.getName() + ": " + e.getMessage());
						}
					} else {
						messageClient("Command not available in this context");
					}
				}
			}
			Server.logger.info("Server has closed the connection to client");

			if (connectedChannel != null) disconnectFromChannel();
			if (connectedAccount != null) unSetAccount();
			
			fromClient.close();
			toClient.close();
			socket.close();
			Server.logger.info("Session terminated");
			server.removeSession(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void registerCommands() {
//		for (String c : Command.commandMasterList) {
//			String classname = c + "Command";
//			try {
//				Class cls = Class.forName(classname);
//				Constructor commandConstructor = cls.getDeclaredConstructor(Session.class);
//				commandConstructor.setAccessible(true);
//				Command command = (Command)commandConstructor.newInstance(this);
//				parser.addCommand(c, command);
//			} catch (Throwable e) {
//				System.err.println(e); // command class not implemented
//			}
//		}
		parser.addCommand("help", 		new helpCommand(this));
		parser.addCommand("setname", 	new setnameCommand(this));
		parser.addCommand("setpwd", 	new setpwdCommand(this));
		parser.addCommand("login", 		new loginCommand(this));
		parser.addCommand("logout", 	new logoutCommand(this));
		parser.addCommand("accounts", 	new accountsCommand(this));
		parser.addCommand("sessions", 	new sessionsCommand(this));
		parser.addCommand("exit", 		new exitCommand(this));
		parser.addCommand("users", 		new usersCommand(this));
		parser.addCommand("whisper", 	new whisperCommand(this));
		parser.addCommand("shout", 		new shoutCommand(this));
		parser.addCommand("delete", 	new deleteCommand(this));
		parser.addCommand("cancel", 	new cancelCommand(this));
		parser.addCommand("friends", 	new friendsCommand(this));
		parser.addCommand("befriend", 	new befriendCommand(this));
		parser.addCommand("unfriend", 	new unfriendCommand(this));
		parser.addCommand("accept", 	new acceptCommand(this));
		parser.addCommand("refuse", 	new refuseCommand(this));
		parser.addCommand("forget", 	new forgetCommand(this));
		parser.addCommand("nop", 		new noCommand(this));
		parser.addCommand("invalid", 	new invalidCommand(this));
	}

	@Override
	public String toString() {
		return id.toString();
	}

	private void connectToChannel(Channel channel) {
		if (this.connectedChannel == null) {
			this.connectedChannel = channel;
			channel.subscribe(this);
			Server.logger.info("Session bound to channel " + channel);
		}
		else Server.logger.warn("Session already bound to channel " + this.connectedChannel);
	}

	private void disconnectFromChannel() {
		if (connectedChannel != null) {
			Server.logger.info("Session unbound from channel " + connectedChannel);
			connectedChannel.unSubscribe(this);
			connectedChannel = null;
		}
		else Server.logger.warn("Session not bound to a channel");
	}

	private void broadcastToChannel(String argument) {
		connectedChannel.shout(argument, this);
	}

	private ArrayList<Session> sessionsInSameChannel() {
		if (connectedChannel == null) {
			Server.logger.warn("Session " + this + " not connected to a channel");
			return new ArrayList<>();
		}
		else {
			return connectedChannel.getConnectedSessions();
		}
	}

//	ArrayList<Session> sessionsInChannel(Channel connectedChannel) {
//		return connectedChannel.connectedSessions;
//	}

	private void setAccount(Account account) {
		if (this.connectedAccount == null) {
			this.connectedAccount = account;
			account.login(this);
			Server.logger.info("Session bound to account " + account);
		}
		else Server.logger.warn("Session already bound to account " + account);
	}

	private void unSetAccount() {
		if (this.connectedAccount != null) {
			Server.logger.info("Session unbound from account " + connectedAccount);
			this.connectedAccount.logout();
			if (!this.connectedAccount.isPermanent()) {
				this.server.removeAccount(this.connectedAccount);
			}
			this.connectedAccount = null;;
		}
		else Server.logger.warn("Session not bound to an account");
	}

	void messageClient(String message){
		if (toClient != null) {
			toClient.println(message);
		} else {
			Server.logger.warn("Message sent to disconnected client");
		}
	}

	void exit() {
		state = EXIT;
	}
	
	void helpCommandImp(List<String> arguments) {
		if (arguments.size() == 0) {
			messageClient("Available commands: " + String.join(", ", state.validCommands));
		} else if (arguments.size() == 1) {
			String command = arguments.get(0);
			if (state.isValid(command)) {
				messageClient(parser.commands.get(command).getDescription());
			} else {
				messageClient("Error: No such command \"" + command + "\"");
			}
		}
	}

	void setnameCommandImp(List<String> arguments) {
		Account account = new Account(arguments.get(0)); // Create temporary account
		try {
			server.addAccount(account);
			setAccount(account);
		} catch (Exception e) {
			account.delete();
			messageClient("Unable to create temporary account with nickname: " + arguments.get(0));
		}
		state = TRANSIENT;
		connectToChannel(Server.defaultChannel);
	}

	void setpwdCommandImp(List<String> arguments) {
		connectedAccount.makePermanent(arguments.get(0).getBytes());
		state = LOGGED_IN;
		messageClient("Account now permanent");
	}

	void loginCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account != null && account.checkPassword(arguments.get(1).getBytes())) {
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

	void logoutCommandImp(List<String> arguments) {
		disconnectFromChannel();
		unSetAccount();
		state = ENTRANCE;
		messageClient("Returning to Entrance");
	}

	void accountsCommandImp(List<String> arguments) {
		String list = "";
		for (Account a : server.accounts) {
			list += a.infoString() + "\n";
		}
		messageClient(list);
	}

	void sessionsCommandImp(List<String> arguments) {
		String list = "";
		for (Session s : server.sessions) {
			list += s.toString() + "\n";
		}
		messageClient(list);
	}

	void exitCommandImp(List<String> arguments) {
		messageClient("Disconnected by server");
		state = EXIT;
	}

	void usersCommandImp(List<String> arguments) {
		if(arguments.size() == 0) {
			String list = "";
			for (Session session : sessionsInSameChannel()) {
				if (!list.equals("")) {
					list += "\n";
				}
				list += session.connectedAccount.getName() + " (" + (session.connectedAccount.isPermanent() ? "permanent" : "transient") +")";
			}
			messageClient(list);
		} else messageClient("??"); // TODO: implement once users can create channels
	}

	void whisperCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No account with username " + arguments.get(0) + " found");
		} else if (account.isOnline()) {
			account.currentSession.messageClient(connectedAccount.getName() + " whispers: " + arguments.get(1));
			messageClient("You whispered a message to " + account.getName());
		} else {
			messageClient("User " + account.getName() + " is not online");
		}
	}

	void shoutCommandImp(List<String> arguments) {
		broadcastToChannel(arguments.get(0));
		messageClient("");
	}

	void deleteCommandImp(List<String> arguments) {
		if(state == LOGGED_IN) {
			state = DELETE_CONF;
			messageClient("This will delete your account!\nType /delete <password> to confirm!");
		} else if (state == DELETE_CONF) {
			if (arguments.size() == 1 && connectedAccount.checkPassword(arguments.get(0).getBytes())) {
				disconnectFromChannel();
				unSetAccount();
				server.removeAccount(connectedAccount);
				state = ENTRANCE;
				messageClient("Account deleted. Returning to Entrance");
			}
			else {
				state = LOGGED_IN;
				messageClient("Missing or incorrect password. Cancelling deletion.");
			}
		}
	}

	void cancelCommandImp(List<String> arguments) {
		state = LOGGED_IN;
		messageClient("Delete cancelled");
	}

	void friendsCommandImp(List<String> arguments) {
		String friendStatus = "";
		friendStatus += "Current friends:\n";
		for (Account friend : connectedAccount.friends) {
			friendStatus += "\t" + friend.getName() + " " + (friend.isOnline() ? friend.currentSession.connectedChannel : "[OFFLINE]") + " \n";
		}
		friendStatus += "Pending sent friend requests: \n";
		for (Account friend : connectedAccount.pendingSentFriendRequests) {
			friendStatus += "\t" + friend.getName() + "\n";
		}
		friendStatus += "Pending received friend requests: \n";
		for (Account friend : connectedAccount.pendingReceivedFriendRequests) {
			friendStatus += "\t" + friend.getName() + "\n";
		}
		messageClient(friendStatus);
	}

	void befriendCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No account with username " + arguments.get(0) + " found");
		} else if (!account.isPermanent()) {
			messageClient("You can only send friend requests to permanent accounts");
		} else if (account.equals(connectedAccount)) {
			messageClient("Get a life!");
		} else {
			connectedAccount.sendFriendRequest(account);
			messageClient("Friend request sent");
		}
	}

	void unfriendCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No connectedAccount with username " + arguments.get(0) + " found");
		} else if (!connectedAccount.friends.contains(account)) {
			messageClient(account.getName() + "is not in your friend list");
		} else {
			connectedAccount.unfriend(account);
			messageClient("You removed " + account.getName() + " from your friend list");
		}
	}

	void acceptCommandImp(List<String> arguments) {
		Account account  = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No account with username " + arguments.get(0) + " found");
		} else if (!connectedAccount.pendingReceivedFriendRequests.contains(account)) {
			messageClient("No pending friend request from " + account.getName());
		} else {
			connectedAccount.acceptFriendRequest(account);
			messageClient("You and " + account.getName() + " are now friends");
		}
	}

	void refuseCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No account with username " + arguments.get(0) + " found");
		} else if (!connectedAccount.pendingReceivedFriendRequests.contains(account)) {
			messageClient("No pending friend request from " + account.getName());
		} else {
			connectedAccount.refuseFriendRequest(account);
			messageClient("You refused " + account.getName() + "'s friend request");
		}
	}

	void forgetCommandImp(List<String> arguments) {
		Account account = server.getAccountByName(arguments.get(0));
		if (account == null){
			messageClient("No account with username " + arguments.get(0) + " found");
		} else if (!connectedAccount.pendingSentFriendRequests.contains(account)) {
			messageClient("No outstanding friend request to " + account.getName());
		} else {
			connectedAccount.refuseFriendRequest(account);
			messageClient("You cancelled friend request to " + account.getName());
		}
	}

	void noCommandImp(List<String> arguments) {
		if (state == ENTRANCE) {
			helpCommandImp(arguments);
		}
	}
	
	void invalidCommandImp(List<String> arguments) {
		messageClient("Invalid command '" + arguments.get(0) + "'");
	}
	

	private String welcomeMessage() {
		String[] lines = new String[]{
				"--------------------------------------------------",
				"Welcome to the EchoChamber chat server!",
				"Local time is: " + new Date(),
				"You are client " + server.numberOfConnectedClients() + " of " + Server.maxConnectedClients + ".",
				"Use /help or /help <command> for more information.",
				"--------------------------------------------------"
		};
		return String.join("\n", lines);
	}
}
