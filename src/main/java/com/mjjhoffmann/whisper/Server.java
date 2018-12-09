package com.mjjhoffmann.whisper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mjjhoffmann.whisper.Communicator;
import com.mjjhoffmann.whisper.CommunicatorId;

public class Server implements Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
	
	private class ClientThread extends Thread {
		private Server chatServer;
		private Socket socket;
		private int port;
		private DataInputStream dis;
		private DataOutputStream dos;
		
		public ClientThread(Server chatServer, Socket socket) {
			super();
			this.chatServer = chatServer;
			this.socket = socket;
			this.port = socket.getPort();
		}
		public int getPort() {return this.port;}
		public void open() throws IOException {
			this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		}
		public void close() throws IOException {
			if (this.socket != null) this.socket.close();
			if (this.dis != null) this.dis.close();
			if (this.dos != null) this.dos.close();
		}
		public void run() {
			LOGGER.debug("Server thread " + this.port + " is running");
			while (true) {
				try {
					this.chatServer.handleMessage(this.port, this.dis.readUTF());
				}
				catch (IOException e) {
					LOGGER.error("error reading " + this.port + ": " + e.getMessage());
					this.chatServer.remove(this.port);
				}
			}
		}
		public void send(String message) {
			try {
				dos.writeUTF(message);
				dos.flush();
			}
			catch (IOException e) {
				LOGGER.error("error sending message to " + this.port + ": " + e.getMessage());
				this.chatServer.remove(port);
				this.stop();
			}
		}
	}

	private HashSet<ClientThread> clientThreads;
	private HashSet<CommunicatorId> clients;
	private ServerSocket server;
	private Thread thread;
	
	public Server (int port) {
		this.clientThreads = new HashSet<ClientThread>();
		this.clients = new HashSet<CommunicatorId>();
		try {
			this.server = new ServerSocket(port);
			this.start();
		} catch (IOException e) {
			LOGGER.error("Could not bind server on port " + port + ": " + e.getMessage());
		}
	}
	
	public void start() {
		if (this.thread == null ) {
			this.thread = new Thread(this);
			this.thread.start();
		}
	}
	
	public void stop() {
		if (thread != null) {
			this.thread.stop();
			this.thread = null;
		}
	}
	
	public void run() {
		while (thread != null) {
			try {
				LOGGER.debug("waiting for client connection");
				this.handleConnection(server.accept());
			}
			catch (IOException e) {
				LOGGER.error("server accept error: " + e.getMessage());
			}
		}
	}
	
	public void handleConnection(Socket socket) {
		LOGGER.debug("client accepted: " + socket);
		ClientThread toAdd = new ClientThread(this, socket);
		this.clientThreads.add(toAdd);
		try {
			toAdd.open();
			toAdd.start();
		}
		catch (IOException e) {
			LOGGER.error("error opening thread: " + e.getMessage());
		}
	}
	
	public synchronized void remove(int id) {
		//ClientThread[] clients = this.clientThreads.stream().filter(c -> c.getId() == id).toArray(ClientThread[]::new);
		this.clientThreads.stream().filter(c -> c.getPort() == id).forEach(c -> {
			try {
				c.close();
			}
			catch (IOException e) {
				LOGGER.error("error closing thread " + id + ": " + e.getMessage());
			}
			c.stop();
		});
	}
	
	public synchronized void handleMessage(int port, String input) {
		if (input.equals(".bye")) {
			this.clientThreads.stream().filter(c -> c.getPort() == port).forEach(c -> {
				c.send(".bye");
				this.remove(port);
			});
		}
		else
			this.clientThreads.stream().forEach(c -> c.send(port + ": " + input));
	}
	
	public static void main(String[] args) {
		Server chatServer = null;
		if (args.length != 1)
			LOGGER.error("please specify port number");
		else
			chatServer = new Server(Integer.parseInt(args[0]));
	}
}