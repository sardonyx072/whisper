package com.mjjhoffmann.examples.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mjjhoffmann.whisper.Communicator;

public class ChatClient implements Runnable {
	
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
			try {
				this.dis = new DataInputStream(socket.getInputStream());
			}
			catch (IOException e) {
				LOGGER.error("error getting input stream: " + e.getMessage());
				this.chatClient.stop();
			}
		}
		
		public void close() {
			try {
				if (dis != null) dis.close();
			}
			catch (IOException e) {
				LOGGER.error("error closing input stream: " + e.getMessage());
			}
		}
		
		public void run() {
			while (true) {
				try {
					this.chatClient.handle(dis.readUTF());
				}
				catch (IOException e) {
					LOGGER.error("listening error: " + e.getMessage());
					this.chatClient.stop();
				}
			}
		}
	}
	
	private Socket socket;
	private Thread thread;
	private DataInputStream dis;
	private DataOutputStream dos;
	private ClientThread client;
	
	public ChatClient (String serverName, int serverPort) {
		LOGGER.info("Establishing connection...");
		try {
			this.socket = new Socket(serverName, serverPort);
			LOGGER.info("Connected: " + this.socket);
			this.start();
		}
		catch (UnknownHostException e) {
			LOGGER.error("Unknown host: " + e.getMessage());
		}
		catch (IOException e) {
			LOGGER.error("Unexpected exception: " + e.getMessage());
		}
	}
	
	public void start() throws IOException {
		this.dis = new DataInputStream(System.in);
		this.dos = new DataOutputStream(this.socket.getOutputStream());
		if (this.thread == null) {
			this.client = new ClientThread(this, this.socket);
			this.thread = new Thread(this);
			this.thread.start();
		}
	}
	
	public void stop() {
		if (this.thread != null) {
			this.thread.stop();
			this.thread = null;
		}
		try {
			if (dis != null) dis.close();
			if (dos != null) dos.close();
			if (socket != null) socket.close();
		}
		catch (IOException e) {
			LOGGER.error("error closing");
		}
		this.client.close();
		this.client.stop();
	}
	
	public void run() {
		while (this.thread != null) {
			try {
				dos.writeUTF(dis.readLine());
				dos.flush();
			}
			catch (IOException e) {
				LOGGER.error("sending error: " + e.getMessage());
				this.stop();
			}
		}
	}
	
	public void handle(String message) {
		if (message.equals(".bye")) {
			LOGGER.info("Good bye. Press RETURN to exit ...");
			this.stop();
		}
		else
			LOGGER.info(message);
	}
	
	public static void main(String[] args) {
		ChatClient chatClient = null;
		if (args.length != 2)
			LOGGER.error("client requires server name and port");
		else
			chatClient = new ChatClient(args[0], Integer.parseInt(args[1]));
	}
}
