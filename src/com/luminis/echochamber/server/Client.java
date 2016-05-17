package com.luminis.echochamber.server;
import java.util.*;

import static com.luminis.echochamber.server.ClientState.*;

enum ClientState {
	ENTRANCE	(new String[]{"exit", "help", "status", "setname", "login"}),
	TRANSIENT	(new String[]{"exit", "help", "status", "logout", "whisper", "shout", "users", "setpwd"}),
	LOGGED_IN	(new String[]{"exit", "help", "status", "logout", "whisper", "shout", "users", "befriend", "unfriend", "delete", "accounts", "shutdown"}),
	DELETE_CONF	(new String[]{"exit", "help", "cancel", "delete"}),
	EXIT		(new String[]{});

	String[] validCommands;
	ClientState(String[] validCommands){
		this.validCommands = validCommands;
	}

	public boolean accepts(String command) {
		for(String s : validCommands) {
			if (s.equals(command)) return true;
		}
		return false;
	}
}

class Client {
	private Server server;
	private InputParser parser = new InputParser();
	private ClientState state;
	private Queue<String> output = new LinkedList<>();
	public UUID id;
	Channel connectedChannel = null;
	Account connectedAccount = null;

	Client(Server server, UUID id) {
		this.id = id;
		this.server = server;
		this.server.add(this);
		registerCommands();
		state = ENTRANCE;
	}

	public boolean isActive() {
		return state != EXIT;
	}

	public void inputFromRemote(String input) {
		try {
			String output = parser.evaluate(state, input);
			if (output != null) {
				message(output);
			}
		} catch (Exception e) {
			message(e.getMessage());
		}
	}

	boolean outputForRemoteAvailable() {
		return ( output.peek() != null );
	}
	String outputForRemote() {
		return output.poll();
	}

	void message(String message){ // TODO temporary
		output.add(message);
	}

	public void shutdown(String s) {
		message(s);
		state = EXIT;
	}

	public void cleanup() {
		if (connectedChannel != null) {
			disconnectFromChannel();
		}
		if (connectedAccount != null) {
			unSetAccount();
		}
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
		parser.addCommand(new statusCommand		(this));
		parser.addCommand(new befriendCommand	(this));
		parser.addCommand(new unfriendCommand	(this));
		parser.addCommand(new shutdownCommand	(this.server));
		parser.addCommand(new noCommand			());
	}

	private void connectToChannel(Channel channel) {
		if (this.connectedChannel == null) {
			this.connectedChannel = channel;
			channel.subscribe(this);
			Main.logger.info("Client bound to channel " + channel);
		}
		else Main.logger.warn("Client already bound to channel " + this.connectedChannel);
	}

	private void disconnectFromChannel() {
		if (connectedChannel != null) {
			Main.logger.info("Client unbound from channel " + connectedChannel);
			connectedChannel.unSubscribe(this);
			connectedChannel = null;
		}
		else Main.logger.warn("Client not bound to a channel");
	}

	private void broadcastToChannel(String argument) {
		connectedChannel.shout(argument, this);
	}

	private ArrayList<Client> clientsInSameChannel() {
		if (connectedChannel == null) {
			Main.logger.warn("Client " + this + " not connected to a channel");
			return new ArrayList<>();
		}
		else {
			return connectedChannel.getConnectedClients();
		}
	}

	private void setAccount(Account account) {
		if (connectedAccount == null) {
			connectedAccount = account;
			account.login(this);
			Main.logger.info("Client bound to account " + account);
		}
		else Main.logger.warn("Client already bound to account " + account);
	}

	private void unSetAccount() {
		if (connectedAccount != null) {
			Main.logger.info("Client unbound from account " + connectedAccount);
			this.connectedAccount.logout();
			if (!connectedAccount.isPermanent()) {
				server.removeAccount(connectedAccount);
				connectedAccount.delete();
			}
			this.connectedAccount = null;
		}
		else Main.logger.warn("Client not bound to an account");
	}

	String helpCommandImp(Map<String, String> arguments) {
		if (arguments.size() == 0 || (arguments.get("command name") == null)) {
			return "Available commands: " + String.join(", ", state.validCommands);
		} else {
			String command = arguments.get("command name");
			if (state.accepts(command)) {
				return parser.commands.get(command).getDescription() + " " + parser.commands.get(command).getUsage();
			} else {
				return "Error: No such command \'" + command + "\'";
			}
		}
	}

	String setnameCommandImp(Map<String, String> arguments) {
		Account account = new Account(arguments.get("username")); // Create temporary account
		try {
			server.addAccount(account);
		} catch (Exception e) {
			account.delete();
			return "Unable to create temporary account with name: " + arguments.get("username");
		}
		state = TRANSIENT;
		setAccount(account);
		connectToChannel(Server.defaultChannel);
		return "You are now logged in as " + arguments.get("username");
	}

	String setpwdCommandImp(Map<String, String> arguments) {
		connectedAccount.makePermanent(arguments.get("password").getBytes());
		state = LOGGED_IN;
		return "Account now permanent";
	}

	String loginCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account != null && account.checkPassword(arguments.get("password").getBytes())) {
			if (account.isOnline()) {
				return "Account already logged in";
			}
			else {
				String oldLastLoginDate = account.lastLoginDate.toString();
				setAccount(account);
				connectToChannel(Server.defaultChannel);
				state = LOGGED_IN;
				return "Login successful. Last login: " + oldLastLoginDate;
			}
		} else {
			return "Incorrect username or password";
		}
	}

	String logoutCommandImp() {
		disconnectFromChannel();
		unSetAccount();
		state = ENTRANCE;
		return "Returning to Entrance";
	}

	String accountsCommandImp() {
		String list = "";
		for (Account a : server.accounts.getAccounts()) {
			list += a.infoString() + "\n";
		}
		return list;
	}

	String exitCommandImp() {
		state = EXIT;
		return "Disconnected by server";
	}

	String usersCommandImp(Map<String, String> arguments) {
		if(arguments.size() == 0) {
			String list = "";
			for (Client client : clientsInSameChannel()) {
				if (!list.equals("")) {
					list += "\n";
				}
				list += client.connectedAccount.username() + " (" + (client.connectedAccount.isPermanent() ? "permanent" : "transient") +")";
			}
			return list;
		} else return "??"; // TODO: implement once users can create channels
	}

	String whisperCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			return "No account with username " + arguments.get("username") + " found";
		} else if (account.isOnline()) {
			account.currentClient.message(connectedAccount.username() + " whispers: " + arguments.get("message"));
			return "You whispered a message to " + account.username();
		} else {
			return "User " + account.username() + " is not online";
		}
	}

	String shoutCommandImp(Map<String, String> arguments) {
		if (arguments.get("message") != null) {
			broadcastToChannel(arguments.get("message"));
		}
		return null;
	}

	String deleteCommandImp(Map<String, String> arguments) {
		if(state == LOGGED_IN) {
			state = DELETE_CONF;
			return "This will delete your account!\nType /delete <password> to confirm!";
		} else if (state == DELETE_CONF) {
			Account account = connectedAccount;
			if (arguments.size() == 1 && account.checkPassword(arguments.get("password").getBytes())) {
				disconnectFromChannel();
				unSetAccount();
				server.removeAccount(account);
				account.delete();
				state = ENTRANCE;
				return "Account deleted. Returning to Entrance";
			}
			else {
				state = LOGGED_IN;
				return "Missing or incorrect password. Cancelling deletion.";
			}
		} else {
			return null;
		}
	}

	String cancelCommandImp() {
		state = LOGGED_IN;
		return "Delete cancelled";
	}

	String status() {
		return String.join("\n",
				"Status: ",
				connectedAccount == null ?
						"not logged in" :
						connectedAccount.isPermanent() ?
								String.join("\n",
										"You are logged in as permanent user '" + connectedAccount.username() + "'",
										"\tAccount created: " + connectedAccount.creationDate,
										"\tAccount online since: " + connectedAccount.lastLoginDate,
										"Current channel: " + connectedChannel.toString(),
										"Relations: ",
										connectedAccount.relations.toString()
								) :
								String.join("\n",
										"You are logged in as temporary user '" + connectedAccount.username() + "'",
										"\tOnline since: " + connectedAccount.lastLoginDate,
										"Current channel: " + connectedChannel.toString()
								)
		);
	}

	String befriendCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			return "No account with username " + arguments.get("username") + " found";
		} else if (!account.isPermanent()) {
			return "You can only send friend requests to permanent accounts";
		} else if (account.equals(connectedAccount)) {
			return "Get a life!";
		} else {
			connectedAccount.addRelation(account);
			return "Friend request sent";
		}
	}

	String unfriendCommandImp(Map<String, String> arguments) {
		Account account = server.accounts.getAccountByName(arguments.get("username"));
		if (account == null){
			return "No account with username " + arguments.get("username") + " found";
//		} else if (!connectedAccount.friends.contains(account)) {
//			return account.username() + "is not in your friend list";
		} else {
			connectedAccount.removeRelation(account);
			return "You removed " + account.username() + " from your relations";
		}
	}
}