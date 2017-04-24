package tp.pdc.proxy.statistics;

class HostStatistics {
	
	private long connections;
	private long bytesRead;
	private long bytesWritten;
	
	public long getConnections() {
		return connections;
	}
	
	public long getBytesRead() {
		return bytesRead;
	}
	
	public long getBytesWritten() {
		return bytesWritten;
	}
	
	public void addConnection() {
		connections++;
	}
	
	public void addBytesRead(long bytes) {
		bytesRead += assertNonNegative(bytes);
	}
	
	public void addWrittenBytes(long bytes) {
		bytesWritten += assertNonNegative(bytes);
	}
	
	private long assertNonNegative(long bytes) {
		if (bytes < 0)
			throw new IllegalArgumentException("Invalid negative amount of bytes: " + bytes);
		return bytes;
	}
}
