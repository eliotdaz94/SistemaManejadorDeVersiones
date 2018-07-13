import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;

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
	
	public MulticastServer() {
		try {
			this.group = InetAddress.getByName("228.5.6.7");
			this.port = 6789;
			this.socket = new MulticastSocket(this.port);
			this.socket.joinGroup(this.group);
			this.recvBuf = new byte[4096];
			this.outByteStream = new ByteArrayOutputStream(4096);
			this.inByteStream = new ByteArrayInputStream(recvBuf);
			this.os = new ObjectOutputStream(this.outByteStream);
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
			os.writeObject(msg);
			os.flush();

			// Retrieves byte array.
			this.sendBuf = this.outByteStream.toByteArray();
			DatagramPacket packet = new DatagramPacket(this.sendBuf, 
													   this.sendBuf.length,
													   this.group, this.port);
			int byteCount = packet.getLength();
			this.socket.send(packet);
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(this.recvBuf, 
														   this.recvBuf.length);
				this.socket.receive(packet);
				this.is = new ObjectInputStream(this.inByteStream);
				int byteCount = packet.getLength();
				Message msg = (Message)is.readObject();
				System.out.println(msg.getMessage());
				System.out.println(msg.getFileName());
				System.out.println(msg.getFileSize());
				if (msg.getMessage().equals("end")) {
					break;
				}
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