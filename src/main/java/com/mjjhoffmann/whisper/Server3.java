package com.mjjhoffmann.whisper;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server3 {
	private class ClientHandler extends Thread {
		private Server3 server;
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		public ClientHandler(Server3 chatServer, Socket socket) {
			super();
			this.server = chatServer;
			this.socket = socket;
			try {
				this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			}
			catch (IOException e) {System.out.println("cannot open streams! " + e.getMessage());}
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
					System.out.println("error reading " + this.socket.getPort() + ": " + e.getMessage());
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
				System.out.println("error sending message to " + this.socket.getPort() + ": " + e.getMessage());
				this.server.remove(this.socket.getPort());
				this.stop();
			}
		}
		public String toString() {return this.socket.toString();}
	}
	
	private ServerSocket server;
	private List<ClientHandler> clients;
	public Server3 (int port) {
		this.clients = new ArrayList<ClientHandler>();
	}
	public void run() {
		while (true) {
			try {
				System.out.println("waiting for client connection");
				this.handleConnection(server.accept());
			}
			catch (IOException e) {System.out.println("server accept error: " + e.getMessage());}
		}
	}
	public void handleConnection(Socket socket) {
		System.out.println("client accepted: " + socket);
		ClientHandler toAdd = new ClientHandler(this, socket);
		this.clients.add(toAdd);
		toAdd.start();
	}
	public synchronized void remove(int id) {
		this.clients.stream().filter(c -> c.getPort() == id).forEach(c -> {
			System.out.println("client disconnected: " + c);
			try {c.close();}
			catch (IOException e) {System.out.println("error closing thread " + id + ": " + e.getMessage());}
			c.stop();
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
		if (args.length != 1) new Server3(4444);
		else new Server3(Integer.parseInt(args[0]));
	}
}
