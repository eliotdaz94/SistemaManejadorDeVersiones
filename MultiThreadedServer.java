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
import java.io.DataInputStream;
import java.io.FileOutputStream;


class StorageService extends Thread {
	private ServerSocket serverSocket;

	public StorageService(int port) {
		try {
			this.serverSocket = new ServerSocket(port);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			Socket accept = this.serverSocket.accept();
			DataInputStream dis = new DataInputStream(accept.getInputStream());
			FileOutputStream fos = new FileOutputStream("justificacionPorSocket.txt");
			byte[] buffer = new byte[8192];
			int count;

			while((count = dis.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
			fos.close();
			dis.close();
			System.out.println("File created successfully");
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

public class MultiThreadedServer extends Thread {

	private int tolerance;
	private int port;
	private ServerSocket socket;
	private boolean isRunning;
	private HashMap<String, ArrayList<FileVersion>> storedFiles;
	private HashMap<InetAddress, Long> serverBytes;

	public MultiThreadedServer(int tolerance, int port){
		try {
			this.tolerance = tolerance + 1;
			this.port = port;
			this.socket = new ServerSocket(port);
			this.isRunning = true;
			this.storedFiles = new HashMap<String, ArrayList<FileVersion>>();
			this.serverBytes = new HashMap<InetAddress, Long>();
			// Datos de prueba.
			ArrayList<FileVersion> testArray = new ArrayList<FileVersion>();
			InetAddress testIP = InetAddress.getByName("159.90.8.140");
			FileVersion testFileVersion = new FileVersion(200,testIP);
			testFileVersion.addIP(InetAddress.getByName("159.95.8.150"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.160"));
			testFileVersion.addIP(InetAddress.getByName("159.95.8.180"));
			testArray.add(testFileVersion);
			storedFiles.put("HarryPotter",testArray);
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

	public class ServerWorker extends Thread {
		
		private Socket clientSocket;
		private ObjectOutputStream out;
		private ObjectInputStream in;

		public ServerWorker(Socket clientSocket) {
			try {
				this.clientSocket = clientSocket;
				out = new ObjectOutputStream(this.clientSocket.
											 getOutputStream());
				in = new ObjectInputStream(this.clientSocket.getInputStream());
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
				System.out.println("Hello from a thread!");
				Message command = (Message)in.readObject();
				if (command.getMessage().equals("commit")) {
					System.out.println("Vamoa hacer un commit.");
					System.out.println(command.getMessage());
					System.out.println(command.getFileName());
					System.out.println(command.getFileSize());
					printStoredFiles();
					command.setTimestamp();
					// Balance de carga
					ArrayList<Long> auxSizes = new ArrayList<Long>();
					int i = 0;
					int maxIndex;
					long maxSize;
					long auxValue;
					for (InetAddress auxAddress : serverBytes.keySet()) {
						auxValue = serverBytes.get(auxAddress);
						if (i < tolerance) {
							command.addIP(auxAddress);
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
								command.replaceIP(maxIndex, auxAddress);
							}
						}
					}
					System.out.println(serverBytes);
					System.out.println(command.getFileSize());
					// Necesitamos hacer control de concurrencia.
					for (InetAddress auxAddress : serverBytes.keySet()) {
						if (command.getIPs().contains(auxAddress)) {
							auxValue = serverBytes.get(auxAddress);
							serverBytes.replace(auxAddress, auxValue + 
												command.getFileSize());
						}
					}
					// Fin Seccion Critica
					System.out.println(serverBytes);
					System.out.println(command.getIPs());
					// Fin del balance de carga.
					
					// Enviamos el mensaje de vuelta.
					command.setMessage("ACK");
					out.writeObject(command);
					out.flush();
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
}

class FileVersion {

	private Timestamp timestamp;
	private long fileSize;
	private InetAddress client;
	private ArrayList<InetAddress> replicas;

	public FileVersion(long fileSize, InetAddress client) {
		this.timestamp = new Timestamp(System.currentTimeMillis());
		this.fileSize = fileSize;
		this.client = client;
		this.replicas = new ArrayList<InetAddress>();
	}

	public Timestamp getTimestamp() { return this.timestamp; }

	public long getfileSize() { return this.fileSize; }

	public InetAddress getClient() { return this.client; }

	public ArrayList<InetAddress> getReplicas() { return this.replicas; }

	public void setTimestamp() { this.timestamp = new Timestamp(System.currentTimeMillis()); }

	public void setFilesize(long fileSize) { this.fileSize = fileSize; }

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
}

class ServerTest {
	public static void main(String[] args) {
		MultiThreadedServer server = new MultiThreadedServer(2, 8888);
		System.out.println("Inicializing server...");
		//server.printStoredFiles();
		server.start();
		System.out.println("Server initialized");
		StorageService ss = new StorageService(2307);
		ss.start();
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

	private String fileName;
	private Timestamp version;

	public FileInformation(String fileName, Timestamp version) {
		this.fileName = fileName;
		this.version = version;
	}

	public String getFile() { return this.fileName; }

	public void setVersion(Timestamp version) { this.version = version; }
}
*/