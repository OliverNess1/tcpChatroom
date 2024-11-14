package chatroom;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    static String hostname = "net01.utdallas.edu";//I just hardcoded the host instead of taking it as input
    static int serverPort = 12321;//must match server port

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(hostname, serverPort);//make new socket
            System.out.println("Connected to chat");//print so we know it's running
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);//sent to server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//read from server
            new Thread(() -> {//use thread so that we can send and receive at the same time
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        String Arr[] = fromServer.split(" ",2); //split the incoming message into 2 parts
                        String first = Arr[0];//this is the message type, MESG, PMSG, etc...
                        String rest = Arr[1];
                        if(first.equals("ACK")) {//do different things depending on what the first part is
                        	String restArr[] = rest.split(" ", 2);
                        	String count = restArr[0];// for ack the second part is user count
                        	if(Integer.valueOf(count) > 1) {
                        		System.out.println("There are " + count + " users in chat");//if there are 
                        	}
                        	else {
                        		System.out.println("There is 1 user in chat");
                        	}
                        	if(restArr.length > 1) {
                        		String users = "Other users in chat: " +restArr[1];// the second part is the list of other users
                        		System.out.println(users);
                        	}
                        }
                        else if(first.equals("PMSG")) {
                        	String restArr[] = rest.split(" ", 2);
                        	String from = restArr[0];//for pmsg the second part is the sender
                        	String pmsg = restArr[1];//the rest is the message
                        	System.out.println("Whisper from " + from + ": " + pmsg);//let user know its a whisper
                        }
                        else if(first.equals("MESG")) {
                        	String restArr[] = rest.split(" ", 2);
                        	String from = restArr[0];//second part is sender
                        	String msg = restArr[1];//rest is message
                        	System.out.println(from + ": " + msg);
                        }
                        else if(first.equals("ERR")) {//reads error code and prints error message
                        	if(rest.equals("0")) {
                        		System.out.println("Error: Username taken");
                        	}
                        	if(rest.equals("1")) {
                        		System.out.println("Error: Username too long, should be between 0 and 32 characters");
                        	}
                        	if(rest.equals("2")) {
                        		System.out.println("Error: Username contains spaces");
                        	}
                        	if(rest.equals("3")) {
                        		System.out.println("Error: User not found for private message");
                        	}
                        	if(rest.equals("4")) {
                        		System.out.println("Error: Bad format");
                        	}
                        }
                    }
                } catch (IOException e) { 
                }
            }).start();

           
            Scanner scnr = new Scanner(System.in);
            String In;
            while (true) {//capture user input and send it to the server forever
                In = scnr.nextLine();
                out.println(In);
            }  
        } catch (IOException e) {
        	System.out.println("Unable to connect to host");
        	return;
        }
    }
}