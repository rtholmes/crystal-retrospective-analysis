/**
 * 
 */
package edu.washington.cse.se.speculation.scm.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.generators.GitGraphGenerator;
import edu.washington.cse.se.speculation.scm.git.Constants;
import edu.washington.cse.se.speculation.scm.git.GitGraph;
import edu.washington.cse.se.speculation.util.RunIt;
import edu.washington.cse.se.speculation.util.ThriftClient;
import edu.washington.cse.se.speculation.util.TimeUtility;

/**
 * @author Yuriy
 * @author rtholmes (make it actually work)
 * 
 */
public class GitProjectTestResultGenerator {

	private static final boolean USE_REMOTE = true;

	private String _repoPath;
	private String _workingPath;
	private String _metadataPath;
	private String _outputPath;

	public GitProjectTestResultGenerator(String metadataPath, String repoPath, String workingPath, String outputPath) {
		_metadataPath = metadataPath;
		_repoPath = repoPath;
		_workingPath = workingPath;
		_outputPath = outputPath;

		if (!new File(_workingPath).exists()) {
			// populate the working path the first time
			try {
				System.out.println(TimeUtility.getCurrentLSMRDateString() + ": GitConflictDetector - creating working path: " + _workingPath);

				// GitConflictDetector.execute("cp -R " + _repoPath + " " + _workingPath, null);

				RunIt.execute("/bin/cp", new String[] { "-R", _repoPath, _workingPath }, outputPath);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Takes a path and generates a filenamePrefix.hex of "make test" for each check out.
	// gitPath must end in .git
	// throws Exception because Reid wants to know when something bad happens
	// public static void generateTestResults(String metadataFile, String dotGitPath, String testPath, String
	// outputPath, String filenamePrefix)
	public void generateTestResults(final String clientName) throws Exception {
		VCSGraph gitGraph = null;

		System.out.println("GitProjectTestResultGenerator::generateTestResults( " + clientName + " )");

		if (!USE_REMOTE) {
			if (new File(_metadataPath).exists()) {
				gitGraph = VCSGraph.readXML(_metadataPath);
				System.out.println(TimeUtility.getCurrentLSMRDateString() + ": GitProjectTestResultGenerator - Loading cached metatdata file: "
						+ _metadataPath);
			} else {
				gitGraph = (new GitGraphGenerator(_workingPath + ".git/")).generateGraph();
				GitGraph.writeXML(_metadataPath, gitGraph);
			}

			// for (String hex : gitGraph.getNodes().keySet()) {
			for (VCSNode node : gitGraph.getVertices()) {
				String hex = node.getHex();

				computeResults(hex);

			}
		} else {
			boolean done = false;
			String hex = null;
			String result = "";

			while (!done) {
				hex = ThriftClient.getNextJob(hex, clientName, result);
				if (hex != null) {
					result = computeResults(hex);
				} else {
					done = true;
				}
			}

		}

	}

	private String computeResults(String hex) {
		String outputFile = _outputPath + "gitTestResults_" + hex;
		System.out.println(TimeUtility.getCurrentLSMRDateString() + ": Generating test results for hex: " + hex);
		try {
			if (new File(outputFile).createNewFile()) {

				String testOutput = "";
				// GitConflictDetector.execute(Constants.GIT_PATH + " checkout " + hex, _workingPath);
				String[] args;

				// git reset hard
				args = new String[] { "reset", "--hard" };
				testOutput += "*****Executing: " + Constants.GIT_PATH + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice(Constants.GIT_PATH, args, _workingPath);

				// git checkout
				args = new String[] { "checkout", hex };
				testOutput += "*****Executing: " + Constants.GIT_PATH + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice(Constants.GIT_PATH, args, _workingPath);

				// make clean
				args = new String[] { "clean" };
				testOutput += "*****Executing: " + "/usr/bin/make" + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice("/usr/bin/make", args, _workingPath);

				// make configure
				args = new String[] { "configure" };
				testOutput += "*****Executing: " + "/usr/bin/make" + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice("/usr/bin/make", args, _workingPath);

				// configure
				args = new String[] {};
				testOutput += "*****Executing: " + _workingPath + "configure" + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice(_workingPath + "configure", args, _workingPath);

				
				// make 
				args = new String[] { "" };
				testOutput += "*****Executing: " + "/usr/bin/make" + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.executeTwice("/usr/bin/make", args, _workingPath);
				
				// make test
				args = new String[] { "test" };
				testOutput += "*****Executing: " + "/usr/bin/make" + " " + Arrays.asList(args).toString() + " in: " + _workingPath + "\n";
				testOutput += RunIt.execute("/usr/bin/make", args, _workingPath);

				// GitConflictDetector.execute("/usr/bin/make configure", _workingPath);
				// GitConflictDetector.execute("configure", _workingPath);
				// String testOutput = GitConflictDetector.execute("/usr/bin/make test", _workingPath);

				// XXX: i don't know why but 20% of the time this command returns in 200 ms with no action
				// String testOutput = GitConflictDetector.execute("/usr/bin/make test", _workingPath);
				// testOutput += RunIt.execute("/usr/bin/make", new String[] { "test" }, _workingPath);

				BufferedWriter testOutputFile = new BufferedWriter(new FileWriter(outputFile));
				testOutputFile.write(testOutput);
				testOutputFile.close();

				return testOutput;
			} else {
				System.out.println("Result already computed: " + hex);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void main(String[] args) throws Exception {
		try {
			final String wpDateStamp = TimeUtility.getCurrentLSMRDateString();

			Vector<String> possiblePaths = new Vector<String>();
			possiblePaths.add("/Users/rtholmes/tmp/git-test/"); // os x
			possiblePaths.add("/homes/gws/rtholmes/tmp/git-work/"); // UW NFS
			possiblePaths.add("/home/rtholmes/tmp/git-work/"); // mackey
			possiblePaths.add("/home/grads/rtholmes/tmp/git-work/");// ucalgary csl etc.
			String outputPath = null;
			for (String path : possiblePaths) {
				File fPath = new File(path);
				if (fPath.exists() && fPath.isDirectory()) {
					outputPath = path;
					break;
				}
			}
			// rtholmes MB
			// String path = "/Users/rtholmes/tmp/git-test/git/.git";
			// String testPath = "/Users/rtholmes/tmp/git-test/git/";
			// String outputPath = "/Users/rtholmes/tmp/git-test/";

			// rtholmes recycle
			// String dotGitPath = "/homes/gws/rtholmes/tmp/git-work/git/.git";
			// String testPath = "/homes/gws/rtholmes/tmp/git-work/git/";
			// String outputPath = "/homes/gws/rtholmes/tmp/git-work/";
			// String metadataPath = "/homes/gws/rtholmes/tmp/git-work/git_repositoryMetadata.xml";

			final String metadataPath = outputPath + "git_repositoryMetadata.xml";
			final String repoPath = outputPath + "git/";
			final String workingPath = outputPath + "tmp_git_" + wpDateStamp + File.separator;

			GitProjectTestResultGenerator gptrg = new GitProjectTestResultGenerator(metadataPath, repoPath, workingPath, outputPath);
			if (args.length > 0)
				gptrg.generateTestResults(args[0]);
			else
				gptrg.generateTestResults("n/a");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
