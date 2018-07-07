import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.InetAddress;
import java.sql.Timestamp;

class ServerWorker extends Thread {
	
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public ServerWorker(Socket clientSocket) {
		try {
			this.clientSocket = clientSocket;
			out = new ObjectOutputStream(this.clientSocket.getOutputStream());
			in = new ObjectInputStream(this.clientSocket.getInputStream());
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println("Cannot create the input/output stream.");
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			System.out.println("Hello from a thread!");
			Message command = (Message)in.readObject();
			if (command.getMessage().equals("commit")) {
				System.out.println("Vamoa hacer un commit.");
				System.out.println(command.getMessage());
				System.out.println(command.getFilename());
				System.out.println(command.getFilesize());
			}
			else if (command.equals("checkout")) {
				System.out.println("Vamoa hacer un checkout.");
			}
			else {
				System.out.println("Mensaje erroneo.");
			}
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			ioe.printStackTrace();
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
    }
}

public class MultiThreadedServer extends Thread {

	private int port;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;

	public MultiThreadedServer(int port){
		try {
			this.port = port;
			this.socket = new ServerSocket(port);
			this.isRunning = true;
			this.storedFiles = new HashMap<String, ArrayList<FileVersion>>();
		} 
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			System.out.println("Cannot connect to the port.");
		}
	}

	public void run(){
		while(isRunning) {
			Socket clientSocket;
			try {
				clientSocket = socket.accept();
				(new ServerWorker(clientSocket)).start();
			} 
			catch (IOException ioe) {
				System.out.println();
				System.out.println("IOException");
				System.out.println(ioe);
				isRunning = false;
			}
		}
		try {
			socket.close();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
		}
		System.out.println("Server stopped.") ;
	}
}

class ServerTest {
	public static void main(String[] args) {
		MultiThreadedServer server = new MultiThreadedServer(8888);
		System.out.println("Inicializing server...");
		server.start();
		System.out.println("Server initialized");
	}
}

class FileVersion {

	private Timestamp timestamp;
	private ArrayList<InetAddress> replicas;
	private long filesize;
	private InetAddress client;

	public FileVersion(long filesize, InetAddress client) {
		this.timestamp = new Timestamp(System.currentTimeMillis());
		this.replicas = new ArrayList<InetAddress>();
		this.filesize = filesize;
		this.client = client;
	}

	public ArrayList<InetAddress> getReplicas() { return this.replicas; }

	public void setTimestamp() { this.timestamp = new Timestamp(System.currentTimeMillis()); }

	public void setFilesize(long filesize) { this.filesize = filesize; }

	public void addIP(InetAddress ip) {
		if (!this.replicas.contains(ip)) {
			this.replicas.add(ip);
		}
	}
	
	public boolean searchIP(InetAddress ip) { 
		if (this.replicas.contains(ip)) {
			return true;
		}
		else {
			return false;
		}
	}

/*
	public int removeIP(InetAddress ip) {
		if (this.replicas.contains(ip)) {
			this.replicas.remove(ip);
			return 0;
		}
		else {
			return 1;
		}
	}
*/

}

/*
class ServerInformation {
	
	private ArrayList<FileInformation> filesInfo;
	private long totalBytes;

	public ServerInformation() {
		this.totalBytes = 0;
		this.filesInfo = new ArrayList<FileInformation>();
	}

	public long getTotalBytes() { return this.totalBytes; }

	public ArrayList<FileInformation> getFilesInfo() { return this.filesInfo; }

	public void setTotalBytes(long bytes) { this.totalBytes = this.totalBytes + bytes; }

	public void addFileInfo(FileInformation newFileInfo) { this.filesInfo.add(newFileInfo); }
}

class FileInformation {

	private String filename;
	private Timestamp version;

	public FileInformation(String filename, Timestamp version) {
		this.filename = filename;
		this.version = version;
	}

	public String getFile() { return this.filename; }

	public void setVersion(Timestamp version) { this.version = version; }
}
*/