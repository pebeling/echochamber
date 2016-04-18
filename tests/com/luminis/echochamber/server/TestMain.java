package com.luminis.echochamber.server;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Date;
import static org.junit.Assert.*;

public class TestMain {
	@Test
	public void testPasswordIsZeroedBySalting() throws Exception {
		byte[] salt = Security.getNewSalt();
		byte[] password = new byte[] {'P', 'W', 'D'};

		Security.saltPassword(salt, password);

		boolean isZeroed = true;
		for(byte b : password) isZeroed = isZeroed && (b == 0);
		assertTrue(isZeroed);
	}

	@Test
	public void testPasswordIsZeroedByHashing() throws Exception {
		byte[] password = new byte[] {'P', 'W', 'D'};

		Security.calculateHash(password);

		boolean isZeroed = true;
		for(byte b : password) isZeroed = isZeroed && (b == 0);
		assertTrue(isZeroed);
	}

	@Test
	public void testByteHexStringConversion() throws Exception {
		byte[] original = Security.getNewSalt();

		String hexString = Security.byteArrayToHexString(original);
		byte[] derived = Security.hexStringToByteArray(hexString);

		boolean byteArraysEqueal = (original.length == derived.length);
		for (int i = 0; i < original.length; i++) byteArraysEqueal = byteArraysEqueal && (original[i] == derived[i]);

		assertTrue(byteArraysEqueal);
	}

	@Test
	public void testHexStringByteConversion() throws Exception {
		String original = Security.byteArrayToHexString(Security.getNewSalt());

		byte[] bytes = Security.hexStringToByteArray(original);
		String derived = Security.byteArrayToHexString(bytes);

		assertEquals(original, derived);
	}

	@Test
	public void testCreateAccount() throws Exception {
		Date start = new Date();
		Account account = new Account("TEST_NAME", new byte[] {'P', 'W', 'D'});
		Date end = new Date();

		assertEquals(account.getName(), "TEST_NAME");
		assertEquals(account.lastLoginDate, null);
		assertEquals(account.friends, new ArrayList<Account>());
		assertEquals(account.pendingSentFriendRequests, new ArrayList<Account>());
		assertEquals(account.pendingReceivedFriendRequests, new ArrayList<Account>());
		assertTrue(account.active);
		assertEquals(account.session, null);

//		Date creation = account.creationDate;
//		assertTrue(creation.after(start) || creation.equals(start));
//		assertTrue(creation.before(end) || creation.equals(end));
	}

	@Test
	public void testDeleteAccount() throws Exception {
		Account account = new Account("TEST_NAME", new byte[] {'P', 'W', 'D'});

		account.delete();
//		assertFalse(account.checkPassword(new byte[] {'P', 'W', 'D'}));
		assertFalse(account.active);
		assertEquals(account.session, null);
		assertEquals(account.friends, new ArrayList<Account>());
		assertEquals(account.pendingSentFriendRequests, new ArrayList<Account>());
		assertEquals(account.pendingReceivedFriendRequests, new ArrayList<Account>());
	}

	@Test
	public void testDeleteAccountWithSession() throws Exception {
		Account account = new Account("TEST_NAME", new byte[] {});
		account.startSession();

		account.delete();
		assertEquals(account.session, null);
	}

	@Test
	public void testDeleteAccountWithFriendData() throws Exception {
		Account accountClark = new Account("Clark", new byte[] {});
		Account accountBruce = new Account("Bruce", new byte[] {});
		Account accountDiana = new Account("Diana", new byte[] {});
		Account accountZod = new Account("Zod", new byte[] {});

		accountClark.sendFriendRequest(accountBruce);
		accountClark.sendFriendRequest(accountDiana);
		accountDiana.acceptFriendRequest(accountClark);
		accountZod.sendFriendRequest(accountClark);
		accountClark.delete();

		assertEquals(accountClark.friends, new ArrayList<Account>());
		assertEquals(accountClark.pendingSentFriendRequests, new ArrayList<Account>());
		assertEquals(accountClark.pendingReceivedFriendRequests, new ArrayList<Account>());
		assertFalse(accountDiana.friends.contains(accountClark));
		assertFalse(accountBruce.pendingReceivedFriendRequests.contains(accountClark));
		assertFalse(accountZod.pendingSentFriendRequests.contains(accountClark));
	}

//	@Test
//	public void testAccountPasswordCheck() throws Exception {
//		Account account = new Account("TEST_NAME", new byte[] {'P', 'W', 'D'});
//		assertTrue(account.checkPassword(new byte[] {'P', 'W', 'D'}));
//		assertFalse(account.checkPassword(new byte[] {'Q', 'E', 'D'}));
//	}

	@Test
	public void testFriendRequest() throws Exception {
		Account accountBob = new Account("Bob", new byte[] {});
		Account accountEve = new Account("Eve", new byte[] {});

		accountBob.sendFriendRequest(accountEve);

		assertTrue(accountBob.pendingSentFriendRequests.contains(accountEve));
		assertTrue(accountEve.pendingReceivedFriendRequests.contains(accountBob));

		assertFalse(accountBob.friends.contains(accountEve));
		assertFalse(accountEve.friends.contains(accountBob));
	}

	@Test
	public void testFriendRequestCancel() throws Exception {
		Account accountBob = new Account("Bob", new byte[] {});
		Account accountEve = new Account("Eve", new byte[] {});

		accountBob.sendFriendRequest(accountEve);
		accountBob.cancelFriendRequest(accountEve);

		assertFalse(accountBob.pendingSentFriendRequests.contains(accountEve));
		assertFalse(accountEve.pendingReceivedFriendRequests.contains(accountBob));
	}

	@Test
	public void testFriendRequestRefusal() throws Exception {
		Account accountBob = new Account("Bob", new byte[] {});
		Account accountEve = new Account("Eve", new byte[] {});

		accountBob.sendFriendRequest(accountEve);
		accountEve.refuseFriendRequest(accountBob);

		assertFalse(accountBob.pendingSentFriendRequests.contains(accountEve));
		assertFalse(accountEve.pendingReceivedFriendRequests.contains(accountBob));
	}

	@Test
	public void testFriendRequestAcceptance() throws Exception {
		Account accountBob = new Account("Bob", new byte[] {});
		Account accountEve = new Account("Eve", new byte[] {});

		accountBob.sendFriendRequest(accountEve);
		accountEve.acceptFriendRequest(accountBob);

		assertFalse(accountBob.pendingSentFriendRequests.contains(accountEve));
		assertFalse(accountEve.pendingReceivedFriendRequests.contains(accountBob));

		assertTrue(accountBob.friends.contains(accountEve));
		assertTrue(accountEve.friends.contains(accountBob));
	}

	@Test
	public void testUnFriend() throws Exception {
		Account accountBob = new Account("Bob", new byte[] {});
		Account accountEve = new Account("Eve", new byte[] {});

		accountBob.sendFriendRequest(accountEve);
		accountEve.acceptFriendRequest(accountBob);
		accountBob.unfriend(accountEve);

		assertFalse(accountBob.friends.contains(accountEve));
		assertFalse(accountEve.friends.contains(accountBob));
	}

	@Test
	public void testSessionStart() throws Exception {
		Account account = new Account("Bob", new byte[] {});
		account.startSession();
		Session session = account.session;

		assertTrue(session.active);
		assertEquals(session.currentChannel.getName(), Channel.defaultChannel.getName());
		assertTrue(session.currentChannel.participants.contains(account));
	}
}
