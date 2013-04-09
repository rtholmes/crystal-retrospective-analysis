package edu.washington.cse.se.speculation.scm.git;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

import edu.washington.cse.se.speculation.scm.VCSBaseNode;
import edu.washington.cse.se.speculation.scm.VCSEdge;
import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.util.XMLTools;

/**
 * 
 * Parses and analyzes the xml files that contain information about textual, build, and test failures.
 * 
 * Example XML looks like this:
 * 
 * 
 * <commit id='b51ad4314078298194d23d46e2b4473ffd32a88a' ts='1113851520000' tsH='2005-04-18_19:12:00' dev='Linus Torvalds'> <parents mergeConflict='true'> <parent
 * id='a4b7dbef4ef53f4fffbda0a6f5eada4c377e3fc5'/> <parent id='b5039db6d25ae25f1cb2db541ed13602784fafc3'/> </parents> <dyn> <check code='0'/> <clean code='2'/> <config code='2'/>
 * <build code='2'/> <testClean code='999'/> <testPatch code='999'/> <testAll code='999'/> </dyn> </commit>
 * 
 * @author rtholmes
 * 
 */
public class HigherOrderDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String path = "../../data/higherOrder/";
		// String fName = path + "git_dynamic-results-with-conflicts_june22-2011.xml";
		String fName = path + "perl_dynamic-results-with-conflicts_apr26-2011.xml";

		VCSGraph myGraph = readXML(fName);

		long lowerBound = 0L;
		long upperBound = 1263369712000L;

		// Different lower bounds due to different points when the test harnesses started working
		if (fName.contains("git_")) {
			lowerBound = 1214425162000L;
		} else if (fName.contains("perl_")) {
			lowerBound = 1023712292000L;
		} else {
			System.err.println("Unknown system being parsed");
		}

		System.out.println("Lower bound: " + lowerBound + "; upper bound: " + upperBound);

		Vector<VCSNode> nodesToConsider = new Vector<VCSNode>();
		for (VCSNode node : myGraph.getVertices()) {
			if (node.getTime().getTime() >= lowerBound && node.getTime().getTime() <= upperBound) {
				nodesToConsider.add(node);
			}
		}

		Collections.sort(nodesToConsider, new Comparator<VCSNode>() {
			@Override
			public int compare(VCSNode n0, VCSNode n1) {
				return n0.getTime().compareTo(n1.getTime());
			}
		});

		System.out.println("# nodes in total:\t\t\t\t" + nodesToConsider.size());
		System.out.println("\tTime range from: " + nodesToConsider.firstElement().getTime() + " to: " + nodesToConsider.lastElement().getTime());
		Vector<VCSNode> mergesToConsider = new Vector<VCSNode>();
		for (VCSNode node : nodesToConsider) {
			if (node.getParents().size() == 2) {
				mergesToConsider.add(node);
			}
		}

		System.out.println("# merges in total:\t\t\t\t" + mergesToConsider.size());

		int mergesWithTextualConflicts = 0;
		Iterator<VCSNode> mergeIterator = mergesToConsider.iterator();
		while (mergeIterator.hasNext()) {

			VCSNode merge = mergeIterator.next();

			// this key should exist for any merge but check just to be sure
			boolean hasConflictKey = merge.getDynamicRelationship().containsKey("textualConflict");

			if (!hasConflictKey) {
				throw new RuntimeException("Merge: " + merge.getHex() + " missing textual conflict value");
			}

			// get the textual confilct value
			// 0 means no conflict
			int tConf = merge.getDynamicRelationship().get("textualConflict");

			if (tConf == 0) {
				// no conflict
			} else {
				// conflict
				// in this case we want to ignore conflicting nodes in future stages of the analysis
				mergeIterator.remove();
				mergesWithTextualConflicts++;
			}

		}

		System.out.println("# merges with textual conflicts:\t" + mergesWithTextualConflicts);

		if (fName.contains("git_")) {
			analyze(mergesToConsider, "build", "testAll");
		} else if (fName.contains("perl_")) {
			analyze(mergesToConsider, "build", "test");
		}

	}

	private static void analyze(Vector<VCSNode> mergesToConsider, String buildKey, String testKey) {

		int buildXOld = 0;
		int buildXNew = 0;
		int buildOK = 0;

		int testXOld = 0;
		int testXNew = 0;
		int testOK = 0;

		for (VCSNode merge : mergesToConsider) {
			int buildMerge = Integer.valueOf(merge.getDynamicRelationship().get(buildKey));
			int testMerge = Integer.valueOf(merge.getDynamicRelationship().get(testKey));

			VCSNode parentA = (VCSNode) merge.getParents().toArray()[0];
			VCSNode parentB = (VCSNode) merge.getParents().toArray()[1];

			int buildA = Integer.valueOf(parentA.getDynamicRelationship().get(buildKey));
			int testA = Integer.valueOf(parentA.getDynamicRelationship().get(testKey));

			int buildB = Integer.valueOf(parentB.getDynamicRelationship().get(buildKey));
			int testB = Integer.valueOf(parentB.getDynamicRelationship().get(testKey));

			if (buildMerge == 0) {
				// builds fine
				buildOK++;
			} else if (buildMerge != 0 && (buildA == 0 || buildB == 0)) {
				// builds in one or more parents but not in the child (new build problem)
				buildXNew++;
				System.out.println("{\"" + parentA.getHex() + "\", \"" + parentB.getHex() + "\"}, ");
			} else if (buildMerge != 0 && (buildA != 0 || buildB != 0)) {
				// doesn't build in the child and in at least one parent (persistent build problem)
				buildXOld++;
				System.out.println("{\"" + parentA.getHex() + "\", \"" + parentB.getHex() + "\"}, ");
			} else {
				System.out.println("Unhandled build result condition - merge: " + buildMerge + "; A: " + buildA + "; B: " + buildB);
			}

			if (buildMerge == 0) {
				// don't bother gathering test data when the system doesn't build

				if (testMerge == 0) {
					// tests pass
					testOK++;

				} else if (testMerge != 0 && (testA == 0 || testB == 0)) {
					// tests fail in child but passed in one or more merge parents (new test failure)
					testXNew++;
					System.out.println("{\"" + parentA.getHex() + "\", \"" + parentB.getHex() + "\"}, ");

				} else if (testMerge != 0 && (testA != 0 || testB != 0)) {
					// tests fail in the child and in at least one parent (persistent test problem)
					testXOld++;
					System.out.println("{\"" + parentA.getHex() + "\", \"" + parentB.getHex() + "\"}, ");
				} else {
					System.out.println("Unhandled test result condition - merge: " + testMerge + "; A: " + testA + "; B: " + testB);
				}

			}
		}

		System.out.println("Build Data");
		System.out.println("\tBuildXOld: " + buildXOld);
		System.out.println("\tBuildXNew: " + buildXNew);
		System.out.println("\tBuildXOk: " + buildOK);

		System.out.println("Test Data");
		System.out.println("\tTestXOld: " + testXOld);
		System.out.println("\tTestXNew: " + testXNew);
		System.out.println("\tTestXOk: " + testOK);
	}

	@SuppressWarnings("unchecked")
	public static VCSGraph readXML(String fName) {
		System.out.println("GitFSE2010HigherOrderDriver::readXML( " + fName + " )");

		Document doc = XMLTools.readXMLDocument(fName);

		Hashtable<String, VCSNode> nodes = new Hashtable<String, VCSNode>();
		HashSet<VCSEdge> edges = new HashSet<VCSEdge>();

		for (Element commitElement : (List<Element>) doc.getRootElement().getChildren()) {
			String id = commitElement.getAttributeValue("id");
			long ts = Long.parseLong(commitElement.getAttributeValue("ts"));
			String dev = commitElement.getAttributeValue("dev");

			VCSNode node = new VCSBaseNode(id, dev, new Date(ts));
			nodes.put(id, node);

			Element dynElement = commitElement.getChild("dyn");
			if (dynElement != null) {
				Hashtable<String, Integer> dyn = node.getDynamicRelationship();
				for (Element dChild : (List<Element>) dynElement.getChildren()) {
					String dProperty = dChild.getName();
					String dValueString = dChild.getAttributeValue("code");
					dyn.put(dProperty, Integer.parseInt(dValueString));
				}
			}
		}

		for (Element commitElement : (List<Element>) doc.getRootElement().getChildren()) {
			String id = commitElement.getAttributeValue("id");
			Element parentsElement = commitElement.getChild("parents");
			if (parentsElement != null) {
				VCSNode childNode = nodes.get(id);
				if (parentsElement.getChildren().size() > 2) {
					// silently drop merges of two+ parents right now; the analysis didn't deal with them correctly
				} else {
					boolean textualConflict = Boolean.parseBoolean(parentsElement.getAttributeValue("mergeConflict"));

					if (textualConflict) {
						childNode.getDynamicRelationship().put("textualConflict", 1);
					} else {
						childNode.getDynamicRelationship().put("textualConflict", 0);
					}

					for (Element parentElement : (List<Element>) parentsElement.getChildren()) {
						String parentId = parentElement.getAttributeValue("id");
						VCSNode parentNode = nodes.get(parentId);

						childNode.addParent(parentNode);

						VCSEdge edge = new VCSEdge(parentNode, childNode);
						edges.add(edge);
					}
				}
			}
		}

		VCSGraph vcsGraph = new VCSGraph();
		for (VCSNode node : nodes.values()) {
			vcsGraph.addVertex(node);
		}

		for (VCSEdge edge : edges) {
			vcsGraph.addEdge(edge, edge.getParent(), edge.getChild());
		}
		return vcsGraph;
	}
}
