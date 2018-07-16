import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Integer;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;

public class StorageServer extends Thread {

	private int port;
	private int managementPort;
	private InetAddress myAddress;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;
	private MulticastServer multicast;

	public StorageServer(int port, int managementPort, InetAddress myAddress){

		try {
			this.port = port;
			this.managementPort = managementPort;
			this.myAddress = myAddress;
			this.socket = new ServerSocket(this.port, 50, this.myAddress);
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
	private DataOutputStream dos;
	private MulticastServer multicast;

	public StorageServerWorker(Socket clientSocket, MulticastServer multicast) {
		try {
			this.clientSocket = clientSocket;
			this.out = new ObjectOutputStream(this.clientSocket.
											  getOutputStream());
			this.in = new ObjectInputStream(this.clientSocket.
											getInputStream());
			this.din = new DataInputStream(this.clientSocket.getInputStream());
			this.dos = new DataOutputStream(this.clientSocket.getOutputStream());
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
				this.out.writeObject(reply);
				this.out.flush();

				// Se recibe el archivo.
				FileOutputStream fos = new FileOutputStream(newFileName);
				byte[] buffer = new byte[8192];
				int count;
				while((count = this.din.read(buffer)) > 0) {
					fos.write(buffer, 0, count);
				}
				fos.close();
				System.out.println("Archivo creado exitosamente!");
				System.out.println();
				
				// Se notifica al resto de los servidores sobre la nueva versión 
				// almacenada.
				Message notif = request; 
				notif.setMessage("actualization");
				notif.setSender(myAddress);
				System.out.println("Enviando " + notif.getMessage() + ":");
				System.out.println("  " + notif.getFileName());
				System.out.println("  " + notif.getFileSize());
				System.out.println("  " + notif.getVersion());
				System.out.println();
				multicast.sendMessage(notif);
			}
			else if (request.getMessage().equals("checkout")) {
				System.out.println("  " + request.getFileName());
				System.out.println();

				String[] parts = request.getFileName().split("(?=\\.)");
				String newFileName = parts[0] + "%" + request.getVersion()
									 + parts[1];

				Message reply = new Message("ACK");
				reply.setFileName(request.getFileName());
				reply.setFileSize(request.getFileSize());

				System.out.println("Enviando " + reply.getMessage() + ".");
				System.out.println();
				this.out.writeObject(reply);
				this.out.flush();

				FileInputStream fis = new FileInputStream(newFileName);
				byte[] buffer = new byte[8192];
				int count;
				while ((count = fis.read(buffer)) > 0) {
					this.dos.write(buffer, 0, count);
				}
				fis.close();
				System.out.println("Archivo enviado exitosamente!");
				System.out.println();
			}
			else {
				System.out.println("Mensaje erroneo.");
				System.out.println();
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
		try {
			int port = Integer.parseInt(args[1]);
			int managementPort = Integer.parseInt(args[2]);
			InetAddress address = InetAddress.getByName(args[3]);
			StorageServer server = new StorageServer(port, managementPort, address);
			System.out.println("Iniciando servidor.");
			server.start();
			System.out.println("Servidor iniciado.");
			System.out.println();
		}
		catch (UnknownHostException uhe) {
			System.out.println();
			System.out.println("UnknownHostException");
			System.out.println(uhe);
		}
	}
}