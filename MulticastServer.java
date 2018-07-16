import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.ClassNotFoundException;
import com.google.gson.Gson;

public class MulticastServer extends Thread {
	private InetAddress group;
	private int port;
	private MulticastSocket socket;
	private byte[] recvBuf;
	private byte[] sendBuf;
	private ByteArrayOutputStream outByteStream;
	private ByteArrayInputStream inByteStream;
	private ObjectOutputStream os;
	private ObjectInputStream is;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;
	private Gson gson;
	private String jsonFile;
	
	public MulticastServer(HashMap<String, ArrayList<FileVersion>> storedF,
						   HashMap<InetAddress, Long> serverB) {
		try {
			this.group = InetAddress.getByName("228.5.6.7");
			this.port = 6789;
			this.socket = new MulticastSocket(this.port);
			this.socket.joinGroup(this.group);
			this.recvBuf = new byte[4096];
			this.outByteStream = new ByteArrayOutputStream(4096);
			this.os = new ObjectOutputStream(this.outByteStream);
			this.storedFiles = storedF;
			this.serverBytes = serverB;
			this.jsonFile = System.getProperty("user.dir") + "/storedFiles.json";
		} 
		catch (UnknownHostException uhe) {
			System.out.println();
			System.out.println("UnknownHostException");
			System.out.println(uhe);
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			ioe.printStackTrace();
		}
	}

	public void sendMessage(Message msg) {      
		try {
			this.outByteStream = new ByteArrayOutputStream(4096);
			this.os = new ObjectOutputStream(this.outByteStream);
			this.os.writeObject(msg);
			this.os.flush();
			// Retrieves byte array.
			this.sendBuf = this.outByteStream.toByteArray();
			DatagramPacket packet = new DatagramPacket(this.sendBuf, 
													   this.sendBuf.length,
													   this.group, this.port);
			this.socket.send(packet);
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			ioe.printStackTrace();
		}
	}

	public void register() {
		Message msg = new Message("register");
		msg.setRequester();
		sendMessage(msg);
	}

	public void printStoredFiles() {
		for (String fileName : this.storedFiles.keySet()) {
			System.out.println(fileName);	
			ArrayList<FileVersion> auxFiles = this.storedFiles.get(fileName);
			for (FileVersion auxVersion : auxFiles) {
				System.out.println("  Version: " + auxVersion.getTimestamp());
				System.out.println("  Tamaño: " + auxVersion.getfileSize());
				System.out.println("  Cliente: " + 
								   auxVersion.getClient().getHostAddress());
				System.out.println("  Servidores: ");
				for (InetAddress auxAddress : auxVersion.getReplicas()) {
					System.out.println("    " + auxAddress.getHostAddress());
				}
				System.out.println();
			}	
		}
	}

	public void run() {
		try {
			while (true) {
				//this.recvBuf = new byte[4096];
				DatagramPacket packet = new DatagramPacket(this.recvBuf, 
														   this.recvBuf.length);
				this.socket.receive(packet);
				this.inByteStream = new ByteArrayInputStream(recvBuf);
				this.is = new ObjectInputStream(this.inByteStream);
				Message msg = (Message)is.readObject();
				System.out.println("Recibiendo multicast " + msg.getMessage() 
								   + ":");
				// Si se recibe un "actualization" se debe modificar el mapa de
				// versiones almacenadas.
				if (msg.getMessage().equals("actualization")) {
					System.out.println("  " + msg.getFileName());
					System.out.println("  " + msg.getFileSize());
					System.out.println("  " + msg.getVersion());
					System.out.println("  " + msg.getRequester());
					System.out.println("  " + msg.getSender());
					ArrayList<FileVersion> versionsList;
					FileVersion auxVersion;
					// Si el archivo ya se encuentra en el mapa, se verifica
					// si la version existe.
					if (storedFiles.containsKey(msg.getFileName())) {
						boolean exist = false;
						versionsList = this.storedFiles.get(msg.getFileName());
						for (int i = 0; i < versionsList.size(); i++) {
							auxVersion = versionsList.get(i);
							// En caso de que la version exista, solo se agrega
							// la IP del servidor (que notifica el almacenamiento)
							// en la lista de réplicas de la versión.
							if (auxVersion.equals(msg.getVersion(), 
												  msg.getRequester())) {
								auxVersion.addIP(msg.getSender());
								exist = true;
								break;
							}
						}
						// En caso de que la versión no exista, se crea una
						// nueva versión y se añade al mapa en el archivo
						// correspondiente.
						if (!exist) {
							auxVersion = new FileVersion(msg.getVersion(),
														 msg.getFileSize(), 
														 msg.getRequester());
							auxVersion.addIP(msg.getSender());
							versionsList.add(auxVersion);
						} 
					}
					// Si el archivo no se encuentra en el mapa, se crea la 
					// versión y se añade una entrada al mapa cuya clave será
					// el nombre del archivo almacenado.
					else {
						auxVersion = new FileVersion(msg.getVersion(),
													 msg.getFileSize(), 
													 msg.getRequester());
						auxVersion.addIP(msg.getSender());
						versionsList = new ArrayList<FileVersion>();
						versionsList.add(auxVersion);
						storedFiles.put(msg.getFileName(), versionsList);
					}
					System.out.println();
					try (Writer writer = new FileWriter(this.jsonFile)) {
    					Gson gson = new Gson();
    					gson.toJson(storedFiles, writer);
					}
					printStoredFiles();
					// También se debe modificar el mapa de total de bytes
					// almacenados por servidor.
					for (InetAddress auxAddress : this.serverBytes.keySet()) {
						if (auxAddress.equals(msg.getSender())) {
							long auxValue = serverBytes.get(auxAddress);
							this.serverBytes.replace(auxAddress, auxValue + 
												 	 msg.getFileSize());
						}
					}
					System.out.println(serverBytes);
				}
				else if (msg.getMessage().equals("register")) {
					System.out.println("  " + msg.getRequester());
					if (!serverBytes.containsKey(msg.getRequester())) {
						serverBytes.put(msg.getRequester(), new Long(0));
						register();
					}
					System.out.println(this.serverBytes);
				}
				else if (msg.getMessage().equals("end")) {
					break;
				}
				else {
					System.out.println("  Mensaje desconocido.");	
				}
				System.out.println();
			}
			socket.leaveGroup(group);
			socket.close();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			ioe.printStackTrace();
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println();
			System.out.println("ClassNotFoundException");
			cnfe.printStackTrace();
		}
	}
}
	/*
	public void multicastFile(String pathname) {
		try {
			File multiFile = new File(pathname);
			
			// Envio el nombre del archivo por la direccion multicast.
			String fileName = multiFile.getName();
			System.out.println(fileName); 
			DatagramPacket name = new DatagramPacket(fileName.getBytes(),
													 fileName.length(), 
													 group, port);
			socket.send(name);

			// Envio el tamaño del archivo por la direccion multicast.
			long fileSize = multiFile.length();
			String strFileSize = Long.toString(fileSize); 
			DatagramPacket size = new DatagramPacket(strFileSize.getBytes(),
													 strFileSize.length(), 
													 group, port);
			socket.send(size);
			
			// Calculo la cantidad de chunks en que será enviado el archivo.
			System.out.println(fileSize);
			byte[] b = new byte[1024];
			long chunks = fileSize / 1024;
			if ((fileSize % 1024) != 0) {
				chunks++;
			}
			System.out.println(chunks);

			// Envío el archivo por la dirección multicast.
			FileInputStream in = new FileInputStream(multiFile);
			for (int i = 0; i < chunks; i++) {
				in.read(b);
				System.out.println(b.length);
				DatagramPacket fileChunk = new DatagramPacket(b, b.length, 
															  group, port);
				socket.send(fileChunk);
			}
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();    
		}
	}

class MulticastPublishingTest {
	public static void main(String[] args) {    
		MulticastPublisher publisher = new MulticastPublisher();
		//publisher.multicastFile("/home/eliot/Documents/Trimestre XIV/Sistemas Operativos II/MulticastPublisher.class");
		publisher.multicastFile("/home/eliot/mozilla.pdf");
		/*
		try {
			//publisher.multicast();
			//publisher.socket.leaveGroup(group);
			//publisher.socket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
*/