import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;

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
			
			// Se envia el mensaje de commit.
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
			out.writeObject(this.request);
			out.flush();
			
			// Se espera por el mensaje de respuesta.
			Message reply = (Message)in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ".");
			System.out.println();

			// En caso de ACK, se inicia la transferencia del archivo.
			if (reply.getMessage().equals("ACK")) {
				FileInputStream fis = new FileInputStream(this.filePath);
				byte[] buffer = new byte[4096];
				int count;
				while ((count = fis.read(buffer)) > 0) {
					dos.write(buffer, 0, count);
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

class ClientTest {
	public static void main(String[] args) {

		//String file = "/home/eliot/Documents/etiquetas.pdf";
		String file = "/home/eliot/Documents/FormularioProyectodeGrado.odt";
		Client client = new Client(8888);
		client.commit(file);
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