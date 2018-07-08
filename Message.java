import java.sql.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.net.InetAddress;

class Message implements Serializable {
	private String message;
	private String fileName;
	private long fileSize;
	private Timestamp version;
	private ArrayList<InetAddress> IPs;

	public Message(String message, String fileName, long fileSize) {
		this.message = message;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.IPs = new ArrayList<InetAddress>();
	}

	public String getMessage() { return this.message; }
	
	public String getFileName() { return this.fileName; }
	
	public long getFileSize() {return this.fileSize; }
	
	public Timestamp getTimestamp() {return this.version; }
	
	public ArrayList<InetAddress> getIPs() { return this.IPs; }

	public void setMessage(String message) { this.message = message; }
	
	public void setFilename(String fileName) { this.fileName = fileName; }
	
	public void setFilesize(long fileSize) { this.fileSize = fileSize; }
	
	public void setTimestamp() { this.version = new Timestamp(System.currentTimeMillis()); }
	
	public void addIP(InetAddress ip) {
		if (!this.IPs.contains(ip)) {
			this.IPs.add(ip);
		}
	}

	public void replaceIP(int i, InetAddress ip) {
		IPs.set(i,ip);
	}

}