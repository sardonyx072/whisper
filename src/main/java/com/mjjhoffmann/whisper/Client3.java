package com.mjjhoffmann.whisper;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client3 extends JFrame {
	private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
	
	private class ServerHandler extends Thread {
		private Socket socket;
		private Client3 client;
		private DataInputStream dis;
		public ServerHandler (Client3 client, Socket socket) {
			this.socket = socket;
			this.client = client;
			try {this.dis = new DataInputStream(this.socket.getInputStream());}
			catch (IOException e) {LOGGER.error("cannot open stream! " + e.getMessage());}
			this.start();
		}
		public void close() {
			try {
				if (this.dis != null) {
					this.dis.close();
					this.dis = null;
				}
			}
			catch (IOException e) {LOGGER.error("cannot close socket " + e.getMessage());}
		}
		public void run() {
			while (true) {
				try {this.client.handle(this.dis.readUTF());}
				catch (IOException e) {
					LOGGER.error("listening error: " + e.getMessage());
					this.client.disconnect();
				}
			}
		}
	}
	
	private Socket socket;
	private DataOutputStream dos;
	private ServerHandler server;
	private JTextArea display = new JTextArea();
	private JScrollPane displayScrolling = new JScrollPane(display);
	private JTextField host = new JTextField(), port = new JTextField(), name = new JTextField(), chat = new JTextField();
	private JButton connect = new JButton("Connect");
	public Client3 () {
		super("Whisper");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel connection = new JPanel();
		connection.setLayout(new GridLayout(1,5));
		connection.add(new JLabel("Host:"));
		connection.add(this.host);
		connection.add(new JLabel("Port:"));
		connection.add(this.port);
		connection.add(this.connect);
		JPanel input = new JPanel();
		input.setLayout(new GridLayout(1,2));
		input.add(this.name);
		input.add(this.chat);
		this.chat.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {if (e.getKeyCode() == KeyEvent.VK_ENTER) send(chat.getText());}
		});
		this.display.setEditable(false);
		this.setLayout(new BorderLayout());
		this.add("North", connection);
		this.add("Center", this.displayScrolling);
		this.add("South", input);
		this.host.setText("localhost");
		this.port.setText("4444");
		this.connect.setBackground(Color.RED);
		this.connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {toggleConnected();}
		});
		this.name.disable();
		this.chat.disable();
		this.setPreferredSize(new Dimension(750, 500));
		this.pack();
	}
	public void toggleConnected() {
		if (this.socket == null) {this.connect();}
		else {this.send(".bye");}
	}
	public void connect() {
		try {
			LOGGER.info("Establishing connection...");
			this.socket = new Socket(this.host.getText(), Integer.parseInt(this.port.getText()));
			LOGGER.info("Connected: " + this.socket);
			this.dos = new DataOutputStream(this.socket.getOutputStream());
			this.server = new ServerHandler(this, this.socket);
			this.host.disable();
			this.port.disable();
			this.connect.setBackground(Color.GREEN);
			this.connect.setText("Disconnect");
			this.name.enable();
			this.chat.enable();
			if (this.name.getText().isEmpty()) this.name.setText("guest"+this.socket.getLocalPort());
			this.chat.requestFocus();
		}
		catch (UnknownHostException e) {LOGGER.error("Unknown host: " + e.getMessage());}
		catch (NumberFormatException e) {LOGGER.error("bad port number format: " + e.getMessage());}
		catch (IOException e) {LOGGER.error("Unexpected exception: " + e.getMessage());}
	}
	public void disconnect() {
		try {
			if (this.socket != null) {
				this.dos.close();
				this.socket.close();
				this.dos = null;
				this.socket = null;
			}
			this.host.enable();
			this.port.enable();
			this.connect.setBackground(Color.RED);
			this.connect.setText("Connect");
			this.name.disable();
			this.chat.disable();
		}
		catch (IOException e) {LOGGER.error("error closing");}
		this.server.close();
		this.server.stop();
	}
	public void send(String message) {
		try {
			//this.dos.writeUTF(this.name.getText() + ": " + message);
			this.dos.writeUTF(message);
			this.dos.flush();
			this.chat.setText("");
		}
		catch (IOException e) {
			LOGGER.error("sending error: " + e.getMessage());
			this.disconnect();
		}
	}
	public void handle(String message) {
		if (message.equals(".bye")) {
			this.disconnect();
			this.println("[Disconnected]");
		}
		else this.println(message);
	}
	public void println(String message) {
		this.display.append(message + "\n");
	}
	
	public static void main(String[] args) {
		//if (args.length != 2) LOGGER.error("client requires server name and port");
		//else new Client(args[0], Integer.parseInt(args[1]));
		new Client3().setVisible(true);
	}
}
