package com.mjjhoffmann.whisper;
import java.util.UUID;

public class CommunicatorId {
	private String name;
	private UUID id;
	
	public CommunicatorId(String name) {
		this.name = name;
		this.id = UUID.randomUUID();
	}
	
	public String getName() {return this.name;}
	public void setName(String name) {this.name = name;}
	public UUID getId() {return this.id;}
}
