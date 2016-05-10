package com.luminis.echochamber.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccountCollection extends ArrayList<Account> {
	synchronized public Account getAccountByName(String username) {
		for (Account account : this) {
			if (account.username().equals(username)) {
				return account;
			}
		}
		return null;
	}

	@Override
	synchronized public boolean add(Account account) {
		if (account == null) { return false ;}
		else {
			String username = account.username();
			if (getUsernames().contains(username)) { return false; }
			else {
				super.add(account);
				return true;
			}
		}
	}

	synchronized public boolean removeByName(String username) {
		Account account = getAccountByName(username);
		return remove(account);
	}

	synchronized public List<String> getUsernames() {
		return this.stream().map(Account::username).collect(Collectors.toList());
	}

	synchronized public List<Account> getAccounts() {
		return this;
	}
}
