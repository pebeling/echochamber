package com.luminis.echochamber.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccountCollection {
	private ArrayList<Account> accounts;

	AccountCollection(){
		accounts = new ArrayList<>();
	}

	synchronized public Account getAccountByName(String username) {
		for (Account account : accounts) {
			if (account.username().equals(username)) {
				return account;
			}
		}
		return null;
	}

	synchronized public boolean add(Account account) {
		if (account == null) { return false ;}
		else {
			String username = account.username();
			if (getUsernames().contains(username)) { return false; }
			else {
				accounts.add(account);
				return true;
			}
		}
	}

	synchronized public boolean remove(Account account) {
		if (account == null) { return true; }
		else {
			if (accounts.remove(account)) {
				return true;
			} else {
				return false;
			}
		}
	}

	synchronized public boolean removeByName(String username) {
		Account account = getAccountByName(username);
		return remove(account);
	}

	synchronized public List<String> getUsernames() {
		return accounts.stream().map(Account::username).collect(Collectors.toList());
	}

	synchronized public List<Account> getAccounts() {
		return accounts;
	}

	synchronized public void clear(){
		accounts.clear();
	}

	synchronized public boolean contains(Account account) {
		return accounts.contains(account);
	}

	public int size() {
		return accounts.size();
	}
}
