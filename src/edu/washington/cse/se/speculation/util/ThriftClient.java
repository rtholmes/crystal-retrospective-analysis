package edu.washington.cse.se.speculation.util;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class ThriftClient {
	// public static final String HOST = "localhost";
	public static final String HOST = "strathcona.cpsc.ucalgary.ca";
	public static final int PORT = 8091;

	public static String getNextJob(final String hexCompleted, final String clientName, final String content) {
		try {

			if (content != null)
				System.out.println("ThriftClient::getNextJob(..) - sending; hex: " + hexCompleted + "; clientName: " + clientName
						+ "; content length: " + content.length());
			else
				System.out
						.println("ThriftClient::getNextJob(..) - sending; hex: " + hexCompleted + "; clientName: " + clientName + "; content: none");

			long start = System.currentTimeMillis();
			TSocket socket = new TSocket(HOST, PORT);
			socket.setTimeout(10000);
			TTransport transport = new TFramedTransport(socket);
			TProtocol protocol = new TCompactProtocol(transport);
			JobService.Client client = new JobService.Client(new TCompactProtocol(transport));
			transport.open();

			String result = client.getNextJob(hexCompleted, clientName, content);

			System.out.println("ThriftClient::getNextJob(..) - received response. took: " + TimeUtility.msToHumanReadableDelta(start) + "; length: "
					+ result.length() + "; content: " + result);

			return result;
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 8091;
		String host = "strathcona.cpsc.ucalgary.ca";
		// String host = "localhost";

		try {

			System.out.println("Client sending message");

			long start = System.currentTimeMillis();
			TSocket socket = new TSocket(host, port);
			socket.setTimeout(10000);
			TTransport transport = new TFramedTransport(socket);
			TProtocol protocol = new TCompactProtocol(transport);
			JobService.Client client = new JobService.Client(new TCompactProtocol(transport));
			transport.open();

			String result = client.getNextJob(null, null, null);

			System.out.println("Client received response - took: " + TimeUtility.msToHumanReadableDelta(start) + "; length: " + result.length()
					+ "; content: " + result);

		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {

			e.printStackTrace();
		}

	}

}
