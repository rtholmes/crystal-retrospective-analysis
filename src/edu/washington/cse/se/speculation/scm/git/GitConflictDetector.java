package edu.washington.cse.se.speculation.scm.git;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

import edu.washington.cse.se.speculation.util.TimeUtility;

/**
 * Command executor for determining if two versions of a repository are in conflict. This code has
 * been augmented and improved in crystalvc (crystalvc.googlecode.com); using that code is
 * recommended.
 * 
 * @author rtholmes
 * 
 */
public class GitConflictDetector {

	private final String _repoPath;
	private final String _workingPath;

	public GitConflictDetector(String repoPath, String workingPath) {
		_repoPath = repoPath;
		_workingPath = workingPath;

		if (!new File(_workingPath).exists()) {
			// populate the working path the first time
			try {
				System.out.println("\tGitConflictDetector - creating working path: " + _workingPath);
				execute("cp -R " + _repoPath + " " + _workingPath, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param fromHex
	 * @param toHex
	 * @return
	 * @throws IOException
	 */
	public boolean hasConflict(String fromHex, String toHex) throws IOException {

		// reset the repository
		// these don't work. if they did things would be a lot faster
		// execute(Constants.GIT_PATH + " reset --hard" + fromHex, _workingPath);
		// execute(Constants.GIT_PATH + " reset --merge HEAD" + fromHex, _workingPath);
		// execute(Constants.GIT_PATH + " reset --hard ORIG_HEAD" + fromHex, _workingPath);

		execute(Constants.GIT_PATH + " checkout " + fromHex, _workingPath);

		// execute merge attempt
		String mergeOutput = execute(Constants.GIT_PATH + " merge " + toHex, _workingPath);

		boolean answer = (mergeOutput.indexOf("CONFLICT") > -1);

		// delete the working copy
		// we don't want to do this either, but it doesn't work otherwise
		// execute("rm -rf " + _workingPath, null);

		return answer;
	}

	public void checkout(String hex) throws IOException {
		execute(Constants.GIT_PATH + " checkout " + hex, _workingPath);
	};

	public static String execute(String command, String path) throws IOException {
		final long start = System.currentTimeMillis();
		System.out.print("\t" + TimeUtility.getCurrentLSMRDateString() + ": Executing command: " + command + " ...");
		Runtime myRuntime = Runtime.getRuntime();
		// System.out.println("********** Executing command: " + command);
		// System.out.println("\t********** Executing command in path: " + path);

		// Process myProcess = myRuntime.exec(args[0], null, new
		// File("/home/ybrun/myPlay/voldemort/"));
		// java -cp "../ProjectMerger:$CLASSPATH" merger.ProjectMerger "../git-1.6.5.5/git log"
		Process myProcess;
		if (path == null)
			myProcess = myRuntime.exec(command);
		else
			myProcess = myRuntime.exec(command, null, new File(path));

		BufferedInputStream myStream = new BufferedInputStream(myProcess.getInputStream());
		int current = myStream.read();
		String output = "";
		while (current != -1) {
			output += (char) current;
			current = myStream.read();
		}

		System.out.println(" took: " + TimeUtility.msToHumanReadableDelta(start));
		return output;
	}

	public String getBranchName() {
		// this works
		// git branch --no-color 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/\1/'

		return "";

	}

	// /**
	// * Test code only; do not use otherwise.
	// *
	// * @param args
	// */
	// public static void main(String[] args) {
	// String repoPath = "/Users/rtholmes/tmp/git-test/TestProject/";
	// String workingPath = "/Users/rtholmes/tmp/git-test/workingDir/TestProject/";
	//
	// GitConflictDetector gcd = new GitConflictDetector(repoPath, workingPath);
	//
	// String c1 = "ff5c0e97b83d428beaa914505fadb808c5a57e02";
	// String c2 = "541e9b95c415b341546bb03fe5b04ea41125473e";
	// // String c2 = "ff5c0e97b83d428beaa914505fadb808c5a57e02";
	// try {
	// boolean hasConflict = gcd.hasConflict(c1, c2);
	// System.out.println("Conflict: " + hasConflict);
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public void reset(String tipHex) {
		try {
			execute(Constants.GIT_PATH + " reset --hard " + tipHex, _workingPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}