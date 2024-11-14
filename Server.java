 package chatroom;

 import java.io.*;
 import java.net.*;
 import java.util.concurrent.CopyOnWriteArrayList;

 public class Server { 
 static int port = 12321; //must match client
 static CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>(); 
 //Needed to find a thread safe array list and found this on stackoverflow at https://stackoverflow.com/questions/2444005/how-do-i-make-my-arraylist-thread-safe-another-approach-to-problem-in-java

 public static void main(String[] args) { 
     try { 
         ServerSocket server = new ServerSocket(port); //make a listening port
         System.out.println("RUNNING");
         while (true) { //accept clients as long as program is running
             Socket userSock = server.accept(); 
             Client client = new Client(userSock);//make a new client for each user 
             clients.add(client); //add the client to the list of users
             new Thread(client).start(); //use threads so server can handle everything at the same time
         } 
     } catch (Exception e) { 
     } 
 } 

 public static void post(String message, Client sender) { //used for MESG
     for (Client client : clients) { 
         if (client != sender) { //send the message to each different client that is connected, but not the sender
             client.sendMessage(message); 
         } 
     } 
 } 
 public static int whisper(String message, String reciever) { //used for PMSG
     for (Client client : clients) { 
         if (client.Uname.equals(reciever)) { //find the client who's username is specified
             client.sendMessage(message);
             return(0);//return a number so we can tell if it was sent or not
         } 
     }
    	 return(1);//couldnt find specified user
     
 }

 private static class Client implements Runnable { 
     private String Uname;
     private Socket socket; //each client gets their own instance of Client
     private BufferedReader inFromClient;
     private PrintWriter outToClient; 
     
     
     public Client(Socket socket) { 
         this.socket = socket; //set the ClientHandler.clientSocket to the users socket

         try { 
             outToClient = new PrintWriter(socket.getOutputStream(), true); //make a writer and reader for each client
             inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
         } catch (Exception e) { 
         } 
     } 

     @Override
     public void run() { 
         try {  
             Uname = getUsername(); //call function to get username
             post("MESG SERVER " + Uname + " has joined chat", this);//message everyone else once you join and have a username

             String inputLine; 
             boolean didexit = false;
             while (!didexit) { //keep in the loop until user exits
            	 inputLine = inFromClient.readLine();
                 System.out.println(Uname + " " + inputLine); // print the chat on servers console so we can see it work
                 String inArr[] = inputLine.split(" ", 2);//split input into 2 parts
                 String first = inArr[0];//this part tells us what the client wants to do, MESG, PMSG, etc..
                 
                 if(inArr.length > 1) {
                	 String rest = inArr[1];//catch cases where user just types MESG and nothing else
                 
                	 if(first.equals("MESG")) {
                		 post("MESG " + Uname + " " + rest, this); // format and post message
                		 }
       
                	 else if(first.equals("PMSG")) {
                		 String pmsg[] = rest.split(" ", 2);
                		 String whisperTo = pmsg[0];//in pmsg the second part is receiver
                		 if(pmsg.length > 1) {//make sure there is a message
                			String whispermsg = pmsg[1];
                		 	whispermsg = "PMSG " +  Uname + " " + whispermsg;//formatting
                		 	int comp = whisper(whispermsg, whisperTo);
                		 	if(comp == 1 ) {
                			 	outToClient.println("ERR 3");//if the whisper function returns 1 then couldnt find receiver
                		 	}
                		 }
                		 else {
                			 outToClient.println("ERR 4");//if no message then bad format
                		 }
                	 
                	 }
                	 else if(first.equals("EXIT")) {
                		 if(rest.equals(Uname)) {//only exit if username matches
                			 didexit = true;//set this so we exit the loop
                			 String ack = "ACK " + (clients.size()-1);//ack with list of remaining users
                			 for(Client client : clients) {
                				 if(client.Uname != Uname) {
                					 ack = ack + " " + client.Uname;
                				 }
                			 }
                			 outToClient.println(ack);
                			 post("MESG SERVER " + Uname + " has left the chat", this);//send this before user leaves
                	 }
                		 else {
                			 outToClient.println("ERR 4");//if usernames dont match send bad format error
                		 }
                	 }
                	 else {
                		 outToClient.println("ERR 4");//if first item wasnt in that list then format is bad
                	 }
                }
                 else {
                	 outToClient.println("ERR 4");//if there is no second part of the message then bad format
                 }
                 
             } 
             
             clients.remove(this); //remove user from list of clients
             inFromClient.close(); //close up resources
             outToClient.close(); 
             socket.close(); 
         } catch (Exception e) {
        	 post("MESG SERVER " + Uname + " has left the chat", this);
        	 clients.remove(this); //remove user from list of clients
             
         } 
     } 
     public void sendMessage(String message) { //simple function to send message to client
         outToClient.println(message); 
     }
     private String getUsername() throws IOException { //called when user joins the server
         while(true) {
        	 outToClient.println("MESG SERVER Register your username:");
        	 String inLine;
        	 inLine = inFromClient.readLine();
        	 String Arr[] = inLine.split(" ");//split the input on spaces
        	 if(Arr.length > 2) {
        		 outToClient.println("ERR 2");//no spaces allowed in username
        	 }
        	 else if(Arr.length < 2) {
        		 outToClient.println("ERR 4");//there must be two arguments REG and USERNAME
        	 }
        	 else {
        		 String first = Arr[0];
        		 String rest = Arr[1];
        		 if(first.equals("REG")) {//first word must be REG to register username
        			 if(rest.length()<= 32 && rest.length() > 0) {//make sure length is correct
        				 boolean uNameTaken = false;
        				 if(clients.size() != 1) {//only check against other usernames if there are other people in chat
        					 for(Client client : clients) {//make sure username isnt already taken
        						 if(client.Uname != null) {
        							 if ( client.Uname.equals(rest)){
        								 uNameTaken = true;
        							 }	 
        						 } 
        					 }
        				 }
        				 if(uNameTaken == false) {//if username wasnt taken accept and ack with users currently in chat
        					 String ack = "ACK " + clients.size();
        					 for(Client client : clients) {
        						 if(client.Uname != null) {
        							 ack = ack + " " + client.Uname;
        						 }
        					 }
        					 outToClient.println(ack);
        					 return rest;//return the username so we can assign it to the client
        				 }
        				 else {
        					 outToClient.println("ERR 0");//error 0 if username is taken
        				 }
        			 }
        			 else {
        				 outToClient.println("ERR 1");//error 1 if username is too long
        			 }
        		 }
        		 else {
        			 outToClient.println("ERR 4");//if first part isnt REG the format is bad
        		 }
        	}
         }
     } 

      
 }
}