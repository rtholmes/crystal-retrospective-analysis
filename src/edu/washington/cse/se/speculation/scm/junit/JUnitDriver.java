package edu.washington.cse.se.speculation.scm.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.git.Constants;
import edu.washington.cse.se.speculation.scm.git.GitConflictDetector;
import edu.washington.cse.se.speculation.scm.git.GitGraph;
import edu.washington.cse.se.speculation.scm.git.VCSNodePair;
import edu.washington.cse.se.speculation.util.TimeUtility;
import edu.washington.cse.se.speculation.util.XMLTools;

public class JUnitDriver {

	private GitConflictDetector _gcd;
	private String _tipHex;
	private String _testPath;
	private String _testWritePath = "/Users/rtholmes/tmp/git-test/";

	private String _workingPath;
	private String _xmlPath;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String wpDateStamp = TimeUtility.getCurrentLSMRDateString();

		JUnitDriver jd = new JUnitDriver();

		String path = Constants.PROJECT_PATH;
		String xmlPath = "/Users/rtholmes/tmp/git-test/voldemort_out_final.xml";

		final String workingPath = Constants.REPOSITORY_PREFIX + "tmp_" + Constants.PROJECT + "_" + wpDateStamp + File.separator;

		jd.run(xmlPath, path, workingPath);
	}

	private void run(String xmlPath, String repoPath, String workingPath) {

		VCSGraph graph = VCSGraph.readXML(xmlPath);

		Vector<Triple> commitsToAnalyze = determineCommitSet(graph);

		// runTests(graph, commitsToAnalyze, repoPath, workingPath);

		Hashtable<String, ResultSet> results = gatherResults(commitsToAnalyze);

		Hashtable<String, NodeProperties> nodeProperties = analyzeResults(graph, commitsToAnalyze, results);

		String dotPath = "/Users/rtholmes/tmp/git-test/voldermort_junit.dot";

		writeDOT(graph, nodeProperties, dotPath);
	}

	private void writeDOT(VCSGraph vcsGraph, Hashtable<String, NodeProperties> nodeProperties, String fName) {
		System.out.println("JUnitFSEDriver::writeDOT( " + fName + " )");

		try {
			File outFile = new File(fName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}

			Hashtable<VCSNode, com.oy.shared.lm.graph.GraphNode> nodeMap = new Hashtable<VCSNode, com.oy.shared.lm.graph.GraphNode>();

			com.oy.shared.lm.graph.Graph graph = com.oy.shared.lm.graph.GraphFactory.newGraph();
			graph.getInfo().setCaption("Title");

			for (VCSNode gitNode : vcsGraph.getVertices()) {

				if (!nodeMap.contains(gitNode)) {
					com.oy.shared.lm.graph.GraphNode graphNode = graph.addNode();
					nodeMap.put(gitNode, graphNode);
				} else {
					System.err.println("This should never happen");
				}
				com.oy.shared.lm.graph.GraphNode graphNode = nodeMap.get(gitNode);

				NodeProperties props = nodeProperties.get(gitNode.getHex());

				if (props.labelA != null)
					graphNode.getInfo().setHeader(props.labelA);

				if (props.labelB != null)
					graphNode.getInfo().setCaption(props.labelB);

				if (props.labelC != null)
					graphNode.getInfo().setFooter(props.labelC);

				if (props.colour != null)
					graphNode.getInfo().setFillColor(props.colour);

				if (props.shape != null) {
					if (props.shape.equals("square"))
						graphNode.getInfo().setShapeRecord();
					else if (props.shape.equals("round"))
						graphNode.getInfo().setShapeCircle();
					else if (props.shape.equals("diamond"))
						graphNode.getInfo().setShapeDiamond();
					else if (props.shape.equals("upTriangle"))
						graphNode.getInfo().setShapeTriangle();
					else if (props.shape.equals("downTriangle"))
						graphNode.getInfo().setShapeInvtriangle();
					else {
						graphNode.getInfo().setShapeRecord();
					}
				} else {
					graphNode.getInfo().setShapeRecord();
				}

			}

			for (VCSNode gitChildNode : vcsGraph.getVertices()) {
				com.oy.shared.lm.graph.GraphNode childNode = nodeMap.get(gitChildNode);

				for (VCSNode gitParentNode : gitChildNode.getParents()) {
					com.oy.shared.lm.graph.GraphNode parentNode = nodeMap.get(gitParentNode);

					if (childNode == null || parentNode == null) {
						System.err.println("This should never happen");
					}
					com.oy.shared.lm.graph.GraphEdge edge = graph.addEdge(parentNode, childNode);
				}
			}

			com.oy.shared.lm.out.GRAPHtoDOT.transform(graph, new FileOutputStream(outFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Hashtable<String, NodeProperties> analyzeResults(VCSGraph graph, Vector<Triple> commitsToAnalyze, Hashtable<String, ResultSet> results) {
		Hashtable<String, NodeProperties> properties = new Hashtable<String, NodeProperties>();

		for (VCSNode node : graph.getVertices()) {
			NodeProperties prop = new NodeProperties();

			prop.labelA = node.getHex().substring(node.getHex().length() - 8);

			// set this one to something useful
			prop.labelB = node.getCommitter();

			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH:mm");
			prop.labelC = dateFormat.format(node.getTime());

			properties.put(node.getHex(), prop);
		}

		int merge_infiniteCounter = 0;
		int merge_didNotCompileCounter = 0;
		int merge_ranTestsSuccessfully = 0;

		HashSet<Triple> triplesThatSuccessfullyRanTestSuite = new HashSet<Triple>();

		for (Triple t : commitsToAnalyze) {
			String mergeHex = t._merge;
			ResultSet rs = results.get(mergeHex);
			if (rs.didGoInfinite) {
				merge_infiniteCounter++;
			} else if (!rs.didCompile) {
				merge_didNotCompileCounter++;
			} else {
				merge_ranTestsSuccessfully++;
				triplesThatSuccessfullyRanTestSuite.add(t);
			}

		}

		int infiniteCounter = 0;
		int didNotCompileCounter = 0;
		int ranTestsSuccessfully = 0;
		for (String hex : results.keySet()) {
			NodeProperties prop = properties.get(hex);
			if (prop == null)
				throw new RuntimeException("Prop null: " + hex);

			ResultSet rs = results.get(hex);

			if (rs.didGoInfinite) {
				prop.shape = "downTriangle";
				System.out.println("WentInfinite: " + hex);
				infiniteCounter++;
			} else if (!rs.didCompile) {
				prop.shape = "upTriangle";
				System.out.println("DidNotCompile: " + hex);
				didNotCompileCounter++;
			} else {
				prop.shape = "round";
				ranTestsSuccessfully++;
			}

			int passed = 0;
			int failed = 0;
			for (TestResult tr : rs.results) {
				if (tr.getResult())
					passed++;
				else
					failed++;
			}

			prop.labelB = "P: " + passed + "; F: " + failed;
		}

		int mp_passedInBothParentsAndMerge = 0;
		int mp_failedInBothParentsAndMerge = 0;

		int mp_failedInBothParentsButPassedInMerge = 0;
		int mp_passedInBothParentsButFailedInMerge = 0;

		int mp_passedInParentAButFailedInMerge = 0;
		int mp_passedInParentBButFailedInMerge = 0;

		int mp_failedInParentAButPassedInMerge = 0;
		int mp_failedInParentBButPassedInMerge = 0;

		int mp_someParentIsWorseOff = 0;
		int mp_bothParentsAreWorseOff = 0;

		int buildX_old = 0;
		int buildX_new = 0;
		int buildOK = 0;

		HashSet<Triple> buildOKmerges = new HashSet<Triple>();
		for (Triple t : commitsToAnalyze) {
			ResultSet mergeResultSet = results.get(t._merge);
			ResultSet parentAresultSet = results.get(t._parentA);
			ResultSet parentBresultSet = results.get(t._parentB);

			if ((parentAresultSet.didCompile || parentBresultSet.didCompile) && !mergeResultSet.didCompile) {
				System.err.println("NOTE: new-buildX + 1");
				buildX_new++;
			} else if ((parentAresultSet.didCompile && parentBresultSet.didCompile) && !mergeResultSet.didCompile) {
				System.err.println("NOTE: old-buildX +1");
				buildX_old++;
			} else if (!parentAresultSet.didCompile && !parentBresultSet.didCompile && !mergeResultSet.didCompile) {
				System.err.println("NOTE: old-buildX +1 (all fail)");
				buildX_old++;
			} else if (mergeResultSet.didCompile) {
				System.err.println("NOTE: buildOK +1");
				buildOK++;
				buildOKmerges.add(t);
			} else {
				throw new RuntimeException("Shouldn't happen: merge: " + mergeResultSet.didCompile + " a: " + parentAresultSet.didCompile + " b: " + parentBresultSet.didCompile);
			}
		}

		System.out.println("BUILD Data ( " + commitsToAnalyze.size() + " total merges )");
		System.out.println("\told-BuildX: " + buildX_old);
		System.out.println("\tnew-BuildX: " + buildX_new);
		System.out.println("\tBuildOK: " + buildOK);

		int testX_old = 0;
		int testX_new = 0;
		int testOK = 0;

		for (Triple t : buildOKmerges) {
			ResultSet mergeResultSet = results.get(t._merge);
			ResultSet parentAresultSet = results.get(t._parentA);
			ResultSet parentBresultSet = results.get(t._parentB);

			MergeTestResults mte = computeTestResults(mergeResultSet, parentAresultSet, parentBresultSet);

			if (parentAresultSet.didGoInfinite || parentBresultSet.didGoInfinite || mergeResultSet.didGoInfinite) {
				System.err.println("must handle INF - m: " + mergeResultSet.didGoInfinite + " a: " + parentAresultSet.didGoInfinite + " b: " + parentBresultSet.didGoInfinite);

				if ((!parentAresultSet.didGoInfinite || !parentBresultSet.didGoInfinite) && mergeResultSet.didGoInfinite) {
					System.err.println("Inf worse for child than for a parent");
				} else {
					// if it's not worse then it must be better
					testOK++;
				}
			} else if ((!parentAresultSet.didCompile || !parentBresultSet.didCompile) && mergeResultSet.didCompile) {
				testOK++;
			} else {
				if (mte.passedInParentAButFailedInMerge > 0 || mte.passedInParentBButFailedInMerge > 0) {
					// worse
					testX_new++;

				} else if (mte.passedInBothParentsAndMerge > 0) {

					testOK++;

				} else {

					System.out.println("");
				}
			}

		}

		System.out.println("TEST Data ( " + buildOKmerges.size() + " based on successful builds )");
		System.out.println("\told-TestX: " + testX_old);
		System.out.println("\tnew-TestX: " + testX_new);
		System.out.println("\tTestOK: " + testOK);

		for (Triple t : triplesThatSuccessfullyRanTestSuite) {
			MergeTestResults mte = computeTestResults(results.get(t._merge), results.get(t._parentA), results.get(t._parentB));
			NodeProperties mergeProp = properties.get(t._merge);

			if (mte.passedInBothParentsAndMerge > 0) {
				mp_passedInBothParentsAndMerge++;
			}

			if (mte.failedInBothParentsAndMerge > 0) {
				mp_failedInBothParentsAndMerge++;
			}

			if (mte.failedInBothParentsButPassedInMerge > 0) {
				mp_failedInBothParentsButPassedInMerge++;
				mergeProp.colour = "green";
			}

			if (mte.passedInBothParentsButFailedInMerge > 0) {
				mp_passedInBothParentsButFailedInMerge++;
				System.out.println("Mike_fav - mergeHex: " + mte.mergeHex);
				System.out.println("Mike_fav - aHex: " + mte.aHex);
				System.out.println("Mike_fav - bHex: " + mte.bHex);
				mergeProp.colour = "red";
			}

			if (mte.passedInParentAButFailedInMerge > 0) {
				mp_passedInParentAButFailedInMerge++;
				mergeProp.colour = "red";
			}

			if (mte.passedInParentBButFailedInMerge > 0) {
				mp_passedInParentBButFailedInMerge++;
				mergeProp.colour = "red";
			}

			if (mte.failedInParentAButPassedInMerge > 0) {
				mp_failedInParentAButPassedInMerge++;
			}

			if (mte.failedInParentBButPassedInMerge > 0) {
				mp_failedInParentBButPassedInMerge++;
			}

			if (mte.passedInParentAButFailedInMerge > 0 || mte.passedInParentBButFailedInMerge > 0) {
				mp_someParentIsWorseOff++;
				mergeProp.colour = "red";
				System.out.println("\tWorse off parent: " + mte);
			}

			if (mte.passedInParentAButFailedInMerge > 0 && mte.passedInParentBButFailedInMerge > 0)
				mp_bothParentsAreWorseOff++;

		}

		HashSet<String> nodesInMerges = new HashSet<String>();
		for (Triple t : commitsToAnalyze) {
			nodesInMerges.add(t._parentA);
			nodesInMerges.add(t._parentB);
			nodesInMerges.add(t._merge);
		}

		System.out.println("Total nodes: " + graph.getVertexCount());
		System.out.println("Known merges: " + commitsToAnalyze.size());
		System.out.println("***");
		System.out.println("Nodes involved in known merges: " + nodesInMerges.size());
		System.out.println("\tRan tests successfully: " + ranTestsSuccessfully);
		System.out.println("\tDid not compile: \t " + didNotCompileCounter);
		System.out.println("\tInfinite loop: \t\t " + infiniteCounter);
		System.out.println("Known merge points (that didn't conflict): " + (merge_ranTestsSuccessfully + merge_infiniteCounter + merge_didNotCompileCounter));
		System.out.println("\tRan tests successfully: " + merge_ranTestsSuccessfully);
		System.out.println("\tDid not compile: \t " + merge_didNotCompileCounter);
		System.out.println("\tInfinite loop: \t\t " + merge_infiniteCounter);
		System.out.println("***");
		System.out.println("1 good + 1 compilation parent  -> compilation error: ");
		System.out.println("2 good parents -> compilation error: ");
		System.out.println("2 compilation erros -> good merge: ");
		System.out.println("***");
		System.out.println("1 good + 1 infinite parent  -> infinite: ");
		System.out.println("2 good parents -> infinite: ");
		System.out.println("2 infinites -> good merge: ");
		System.out.println("***");
		System.out.println("Merge where test result is worse for at least one parent: ");
		System.out.println("Merge where test result is worse for both parents: ");
		System.out.println("Merge where the same test passes for both parents but fails in the merge: ");
		System.out.println("Merge where the test results improve for at least one parent: ");
		System.out.println("***");

		System.out.println("Number of merges being considered: " + triplesThatSuccessfullyRanTestSuite.size());
		System.out.println("merges that had tests that passed in both parents and merge: " + mp_passedInBothParentsAndMerge);
		System.out.println("merges that had tests that failed in both parents and merge: " + mp_failedInBothParentsAndMerge);

		System.out.println("merges that had tests that failed in both parents but passed in merge: " + mp_failedInBothParentsButPassedInMerge);
		System.out.println("merges that had tests that passed in both parents but failed in merge: " + mp_passedInBothParentsButFailedInMerge);

		System.out.println("merges that had tests that passed in parent A but failed in merge: " + mp_passedInParentAButFailedInMerge);
		System.out.println("merges that had tests that passed in parent B but failed in merge: " + mp_passedInParentBButFailedInMerge);

		System.out.println("merges that had tests that failed in parent A but passed in merge: " + mp_failedInParentAButPassedInMerge);
		System.out.println("merges that had tests that failed in parent B but passed in merge: " + mp_failedInParentBButPassedInMerge);

		System.out.println("merges that had a parent worse off: " + mp_someParentIsWorseOff);
		System.out.println("merges that had both parents worse off: " + mp_bothParentsAreWorseOff);

		return properties;
	}

	private MergeTestResults computeTestResults(ResultSet mergeResultSet, ResultSet parentAresultSet, ResultSet parentBresultSet) {

		HashSet<String> mergeTestFailures = new HashSet<String>();
		HashSet<String> mergeTestPasses = new HashSet<String>();

		HashSet<String> aTestFailures = new HashSet<String>();
		HashSet<String> aTestPasses = new HashSet<String>();

		HashSet<String> bTestFailures = new HashSet<String>();
		HashSet<String> bTestPasses = new HashSet<String>();

		for (TestResult res : mergeResultSet.results) {
			if (!res.getResult()) {
				mergeTestFailures.add(res.getName());
			} else {
				mergeTestPasses.add(res.getName());
			}
		}

		for (TestResult res : parentAresultSet.results) {
			if (!res.getResult()) {
				aTestFailures.add(res.getName());
			} else {
				aTestPasses.add(res.getName());
			}
		}

		for (TestResult res : parentBresultSet.results) {
			if (!res.getResult()) {
				bTestFailures.add(res.getName());
			} else {
				bTestPasses.add(res.getName());
			}
		}

		MergeTestResults result = new MergeTestResults();
		result.mergeHex = mergeResultSet.hex;
		result.aHex = parentAresultSet.hex;
		result.bHex = parentBresultSet.hex;

		// int passedInBothParentsAndMerge = 0;
		for (String test : mergeTestPasses) {
			if (aTestPasses.contains(test) && bTestPasses.contains(test))
				result.passedInBothParentsAndMerge++;
		}

		// int failedInBothParentsAndMerge = 0;
		for (String test : mergeTestFailures) {
			if (aTestFailures.contains(test) && bTestFailures.contains(test))
				result.failedInBothParentsAndMerge++;
		}

		// int failedInBothParentsButPassedInMerge = 0;
		for (String test : mergeTestPasses) {
			if (aTestFailures.contains(test) && bTestFailures.contains(test)) {
				result.failedInBothParentsButPassedInMerge++;
				System.out.println("Hex: " + mergeResultSet.hex + " - passing test: " + test);
			}
		}

		// int passedInBothParentsButFailedInMerge = 0;
		for (String test : mergeTestFailures) {
			if (aTestPasses.contains(test) && bTestPasses.contains(test)) {
				result.passedInBothParentsButFailedInMerge++;
				System.out.println("Hex: " + mergeResultSet.hex + " - failed test: " + test);
				System.out.println("\tParent A: " + parentAresultSet.hex);
				System.out.println("\tParent B: " + parentBresultSet.hex);
			}
		}

		// int passedInParentAButFailedInMerge = 0;
		for (String test : mergeTestFailures) {
			if (aTestPasses.contains(test)) {
				result.passedInParentAButFailedInMerge++;
				System.out.println("Hex: " + parentAresultSet.hex + " - (A pass M fail) failed test: " + test);
				System.out.println("\tOther Parent: " + parentBresultSet.hex);
				System.out.println("\tMerge hex: " + mergeResultSet.hex);
			}
		}

		// int passedInParentBButFailedInMerge = 0;
		for (String test : mergeTestFailures) {
			if (bTestPasses.contains(test)) {
				result.passedInParentBButFailedInMerge++;
				System.out.println("Hex: " + parentBresultSet.hex + " - (B pass M fail) failed test: " + test);
				System.out.println("\tOther Parent: " + parentAresultSet.hex);
				System.out.println("\tMerge hex: " + mergeResultSet.hex);
			}
		}

		// int failedInParentAButPassedInMerge = 0;
		for (String test : mergeTestPasses) {
			if (aTestFailures.contains(test))
				result.failedInParentAButPassedInMerge++;
		}

		// int failedInParentBButPassedInMerge = 0;
		for (String test : mergeTestPasses) {
			if (bTestFailures.contains(test))
				result.failedInParentBButPassedInMerge++;
		}

		return result;
	}

	private Hashtable<String, ResultSet> gatherResults(Vector<Triple> commitsToAnalyze) {
		Hashtable<String, ResultSet> results = new Hashtable<String, ResultSet>();

		for (Triple triple : commitsToAnalyze) {

			String fName;
			ResultSet rs;

			fName = _testWritePath + Constants.PROJECT + "_TestResults_" + triple._parentA + ".xml";
			if (!new File(fName).exists())
				throw new RuntimeException("File doesn't exist (but should): " + fName);
			rs = parseResultSet(fName);
			results.put(triple._parentA, rs);

			fName = _testWritePath + Constants.PROJECT + "_TestResults_" + triple._parentB + ".xml";
			if (!new File(fName).exists())
				throw new RuntimeException("File doesn't exist (but should): " + fName);
			rs = parseResultSet(fName);
			results.put(triple._parentB, rs);

			fName = _testWritePath + Constants.PROJECT + "_TestResults_" + triple._merge + ".xml";
			if (!new File(fName).exists())
				throw new RuntimeException("File doesn't exist (but should): " + fName);
			rs = parseResultSet(fName);
			results.put(triple._merge, rs);
		}

		return results;
	}

	@SuppressWarnings("unchecked")
	private ResultSet parseResultSet(String fName) {
		Document doc = XMLTools.readXMLDocument(fName);

		String hex = "";
		String project = "";
		boolean didGoInfinite = false;
		boolean didCompile = true;

		Element rootElement = doc.getRootElement();

		project = rootElement.getAttributeValue("project");
		hex = rootElement.getAttributeValue("hex");
		if (rootElement.getAttributeValue("isInfinite") != null)
			didGoInfinite = Boolean.parseBoolean(rootElement.getAttributeValue("isInfinite"));

		if (rootElement.getChildren().size() < 1)
			didCompile = false;

		HashSet<TestResult> results = new HashSet<TestResult>();
		for (Element testElement : (List<Element>) rootElement.getChildren()) {
			String testName = testElement.getAttributeValue("name");
			boolean testResult = Boolean.parseBoolean(testElement.getAttributeValue("result"));

			TestResult result = new TestResult(testName, testResult);

			results.add(result);
		}

		ResultSet rs = new ResultSet(hex, didCompile, didGoInfinite, results);

		return rs;
	}

	private Vector<Triple> determineCommitSet(VCSGraph graph) {
		Vector<Triple> commitsToRun = new Vector<Triple>();

		HashSet<VCSNodePair> knownMerges = graph.getKnownMerges();

		HashSet<VCSNodePair> nonConflictingMerges = new HashSet<VCSNodePair>();
		int numNonConflicts = 0;
		int numWitConflicts = 0;
		for (VCSNodePair merge : knownMerges) {
			if (merge.first().getTime().getTime() < 1263369712000L && merge.second().getTime().getTime() < 1263369712000L) {
				if (!merge.hasConflict()) {
					numNonConflicts++;
					nonConflictingMerges.add(merge);
				} else {
					numWitConflicts++;
				}
			}
		}

		System.out.println("# known merges: " + (numNonConflicts + numWitConflicts));
		System.out.println("# non-conflicting merges: " + numNonConflicts);
		System.out.println("# conflicting merges: " + numWitConflicts);

		int mergeNodeCount = 0;
		for (VCSNodePair pair : nonConflictingMerges) {
			VCSNode p1 = pair.first();
			VCSNode p2 = pair.second();

			// commitsToRun.add(p1.getHex());
			// commitsToRun.add(p2.getHex());
			System.out.println("Node with parents: " + p1.getHex() + " & " + p2.getHex());
			boolean alreadyHit = false;
			for (VCSNode node : graph.getVertices()) {
				if (node.getParents().size() == 2) {
					Vector<VCSNode> parents = new Vector<VCSNode>(node.getParents());

					if ((parents.firstElement().equals(p1) && parents.lastElement().equals(p2)) || (parents.firstElement().equals(p2) && parents.lastElement().equals(p1))) {

						System.out.println("\tMerge: " + node.getHex() + " date: " + node.getTime());
						// this node corresponds to the merge of pair
						if (!alreadyHit) {
							// just don't add it if it's a double hit

							// throw new RuntimeException("XXX: " + node.getHex() + " " +
							// commitsToRun.contains(node.getHex()));

							alreadyHit = true;

							// February 13, 2010
							if (node.getTime().getTime() < 1263369712000L) {
								commitsToRun.add(new Triple(p1.getHex(), p2.getHex(), node.getHex()));
								mergeNodeCount++;
							}
						}
					}
				}
			}
		}

		System.out.println("Commits to run size: " + commitsToRun.size());

		Collections.sort(commitsToRun, new Comparator<Triple>() {
			// this sort order is not meaningful but it at least makes it so we look at the tuples
			// in a consistent order
			// across executions (better for caching etc)
			@Override
			public int compare(Triple o1, Triple o2) {
				return o1._merge.compareTo(o2._merge);
			}
		});

		System.out.println("# non-conflicting merges: " + nonConflictingMerges.size());
		System.out.println("# merge nodes (should be equal to # non-conflicting merges): " + mergeNodeCount);

		return commitsToRun;
	}

	private void runTests(GitGraph graph, Vector<Triple> commitsToRun, String repoPath, String workingPath) {

		_workingPath = workingPath;

		// HashSet<VCSNodePair> knownMerges = graph.getKnownMerges();
		//
		// HashSet<VCSNodePair> nonConflictingMerges = new HashSet<VCSNodePair>();
		// int numNonConflicts = 0;
		// for (VCSNodePair merge : knownMerges) {
		// if (!merge.hasConflict()) {
		// numNonConflicts++;
		// nonConflictingMerges.add(merge);
		// }
		// }
		//
		// System.out.println("# non-conflicting merges: " + numNonConflicts);
		// System.out.println("# conflicting merges: " + (knownMerges.size() - numNonConflicts));

		// figure out the final tip hex (for gcd.reset())
		String tmpHex = null;
		long lastTime = 0;
		for (VCSNode node : graph.getVertices()) {
			if (node.getTime().getTime() > lastTime) {
				tmpHex = node.getHex();
				lastTime = node.getTime().getTime();
			}
		}
		_tipHex = tmpHex;
		System.out.println("tipHex: " + _tipHex);

		// Vector<Triple> commitsToRun = new Vector<Triple>();
		//
		// int mergeNodeCount = 0;
		// for (VCSNodePair pair : nonConflictingMerges) {
		// GitNode p1 = pair.first();
		// GitNode p2 = pair.second();
		//
		// // commitsToRun.add(p1.getHex());
		// // commitsToRun.add(p2.getHex());
		// System.out.println("Node with parents: " + p1.getHex() + " & " + p2.getHex());
		// boolean alreadyHit = false;
		// for (GitNode node : graph.getNodes().values()) {
		// if (node.getParents().size() == 2) {
		// Vector<GitNode> parents = new Vector<GitNode>(node.getParents());
		//
		// if ((parents.firstElement().equals(p1) && parents.lastElement().equals(p2))
		// || (parents.firstElement().equals(p2) && parents.lastElement().equals(p1))) {
		//
		// System.out.println("\tMerge: " + node.getHex() + " date: " + node.getWhen());
		// // this node corresponds to the merge of pair
		// if (!alreadyHit) {
		// // just don't add it if it's a double hit
		//
		// // throw new RuntimeException("XXX: " + node.getHex() + " " +
		// // commitsToRun.contains(node.getHex()));
		//
		// alreadyHit = true;
		// commitsToRun.add(new Triple(p1.getHex(), p2.getHex(), node.getHex()));
		// mergeNodeCount++;
		// }
		// }
		// }
		// }
		// }
		// System.out.println("# non-conflicting merges: " + nonConflictingMerges.size());
		// System.out.println("# merge nodes (should be equal to # non-conflicting merges): " +
		// mergeNodeCount);

		_gcd = new GitConflictDetector(repoPath, workingPath);

		_testPath = workingPath + "dist/junit-reports/";
		// _testWritePath = "/Users/rtholmes/tmp/git-test/";

		HashSet<String> hexes = new HashSet<String>();
		for (Triple t : commitsToRun) {
			hexes.add(t._parentA);
			hexes.add(t._parentB);
			hexes.add(t._merge);
		}
		System.out.println("# of merge sets to consider: " + commitsToRun.size());
		System.out.println("# of unique commits to run tests on: " + hexes.size());

		int remainingRuns = 0;

		for (Triple t : commitsToRun) {
			System.out.println("\nProcessing triple: " + remainingRuns + " of " + commitsToRun.size());

			HashSet<TestResult> results = null;

			runTests(t._parentA);
			runTests(t._parentB);
			runTests(t._merge);
			remainingRuns++;
		}
	}

	final Vector<String> _badHexes = new Vector<String>();
	final Stack<String> _currentHex = new Stack<String>();

	private void runTests(final String hex) {
		// if (!_badHexes.contains("29a6e72b55c4498242deb84daf7a2b84b93086e5")) {
		// _badHexes.add("29a6e72b55c4498242deb84daf7a2b84b93086e5");
		// _badHexes.add("7e263fe762ab8c6902f6bc543270d32e7dcdfec2");
		// }

		System.out.println("JUnitFSEDriver::runTests - commit id: " + hex + " ( " + TimeUtility.getCurrentLSMRDateString() + " )");
		_currentHex.add(hex);

		final String fName = _testWritePath + Constants.PROJECT + "_TestResults_" + hex + ".xml";

		// skip precomputed tests
		File testFile = new File(fName);
		if (testFile.exists()) {
			System.out.println("\tJUnitFSEDriver::runTests - already computed in: " + fName);
			return;
		}

		final Thread actionThread = new Thread() {
			@Override
			public void run() {
				System.out.println("\tJUnitFSEDriver::runTests - action thread starting");
				try {
					_gcd.reset(_tipHex);
					_gcd.checkout(hex);
					GitConflictDetector.execute("ant clean", _workingPath);

					// skip hexes that are killing our tests (e.g. go infinite)
					if (!_badHexes.contains(hex)) {
						GitConflictDetector.execute("ant junit", _workingPath);
						HashSet<TestResult> results = JUnitAnalyzer.parseTestResults(_testPath);
						JUnitAnalyzer.writeTestResults(fName, Constants.PROJECT, hex, results, false);
					} else {
						System.out.println("\tSkipping junit stuff as this is a known bad hex");
						JUnitAnalyzer.writeTestResults(fName, Constants.PROJECT, hex, new HashSet<TestResult>(), true);
					}

					// we're done, get the thread out of there
					// monitorThread.interrupt();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("\tJUnitFSEDriver::runTests - action thread exiting");
			}
		};

		Thread monitorThread = new Thread() {
			// 15 minutes
			final int timeout = 15 * 60 * 1000;

			// final int timeout = 10000;

			@Override
			public void run() {
				System.out.println("\tJUnitFSEDriver::runTests - monitor thread starting");
				try {

					String currentlyRunning = _currentHex.peek();
					sleep(timeout);

					// is the same thing executing? if so, kill it and write a blank
					if (!_currentHex.isEmpty() && _currentHex.peek().equals(currentlyRunning)) {
						System.err.println("Hex must be bad: " + currentlyRunning + " we should terminate this process.");

						_badHexes.add(currentlyRunning);

						// write a blank set; maybe specify infinite
						HashSet<TestResult> results = new HashSet<TestResult>();
						JUnitAnalyzer.writeTestResults(fName, Constants.PROJECT, hex, results, true);

						// kill the execution thread
						// this is bad, but it'll have to do for now
						System.out.println("\tJUnitFSEDriver::runTests - Stopping the action thread: " + TimeUtility.getCurrentLSMRDateString());
						actionThread.stop();
					}
				} catch (InterruptedException e) {
					// exit
					// don't bother writing this to screen, it's all good
					System.out.println("\tJUnitFSEDriver::runTests - monitor interrupted; exiting");
				}

				System.out.println("\tJUnitFSEDriver::runTests - monitor thread exiting");
			}

		};

		actionThread.start();
		monitorThread.start();

		try {
			System.out.println("JUnitFSEDriver::runTests - waiting for join");
			actionThread.join();
			System.out.println("JUnitFSEDriver::runTests - join done");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		_currentHex.pop();
		monitorThread.interrupt();
		System.out.println("JUnitFSEDriver::runTests - done");
	}

	class Triple {
		public String _parentA;
		public String _parentB;
		public String _merge;

		Triple(String parentA, String parentB, String merge) {
			_parentA = parentA;
			_parentB = parentB;
			_merge = merge;
		}
	}

	class NodeProperties {
		String labelA = null;
		String labelB = null;
		String labelC = null;
		String shape;
		String colour;
		String border;

	}

	class MergeTestResults {
		String mergeHex = "";
		String aHex = "";
		String bHex = "";

		int passedInBothParentsAndMerge = 0;
		int failedInBothParentsAndMerge = 0;

		int failedInBothParentsButPassedInMerge = 0;
		int passedInBothParentsButFailedInMerge = 0;

		int passedInParentAButFailedInMerge = 0;
		int passedInParentBButFailedInMerge = 0;

		int failedInParentAButPassedInMerge = 0;
		int failedInParentBButPassedInMerge = 0;

		public String toString() {
			return "MergeTestResults for: " + mergeHex + " ( parents: " + aHex + ", " + bHex + " )";
		}
	}

}
