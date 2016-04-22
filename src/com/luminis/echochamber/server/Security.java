package com.luminis.echochamber.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

class Security {
	final public static SecureRandom random = new SecureRandom();
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	static public byte[] getNewSalt() {
		byte salt[] = new byte[16];
		random.nextBytes(salt);
		random.nextInt();
		return salt;
	}

	static public byte[] saltPassword(byte[] salt, byte[] password){
		byte[] saltedPwd = new byte[salt.length + password.length];
		for (int i = 0; i < salt.length; i++){
			saltedPwd[i] = salt[i];
		}
		for (int i = 0; i < password.length; i++){
			saltedPwd[i + salt.length] = password[i];
			password[i] = 0;
		}
		return saltedPwd;
	}

	static public byte[] calculateHash(byte[] message) {
		try {
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			hash.update(message);
			for (int i = 0; i < message.length; i++) {
				message[i] = 0;
			}
			return hash.digest();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	static String byteArrayToHexString(byte[] bytes){
		char[] hexChars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = hexArray[v >>> 4];
			hexChars[i * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	static byte[] hexStringToByteArray(String hexString){
		int length = hexString.length();
		byte[] bytes = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i+1), 16));
		}
		return bytes;
	}

	static UUID createUUID() {
		return UUID.randomUUID();
	}
}
