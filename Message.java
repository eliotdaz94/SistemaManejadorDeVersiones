import java.sql.Timestamp;
import java.io.Serializable;
import java.net.InetAddress;

class Message implements Serializable {
	private String message;
	private String filename;
	private long filesize;
	private Timestamp version;
	private InetAddress[] ips;

	public Message(String message, String filename, long filesize) {
		this.message = message;
		this.filename = filename;
		this.filesize = filesize;
	}

//	public Message(String message, String filename, long filesize) {
//		this.message = message;
//		this.filename = filename;
//		this.filesize = filesize;
//	}

	public String getMessage() { return this.message; }
	public String getFilename() { return this.filename; }
	public long getFilesize() {return this.filesize; }
	public Timestamp getTimestamp() {return this.version; }
	public InetAddress[] getIPs() { return this.ips; }

	public void setMessage(String message) { this.message = message; }
	public void setFilename(String filename) { this.filename = filename; }
	public void setFilesize(long filesize) { this.filesize = filesize; }
	public void setTimestamp() { this.version = new Timestamp(System.currentTimeMillis()); }
	public void setIPs(InetAddress[] ips) { this.ips = ips; }

}