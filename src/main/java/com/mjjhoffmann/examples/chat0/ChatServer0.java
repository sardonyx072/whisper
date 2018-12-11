package com.mjjhoffmann.examples.chat0;

import java.net.*;
import java.io.*;

public class ChatServer0 implements Runnable
{  
	private class ChatServerThread extends Thread
	{  private ChatServer0       server    = null;
	   private Socket           socket    = null;
	   private int              ID        = -1;
	   private DataInputStream  streamIn  =  null;
	   private DataOutputStream streamOut = null;

	   public ChatServerThread(ChatServer0 _server, Socket _socket)
	   {  super();
	      server = _server;
	      socket = _socket;
	      ID     = socket.getPort();
	   }
	   public void send(String msg)
	   {   try
	       {  streamOut.writeUTF(msg);
	          streamOut.flush();
	       }
	       catch(IOException ioe)
	       {  System.out.println(ID + " ERROR sending: " + ioe.getMessage());
	          server.remove(ID);
	          stop();
	       }
	   }
	   public int getID()
	   {  return ID;
	   }
	   public void run()
	   {  System.out.println("Server Thread " + ID + " running.");
	      while (true)
	      {  try
	         {  server.handle(ID, streamIn.readUTF());
	         }
	         catch(IOException ioe)
	         {  System.out.println(ID + " ERROR reading: " + ioe.getMessage());
	            server.remove(ID);
	            stop();
	         }
	      }
	   }
	   public void open() throws IOException
	   {  streamIn = new DataInputStream(new 
	                        BufferedInputStream(socket.getInputStream()));
	      streamOut = new DataOutputStream(new
	                        BufferedOutputStream(socket.getOutputStream()));
	   }
	   public void close() throws IOException
	   {  if (socket != null)    socket.close();
	      if (streamIn != null)  streamIn.close();
	      if (streamOut != null) streamOut.close();
	   }
	}
   private ChatServerThread clients[] = new ChatServerThread[50];
   private ServerSocket server = null;
   private Thread       thread = null;
   private int clientCount = 0;

   public ChatServer0(int port)
   {  try
      {  System.out.println("Binding to port " + port + ", please wait  ...");
         server = new ServerSocket(port);  
         System.out.println("Server started: " + server);
         start(); }
      catch(IOException ioe)
      {  System.out.println("Can not bind to port " + port + ": " + ioe.getMessage()); }
   }
   public void run()
   {  while (thread != null)
      {  try
         {  System.out.println("Waiting for a client ..."); 
            addThread(server.accept()); }
         catch(IOException ioe)
         {  System.out.println("Server accept error: " + ioe); stop(); }
      }
   }
   public void start()
   {  if (thread == null)
      {  thread = new Thread(this); 
         thread.start();
      }
   }
   public void stop()
   {  if (thread != null)
      {  thread.stop(); 
         thread = null;
      }
   }
   private int findClient(int ID)
   {  for (int i = 0; i < clientCount; i++)
         if (clients[i].getID() == ID)
            return i;
      return -1;
   }
   public synchronized void handle(int ID, String input)
   {  if (input.equals(".bye"))
      {  clients[findClient(ID)].send(".bye");
         remove(ID); }
      else
         for (int i = 0; i < clientCount; i++)
            clients[i].send(ID + ": " + input);   
   }
   public synchronized void remove(int ID)
   {  int pos = findClient(ID);
      if (pos >= 0)
      {  ChatServerThread toTerminate = clients[pos];
         System.out.println("Removing client thread " + ID + " at " + pos);
         if (pos < clientCount-1)
            for (int i = pos+1; i < clientCount; i++)
               clients[i-1] = clients[i];
         clientCount--;
         try
         {  toTerminate.close(); }
         catch(IOException ioe)
         {  System.out.println("Error closing thread: " + ioe); }
         toTerminate.stop(); }
   }
   private void addThread(Socket socket)
   {  if (clientCount < clients.length)
      {  System.out.println("Client accepted: " + socket);
         clients[clientCount] = new ChatServerThread(this, socket);
         try
         {  clients[clientCount].open(); 
            clients[clientCount].start();  
            clientCount++; }
         catch(IOException ioe)
         {  System.out.println("Error opening thread: " + ioe); } }
      else
         System.out.println("Client refused: maximum " + clients.length + " reached.");
   }
   public static void main(String args[])
   {  ChatServer0 server = null;
      if (args.length != 1)
         System.out.println("Usage: java ChatServer port");
      else
         server = new ChatServer0(Integer.parseInt(args[0]));
   }
}
