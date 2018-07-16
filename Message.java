import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.util.ArrayList;
import java.sql.Timestamp;

class Message implements Serializable {
	private String message;
	private String fileName;
	private long fileSize;
	private Timestamp version;
	private InetAddress requester;
	private InetAddress sender;
	private ArrayList<InetAddress> IPs;

	public Message(String message) {
		this.message = message;
	}

	public String getMessage() { return this.message; }
	
	public String getFileName() { return this.fileName; }
	
	public long getFileSize() {return this.fileSize; }
	
	public Timestamp getVersion() {return this.version; }

	public InetAddress getRequester() { return this.requester; }

	public InetAddress getSender() { return this.sender; }
	
	public ArrayList<InetAddress> getIPs() { return this.IPs; }

	public void setMessage(String message) { this.message = message; }
	
	public void setFileName(String fileName) { this.fileName = fileName; }
	
	public void setFileSize(long fileSize) { this.fileSize = fileSize; }
	
	public void setVersion(Timestamp timestamp) { this.version = timestamp; }

	public void setVersion() { this.version = new Timestamp(System.currentTimeMillis()); }

	public void setRequester(InetAddress req) { 
		try {
			this.requester = req; 
		}
		catch (UnknownHostException uhe) {
			System.out.println();
			System.out.println("UnknownHostException");
			System.out.println(uhe);
		}
	}

	public void setSender(InetAddress sndr) { 
		try {
			this.sender = sndr; 
		}
		catch (UnknownHostException uhe) {
			System.out.println();
			System.out.println("UnknownHostException");
			System.out.println(uhe);
		}
	}
	
	public void createIPs() { this.IPs = new ArrayList<InetAddress>(); }

	public void addIP(InetAddress ip) {
		if (!this.IPs.contains(ip)) {
			this.IPs.add(ip);
		}
	}

	public void replaceIP(int i, InetAddress ip) {
		IPs.set(i,ip);
	}

}