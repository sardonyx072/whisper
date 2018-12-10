package com.mjjhoffmann.whisper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server2 extends JFrame {
	private static final Logger LOGGER = LoggerFactory.getLogger(Server2.class);
	
	private class ClientHandler extends Thread {
		private ServerThread server;
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		private String clientName;
		public ClientHandler(ServerThread server, Socket socket) throws IOException {
			this.server = server;
			this.socket = socket;
			this.dis = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
			this.dos = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
		}
		public void setClientName(String clientName) {this.clientName = clientName;}
		public String getClientName() {return this.clientName;}
		public void run() {
			
		}
	}
	
	private class ServerThread extends Thread {
		private ServerSocket socket;
		private HashSet<ClientHandler> clients;
		private JTextArea clientsArea, logArea;
		public ServerThread(int port, JTextArea clientsArea, JTextArea logArea) throws IOException {
			this.socket = new ServerSocket(port);
			this.clients = new HashSet<ClientHandler>();
			this.clientsArea = clientsArea;
			this.logArea = logArea;
		}
		public void run() {
			while(!this.isInterrupted()) {
				try {
					ClientHandler client = new ClientHandler(this, this.socket.accept());
					this.clients.add(client);
					client.start();
				}
				catch (IOException e) {LOGGER.error("cannot acecpt client " + e.getMessage());}
			}
		}
		public String toString() {return this.socket.toString();}
	}
	
	private ServerThread server;
	private JFormattedTextField port;
	private JButton btn = new JButton("Open");
	private ActionListener openListener, closeListener;
	private JTextArea clientsArea = new JTextArea(), logArea = new JTextArea();
	private JScrollPane clientsSArea = new JScrollPane(clientsArea), logSArea = new JScrollPane(logArea);
	public Server2() {
		super("Whisper - Server");
		NumberFormat fmt = NumberFormat.getIntegerInstance();
		fmt.setGroupingUsed(false);
		this.port = new JFormattedTextField(new PortNumberFormatter(fmt));
		this.port.setText("4444");
		this.btn.setBackground(Color.RED);
		JPanel north = new JPanel();
		north.setLayout(new BorderLayout());
		north.add("West", new JLabel("Port: "));
		north.add("Center", port);
		north.add("East", btn);
		this.clientsArea.setEditable(false);
		this.logArea.setEditable(false);
		this.setLayout(new BorderLayout());
		this.add("North", north);
		this.add("West", clientsSArea);
		this.add("Center", logSArea);
		this.openListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
				btn.removeActionListener(this);
				btn.addActionListener(closeListener);
			}
		};
		this.closeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
				btn.removeActionListener(this);
				btn.addActionListener(openListener);
			}
		};
		this.btn.addActionListener(this.openListener);
		this.setPreferredSize(new Dimension(750, 500));
		this.pack();
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
				System.exit(0);
			}
		});
	}
	public void open() {
		try {
			this.server = new ServerThread(Integer.parseInt(this.port.getText()), this.clientsArea, this.logArea);
			this.server.start();
			this.btn.setText("Close");
			this.btn.setBackground(Color.GREEN);
			LOGGER.info("opened server " + this.server);
		}
		catch (NumberFormatException e) {LOGGER.error("bad port number format");}
		catch (IOException e) {LOGGER.error("cannot open server socket on port " + this.port.getText());}
	}
	public void close() {
		this.server.interrupt();
		this.server.stop();
		this.server = null;
		this.btn.setText("Open");
		this.btn.setBackground(Color.RED);
		LOGGER.info("closed server");
	}
	public static void main(String[] args) {
		new Server2().setVisible(true);
	}
}
