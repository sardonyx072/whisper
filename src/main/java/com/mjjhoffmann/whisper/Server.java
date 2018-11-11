package com.mjjhoffmann.whisper;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server extends Communicator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	private HashSet<CommunicatorId> clients;
	
	public Server (String name) {
		super(name);
		this.clients = new HashSet<CommunicatorId>();
	}
	
	public HashSet<CommunicatorId> getClients() {return this.clients;}
}
