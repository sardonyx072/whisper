package com.mjjhoffmann.examples.chat;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JApplet;
import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatClient extends JApplet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChatClient.class);
	
	private class ClientThread extends Thread {
		private Socket socket;
		private ChatClient chatClient;
		private DataInputStream dis;
		public ClientThread (ChatClient chatClient, Socket socket) {
			this.socket = socket;
			this.chatClient = chatClient;
			this.open();
			this.start();
		}
		public void open() {
			try {this.dis = new DataInputStream(socket.getInputStream());}
			catch (IOException e) {
				LOGGER.error("error getting input stream: " + e.getMessage());
				this.chatClient.stop();
			}
		}
		public void close() {
			try {if (dis != null) dis.close();}
			catch (IOException e) {LOGGER.error("error closing input stream: " + e.getMessage());}
		}
		
		public void run() {
			while (true) {
				try {this.chatClient.handle(dis.readUTF());}
				catch (IOException e) {
					LOGGER.error("listening error: " + e.getMessage());
					this.chatClient.stop();
				}
			}
		}
	}
	
	private Socket socket = null;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	private ClientThread client = null;
	private TextArea display = new TextArea();
	private TextField input = new TextField();
	private Button send = new Button("Send"), connect = new Button("Connect"), quit = new Button("Quit");
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
		Label title = new Label("Whisper", Label.CENTER);
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		this.setLayout(new BorderLayout());
		this.add("North", title);
		this.add("Center", display);
		this.add("South", south);
		this.quit.disable();
		this.send.disable();
		//this.getParameters();
	}
	public boolean action(Event e, Object o) {
		if (e.target == this.quit) {
			input.setText(".bye");
			this.send();
			this.quit.disable();
			this.send.disable();
			this.connect.enable();
		}
		else if (e.target == this.connect) {this.connect(this.serverName, this.serverPort);}
		else if (e.target == this.send) {
			this.send();
			this.input.requestFocus();
		}
		return true;
	}
	public void connect(String serverName, int serverPort) {
		this.println("Establishing connection. Please wait ...");
		try {
			this.socket = new Socket(serverName, serverPort);
			LOGGER.info("Connected: " + this.socket);
			this.open();
			this.send.enable();
			this.connect.disable();
			this.quit.enable();
		}
		catch (UnknownHostException e) {LOGGER.error("Host Unknown: " + e.getMessage());}
		catch (IOException e) {LOGGER.error("Unexpected exception: " + e.getMessage());}
	}
	public void send() {
		try {
			this.dos.writeUTF(input.getText());
			dos.flush();
			input.setText("");
		}
		catch (IOException e) {
			LOGGER.error("Sending error: " + e.getMessage());
			this.close();
		}
	}
	public void send(String message) {
		try {
			this.dos.writeUTF(message);
			dos.flush();
		} catch (IOException e) {
			LOGGER.error("Sending error: " + e.getMessage());
			this.close();
		}
	}
	public void handle(String message) {
		if (message.equals(".bye")) {
			this.println("Goodbye. Press RETURN to exit ...");
			this.close();
		}
		else this.println(message);
	}
	public void open() {
		try {
			dos = new DataOutputStream(socket.getOutputStream());
			this.client = new ClientThread(this, socket);
		}
		catch (IOException e) {LOGGER.error("Error opening output stream: " + e);}
	}
	public void close() {
		try {
			if (dos != null) dos.close();
			if (socket != null) socket.close();
		}
		catch (IOException e) {LOGGER.error("Error closing ...");}
		this.client.close();
		this.client.stop();
	}
	public void println(String message) {this.display.appendText(message + "\n");}
	public static void main(String[] args) {
		JFrame frame = new JFrame("Whisper");
		ChatClient applet = new ChatClient();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.addWindowListener(new WindowAdapter() {
//			@Override
//			public void windowClosing(WindowEvent windowEvent) {
//				applet.send(".bye");
//			}
//		});
		frame.add(applet);
		applet.init();
		frame.pack();
		frame.setVisible(true);
	}
}
