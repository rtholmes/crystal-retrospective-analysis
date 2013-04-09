package edu.washington.cse.se.speculation.scm.junit;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

import edu.washington.cse.se.speculation.util.XMLTools;

public class JUnitAnalyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String path = "/Users/rtholmes/tmp/git-test/workingDir/voldermort/dist/junit-reports/";
		String outXMLPath = "/Users/rtholmes/tmp/git-test/";

		HashSet<TestResult> results = JUnitAnalyzer.parseTestResults(path);
		String fName = outXMLPath + "voldemort" + "_TestResults_" + "somehex" + ".xml";
		JUnitAnalyzer.writeTestResults(fName, "voldemort", "someHex", results, false);
	}

	@SuppressWarnings("unchecked")
	public static HashSet<TestResult> parseTestResults(String path) {
		HashSet<TestResult> results = new HashSet<TestResult>();

		File testDir = new File(path);
		if (!testDir.isDirectory()) {
//			throw new RuntimeException("Not test directory: " + path);
			System.err.println("Not test directory: " + path);
			return results;
		}

		String[] testFileNames = testDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("TEST") && name.endsWith(".xml");
			}
		});

		int passed = 0;
		int failed = 0;
		for (String testFileName : testFileNames) {
			Document doc = XMLTools.readXMLDocument(path + testFileName);

			Element rootElement = doc.getRootElement();
			if (!rootElement.getName().equals("testsuite")) {
				throw new RuntimeException("Not a test suite XML file: " + testFileName);
			}

			
			List<Element> testCaseElements = rootElement.getChildren("testcase");
			for (Element testCaseElement : testCaseElements) {
				String className = testCaseElement.getAttributeValue("classname");
				String testName = testCaseElement.getAttributeValue("name");
				String testId = className + "::" + testName;

				Element errorElement = testCaseElement.getChild("error");
				boolean testPassed = (errorElement == null);

				if (testPassed)
					passed++;
				else
					failed++;

				results.add(new TestResult(testId, testPassed));
			}
		}
		
		System.out.println("JunitFSEAnalyzer::parseTestResults - Tests passed: "+passed+" tests failed: "+failed);
		return results;
	}

	public static void writeTestResults(String fName, String project, String hex, HashSet<TestResult> results, boolean isInfinite) {
		System.out.println("JunitFSEAnalyzer::writeTestResults - fName: "+fName);
		
		Vector<TestResult> sortedResults = new Vector<TestResult>(results);

		Collections.sort(sortedResults, new Comparator<TestResult>() {
			@Override
			public int compare(TestResult o1, TestResult o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		Document doc = XMLTools.newXMLDocument();
		Element rootElement = new Element("testResults");
		rootElement.setAttribute("project", project);
		rootElement.setAttribute("hex", hex);
		rootElement.setAttribute("isInfinite",isInfinite+"");
		
		doc.setRootElement(rootElement);

		for (TestResult result : sortedResults) {

			Element resultRow = new Element("test");
			resultRow.setAttribute("name", result.getName());
			resultRow.setAttribute("result", result.getResult() + "");
			rootElement.addContent(resultRow);
		}

		XMLTools.writeXMLDocument(doc, fName);
	}

	public static void compareTestResults(HashSet<TestResult> baselineResults, HashSet<TestResult> interventionResults) {

		for (TestResult baselineResult : baselineResults) {
			boolean baselinePassed = baselineResult.getResult();
			boolean interventionPresent = false;
			boolean interventionPassed = false;
			for (TestResult interventionResult : interventionResults) {
				if (baselineResult.equals(interventionResult)) {
					interventionPresent = true;
					interventionPassed = interventionResult.getResult();
				}
			}

			if (!interventionPresent) {
				System.out.println("This shouldn't be");
			}

			if (baselinePassed == interventionPassed) {
				System.out.println("Same result: " + baselinePassed + " \tfor test: " + baselineResult.getName());
			} else {
				System.out.println("Diff result - ( baseline: " + baselinePassed + " intervention: " + interventionPassed + " ) for test: "
						+ baselineResult.getName());
			}

		}

	}
}
