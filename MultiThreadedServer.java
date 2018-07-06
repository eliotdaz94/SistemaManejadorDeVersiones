import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;

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
			String command = (String)in.readObject();
			if (command.equals("commit")) {
				System.out.println("Vamoa hacer un commit.");
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

	public MultiThreadedServer(int port){
		try {
			this.port = port;
			socket = new ServerSocket(port);
			isRunning = true;
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
		server.start();
	}
}