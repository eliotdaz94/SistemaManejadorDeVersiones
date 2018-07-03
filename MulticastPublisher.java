import java.io.*;
import java.net.*;

public class MulticastPublisher {
	private String id;
	private InetAddress group;
	private int port; 
	private MulticastSocket socket;
	private byte[] buf;
	
	public MulticastPublisher() {
		try {
			group = InetAddress.getByName("228.5.6.7");
			port = 6789;
			socket = new MulticastSocket(port);
			socket.joinGroup(group);
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
		}
	}

	public void multicast() throws IOException {
		String msg = "Hello";
		DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
											   group, port);
		socket.send(hi);
		//socket.leaveGroup(group);
		//socket.close();
	}

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
		*/
	}
}