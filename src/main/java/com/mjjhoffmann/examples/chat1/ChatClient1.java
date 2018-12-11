package com.mjjhoffmann.examples.chat1;

import java.net.*;
import javax.swing.*;
import java.io.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatClient1 extends Applet {
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
			try {
				this.dis = new DataInputStream(this.socket.getInputStream());
			}
			catch (IOException e) {
				System.out.println("cannot get input stream: " + e.getMessage());
				this.client.stop();
			}
		}
		public void close() {
			try {
				if (this.dis != null) this.dis.close();
			}
			catch (IOException e) {
				System.out.println("cannot close input stream: " + e.getMessage());
			}
		}
		public void run() {
			while (true) {
				try {
					this.client.handle(this.dis.readUTF());
				}
				catch (IOException e) {
					System.out.println("cannot listen to server: " + e.getMessage());
					this.client.stop();
				}
			}
		}
	}
	
	private Socket socket = null;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	private ChatClient1Thread client = null;
	private TextArea display = new TextArea();
	private TextField input = new TextField();
	private Button send = new Button("Send"), connect = new Button("Connect"), quit = new Button("Bye");
	private String serverName = "localhost";
	private int serverPort = 4444;
	public void init() {
		Panel keys = new Panel();
		keys.setLayout(new GridLayout(1,2));
		keys.add(quit);
		keys.add(connect);
		Panel south = new Panel();
		south.setLayout(new BorderLayout());
		south.add("West", keys);
		south.add("Center", input);
		south.add("East", send);
		this.setLayout(new BorderLayout());
		this.add("Center", display);
		this.add("South", south);
		this.quit.disable();
		this.send.disable();
		this.input.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {if (e.getKeyCode() == KeyEvent.VK_ENTER) send();}
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
		else if (e.target == this.connect) {
			this.connect(this.serverName, this.serverPort);
		}
		else if (e.target == this.send) {
			this.send();
			this.input.requestFocus();
		}
		return true;
	}
	public void connect(String serverName, int serverPort) {
		this.println("establishing connection ...");
		try {
			this.socket = new Socket(serverName, serverPort);
			this.println("connected: " + this.socket);
			this.open();
			this.send.enable();
			this.connect.disable();
			this.quit.enable();
		}
		catch (UnknownHostException e) {
			this.println("unknown host: " + e.getMessage());
		}
		catch (IOException e) {
			this.println("unexpected exception: " + e.getMessage());
		}
	}
	private void send() {
		try {
			this.dos.writeUTF(this.input.getText());
			this.dos.flush();
			this.input.setText("");
		}
		catch (IOException e) {
			this.println("cannot send message: " + e.getMessage());
			this.close();
		}
	}
	public void handle(String msg) {
		if (msg.equals(".bye")) {
			this.println("[Disconnected]");
			this.close();
		}
		else this.println(msg);
	}
	public void open() {
		try {
			this.dos = new DataOutputStream(this.socket.getOutputStream());
			this.client = new ChatClient1Thread(this, this.socket);
		}
		catch (IOException e) {
			this.println("cannot open output stream: " + e.getMessage());
		}
	}
	public void close() {
		try {
			if (this.dos != null) this.dos.close();
			if (this.socket != null) this.socket.close();
		}
		catch (IOException e) {
			this.println("cannot close socket: " + e.getMessage());
		}
		this.client.close();
		this.client.stop();
	}
	private void println(String msg) {
		this.display.appendText(msg + "\n");
	}
	
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
