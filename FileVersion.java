import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;

public class FileVersion {

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
}