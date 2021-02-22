import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.PrintWriter;
import org.json.*;

/**
 * This is the main class for the peer2peer program.
 * It starts a client with a username and host:port for the peer and host:port of the initial leader
 * This Peer is basically the client of the application, while the server (the one listening and waiting for requests)
 * is in a separate thread ServerThread
 * In here you should handle the user input and then send it to the server of annother peer or anything that needs to be done on the client side
 * YOU CAN MAKE ANY CHANGES YOU LIKE: this is a very basic implementation you can use to get started
 * 
 */

public class Peer {
	private String username;
	private BufferedReader bufferedReader;
	private ServerThread serverThread;
	//task 4 list of potential jokes and final jokes.  limit one joke in potentialJokes
	public static ArrayList<String> potentialJokes = new ArrayList<String>();
	public static ArrayList<String> finalJokes = new ArrayList<String>();

	private Set<SocketInfo> peers = new HashSet<SocketInfo>();
	private boolean leader = false;
	private SocketInfo leaderSocket;

	public int voteTally = 0;
	public boolean winner = false;

	
	public Peer(BufferedReader bufReader, String username,ServerThread serverThread){
		this.username = username;
		this.bufferedReader = bufReader;
		this.serverThread = serverThread;
	}

	public void setLeader(boolean leader, SocketInfo leaderSocket){
		this.leader = leader;
		this.leaderSocket = leaderSocket;
	}

	// task 2.4 determine the winner of the consensus
	public boolean checkResult(){
		if(getTally() >= (getNumPeers() / 2))
		{
			return true;
		} else {
			return false;
		}
	}
	
		// some helper methods
	// reset tally after election/joke consensus
	public void resetTally()
	{
		voteTally = 0;
	}

	public void addTally()
	{
		++voteTally;
	}

	public void subtractTally()
	{
		--voteTally;
	}

	public int getTally(){
		return voteTally;
	}

	// Task 2.4 get number of peers for consensus
	public int getNumPeers(){
		return peers.size();
	}

	// some helper methods
	// reset winner after election/joke consensus
	public void resetWinner()
	{
		winner = false;
	}

	// task 4 add jokes to each peer
	public void addJoke(String joke)
	{
		// try 
		potentialJokes.add(joke);
	}

	// check if current node is the leader
	public boolean isLeader(){
		return leader;
	}

	// add vote to final list if winner
	public void addFinalJoke(){
		// try
		finalJokes.add(potentialJokes.get(0));
	}

	// clear list of potential jokes after vote
	public void clearPotentialJokes(){
		// try
		potentialJokes.remove(0);
	}



	public void addPeer(SocketInfo si){
		if (peers.contains(si))
		{
			// duplicate peer info will not be added
			System.out.println("Peer already added");
		}
		else 
		{
			peers.add(si);
		}
	}
	
	// get a string of all peers that this peer knows
	public String getPeers(){
		String s = "";
		for (SocketInfo p: peers){
			s = s +  p.getHost() + ":" + p.getPort() + " ";
		}
		return s; 
	}

	/**
	 * Adds all the peers in the list to the peers list
	 * Only adds it if it is not the currect peer (self)
	 *
	 * @param list String of peers in the format "host1:port1 host2:port2"
	 */
	public void updateListenToPeers(String list) throws Exception {
		String[] peerList = list.split(" ");
		for (String p: peerList){
			String[] hostPort = p.split(":");

			// basic check to not add ourself, since then we would send every message to ourself as well (but maybe you want that, then you can remove this)
			if ((hostPort[0].equals("localhost") || hostPort[0].equals(serverThread.getHost())) && Integer.valueOf(hostPort[1]) == serverThread.getPort()){
				continue;
			}
			SocketInfo s = new SocketInfo(hostPort[0], Integer.valueOf(hostPort[1]));
			peers.add(s);
		}
	}
	
	/**
	 * Client waits for user to input can either exit or send a message
	 */
	public void askForInput() throws Exception {
		try {
			
			System.out.println("> You can now start chatting (exit to exit, joke to send a joke, vote to vote on first joke in list, send to send joke to all if leader)");
			while(true) {
				String message = bufferedReader.readLine();


				// start task 2.4 enter joke if there is a the list is empty
				if (message.equals("joke") && potentialJokes.isEmpty()){
					System.out.println("Please enter your joke");
					String joke = bufferedReader.readLine();
					//addJoke(joke);
					// send to leader node
					commLeader("{'type': 'joke', 'username': '"+ username +"','message':'" + joke + "'}");
				} else if (message.equals("joke") && potentialJokes.isEmpty() == false) {
					System.out.println("There is already a joke in the list.  Please vote on it.");
				}
				
					// task 2.4 send joke to all.  Only send node is the leader
				if (message.equals("send") && isLeader()){
					try {
						System.out.println("Sending joke to all");
						System.out.println(potentialJokes);
						String joke = potentialJokes.get(0);
						pushMessage("{'type': 'message', 'username': '"+ username +"','message':'" + joke + "'}");
					} catch (Exception e) {
						System.out.println("Cannot send joke due to: " + e);
					}
				}

				// task 2.4 send vote to leader if there is a joke in the list
				if (message.equals("vote") && potentialJokes.isEmpty() ==  false){
					try {
						System.out.println("enter ok to agree, no to disagree");
						String answer = bufferedReader.readLine();
						if (answer.contains("ok") || answer.contains("no"))
						{
							commLeader("{'type': 'vote', 'username': '"+ username +"','message':'" + answer + "'}");
						} else {
							System.out.println("Invalid response");
						}
					} catch (Exception e) {
						System.out.println("Invalid response: " + e);
					}
				}

				if (message.equals("exit")) {
					//task 2.2
					pushMessage("{'type': 'message', 'username': '"+ username +"','message':'" + " has left the chat" + "'}");
					System.out.println("bye, see you next time");
					break;
				} else {
					pushMessage("{'type': 'message', 'username': '"+ username +"','message':'" + message + "'}");
				}	
			}
			System.exit(0);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

// ####### You can consider moving the two methods below into a separate class to handle communication
	// if you like (they would need to be adapted some of course)


	/**
	 * Send a message only to the leader 
	 *
	 * @param message String that peer wants to send to the leader node
	 * this might be an interesting point to check if one cannot connect that a leader election is needed
	 */
	public void commLeader(String message) {
		try {
			//System.out.println("This is from commLeader: " + message);
			BufferedReader reader = null; 
				Socket socket = null;
				try {
					socket = new Socket(leaderSocket.getHost(), leaderSocket.getPort());
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					
				} catch (Exception c) {
					if (socket != null) {
						socket.close();
					} else {
						System.out.println("Could not connect to " + leaderSocket.getHost() + ":" + leaderSocket.getPort());
					}
					return; // returning since we cannot connect or something goes wrong the rest will not work. 
				}

				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(message);

				if (message.contains("join"))
				{
				JSONObject json = new JSONObject(reader.readLine());
				System.out.println("     Received from server " + json);
				String list = json.getString("list");
				updateListenToPeers(list); // when we get a list of all other peers that the leader knows we update them
				}
				//task 2.4
				if (message.contains("joke"))
				{
					JSONObject json = new JSONObject(message);
					potentialJokes.add(json.getString("message"));
					// may not need to print

					//Task 2.4 thread safe
					synchronized(potentialJokes) 
       				 { 
           				Iterator it = potentialJokes.iterator(); 
  
            			while (it.hasNext()) 
                		System.out.println(it.next()); 
        			}
					//System.out.println(jokes);
				}

				//task 2.4
				if (message.contains("vote"))
				{
					JSONObject json = new JSONObject(message);
				}

				

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	

/**
	 * Send a message to every peer in the peers list, if a peer cannot be reached remove it from list
	 *
	 * @param message String that peer wants to send to other peers
	 */
	public void pushMessage(String message) {
		try {
			System.out.println("     Trying to send to peers: " + peers.size());

			Set<SocketInfo> toRemove = new HashSet<SocketInfo>();
			BufferedReader reader = null; 
			int counter = 0;
			for (SocketInfo s : peers) {
				Socket socket = null;
				try {
					socket = new Socket(s.getHost(), s.getPort());
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (Exception c) {
					if (socket != null) {
						socket.close();
					} else {
						System.out.println("  Could not connect to " + s.getHost() + ":" + s.getPort());
						System.out.println("  Removing that socketInfo from list");
						toRemove.add(s);
						continue;
					}
					System.out.println("     Issue: " + c);
				}

				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println(message);
				counter++;
				socket.close();
		     }
		    for (SocketInfo s: toRemove){
		    	peers.remove(s);
		    }

		    System.out.println("     Message was sent to " + counter + " peers");

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main method saying hi and also starting the Server thread where other peers can subscribe to listen
	 *
	 * @param args[0] username
	 * @param args[1] port for server
	 */
	public static void main (String[] args) throws Exception {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String username = args[0];
		System.out.println("Hello " + username + " and welcome! Your port will be " + args[1]);

		int size = args.length;
		System.out.println(size);
		if (size == 4) {
			System.out.println("Started peer");
        } else {
            System.out.println("Expected: <name(String)> <peer(String)> <leader(String)> <isLeader(bool-String)>");
            System.exit(0);
        }

        System.out.println(args[0] + " " + args[1]);
        ServerThread serverThread = new ServerThread(args[1]);
        Peer peer = new Peer(bufferedReader, username, serverThread);

        String[] hostPort = args[2].split(":");
        SocketInfo s = new SocketInfo(hostPort[0], Integer.valueOf(hostPort[1]));
        System.out.println(args[3]);
        if (args[3].equals("true")){
			System.out.println("Is leader");
			peer.setLeader(true, s);
		} else {
			System.out.println("Pawn");

			// add leader to list 
			peer.addPeer(s);
			peer.setLeader(false, s);

			// send message to leader that we want to join
			peer.commLeader("{'type': 'join', 'username': '"+ username +"','ip':'" + serverThread.getHost() + "','port':'" + serverThread.getPort() + "'}");

		}
		serverThread.setPeer(peer);
		serverThread.start();
		peer.askForInput();

	}
	
}
