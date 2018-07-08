import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataOutputStream;
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
			Message message = new Message("commit", file.getName(), file.length());

			System.out.println("Haciendo commit...");

			out.writeObject(message);
			out.flush();
			message = (Message)in.readObject();
			System.out.println(message.getMessage());
			System.out.println(message.getTimestamp());
			System.out.println(message.getIPs());
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
	private Timestamp version;

	public FileSender(InetAddress serverIP, int port, String filePath, Timestamp version) {
		try {
			this.socket = new Socket(serverIP, port);
			this.filePath = filePath;
			this.version = version;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
			FileInputStream fis = new FileInputStream(this.filePath);
			byte[] buffer = new byte[4096];
			int count;

			while ((count = fis.read(buffer)) > 0) {
				dos.write(buffer, 0, count);
			}
			fis.close();
			dos.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ClientTest {
	public static void main(String[] args) {

		String file = "/home/guillermobet/Documentos/USB/pasantia/justificacion.txt";
		try {
			Client client = new Client(8888);
			client.commit(file);
		
			// el master responde con la lista de ips

			FileSender fs = new FileSender(InetAddress.getByName("127.0.0.1"), 2307, file, new Timestamp(System.currentTimeMillis()));
			fs.start();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
		}
	}
}