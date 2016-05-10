package com.luminis.echochamber.server;

import java.util.ArrayList;
import java.util.Collection;

public class Relations {
	private Account account;
	private AccountCollection friends;
	private AccountCollection sentFriendRequests;
	private AccountCollection receivedFriendRequests;

	Relations(Account account) {
		this.account = account;

		friends = new AccountCollection();
		sentFriendRequests = new AccountCollection();
		receivedFriendRequests = new AccountCollection();
	}

	synchronized public void add(Account target) {
		if (target != null ) {

			checkConsistency(target);

			if (!account.equals(target)) {
				if (!friends.contains(target)) {
					if (receivedFriendRequests.contains(target)) {
						receivedFriendRequests.remove(target);
						target.relations.sentFriendRequests.remove(this.account);
						friends.add(target);
						target.relations.friends.add(this.account);
					} else if (!sentFriendRequests.contains(target)) {
						sentFriendRequests.add(target);
						target.relations.receivedFriendRequests.add(account);
					} else {
						// outgoing friend request already exists
					}
				} else {
					// can't add a friend
				}
			} else {
				// can't add yourself or a null account
			}

			checkConsistency(target);
		}
	}

	synchronized public void remove(Account friend) {
		if (friend != null) {

			checkConsistency(friend);

			friends.remove(friend);
			friend.relations.friends.remove(account);
			receivedFriendRequests.remove(friend);
			friend.relations.sentFriendRequests.remove(account);
			sentFriendRequests.remove(friend);
			friend.relations.receivedFriendRequests.remove(account);

			checkConsistency(friend);
		}
	}

	synchronized public void clear() {
		Collection<Account> all = new ArrayList<>();
		all.addAll(friends.getAccounts());
		all.addAll(sentFriendRequests.getAccounts());
		all.addAll(receivedFriendRequests.getAccounts());
		all.forEach(this::remove);
		account = null;
	}

	synchronized private void checkConsistency(Account target) {
		assert(friends.contains(target) == target.relations.friends.contains(account));
		assert(receivedFriendRequests.contains(target) == target.relations.sentFriendRequests.contains(account));
		assert(sentFriendRequests.contains(target) == target.relations.receivedFriendRequests.contains(account));
		assert(!receivedFriendRequests.contains(target) || !sentFriendRequests.contains(target));
		assert(!friends.contains(target) || !receivedFriendRequests.contains(target));
		assert(!friends.contains(target) || !sentFriendRequests.contains(target));
	}

	@Override
	public String toString() {
		String friendStatus = "";
		friendStatus += "Current friends:\n";
		for (Account friend : friends.getAccounts()) {
			friendStatus += "\t" + friend.username() + " " + (friend.isOnline() ? friend.currentClient.connectedChannel : "[OFFLINE]") + " \n";
		}
		friendStatus += "Pending sent friend requests: \n";
		for (Account friend : sentFriendRequests.getAccounts()) {
			friendStatus += "\t" + friend.username() + "\n";
		}
		friendStatus += "Pending received friend requests: \n";
		for (Account friend : receivedFriendRequests.getAccounts()) {
			friendStatus += "\t" + friend.username() + "\n";
		}
		return friendStatus;
	}
}