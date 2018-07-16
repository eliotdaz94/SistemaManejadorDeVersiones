import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.Map;
import java.util.HashMap;
import java.net.ServerSocket;

public class MasterManagement{

	public static master(Map<InetAddress, Long> serverBytes,
						ServerSocket managementSocket, boolean isRunning)
		return new MasterChecker(serverBytes, managementSocket, isRunning);

	public static slave(Map<InetAddress, Long> serverBytes,
						ServerSocket managementSocket, boolean isRunning)
		return new MasterChecker(serverBytes, managementSocket, isRunning);
}


class MasterChecker extends Thread{

	Map<InetAddress, Long> serverBytes;
	ServerSocket managementSocket;
	boolean isRunning;

	public MasterChecker(Map<InetAddress, Long> serverBytes,
						,ServerSocket managementSocket, boolean isRunning){
		this.serverBytes = serverBytes;
		this.managementSocket = managementSocket;
		this.isRunning = isRunning;
	}


}

class StorageChecker extends Thread{
	Map<InetAddress, Long> serverBytes;
	ServerSocket managementSocket;
	InetAddress masterAddress;
	int managementPort
	int timeout = 10;

	public StorageChecker(Map<InetAddress, Long> serverBytes,
						,ServerSocket managementSocket, boolean isRunning){
		this.serverBytes = serverBytes;
		this.managementSocket = managementSocket;
	}

	public void run() {
		ObjectOutputStream out;
		ObjectInputStream in;
		Message msg,reply;


		while (isRunning) {
			Socket clientSocket;
			try {
				clientSocket = managementSocket.accept();
				clientSocket.setSoTimeout(timeout*1000)
				try {
					out = new ObjectOutputStream(this.clientSocket.
													  getOutputStream());
					in = new ObjectInputStream(this.clientSocket.
													getInputStream());
				}
				msg = (Message)in.readObject();
				if (msg.getMessage().equals("PING"))
					if (masterAddress==null)
						masterAddress = msg.getSender();
					else if (masterAddress==msg.getSender())
						continue;

				if (msg.getMessage().equals("ELECT")){
					int biggers = 0;
					reply = new Message("ACK");
					out.writeObject(reply);
					out.flush();
					serverBytes.forEach(
						(key,value) ->
							if (value > serverBytes.get(msg.getReceiver)){
								try{
									reply = new Message("ELECT");
									reply.setReceiver(key);
									reply.setSender(msg.getReceiver());
									Socket electSock = new Socket(key,managementPort)
									ObjectOutputStream electOut = new ObjectOutputStream(electSock.getOutputStream());
									ObjectInputStream electIn = new ObjectInputStream(electSock.getOutputStream());
									electOut.writeObject(reply);
									electOut.flush();
									if (((Message)electIn.readObject()).getMessage().equals("ACK"))
										biggers++:
								}
								catch{}
							}
					);
					if (!biggers){
						masterAddress = msg.getReceiver;
						serverBytes.forEach(
							(key,value) ->
								if (value <= serverBytes.get(msg.getReceiver)){
									try{
										reply = new Message("COORD");
										reply.setReceiver(key);
										reply.setSender(msg.getReceiver());
										Socket electSock = new Socket(key,managementPort)
										ObjectOutputStream electOut = new ObjectOutputStream(electSock.getOutputStream());
										ObjectInputStream electIn = new ObjectInputStream(electSock.getOutputStream());
										electOut.writeObject(reply);
										electOut.flush();
									}
									catch{}
								}
						);
					}
				}// Finish ELECT
				if (msg.getMessage().equals("COORD"))
					masterAddress = msg.getSender();

			} 
			catch (IOException ioe) {
				System.out.println();
				System.out.println("IOException");
				System.out.println(ioe);
				continue;
			}
		}
		try {
			managementSocket.close();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
		}
		System.out.println("Server stopped.");
	}


}