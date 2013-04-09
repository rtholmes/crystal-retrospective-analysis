package edu.washington.cse.se.speculation.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.jdom.Document;
import org.jdom.Element;

import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;

public class ThriftServer {

	class JobDescription {
		String client;
		boolean complete;
		Date date;
		String hex;
		boolean inFlight = false;
	}

	private String _jobDescriptorPath;

	private String _metadataPath;

	private String _outputPath;

	public ThriftServer(String metadataPath, String jobDescriptorPath, String outputPath) {
		_metadataPath = metadataPath;
		_jobDescriptorPath = jobDescriptorPath;
		_outputPath = outputPath;
	}

	public Vector<String> loadJobStrings(String metadataPath) {
		VCSGraph gitGraph = VCSGraph.readXML(metadataPath);

		Vector<String> jobs = new Vector<String>();

		for (VCSNode node : gitGraph.getVertices()) {
			jobs.add(node.getHex());
		}

		return jobs;
	}

	private void serveJobs(final Vector<JobDescription> jobs, int port) {

		TNonblockingServerSocket socket;
		try {
			socket = new TNonblockingServerSocket(port);
			JobService.Processor processor = new JobService.Processor(new JobService.Iface() {

				private String findNextJob() {
					JobDescription nextJob = null;
					for (JobDescription job : jobs) {
						if (!job.complete && !job.inFlight) {
							nextJob = job;
							nextJob.inFlight = true;
							break;
						}
					}
					if (nextJob != null) {
						return nextJob.hex;
					}
					return null;
				}

				@Override
				public String getNextJob(String hex, String clientName, String content) throws TException {
					System.out.println("ThriftServer::getNextJob(..) - received - client: " + clientName);

					if (hex == null) {
						System.out.println("\tThriftServer::getNextJob(..) - received - client: " + clientName + "; no hex, no content");
					} else {
						if (content != null)
							System.out.println("\tThriftServer::getNextJob(..) - received - client: " + clientName + " hex: " + hex
									+ " content length: " + content.length());
						else
							System.out.println("\tThriftServer::getNextJob(..) - received - client: " + clientName + " hex: " + hex
									+ " XXX no content XXX ");
					}

					handleJob(hex, clientName, content);
					String result = findNextJob();

					if (result == null) {
						result = "";
					}

					System.out.println("\tThriftServer::getNextJob(..) - result: " + result);
					return result;
				}

				private void handleJob(String hex, String clientName, String content) {

					Date date = null;
					boolean complete = false;

					if (hex != null) {
						complete = true;
						date = new Date(System.currentTimeMillis());

						for (JobDescription job : jobs) {
							if (job.hex.equals(hex)) {
								job.date = date;
								job.complete = complete;
								job.client = clientName;
							}
						}

						writeJobList(jobs);

						// record returned results
						String outputFName = _outputPath + "gitTestResults_" + hex;
						BufferedWriter testOutputFile;
						try {
							testOutputFile = new BufferedWriter(new FileWriter(outputFName));

							if (content == null)
								content = "";

							testOutputFile.write(content);

							testOutputFile.close();

							System.out.println("ThriftServer::handleJob(..) - content written to: " + outputFName);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			});

			TServer server = new THsHaServer(processor, socket, new TFramedTransport.Factory(), new TCompactProtocol.Factory());
			System.out.println("ThriftServer::serveJobs - listening.");
			server.serve();

		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}

	private void writeJobList(Vector<JobDescription> jobs) {
		Document doc = XMLTools.newXMLDocument();
		Element rootElement = new Element("jobDescriptions");
		doc.setRootElement(rootElement);

		for (JobDescription job : jobs) {
			Element jobElement = new Element("job");
			jobElement.setAttribute("hex", job.hex);
			jobElement.setAttribute("complete", job.complete + "");
			if (job.date != null)
				jobElement.setAttribute("date", TimeUtility.formatLSMRDate(job.date));
			else
				jobElement.setAttribute("date", "");
			if (job.client != null)
				jobElement.setAttribute("client", job.client + "");
			else
				jobElement.setAttribute("client", "");

			rootElement.addContent(jobElement);
		}
		XMLTools.writeXMLDocument(doc, _jobDescriptorPath);
		System.out.println("ThriftServer::writeJobList(..) - list written: " + _jobDescriptorPath);
	}

	/**
	 * @param args
	 */
	@SuppressWarnings( { "unchecked" })
	public static void main(String[] args) {

		// String jobDescriptorPath = "/Users/rtholmes/tmp/git-test/git_jobDescriptors.xml";
		// String metadataPath = "/Users/rtholmes/tmp/git-test/git_repositoryMetadata.xml";

		// mb path
		// String outputPath = "/Users/rtholmes/tmp/git-test/";

		// UW path
		// String outputPath = "/homes/gws/rtholmes/tmp/git-work/";

		// strathcona path
		String outputPath = "/Users/rtholmes/tmp/git-work/";
		String jobDescriptorPath = outputPath + "git_jobDescriptors.xml";
		String metadataPath = outputPath + "git_repositoryMetadata.xml";

		int port = 8091;

		ThriftServer ts = new ThriftServer(metadataPath, jobDescriptorPath, outputPath);

		if (!new File(jobDescriptorPath).exists()) {
			System.out.println("ThriftServer::main(..) - starting from scratch; creating new jobs list.");
			// create initial job set
			if (!new File(jobDescriptorPath).exists()) {
				Vector<JobDescription> jobs = new Vector<JobDescription>();

				Vector<String> jobHexes = ts.loadJobStrings(metadataPath);
				for (String hex : jobHexes) {

					JobDescription jd = ts.new JobDescription();
					jd.hex = hex;
					jd.complete = false;
					jd.date = null;
					jd.client = null;

					jobs.add(jd);

					// String outputFile = "/homes/gws/rtholmes/tmp/git-work/" + "gitTestResults_" + hex;
					// if (new File(outputFile).exists()) {
					// jd.complete = true;
					// jd.date = new Date();
					// jd.client = "UW NFS";
					// }

				}

				System.out.println("ThriftServer::main(..) - writing new jobs list.");
				ts.writeJobList(jobs);
			}
		}

		// read initial job set

		System.out.println("ThriftServer::main(..) - reading job descriptors: " + jobDescriptorPath);
		Vector<JobDescription> jobs = new Vector<JobDescription>();
		Document doc = XMLTools.readXMLDocument(jobDescriptorPath);
		for (Element jobElement : (Collection<Element>) doc.getRootElement().getChildren("job")) {
			JobDescription job = ts.new JobDescription();
			job.hex = jobElement.getAttributeValue("hex");
			job.complete = Boolean.parseBoolean(jobElement.getAttributeValue("complete"));
			if (!jobElement.getAttributeValue("date").equals("")) {
				job.date = TimeUtility.parseLSMRDate(jobElement.getAttributeValue("date"));
			}
			if (!jobElement.getAttributeValue("client").equals("")) {
				job.client = jobElement.getAttributeValue("client");
			}
			jobs.add(job);
		}

		ts.serveJobs(jobs, port);
	}
}
