package com.mjjhoffmann.examples.chat1;

import java.net.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ChatServer1 implements Runnable {
	private class ChatServer1Thread extends Thread {
		private ChatServer1 server = null;
		private Socket socket = null;
		private int port = -1;
		private DataInputStream dis = null;
		private DataOutputStream dos = null;
		private Cipher encryptionCipher = null, decryptionCipher = null;
		public ChatServer1Thread(ChatServer1 server, Socket socket, SecretKeySpec keySpec) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
			super();
			this.server = server;
			this.socket = socket;
			this.port = this.socket.getPort();
			this.encryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			this.encryptionCipher.init(Cipher.ENCRYPT_MODE, keySpec);
			this.decryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			this.decryptionCipher.init(Cipher.DECRYPT_MODE, keySpec);
		}
		public void send(String msg) {
			try {
				this.dos.writeUTF(Base64.getEncoder().encodeToString(this.encryptionCipher.doFinal(msg.getBytes("UTF-8"))));
				this.dos.flush();
			}
			catch (IOException e) {
				System.out.println("cannot send on port " + this.port + ": " + e.getMessage());
				this.server.remove(this.port);
			} catch (IllegalBlockSizeException e) {
				System.out.println("cannot encrpyt on port " + this.port + ": " + e.getMessage());
				this.server.remove(this.port);
			} catch (BadPaddingException e) {
				System.out.println("cannot encode on port " + this.port + ": " + e.getMessage());
				this.server.remove(this.port);
			}
		}
		public int getPort() {return this.port;}
		public void run() {
			System.out.println("Server listening to client on port " + this.port + "...");
			while (true) {
				try {
					this.server.handle(this.port, new String(this.decryptionCipher.doFinal(Base64.getDecoder().decode(this.dis.readUTF()))));
				}
				catch (IOException e) {
					System.out.println("cannot read on port " + this.port + ": " + e.getMessage());
					this.server.remove(this.port);
					this.stop();
				} catch (IllegalBlockSizeException e) {
					System.out.println("cannot decrypt on port " + this.port + ": " + e.getMessage());
					this.server.remove(this.port);
					this.stop();
				} catch (BadPaddingException e) {
					System.out.println("cannot decode on port " + this.port + ": " + e.getMessage());
					this.server.remove(this.port);
					this.stop();
				}
			}
		}
		public void open() throws IOException {
			this.dis = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
			this.dos = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
		}
		public void close() throws IOException {
			if (this.socket != null) this.socket.close();
			if (this.dis != null) this.dis.close();
			if (this.dos != null) this.dos.close();
		}
	}
	
	private ChatServer1Thread clients[] = new ChatServer1Thread[50];
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;
	public ChatServer1 (int port) {
		try {
			this.server = new ServerSocket(port);
			System.out.println("server started: " + this.server);
			this.start();
		}
		catch (IOException e) {
			System.out.println("cannot bind on port " + port + ": " + e.getMessage());
		}
	}
	public void run() {
		System.out.println("generating DH keys");
		KeyPairGenerator serverKeyGen = null;
		KeyAgreement serverKeyAgree = null;
		KeyFactory serverKeyFactory = null;
		KeyPair serverKeyPair = null;
		DHParameterSpec sharedDHParams = null;
		X509EncodedKeySpec serverKeySpec = null;
		try {
			serverKeyGen = KeyPairGenerator.getInstance("DH");
			serverKeyAgree = KeyAgreement.getInstance("DH");
			serverKeyFactory = KeyFactory.getInstance("DH");
			serverKeyGen.initialize(2048);
			serverKeyPair = serverKeyGen.generateKeyPair();
			serverKeyAgree.init(serverKeyPair.getPrivate());
			sharedDHParams = ((DHPublicKey)serverKeyPair.getPublic()).getParams();
			serverKeySpec = new X509EncodedKeySpec(serverKeyPair.getPublic().getEncoded());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		while (this.thread != null) {
			try {
				System.out.println("waiting for a client ...");
				Socket socket = this.server.accept();
				if (this.clientCount < this.clients.length) {
					System.out.println("accepted client: " + socket);
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					System.out.println("transferring DH public key spec ...");
					oos.writeObject(((DHPublicKey)serverKeyPair.getPublic()).getY());
					oos.writeObject(sharedDHParams.getP());
					oos.writeObject(sharedDHParams.getG());
					oos.flush();
					System.out.println("receiving client public key ...");
					DHPublicKeySpec clientDHPublicKeySpec =  new DHPublicKeySpec((BigInteger)ois.readObject(), (BigInteger)ois.readObject(), (BigInteger)ois.readObject());
					DHPublicKey clientDHPublicKey = (DHPublicKey)serverKeyFactory.generatePublic(clientDHPublicKeySpec);
					System.out.println("finishing secret agreement ...");
					serverKeyAgree.doPhase(clientDHPublicKey, true);
					byte[] serverSharedSecret = serverKeyAgree.generateSecret();
					//serverSharedSecret = MessageDigest.getInstance("SHA-256").digest(serverSharedSecret);
					//byte[] symmetricKey = Arrays.copyOf(serverSharedSecret, 16);
					//byte[] macKey = Arrays.copyOfRange(serverSharedSecret, 16, 32);
					System.out.println("generating ciphers and starting client handler ...");
					SecretKeySpec serverAesKeySpec = new SecretKeySpec(serverSharedSecret, 0, 16, "AES");
					this.clients[this.clientCount] = new ChatServer1Thread(this, socket, serverAesKeySpec);
					this.clients[this.clientCount].open();
					this.clients[this.clientCount].start();
					this.clientCount++;
				}
				else
					System.out.println("client refused: maximum (" + this.clients.length + ") clients reached.");
			}
			catch (IOException e) {
				System.out.println("cannot accept client: " + e.getMessage());
				this.stop();
			} catch (IllegalStateException e) {
				System.out.println("cannot perform key agreement: " + e.getMessage());
				this.stop();
			} catch (ClassNotFoundException e) {
				System.out.println("cannot read client public key params: " + e.getMessage());
				this.stop();
			} catch (InvalidKeySpecException e) {
				System.out.println("cannot generate client public key from params: " + e.getMessage());
				this.stop();
			} catch (InvalidKeyException e) {
				System.out.println("cannot perform key agreement with client public key: " + e.getMessage());
				this.stop();
			} catch (NoSuchAlgorithmException e) {
				System.out.println("cannot digest shared secret: " + e.getMessage());
				this.stop();
			} catch (NoSuchPaddingException e) {
				System.out.println("cannot create ciphers for client: " + e.getMessage());
				this.stop();
			}
		}
	}
	public void start() {
		if (this.thread == null) {
			this.thread = new Thread(this);
			this.thread.start();
		}
	}
	public void stop() {
		if (this.thread != null) {
			this.thread.stop();
			this.thread = null;
		}
	}
	private int findClient(int port) {
		for (int i = 0; i < this.clientCount; i++)
			if (this.clients[i].getPort() == port)
				return i;
		return -1;
	}
	public synchronized void handle(int port, String input) {
		if (input.equals(".bye")) {
			this.clients[this.findClient(port)].send(".bye");
			this.remove(port);
		}
		else
			for (int i = 0; i < this.clientCount; i++)
				this.clients[i].send(port + ": " + input);
	}
	public synchronized void remove(int port) {
		int pos = this.findClient(port);
		if (pos >= 0) {
			System.out.println("terminating connection with client on port " + port);
			ChatServer1Thread toTerminate = this.clients[pos];
			if (pos < this.clientCount-1)
				for (int i = pos+1; i < this.clientCount; i++)
					this.clients[i-1] = clients[i];
			this.clientCount--;
			try {
				toTerminate.close();
			}
			catch (IOException e) {
				System.out.println("cannot terminate thread");
			}
			toTerminate.stop();
		}
	}
	
	public static void main(String[] args) {
		ChatServer1 server = null;
		if (args.length != 1) new ChatServer1(4444);
		else server = new ChatServer1(Integer.parseInt(args[0]));
	}
}
