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
import java.io.FileOutputStream;

public class MasterServer extends Thread {

	private MulticastServer multicast;
	private int tolerance;
	private int port;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;

	public MasterServer(int tolerance, int port){
		try {
			this.multicast = new MulticastServer();
			this.multicast.start();
			this.tolerance = tolerance + 1;
			this.port = port;
			this.socket = new ServerSocket(port);
			this.isRunning = true;
			this.storedFiles = new HashMap<String, ArrayList<FileVersion>>();
			this.serverBytes = new HashMap<InetAddress, Long>();
			
			Message test = new Message("multicast");
			test.setFileName("prueba"); 
			test.setFileSize(new Long(150));
			multicast.sendMessage(test);

			// Datos de prueba.
			ArrayList<FileVersion> testArray = new ArrayList<FileVersion>();
			InetAddress testIP = InetAddress.getByName("159.90.8.140");
			FileVersion testFileVersion = new FileVersion(200,testIP);
			testFileVersion.addIP(InetAddress.getByName("159.95.8.150"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.160"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.180"));
			testArray.add(testFileVersion);
			storedFiles.put("HarryPotter",testArray);
			serverBytes.put(InetAddress.getLocalHost(), new Long(0));
			serverBytes.put(InetAddress.getByName("159.95.8.100"), new Long(10));
			serverBytes.put(InetAddress.getByName("159.95.8.110"), new Long(40));
			serverBytes.put(InetAddress.getByName("159.95.8.120"), new Long(20));
			serverBytes.put(InetAddress.getByName("159.95.8.130"), new Long(30));
			serverBytes.put(InetAddress.getByName("159.95.8.140"), new Long(145));
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
				clientSocket = socket.accept();
				(new MasterServerWorker(clientSocket)).start();
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

	public void printStoredFiles() {
		for (String fileName : storedFiles.keySet()) {
			System.out.println(fileName);	
			ArrayList<FileVersion> auxFiles = storedFiles.get(fileName);
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

	public class MasterServerWorker extends Thread {
		
		private Socket clientSocket;
		private ObjectOutputStream out;
		private ObjectInputStream in;

		public MasterServerWorker(Socket clientSocket) {
			try {
				this.clientSocket = clientSocket;
				this.out = new ObjectOutputStream(this.clientSocket.
											 	  getOutputStream());
				this.in = new ObjectInputStream(this.clientSocket.
												getInputStream());
			}
			catch (IOException ioe) {
				System.out.println();
				System.out.println("IOException");
				System.out.println("Cannot create the input/output stream.");
				ioe.printStackTrace();
			}
		}

		// Recordar que la clase está anidada para acceder a tolerance, 
		// storedFiles y serverBytes.
		public void run() {
			try {
				System.out.println("Desde hilo...");
				Message request = (Message)in.readObject();
				System.out.println("Recibiendo " + request.getMessage() + ":");
				if (request.getMessage().equals("commit")) {
					System.out.println("  " + request.getFileName());
					System.out.println("  " + request.getFileSize());
					System.out.println();
					printStoredFiles();
					Message reply = new Message("ACK");
					reply.setVersion();
					reply.createIPs();
					// Balance de carga
					ArrayList<Long> auxSizes = new ArrayList<Long>();
					int i = 0;
					int maxIndex;
					long maxSize;
					long auxValue;
					for (InetAddress auxAddress : serverBytes.keySet()) {
						auxValue = serverBytes.get(auxAddress);
						if (i < tolerance) {
							reply.addIP(auxAddress);
							auxSizes.add(auxValue);
							i++;
						}
						else {
							//Buscas el maximo valor en auxSizes.
							maxIndex = -1;
							maxSize = Long.MIN_VALUE;
							for (int j = 0; j < auxSizes.size(); j++) {
								if (auxSizes.get(j) > maxSize) {
									maxSize = auxSizes.get(j);
									maxIndex = j;
								}
							}
							if (auxValue < maxSize) {
								auxSizes.set(maxIndex, auxValue);
								reply.replaceIP(maxIndex, auxAddress);
							}
						}
					}
					System.out.println(serverBytes);
					System.out.println(request.getFileSize());
					// Necesitamos hacer control de concurrencia.
					for (InetAddress auxAddress : serverBytes.keySet()) {
						if (reply.getIPs().contains(auxAddress)) {
							auxValue = serverBytes.get(auxAddress);
							serverBytes.replace(auxAddress, auxValue + 
												request.getFileSize());
						}
					}
					// Fin Seccion Critica
					System.out.println(serverBytes);
					// Fin del balance de carga.
					
					// Enviamos el mensaje de vuelta.
					System.out.println("Enviando " + reply.getMessage() + ":");
					System.out.println("  " + reply.getVersion());
					System.out.println("  " + reply.getIPs());
					System.out.println();
					out.writeObject(reply);
					out.flush();
				}
				else if (request.equals("checkout")) {
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
				System.out.println();
				System.out.println("IOException");
				ioe.printStackTrace();
			}
		}
	}
}

class MasterServerTest {
	public static void main(String[] args) {
		MasterServer server = new MasterServer(0, 8888);
		System.out.println("Inicializing server...");
		//server.printStoredFiles();
		server.start();
		System.out.println("Server initialized");
	}
}