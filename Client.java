import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;

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
			//while (true){}
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
		Client client = new Client(8888);
		client.commit("/home/guillermobet/Documentos/USB/pasantia/justificacion.txt");
	}
}