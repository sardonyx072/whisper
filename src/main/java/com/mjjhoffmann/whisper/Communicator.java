package com.mjjhoffmann.whisper;

public abstract class Communicator {
	
	private CommunicatorId id;
	
	public Communicator(String name) {
		this.id = new CommunicatorId(name);
	}
}
