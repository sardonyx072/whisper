package com.mjjhoffmann.examples.algorithms;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSA_AES {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RSA_AES.class);
	
	public static void mockExchange() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, SignatureException {
		KeyPairGenerator clientKeyGen = KeyPairGenerator.getInstance("RSA");
		clientKeyGen.initialize(2048, new SecureRandom());
		KeyPair clientKeyPair = clientKeyGen.generateKeyPair();
		KeyPairGenerator serverKeyGen = KeyPairGenerator.getInstance("RSA");
		serverKeyGen.initialize(2048, new SecureRandom());
		KeyPair serverKeyPair = serverKeyGen.generateKeyPair();
		
		byte[] plaintext = "This is the secret message.".getBytes();
		
		Cipher clientCipher = Cipher.getInstance("RSA");
		clientCipher.init(Cipher.ENCRYPT_MODE, serverKeyPair.getPublic());
		byte[] ciphertext = clientCipher.doFinal(plaintext);
		
		Signature sig = Signature.getInstance("SHA512withRSA");
		sig.initSign(clientKeyPair.getPrivate());
		sig.update(plaintext);
		byte[] sigtext = sig.sign();
		
		Cipher serverCipher = Cipher.getInstance("RSA");
		serverCipher.init(Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
		byte[] plaintext2 = serverCipher.doFinal(ciphertext);
		
		LOGGER.info("The messages do " + (Arrays.equals(plaintext, plaintext2) ? "" : "not ") + "match");
		
		Signature ver = Signature.getInstance("SHA512withRSA");
		ver.initVerify(clientKeyPair.getPublic());
		ver.update(plaintext2);
		boolean verified = ver.verify(sigtext);
		
		LOGGER.info("The signature verification " + (verified ? "PASSES" : "FAILS"));
	}
	
	public static void main(String[] args) throws Exception {
		RSA_AES.mockExchange();
	}
}
