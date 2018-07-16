import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Integer;
import java.lang.reflect.Type;
import java.lang.ClassNotFoundException;
import java.sql.Timestamp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class MasterServer extends Thread {

	private int tolerance;
	private InetAddress myAddress;
	private int port;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;
	private MulticastServer multicast;
	private Gson gson;

	public MasterServer(int port, InetAddress myAddress, int tolerance) {
		try {
			this.tolerance = tolerance + 1;
			this.myAddress = myAddress;
			this.port = port;
			this.socket = new ServerSocket(port);
			this.isRunning = true;
			this.serverBytes = new HashMap<InetAddress, Long>();
			this.serverBytes.put(InetAddress.getLocalHost(), new Long(0));
			this.gson = new Gson();
			String dir = System.getProperty("user.dir");
			System.out.println(dir);
			String jsonFile = dir + "/storedFiles.json";
			System.out.println(jsonFile);
			Type storedFilesType = new TypeToken<HashMap<String, 
									   ArrayList<FileVersion>>>() {}.getType();
			FileReader fileR = new FileReader(jsonFile);
			JsonReader jsonR = new JsonReader(fileR);
			this.storedFiles = this.gson.fromJson(jsonR, storedFilesType);
			System.out.println();
			if (storedFiles == null) {
				System.out.println("Cargando mapa nulo.");
				this.storedFiles = new HashMap<String, ArrayList<FileVersion>>();				
			}
			printStoredFiles();
			this.multicast = new MulticastServer(this.myAddress,
												 this.storedFiles, 
												 this.serverBytes);
			this.multicast.start();
			
			// Datos de prueba.
			ArrayList<FileVersion> testArray = new ArrayList<FileVersion>();
			InetAddress testIP = InetAddress.getByName("159.90.8.140");
			FileVersion testFileVersion = new FileVersion(new Timestamp(System.currentTimeMillis()), 200, testIP);
			testFileVersion.addIP(InetAddress.getByName("159.95.8.150"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.160"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.180"));
			testArray.add(testFileVersion);
			storedFiles.put("HarryPotter",testArray);
			//serverBytes.put(InetAddress.getLocalHost(), new Long(0));
			//serverBytes.put(InetAddress.getByName("159.95.8.100"), new Long(10));
			//serverBytes.put(InetAddress.getByName("159.95.8.110"), new Long(40));
			//serverBytes.put(InetAddress.getByName("159.95.8.120"), new Long(20));
			//serverBytes.put(InetAddress.getByName("159.95.8.130"), new Long(30));
			//serverBytes.put(InetAddress.getByName("159.95.8.140"), new Long(145));
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
				System.out.println("Recibiendo " + request.getMessage()
								   + " de " + request.getRequester() + ":");
				if (request.getMessage().equals("commit")) {
					System.out.println("  " + request.getFileName());
					System.out.println("  " + request.getFileSize());
					System.out.println();
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
					System.out.println();
					// Fin del balance de carga.
					
					// Enviamos el mensaje de vuelta.
					System.out.println("Enviando " + reply.getMessage() + ":");
					System.out.println("  " + reply.getVersion());
					System.out.println("  " + reply.getIPs());
					System.out.println();
					out.writeObject(reply);
					out.flush();
				}
				else if (request.getMessage().equals("checkout")) {
					System.out.println("  " + request.getFileName());
					System.out.println();
					if (storedFiles.containsKey(request.getFileName())) {
						Message reply = new Message("ACK");
						reply.setFileName(request.getFileName());
						ArrayList<FileVersion> fileInfo = storedFiles.getOrDefault(request.getFileName(), new ArrayList<FileVersion>());
						boolean clientExists = false;
						for (int i = fileInfo.size() - 1; i >= 0; i--) {
							if (fileInfo.get(i).getClient().equals(request.getRequester())) {
								reply.createIPs();
								for (InetAddress ip : fileInfo.get(i).getReplicas()) {
									reply.addIP(ip);
								}	
								reply.setFileSize(fileInfo.get(i).getfileSize());
								reply.setVersion(fileInfo.get(i).getTimestamp());
								clientExists = true;
								break;
							}
						}
						if (!clientExists) {
							reply.createIPs();
							for (InetAddress ip : fileInfo.get(fileInfo.size()-1).getReplicas()) {
								reply.addIP(ip);
							}
							reply.setFileSize(fileInfo.get(fileInfo.size()-1).getfileSize());
							reply.setVersion(fileInfo.get(fileInfo.size()-1).getTimestamp());
						}
						reply.setRequester(myAddress);
						
						// Enviamos el mensaje de vuelta.
						System.out.println("Enviando " + reply.getMessage() + ":");
						System.out.println("  " + reply.getVersion());
						System.out.println("  " + reply.getIPs());
						System.out.println();
						out.writeObject(reply);
						out.flush();
					}
					else {
						// Enviamos el mensaje de vuelta.
						Message reply = new Message("reject");
						System.out.println("Enviando " + reply.getMessage() + ".");
						System.out.println();
						out.writeObject(reply);
						out.flush();
					}
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
		try {
			int port = Integer.parseInt(args[0]);
			InetAddress address = InetAddress.getByName(args[1]);
			int tolerance = Integer.parseInt(args[2]);
			MasterServer server = new MasterServer(port, address, tolerance);
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