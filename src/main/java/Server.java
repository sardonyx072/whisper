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
import java.util.HashSet;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server extends Communicator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	private HashSet<CommunicatorId> clients;
	
	public Server (String name) {
		super(name);
		this.clients = new HashSet<CommunicatorId>();
	}
	
	public HashSet<CommunicatorId> getClients() {return this.clients;}
	
	public static void mockExchange() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IllegalStateException, ShortBufferException, NoSuchPaddingException {
		KeyPairGenerator aliceKeyGen = KeyPairGenerator.getInstance("DH");
		aliceKeyGen.initialize(2048);
		KeyPair aliceKeyPair = aliceKeyGen.generateKeyPair();
		KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
		aliceKeyAgree.init(aliceKeyPair.getPrivate());
		byte[] alicePublicKeyEncoded = aliceKeyPair.getPublic().getEncoded();
		KeyFactory bobKeyFactory = KeyFactory.getInstance("DH");
		X509EncodedKeySpec aliceKeySpec = new X509EncodedKeySpec(alicePublicKeyEncoded);
		PublicKey alicePublicKey = bobKeyFactory.generatePublic(aliceKeySpec);
		
		DHParameterSpec shareDHParams = ((DHPublicKey)alicePublicKey).getParams();
		
		KeyPairGenerator bobKeyGen = KeyPairGenerator.getInstance("DH");
		bobKeyGen.initialize(shareDHParams);
		KeyPair bobKeyPair = bobKeyGen.generateKeyPair();
		KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
		bobKeyAgree.init(bobKeyPair.getPrivate());
		byte[] bobPublicKeyEncoded = bobKeyPair.getPublic().getEncoded();
		KeyFactory aliceKeyFactory = KeyFactory.getInstance("DH");
		X509EncodedKeySpec bobKeySpec = new X509EncodedKeySpec(bobPublicKeyEncoded);
		PublicKey bobPublicKey = aliceKeyFactory.generatePublic(bobKeySpec);
		
		aliceKeyAgree.doPhase(bobPublicKey, true);
		bobKeyAgree.doPhase(alicePublicKey, true);
		
		byte[] aliceSharedSecret = aliceKeyAgree.generateSecret();
		byte[] bobSharedSecret = new byte[aliceSharedSecret.length];
		bobKeyAgree.generateSecret(bobSharedSecret, 0);
		
		LOGGER.info("Shared secret is " + (Arrays.equals(aliceSharedSecret, bobSharedSecret) ? "" : "not ") + "a match");

		SecretKeySpec bobAesKeySpec = new SecretKeySpec(bobSharedSecret, 0, 16, "AES");
		SecretKeySpec aliceAesKeySpec = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES");
		
		Cipher bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		
		
	}
	
	public static void mockExchangeExample() throws Exception {
        // Alice creates her own DH key pair with 2048-bit key size
        System.out.println("ALICE: Generate DH keypair ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
        
        // Alice creates and initializes her DH KeyAgreement object
        System.out.println("ALICE: Initialization ...");
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());
        
        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();
        
        // Let's turn over to Bob. Bob has received Alice's public key in encoded format. He instantiates a DH public key from the encoded key material.
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

        // Bob gets the DH parameters associated with Alice's public key. He must use the same parameters when he generates his own key pair.
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey)alicePubKey).getParams();

        // Bob creates his own DH key pair
        System.out.println("BOB: Generate DH keypair ...");
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        // Bob creates and initializes his DH KeyAgreement object
        System.out.println("BOB: Initialization ...");
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());

        // Bob encodes his public key, and sends it over to Alice.
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        // Alice uses Bob's public key for the first (and only) phase of her version of the DH protocol. Before she can do so, she has to instantiate a DH public key from Bob's encoded key material.
        KeyFactory aliceKeyFac = KeyFactory.getInstance("DH");
        x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
        PublicKey bobPubKey = aliceKeyFac.generatePublic(x509KeySpec);
        System.out.println("ALICE: Execute PHASE1 ...");
        aliceKeyAgree.doPhase(bobPubKey, true);

        // Bob uses Alice's public key for the first (and only) phase of his version of the DH protocol.
        System.out.println("BOB: Execute PHASE1 ...");
        bobKeyAgree.doPhase(alicePubKey, true);

        // At this stage, both Alice and Bob have completed the DH key agreement protocol. Both generate the (same) shared secret.
        byte[] aliceSharedSecret = new byte[0];
        byte[] bobSharedSecret = new byte[0];
        try {
        	aliceSharedSecret = aliceKeyAgree.generateSecret();
            int aliceLen = aliceSharedSecret.length;
            bobSharedSecret = new byte[aliceLen];
            int bobLen;
            bobLen = bobKeyAgree.generateSecret(bobSharedSecret, 0);
            System.out.println("Alice secret: " +
                    toHexString(aliceSharedSecret));
            System.out.println("Bob secret: " +
                    toHexString(bobSharedSecret));
        } catch (ShortBufferException e) {
            System.out.println(e.getMessage());
        }        // provide output buffer of required size
        if (!java.util.Arrays.equals(aliceSharedSecret, bobSharedSecret))
            throw new Exception("Shared secrets differ");
        System.out.println("Shared secrets are the same");

        /*
         * Now let's create a SecretKey object using the shared secret and use it for encryption. First, we generate SecretKeys for the "AES" algorithm (based on the raw shared secret data) and
         * Then we use AES in CBC mode, which requires an initialization vector (IV) parameter. Note that you have to use the same IV for encryption and decryption: If you use a different IV for
         * decryption than you used for encryption, decryption will fail.
         *
         * If you do not specify an IV when you initialize the Cipher object for encryption, the underlying implementation will generate a random one, which you have to retrieve using the
         * javax.crypto.Cipher.getParameters() method, which returns an instance of java.security.AlgorithmParameters. You need to transfer the contents of that object (e.g., in encoded format, obtained via
         * the AlgorithmParameters.getEncoded() method) to the party who will do the decryption. When initializing the Cipher for decryption, the (reinstantiated) AlgorithmParameters object must be explicitly
         * passed to the Cipher.init() method.
         */
        System.out.println("Use shared secret as SecretKey object ...");
        SecretKeySpec bobAesKey = new SecretKeySpec(bobSharedSecret, 0, 16, "AES");
        SecretKeySpec aliceAesKey = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES");

        /*
         * Bob encrypts, using AES in CBC mode
         */
        Cipher bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        bobCipher.init(Cipher.ENCRYPT_MODE, bobAesKey);
        byte[] cleartext = "This is just an example".getBytes();
        byte[] ciphertext = bobCipher.doFinal(cleartext);

        // Retrieve the parameter that was used, and transfer it to Alice in
        // encoded format
        byte[] encodedParams = bobCipher.getParameters().getEncoded();

        /*
         * Alice decrypts, using AES in CBC mode
         */

        // Instantiate AlgorithmParameters object from parameter encoding
        // obtained from Bob
        AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
        aesParams.init(encodedParams);
        Cipher aliceCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aliceCipher.init(Cipher.DECRYPT_MODE, aliceAesKey, aesParams);
        byte[] recovered = aliceCipher.doFinal(ciphertext);
        if (!java.util.Arrays.equals(cleartext, recovered))
            throw new Exception("AES in CBC mode recovered text is " +
                    "different from cleartext");
        System.out.println("AES in CBC mode recovered text is " +
                "same as cleartext");
	}

    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }
	
	public static void main (String[] args) throws Exception {
		Server.mockExchange();
	}
}
