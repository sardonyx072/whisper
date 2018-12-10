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

public class Server implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
	
	private class ClientHandler extends Thread {
		private Server server;
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		public ClientHandler(Server chatServer, Socket socket) {
			super();
			this.server = chatServer;
			this.socket = socket;
			try {
				this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			}
			catch (IOException e) {LOGGER.error("cannot open streams! " + e.getMessage());}
		}
		public int getPort() {return this.socket.getPort();}
		public void close() throws IOException {
			if (this.socket != null) {
				this.dis.close();
				this.dos.close();
				this.socket.close();
				this.dis = null;
				this.dos = null;
				this.socket = null;
			}
		}
		public void run() {
			while (true) {
				try {this.server.handleMessage(this.socket.getPort(), this.dis.readUTF());}
				catch (IOException e) {
					LOGGER.error("error reading " + this.socket.getPort() + ": " + e.getMessage());
					this.server.remove(this.socket.getPort());
				}
			}
		}
		public void send(String message) {
			try {
				this.dos.writeUTF(message);
				this.dos.flush();
			}
			catch (IOException e) {
				LOGGER.error("error sending message to " + this.socket.getPort() + ": " + e.getMessage());
				this.server.remove(this.socket.getPort());
				this.stop();
			}
		}
		public String toString() {return this.socket.toString();}
	} 

	private HashSet<ClientHandler> clients;
	private ServerSocket server;
	private Thread thread;
	public Server (int port) {
		this.clients = new HashSet<ClientHandler>();
		try {
			this.server = new ServerSocket(port);
			this.thread = new Thread(this);
			this.thread.start();
		}
		catch (IOException e) {LOGGER.error("Could not bind server on port " + port + ": " + e.getMessage());}
	}
	public void stop() {
		if (this.thread != null) {
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
			catch (IOException e) {LOGGER.error("server accept error: " + e.getMessage());}
		}
	}
	public void handleConnection(Socket socket) {
		LOGGER.debug("client accepted: " + socket);
		ClientHandler toAdd = new ClientHandler(this, socket);
		this.clients.add(toAdd);
		toAdd.start();
		LOGGER.debug("done accepting client");
	}
	public synchronized void remove(int id) {
		this.clients.stream().filter(c -> c.getPort() == id).forEach(c -> {
			LOGGER.debug("client disconnected: " + c);
			try {c.close();}
			catch (IOException e) {LOGGER.error("error closing thread " + id + ": " + e.getMessage());}
			c.stop();
			LOGGER.debug("done removing client");
		});
		this.clients.removeIf(c -> c.getPort() == id);
	}
	public synchronized void handleMessage(int port, String input) {
		if (input.equals(".bye")) {
			this.clients.stream().filter(c -> c.getPort() == port).forEach(c -> {
				c.send(".bye");
				this.remove(port);
			});
		}
		else this.clients.stream().forEach(c -> c.send(port + ": " + input));
	}
	public static void main(String[] args) {
		if (args.length != 1) new Server(4444);
		else new Server(Integer.parseInt(args[0]));
	}
}