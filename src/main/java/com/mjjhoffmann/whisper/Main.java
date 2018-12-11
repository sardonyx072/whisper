package com.mjjhoffmann.whisper;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mjjhoffmann.examples.algorithms.*;
import com.mjjhoffmann.examples.chat.*;
import com.mjjhoffmann.examples.chat0.*;
import com.mjjhoffmann.examples.chat1.*;
import com.mjjhoffmann.examples.date.*;

public class Main {
	private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		LOGGER.info("Reading launch configuration...");
		String key = args.length > 0 ? args[0] : "";
		args = Arrays.copyOfRange(args, Math.min(args.length, 1), args.length);
		switch(key) {
		case "DH":
		case "DiffieHellmann":
			DH_AES.main(args);
			break;
		case "RSA":
			RSA_AES.main(args);
			break;
		case "DateServer":
			DateServer.main(args);
			break;
		case "DateClient":
			DateClient.main(args);
			break;
		case "ChatServer":
			ChatServer.main(args);
			break;
		case "ChatClient":
			ChatClient.main(args);
			break;
		case "Server":
			Server.main(args);
			break;
		case "Client":
			Client.main(args);
			break;
		case "ClientGUI":
			ClientGUI.main(args);
			break;
		case "Server2":
			Server2.main(args);
			break;
		case "Server3":
			Server3.main(args);
			break;
		case "Client3":
			Client3.main(args);
			break;
		case "ChatServer0":
			ChatServer0.main(args);
			break;
		case "ChatClient0":
			ChatClient0.main(args);
			break;
		case "ChatServer1":
			ChatServer1.main(args);
			break;
		case "ChatClient1":
			ChatClient1.main(args);
			break;
		default:
			Client.main(args);
			break;
		}
	}
}
