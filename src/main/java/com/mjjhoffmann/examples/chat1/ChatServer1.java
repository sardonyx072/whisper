package com.mjjhoffmann.examples.chat1;

import java.net.*;
import java.io.*;

public class ChatServer1 implements Runnable {
	private class ChatServer1Thread extends Thread {
		private ChatServer1 server = null;
		private Socket socket = null;
		private int port = -1;
		private DataInputStream dis = null;
		private DataOutputStream dos = null;
		public ChatServer1Thread(ChatServer1 server, Socket socket) {
			super();
			this.server = server;
			this.socket = socket;
			this.port = this.socket.getPort();
		}
		public void send(String msg) {
			try {
				this.dos.writeUTF(msg);
				this.dos.flush();
			}
			catch (IOException e) {
				System.out.println("cannot send on port " + this.port + ": " + e.getMessage());
				this.server.remove(this.port);
			}
		}
		public int getPort() {return this.port;}
		public void run() {
			System.out.println("Server listening to client on port " + this.port + "...");
			while (true) {
				try {
					this.server.handle(this.port, this.dis.readUTF());
				}
				catch (IOException e) {
					System.out.println("cannot read on port " + this.port + ": " + e.getMessage());
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
		while (this.thread != null) {
			try {
				System.out.println("waiting for a client ...");
				this.addThread(this.server.accept());
			}
			catch (IOException e) {
				System.out.println("cannot accept client: " + e.getMessage());
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
	private void addThread(Socket socket) {
		if (this.clientCount < this.clients.length) {
			System.out.println("accepted client: " + socket);
			this.clients[this.clientCount] = new ChatServer1Thread(this, socket);
			try {
				this.clients[this.clientCount].open();
				this.clients[this.clientCount].start();
				this.clientCount++;
			}
			catch (IOException e) {
				System.out.println("cannot open client: " + e.getMessage());
			}
		}
		else
			System.out.println("client refused: maximum (" + this.clients.length + ") clients reached.");
	}
	
	public static void main(String[] args) {
		ChatServer1 server = null;
		if (args.length != 1) new ChatServer1(4444);
		else server = new ChatServer1(Integer.parseInt(args[0]));
	}
}
