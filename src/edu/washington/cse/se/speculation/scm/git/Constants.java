package edu.washington.cse.se.speculation.scm.git;

import java.io.File;
import java.util.Vector;

public class Constants {

	public static String REPOSITORY_PREFIX = "";

	static {
		Vector<String> possiblePaths = new Vector<String>();
		possiblePaths.add("/Users/rtholmes/dev/git-repositories-for-speculation/"); // rtholmes - craigleith
		possiblePaths.add("/Users/rtholmes/tmp/git-test/"); // rtholmes - laptop
		possiblePaths.add("/homes/gws/rtholmes/tmp/git-work/"); // rtholmes - UW;
		possiblePaths.add("/Users/rtholmes/tmp/git-work/"); // rtholmes - strathcona
		possiblePaths.add("/home/rtholmes/dev/speculation/scm/"); // rtholmes - swag
		possiblePaths.add("C:\\Users\\brun\\work\\"); // ybrun - work machine

		for (String possiblePath : possiblePaths) {
			if (new File(possiblePath).exists()) {
				REPOSITORY_PREFIX = possiblePath;
				break;
			}
		}
		if (!REPOSITORY_PREFIX.endsWith(File.separator)) {
			REPOSITORY_PREFIX += File.separator;
		}

		System.out.println("Constants - Working path: " + REPOSITORY_PREFIX);
	}

	private final static String jGit = "jgit";
	private final static String voldemort = "voldemort";
	private final static String test = "TestProject";
	private final static String linux = "linux-2.6";
	private final static String rails = "rails";
	private final static String homebrew = "homebrew";
	private final static String mangos = "mangos";
	private final static String git = "git";
	private final static String jquery = "jquery";

	// Select the project you want to try out
	public static String PROJECT = jquery;

	// don't change this one
	public static String PROJECT_PATH = REPOSITORY_PREFIX + PROJECT + File.separator;

	public static String XML_TMP = REPOSITORY_PREFIX + PROJECT + "_out_tmp.xml";
	public static String XML_FINAL = REPOSITORY_PREFIX + PROJECT + "_out_final.xml";
	public static String DOT_FINAL = REPOSITORY_PREFIX + PROJECT + "_out_final.dot";

	public static final boolean RECOVER_PREVIOUS = true;

	public final static int POOL_SIZE = 1;

	public final static int TMP_WRITE_FREQ = 100;

	public static String GIT_PATH = "";

	static {
		String opt1 = "/usr/local/git/bin/git";
		String opt2 = "/usr/bin/git";

		File f = new File(opt1);
		if (new File(opt1).exists()) {
			GIT_PATH = opt1;
		} else if (new File(opt2).exists()) {
			GIT_PATH = opt2;
		} else {
			throw new RuntimeException("Add another git path option clause");
		}
		System.out.println("Constants - Git install path: " + GIT_PATH);
	}

	public static void setProject(String project) {
		System.out.println("Constants::setProject( " + project + " )");
		PROJECT = project;

		PROJECT_PATH = REPOSITORY_PREFIX + PROJECT + File.separator;

		XML_TMP = REPOSITORY_PREFIX + PROJECT + "_out_tmp.xml";
		XML_FINAL = REPOSITORY_PREFIX + PROJECT + "_out_final.xml";
		DOT_FINAL = REPOSITORY_PREFIX + PROJECT + "_out_final.dot";

	}
}
