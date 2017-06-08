package tp.pdc.proxy.metric;

import tp.pdc.proxy.metric.interfaces.HostMetric;

public class HostMetricImpl implements HostMetric {

	private long connections;
	private long bytesRead;
	private long bytesWritten;

	@Override
	public long getConnections () {
		return connections;
	}

	@Override
	public long getBytesRead () {
		return bytesRead;
	}

	@Override
	public long getBytesWritten () {
		return bytesWritten;
	}

	@Override
	public void addConnection () {
		connections++;
	}

	@Override
	public void addBytesRead (long bytes) {
		bytesRead += assertNonNegative(bytes);
	}

	@Override
	public void addBytesWritten (long bytes) {
		bytesWritten += assertNonNegative(bytes);
	}

	private long assertNonNegative (long bytes) {
		if (bytes < 0)
			throw new IllegalArgumentException("Invalid negative amount of bytes: " + bytes);
		return bytes;
	}
}
