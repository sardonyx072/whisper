package com.mjjhoffmann.whisper;

public abstract class Communicator implements Runnable {
	
	private CommunicatorId id;
	
	public Communicator(String name) {
		this.id = new CommunicatorId(name);
	}
}
