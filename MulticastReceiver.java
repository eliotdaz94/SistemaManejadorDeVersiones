import java.io.*;
import java.net.*;

public class MulticastReceiver extends Thread {
	private InetAddress group;
	private int port; 
	private MulticastSocket socket;
	private byte[] buf = new byte[4096];
	
	public MulticastReceiver() {
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

	public void run() {
		try {
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String received = new String(packet.getData(), 0, 
											 packet.getLength());
				System.out.println(received);
				if ("end".equals(received)) {
					break;
				}
			}
			socket.leaveGroup(group);
			socket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveFile() {
		try {
			// Recibo el nombre del archivo.
			DatagramPacket name = new DatagramPacket(buf, buf.length);
			socket.receive(name);
			String fileName = new String(name.getData(), 0, name.getLength());
			//fileName = fileName + "v1.0"
			System.out.println(fileName);

			// Recibo el tamaño del archivo.
			DatagramPacket size = new DatagramPacket(buf, buf.length);
			socket.receive(size);
			String strFileSize = new String(size.getData(), 0, 
											size.getLength());
			long fileSize = Long.parseLong(strFileSize);
			System.out.println(fileSize);

			// Calculo la cantidad de chunks en que será recibido el archivo.
			long chunks = fileSize / 1024;
			if ((fileSize % 1024) != 0) {
				chunks++;
			}
			System.out.println(chunks);
			
			// Recibo el archivo.
			File receivedFile = new File(fileName);
			FileOutputStream out = new FileOutputStream(receivedFile);
			for (int i = 0; i < chunks; i++) {
				DatagramPacket fileChunk = new DatagramPacket(buf, buf.length);
				socket.receive(fileChunk);
				out.write(fileChunk.getData());
			}
			out.close();
		}
		catch (IOException ioe) {
			System.out.println();
			System.out.println("IOException");
			System.out.println(ioe);	
		}
	}
}

class MulticastReceivingTest {
	public static void main(String[] args) {
		MulticastReceiver receiver = new MulticastReceiver();
		receiver.receiveFile();
	}
}