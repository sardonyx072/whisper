package com.mjjhoffmann.examples.chat1;

import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.applet.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatClient1 extends Applet {
	private static final long serialVersionUID = 4851965163805711895L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ChatClient1.class);
	private class ChatClient1Thread extends Thread {
		private Socket socket = null;
		private ChatClient1 client = null;
		private DataInputStream dis = null;
		public ChatClient1Thread(ChatClient1 client, Socket socket) {
			this.client = client;
			this.socket = socket;
			this.open();
			this.start();
		}
		public void open() {
			try {this.dis = new DataInputStream(this.socket.getInputStream());}
			catch (IOException e) {
				LOGGER.error("cannot get input stream: " + e.getMessage());
				this.client.stop();
			}
		}
		public void close() {
			try {if (this.dis != null) this.dis.close();}
			catch (IOException e) {LOGGER.error("cannot close input stream: " + e.getMessage());}
		}
		public void run() {
			while (true) {
				try {this.client.handle(this.dis.readUTF());}
				catch (IOException e) {
					LOGGER.error("cannot listen to server: " + e.getMessage());
					this.client.stop();
				}
			}
		}
	}
	
	private Socket socket = null;
	private DataOutputStream dos = null;
	private ChatClient1Thread client = null;
	private TextArea display = new TextArea();
	private TextField sHost = new TextField("Host: "), sPort = new TextField("Port: "), input = new TextField();
	private Button send = new Button("Send"), connect = new Button("Connect"), quit = new Button("Quit");
	private Cipher encryptionCipher = null, decryptionCipher = null;
	public void init() {
		this.sHost.setText("localhost");
		this.sPort.setText("4444");
		Panel north = new Panel();
		north.setLayout(new GridLayout(1,5));
		north.add(new Label("Host:"));
		north.add(sHost);
		north.add(new Label("Port:"));
		north.add(sPort);
		north.add(quit);
		north.add(connect);
		Panel south = new Panel();
		south.setLayout(new BorderLayout());
		south.add("Center", input);
		south.add("East", send);
		this.setLayout(new BorderLayout());
		this.add("North", north);
		this.add("Center", display);
		this.add("South", south);
		this.quit.disable();
		this.send.disable();
		this.input.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) {if (e.getKeyCode() == KeyEvent.VK_ENTER) send();}
		});
	}
	public boolean action(Event e, Object o) {
		if (e.target == this.quit) {
			this.input.setText(".bye");
			this.send();
			this.quit.disable();
			this.send.disable();
			this.connect.enable();
		}
		else if (e.target == this.connect) {this.connect(this.sHost.getText(), Integer.parseInt(this.sPort.getText()));}
		else if (e.target == this.send) {
			this.send();
			this.input.requestFocus();
		}
		return true;
	}
	public void connect(String serverName, int serverPort) {
		LOGGER.info("establishing connection ...");
		try {
			this.socket = new Socket(serverName, serverPort);
			this.println("[Connected: " + this.socket + "]");
			ObjectInputStream ois = new ObjectInputStream(this.socket.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(this.socket.getOutputStream());
			KeyPairGenerator clientKeyGen = KeyPairGenerator.getInstance("DH");
			KeyAgreement clientKeyAgree = KeyAgreement.getInstance("DH");
			KeyFactory clientKeyFactory = KeyFactory.getInstance("DH");
			clientKeyGen.initialize(2048);
			KeyPair clientKeyPair = clientKeyGen.generateKeyPair();
			DHParameterSpec sharedDHParams = ((DHPublicKey)clientKeyPair.getPublic()).getParams();
			clientKeyAgree.init(clientKeyPair.getPrivate());
			LOGGER.info("receiving DH shared params ...");
			DHPublicKeySpec serverDHPublicKeySpec = new DHPublicKeySpec((BigInteger)ois.readObject(), (BigInteger)ois.readObject(), (BigInteger)ois.readObject());
			DHPublicKey serverPublicKey = (DHPublicKey)clientKeyFactory.generatePublic(serverDHPublicKeySpec);
			LOGGER.info("sending public key to server ...");
			oos.writeObject(((DHPublicKey)clientKeyPair.getPublic()).getY());
			oos.writeObject(sharedDHParams.getP());
			oos.writeObject(sharedDHParams.getG());
			oos.flush();
			LOGGER.info("finishing shared secret agreement ...");
			clientKeyAgree.doPhase(serverPublicKey, true);
			byte[] clientSharedSecret = clientKeyAgree.generateSecret();
			LOGGER.info("generating ciphers ...");
			SecretKeySpec clientAesKeySpec = new SecretKeySpec(clientSharedSecret, 0, 32, "AES");
			LOGGER.info("algo: " + clientAesKeySpec.getAlgorithm() + ", fmt: " + clientAesKeySpec.getFormat() + " len: " + (clientAesKeySpec.getEncoded().length*8));
			this.encryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			this.encryptionCipher.init(Cipher.ENCRYPT_MODE, clientAesKeySpec);
			this.decryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			this.decryptionCipher.init(Cipher.DECRYPT_MODE, clientAesKeySpec);
			this.open();
			this.send.enable();
			this.connect.disable();
			this.quit.enable();
		}
		catch (UnknownHostException e) {LOGGER.error("unknown host: " + e.getMessage());}
		catch (IOException e) {LOGGER.error("unexpected exception: " + e.getMessage());}
		catch (ClassNotFoundException e) {LOGGER.error("cannot read DH params: " + e.getMessage());}
		catch (NoSuchAlgorithmException e) {LOGGER.error("cannot use DH algorithm: " + e.getMessage());}
		catch (InvalidKeySpecException e) {LOGGER.error("cannot recover server's public key: " + e.getMessage());}
		catch (InvalidKeyException e) {LOGGER.error("cannot start key agreement: " + e.getMessage());}
		catch (NoSuchPaddingException e) {LOGGER.error("cannot create ciphers: " + e.getMessage());}
	}
	private void send() {
		try {
			this.dos.writeUTF(Base64.getEncoder().encodeToString(this.encryptionCipher.doFinal(this.input.getText().getBytes("UTF-8"))));
			this.dos.flush();
			this.input.setText("");
		}
		catch (IOException e) {
			LOGGER.error("cannot send message: " + e.getMessage());
			this.close();
		} catch (IllegalBlockSizeException e) {
			LOGGER.error("cannot encrypt message: " + e.getMessage());
			this.close();
		} catch (BadPaddingException e) {
			LOGGER.error("cannot encode encrypted message: " + e.getMessage());
			this.close();
		}
	}
	public void handle(String msg) {
		try {
			msg = new String(this.decryptionCipher.doFinal(Base64.getDecoder().decode(msg)));
			if (msg.equals(".bye")) {
				this.println("[Disconnected]");
				this.close();
			}
			else this.println(msg);
		}
		catch (IllegalBlockSizeException e) {LOGGER.error("cannot decrypt message");}
		catch (BadPaddingException e) {LOGGER.error("cannot decode encrypted message");}
	}
	public void open() {
		try {
			this.dos = new DataOutputStream(this.socket.getOutputStream());
			this.client = new ChatClient1Thread(this, this.socket);
		}
		catch (IOException e) {LOGGER.error("cannot open output stream: " + e.getMessage());}
	}
	public void close() {
		try {
			if (this.dos != null) this.dos.close();
			if (this.socket != null) this.socket.close();
		}
		catch (IOException e) {LOGGER.error("cannot close socket: " + e.getMessage());}
		this.client.close();
		this.client.stop();
	}
	private void println(String msg) {this.display.appendText(msg + "\n");}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Whisper");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		ChatClient1 client = new ChatClient1();
		frame.add("Center", client);
		client.init();
		frame.pack();
		frame.setVisible(true);
	}
}
