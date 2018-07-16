import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class Client {

	private InetAddress serverAddress;
	private int serverPort;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public Client(int port) {
		try {
			serverAddress = InetAddress.getLocalHost();
			serverPort = port;
			socket = new Socket(serverAddress, port);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		}
		catch (UnknownHostException uhe) {
			System.out.println();
			System.out.println("UnknownHostException");
			System.out.println(uhe);
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println("Cannot create the input/output stream.");
			ioe.printStackTrace();
		}
	}
	
	public void commit(String pathName) {
		try {
			File file = new File(pathName);
			Message request = new Message("commit");
			request.setFileName(file.getName()); 
			request.setFileSize(file.length());
			request.setRequester();
			
			// Se envia el mensaje de |.
			System.out.println("Enviando " + request.getMessage() + ":");
			System.out.println("  " + request.getFileName());
			System.out.println("  " + request.getFileSize());
			System.out.println();
			out.writeObject(request);
			out.flush();
			
			// Se espera por el mensaje de respuesta.
			Message reply = (Message)in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ":");
			System.out.println("  " + reply.getVersion());
			System.out.println("  " + reply.getIPs());
			System.out.println();
			if (reply.getMessage().equals("ACK")) {
				request.setVersion(reply.getVersion());
				ArrayList<InetAddress> storageServers = reply.getIPs();
				for (int i = 0; i < storageServers.size(); i++) {
					new FileSender(storageServers.get(i), 8889, pathName,
					 			   request).start();	
				}
			}
			else {
				System.out.println("No hay ACK.");
			}
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			System.out.println("Error sending message.");	
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
	}

	public void checkout(String file) {
		try {
			Message request = new Message("checkout");
			request.setFileName(file);
			request.setRequester();
			// Se envia el mensaje de checkout.
			System.out.println("Enviando " + request.getMessage() + ":");
			System.out.println("  " + request.getFileName());
			System.out.println();
			out.writeObject(request);
			out.flush();

			// Se espera por el mensaje de respuesta
			Message reply = (Message)in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ":");
			System.out.println("  " + reply.getVersion());
			System.out.println("  " + reply.getIPs());
			System.out.println();

			if (reply.getMessage().equals("ACK")) {
				for (InetAddress ip : reply.getIPs()) {
					try {
						reply.setMessage("checkout");
						new FileReceiver(ip, 8889, reply.getFileName(), reply);
						break;
					}
					catch (Exception e) {}
				}
			}
			else {
				System.out.println("No hay ACK.");
			}
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			System.out.println("Error sending message.");	
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
	}
}

class FileSender extends Thread {
	private Socket socket;
	private String filePath;
	private Message request;
	private ObjectOutputStream out;
	private DataOutputStream dos;
	private ObjectInputStream in;

	public FileSender(InetAddress serverIP, int port, String filePath, 
					  Message message) {
		try {
			this.socket = new Socket(serverIP, port);
			this.filePath = filePath;
			this.request = message;
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			this.dos = new DataOutputStream(this.socket.getOutputStream());
			this.in = new ObjectInputStream(this.socket.getInputStream());
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			// Se envia el mensaje de commit.
			System.out.println("Desde hilo...");
			System.out.println("Enviando " + this.request.getMessage() + ":");
			System.out.println("  " + this.request.getFileName());
			System.out.println("  " + this.request.getFileSize());
			System.out.println("  " + this.request.getVersion());
			System.out.println();
			this.out.writeObject(this.request);
			this.out.flush();
			
			// Se espera por el mensaje de respuesta.
			Message reply = (Message)this.in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ".");
			System.out.println();

			// En caso de ACK, se inicia la transferencia del archivo.
			if (reply.getMessage().equals("ACK")) {
				FileInputStream fis = new FileInputStream(this.filePath);
				byte[] buffer = new byte[8192];
				int count;
				while ((count = fis.read(buffer)) > 0) {
					this.dos.write(buffer, 0, count);
				}
				fis.close();
			}
			else {
				System.out.println("No hay ACK.");
			}
		}
		catch (FileNotFoundException fnfe) {
			System.out.println();
			System.out.println("FileNotFoundException");
			System.out.println(fnfe);
			System.out.println("File to be sent not found.");	
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			System.out.println("Error sending message.");	
		}
	}
}

class FileReceiver extends Thread {
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private DataInputStream din;
	private String file;
	private Message request;
	private InetAddress serverIP;
	private int serverPort;

	public FileReceiver(InetAddress serverIP, int port, String file, 
					  Message request) {
		try {
			this.socket = new Socket(serverIP, port);
			this.in = new ObjectInputStream(this.socket.getInputStream());
			this.out = new ObjectOutputStream(this.socket.getOutputStream());
			this.din = new DataInputStream(this.socket.getInputStream());
			this.request = request;
			this.file = file;
			this.serverIP = serverIP;
			this.serverPort = port;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			this.socket.connect(new InetSocketAddress(this.serverIP, this.serverPort), 1000);
			this.out.writeObject(this.request);
			this.out.flush();

			// Se espera por el mensaje de respuesta.
			Message reply = (Message)this.in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ".");
			System.out.println();

			FileOutputStream fos = new FileOutputStream(reply.getFileName());
			byte[] buffer = new byte[8192];
			int count;
			while((count = in.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
			fos.close();
			System.out.println("File received successfully!");
		}
		catch (SocketTimeoutException ste) {
			ste.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ClientTest {
	public static void main(String[] args) {

		//String file = "/home/eliot/Documents/etiquetas.pdf";
		String file = "/home/eliot/Documents/FormularioProyectodeGrado.odt";
		Client client = new Client(8888);
		client.commit(file);
		client.checkout(file);
		/*
		try {
			Client client = new Client(8888);
			client.commit(file);
		
			// el master responde con la lista de ips

			//FileSender fs = new FileSender(InetAddress.getByName("127.0.0.1"), 2307, file, new Timestamp(System.currentTimeMillis()));
			//fs.start();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
		}
		*/
	}
}