package examples;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DH_AES {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DH_AES.class);
	
	public static void mockExchange() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IllegalStateException, ShortBufferException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		KeyPairGenerator clientKeyGen = KeyPairGenerator.getInstance("DH");
		clientKeyGen.initialize(2048);
		KeyPair clientKeyPair = clientKeyGen.generateKeyPair();
		KeyAgreement clientKeyAgree = KeyAgreement.getInstance("DH");
		clientKeyAgree.init(clientKeyPair.getPrivate());
		byte[] clientPublicKeyEncoded = clientKeyPair.getPublic().getEncoded();
		KeyFactory serverKeyFactory = KeyFactory.getInstance("DH");
		X509EncodedKeySpec clientKeySpec = new X509EncodedKeySpec(clientPublicKeyEncoded);
		PublicKey clientPublicKey = serverKeyFactory.generatePublic(clientKeySpec);
		
		DHParameterSpec shareDHParams = ((DHPublicKey)clientPublicKey).getParams();
		
		KeyPairGenerator serverKeyGen = KeyPairGenerator.getInstance("DH");
		serverKeyGen.initialize(shareDHParams);
		KeyPair serverKeyPair = serverKeyGen.generateKeyPair();
		KeyAgreement serverKeyAgree = KeyAgreement.getInstance("DH");
		serverKeyAgree.init(serverKeyPair.getPrivate());
		byte[] serverPublicKeyEncoded = serverKeyPair.getPublic().getEncoded();
		KeyFactory clientKeyFactory = KeyFactory.getInstance("DH");
		X509EncodedKeySpec serverKeySpec = new X509EncodedKeySpec(serverPublicKeyEncoded);
		PublicKey serverPublicKey = clientKeyFactory.generatePublic(serverKeySpec);
		
		clientKeyAgree.doPhase(serverPublicKey, true);
		serverKeyAgree.doPhase(clientPublicKey, true);
		
		byte[] clientSharedSecret = clientKeyAgree.generateSecret();
		byte[] serverSharedSecret = new byte[clientSharedSecret.length];
		serverKeyAgree.generateSecret(serverSharedSecret, 0);
		
		LOGGER.info("Shared secret is" + (Arrays.equals(clientSharedSecret, serverSharedSecret) ? "" : " not") + " a match");

		SecretKeySpec serverAesKeySpec = new SecretKeySpec(serverSharedSecret, 0, 16, "AES");
		SecretKeySpec clientAesKeySpec = new SecretKeySpec(clientSharedSecret, 0, 16, "AES");
		
		Cipher serverCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		serverCipher.init(Cipher.ENCRYPT_MODE, serverAesKeySpec);
		
		byte[] cleartext = "This is the secret message.".getBytes();
		byte[] ciphertext = serverCipher.doFinal(cleartext);
		byte[] encodedParams = serverCipher.getParameters().getEncoded();
		
		AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
		aesParams.init(encodedParams);
		
		Cipher clientCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		clientCipher.init(Cipher.DECRYPT_MODE, clientAesKeySpec, aesParams);
		
		byte[] recovered = clientCipher.doFinal(ciphertext);
		
		LOGGER.info("Plaintext is" + (Arrays.equals(cleartext, recovered) ? "" : " not") + " a match");
	}
	
	public static void main (String[] args) throws Exception {
		DH_AES.mockExchange();
	}
}
