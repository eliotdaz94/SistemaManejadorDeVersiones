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
import java.util.Scanner;
import java.lang.Integer;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.net.ConnectException;

public class Client {

	private InetAddress masterAddress;
	private int masterPort;
	private int storagePort;
	private InetAddress myAddress;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public Client(InetAddress masterAddress, int masterPort, int storagePort,
				  InetAddress myAddress) {	
		this.masterAddress =  masterAddress;
		this.masterPort = masterPort;
		this.storagePort = storagePort;
		this.myAddress = myAddress;
	}
	
	public void commit(String pathName) {
		try {
			this.socket = new Socket(this.masterAddress, this.masterPort);
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());
			File file = new File(pathName);
			Message request = new Message("commit");
			request.setFileName(file.getName()); 
			request.setFileSize(file.length());
			request.setRequester(this.myAddress);
			
			// Se envia el mensaje de commit.
			System.out.println("Enviando " + request.getMessage() + ":");
			System.out.println("  " + request.getFileName());
			System.out.println("  " + request.getFileSize());
			System.out.println();
			this.out.writeObject(request);
			this.out.flush();
		
			// Se espera por el mensaje de respuesta.
			Message reply = (Message)in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ":");
			if (reply.getMessage().equals("ACK")) {
				System.out.println("  " + reply.getVersion());
				System.out.println("  " + reply.getIPs());
				System.out.println();
				request.setVersion(reply.getVersion());
				ArrayList<InetAddress> storageServers = reply.getIPs();
				for (int i = 0; i < storageServers.size(); i++) {
					new FileSender(storageServers.get(i), storagePort, pathName,
								   request).start();	
				}
			}
			else {
				System.out.println("No hay ACK.");
				System.out.println();
			}
			this.socket.close();
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
			this.socket = new Socket(this.masterAddress, this.masterPort);
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());
			Message request = new Message("checkout");
			request.setFileName(file);
			request.setRequester(this.myAddress);

			// Se envia el mensaje de checkout.
			System.out.println("Enviando " + request.getMessage() + ":");
			System.out.println("  " + request.getFileName());
			System.out.println();
			out.writeObject(request);
			out.flush();

			// Se espera por el mensaje de respuesta.le
			Message reply = (Message)in.readObject();
			if (reply.getMessage().equals("ACK")) {
				System.out.println("Recibiendo " + reply.getMessage() + ":");
				System.out.println("  " + reply.getVersion());
				System.out.println("  " + reply.getIPs());
				System.out.println();
				for (InetAddress ip : reply.getIPs()) {
					System.out.println(ip);
					try {
						reply.setMessage("checkout");
						System.out.println(reply.getMessage());
						fileReceiver(ip, this.storagePort, reply.getFileName(),
									 reply);
						break;
					}
					catch (Exception e) { 
						System.out.println(ip + " no disponible");
						continue;
					}
				}
			}
			else if (reply.getMessage().equals("reject")) {
				System.out.println("Recibiendo " + reply.getMessage() + ".");
				System.out.println("El archivo solicitado no existe.");
			}
			this.socket.close();
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

	public void fileReceiver(InetAddress serverIP, int port, String file,
							 Message request) throws SocketTimeoutException,
							 						 ClassNotFoundException, 
													 IOException {
		try {
			Socket storageSocket = new Socket(serverIP, port);
			ObjectInputStream in = new ObjectInputStream(storageSocket.
														 getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(storageSocket.
															getOutputStream());
			DataInputStream din = new DataInputStream(storageSocket.
													  getInputStream());
			out.writeObject(request);
			out.flush();

			// Se espera por el mensaje de respuesta.
			Message reply = (Message)in.readObject();
			System.out.println("Recibiendo " + reply.getMessage() + ".");
			System.out.println();

			FileOutputStream fos = new FileOutputStream(reply.getFileName());
			byte[] buffer = new byte[8192];
			int count;
			while((count = din.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
			fos.close();
			System.out.println("Archivo recibido exitosamente!");
			out.close();
			in.close();
			din.close();
			storageSocket.close();
		}
		catch (SocketTimeoutException ste) {
			System.out.println();
			System.out.println("SocketTimeoutException");
			System.out.println(ste);
			ste.printStackTrace();
			throw ste;
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
			ioe.printStackTrace();
			throw ioe;
		}
	}
}

class FileSender extends Thread {
	private Socket storageSocket;
	private String filePath;
	private Message request;
	private ObjectOutputStream out;
	private DataOutputStream dos;
	private ObjectInputStream in;

	public FileSender(InetAddress serverIP, int port, String filePath, 
					  Message message) {
		try {
			this.storageSocket = new Socket(serverIP, port);
			this.filePath = filePath;
			this.request = message;
			this.out = new ObjectOutputStream(this.storageSocket.
											  getOutputStream());
			this.dos = new DataOutputStream(this.storageSocket.
											getOutputStream());
			this.in = new ObjectInputStream(this.storageSocket.
											getInputStream());
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
			this.out.close();
			this.dos.close();
			this.in.close();
			this.storageSocket.close();
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
		try {
			InetAddress masterAddress = InetAddress.getByName(args[0]);
			int masterPort = Integer.parseInt(args[1]);
			int storagePort = Integer.parseInt(args[2]);
			InetAddress myPublicIP = InetAddress.getByName(args[3]);
			Client client = new Client(masterAddress, masterPort, storagePort,
									   myPublicIP);
			Scanner sc = new Scanner(System.in);
			String output;
			String input;
			while (true) {
				input = sc.nextLine();
				System.out.println();
				String[] parts = input.split(" ");
				if (parts[0].equals("commit")) {
					File commitFile = new File(parts[1]);
					if (commitFile.exists()) {
						client.commit(parts[1]);
					}
					else {
						System.out.println("Archivo no encontrado.");	
					}
				}
				else if (parts[0].equals("checkout")) {
					client.checkout(parts[1]);	
				}
				else {
					System.out.println("Comando no válido. Uso:");
					System.out.println("  commit pathName");
					System.out.println("  checkout pathName");	
				}
				System.out.println();
			}
		}
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		}
	}
}