import java.net.InetAddress;
import java.util.ArrayList;
import java.sql.Timestamp;

public class FileVersion {

	private Timestamp timestamp;
	private long fileSize;
	private InetAddress client;
	private ArrayList<InetAddress> replicas;

	public FileVersion(Timestamp timestamp, long fileSize, InetAddress client) {
		this.timestamp = timestamp;
		this.fileSize = fileSize;
		this.client = client;
		this.replicas = new ArrayList<InetAddress>();
	}

	public Timestamp getTimestamp() { return this.timestamp; }

	public long getfileSize() { return this.fileSize; }

	public InetAddress getClient() { return this.client; }

	public ArrayList<InetAddress> getReplicas() { return this.replicas; }

	public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

	public void setTimestamp() { 
		this.timestamp = new Timestamp(System.currentTimeMillis()); 
	}

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

	public boolean equals(Timestamp timestamp, InetAddress client) {
		return this.timestamp.equals(timestamp) && this.client.equals(client);
	}
}