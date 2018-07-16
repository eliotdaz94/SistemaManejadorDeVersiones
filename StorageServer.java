import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;

public class StorageServer extends Thread {

	private int port;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;
	private MulticastServer multicast;

	public StorageServer(int port){
		try {
			this.port = port;
			this.socket = new ServerSocket(port);
			this.isRunning = true;
			this.storedFiles = new HashMap<String, ArrayList<FileVersion>>();
			this.serverBytes = new HashMap<InetAddress, Long>();
			this.serverBytes.put(InetAddress.getLocalHost(), new Long(0));
			this.multicast = new MulticastServer(storedFiles, serverBytes);
			this.multicast.start();
			this.multicast.register();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			System.out.println("Cannot connect to the port.");
		}
	}

	public void run() {
		while (isRunning) {
			Socket clientSocket;
			try {
				clientSocket = this.socket.accept();
				(new StorageServerWorker(clientSocket, multicast)).start();
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
		System.out.println("Server stopped.");
	}

}

class StorageServerWorker extends Thread {
	
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private DataInputStream din;
	private MulticastServer multicast;

	public StorageServerWorker(Socket clientSocket, MulticastServer multicast) {
		try {
			this.clientSocket = clientSocket;
			this.out = new ObjectOutputStream(this.clientSocket.
											  getOutputStream());
			this.in = new ObjectInputStream(this.clientSocket.
											getInputStream());
			this.din = new DataInputStream(this.clientSocket.getInputStream());
			this.multicast = multicast;
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
			System.out.println("Desde un hilo de almacenamiento...");
			Message request = (Message)in.readObject();
			System.out.println("Recibiendo " + request.getMessage() + " de " 
							   + request.getRequester() + ":");
			if (request.getMessage().equals("commit")) {
				System.out.println("  " + request.getFileName());
				System.out.println("  " + request.getFileSize());
				System.out.println("  " + request.getVersion());
				System.out.println();

				// Se crea el nombre del archivo que se recibirá.
				String[] parts = request.getFileName().split("(?=\\.)");
				String newFileName = parts[0] + "%" + request.getVersion()
									 + parts[1];
				
				// Se retorna ACK para comenzar la transferencia del archivo.
				Message reply = new Message("ACK");
				System.out.println("Enviando " + reply.getMessage() + ".");
				System.out.println();
				out.writeObject(reply);
				out.flush();

				// Se recibe el archivo.
				FileOutputStream fos = new FileOutputStream(newFileName);
				byte[] buffer = new byte[8192];
				int count;
				while((count = din.read(buffer)) > 0) {
					fos.write(buffer, 0, count);
				}
				fos.close();
				System.out.println("File created successfully!");
				
				// Se notifica al resto de los servidores sobre la nueva versión 
				// almacenada.
				Message notif = request; 
				notif.setMessage("actualization");
				System.out.println("Enviando " + notif.getMessage() + ":");
				System.out.println("  " + notif.getFileName());
				System.out.println("  " + notif.getFileSize());
				System.out.println("  " + notif.getVersion());
				System.out.println();
				multicast.sendMessage(notif);
			}
			else if (request.getMessage().equals("checkout")) {
				System.out.println("Vamoa hacer un checkout.");
			}
			else {
				System.out.println("Mensaje erroneo.");
			}
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

class StorageServerTest {
	public static void main(String[] args) {
		StorageServer server = new StorageServer(8889);
		System.out.println("Iniciando servidor.");
		server.start();
		System.out.println("Servidor iniciado.");
		System.out.println();
	}
}