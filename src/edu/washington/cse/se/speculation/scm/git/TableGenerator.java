package edu.washington.cse.se.speculation.scm.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.washington.cse.se.speculation.scm.VCSEdge;
import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.util.Assert;
import edu.washington.cse.se.speculation.util.TimeUtility;

/**
 * This analysis function takes a directory full of *_out_final.xml files and generates an overview for the file.
 * 
 * @author rtholmes
 * 
 */
public class TableGenerator {

	public static void main(String[] args) throws IOException {
		String basePath = "../../data/repositories/";

		File basePathFile = new File(basePath);
		String[] fNamesToAnalyze = basePathFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String fName) {
				// return fName.endsWith("git_out_final.xml");
				// return fName.endsWith("voldemort_out_final.xml");
				// return fName.endsWith("jquery_out_final.xml");
				// return fName.endsWith("gallery3_out_final.xml");
				// return fName.endsWith("insoshi_out_final.xml");
				// return fName.endsWith("mangos_out_final.xml");

				 return fName.endsWith("_out_final.xml");
			}
		});

		List<String> sortedfNamesToAnalyze = Arrays.asList(fNamesToAnalyze);

		Collections.sort(sortedfNamesToAnalyze, new Comparator<String>() {

			@Override
			public int compare(String arg0, String arg1) {
				return arg0.compareTo(arg1);
			}
		});

		for (String fName : sortedfNamesToAnalyze) {
			try {
				System.out.println("");
				System.out.println("*****");
				System.out.println("");
				runAnalysis(basePath + fName);
			} catch (Exception e) {
				System.err.println("Problem processing file: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	/**
	 * Runs the table generator for the TSE paper
	 * 
	 * @param args
	 */
	public static void runAnalysis(String fName) {
		System.out.println("Analyzing: " + fName);

		VCSGraph myGraph = VCSGraph.readXML(fName);

		long lowerBound = 0L; // full repository
		// long lowerBound = 1214425162000L; // Merge analysis
		long upperBound = 1266105599000L; // Feb 13, 2010

		runAnalysisWithBounds(myGraph, lowerBound, upperBound);
	}

	private static void runAnalysisWithBounds(VCSGraph myGraph, long lowerBound, long upperBound) {

		System.out.println("===");

		Vector<VCSNode> inRangeNodes = new Vector<VCSNode>();

		for (VCSNode vert : myGraph.getVertices()) {

			if (vert.getTime().getTime() >= lowerBound && vert.getTime().getTime() <= upperBound) {

				inRangeNodes.add(vert);

			}
		}

		System.out.println("# commits (total): \t\t\t\t\t" + myGraph.getVertices().size());
		System.out.println("# commits (in range): \t\t\t\t\t" + inRangeNodes.size());
		System.out.println("===");

		Collections.sort(inRangeNodes, new Comparator<VCSNode>() {
			@Override
			public int compare(VCSNode n0, VCSNode n1) {
				return n0.getTime().compareTo(n1.getTime());
			}
		});

		System.out.println("Time range from: \t" + inRangeNodes.firstElement().getTime() + " \tto: " + inRangeNodes.lastElement().getTime());
		System.out.println("First commit: \t\t" + inRangeNodes.firstElement().getHex().substring(0,7) + " \t\t\tto: " + inRangeNodes.lastElement().getHex().substring(0,7));

		HashSet<VCSNodePair> myRealMergePairs = myGraph.getKnownMerges();
		System.out.println("# Known merges (total): \t\t\t\t" + myRealMergePairs.size());
		Iterator<VCSNodePair> myRealMergePairsIt = myRealMergePairs.iterator();
		while (myRealMergePairsIt.hasNext()) {

			VCSNodePair pair = myRealMergePairsIt.next();

			VCSNode commonChildMerge = findClosestCommonChild(myGraph, pair.first(), pair.second());
			VCSNode commonParentMerge = findClosestCommonParent(myGraph, pair.first(), pair.second());

			if (!pairInRange(pair, lowerBound, upperBound) || (commonChildMerge != null && commonChildMerge.getTime().getTime() > upperBound)
					|| (commonParentMerge != null && commonParentMerge.getTime().getTime() < lowerBound)) {
				myRealMergePairsIt.remove();
			}
		}
		System.out.println("# Known merges (in range): \t\t\t" + myRealMergePairs.size());
		int knownConflictsInRange = 0;
		int knownNonConflictsInRange = 0;
		Iterator<VCSNodePair> realIt = myRealMergePairs.iterator();

		System.out.println("TextualOK Details");
		System.out.println("merge\tparentA\tparentB");
		while (realIt.hasNext()) {
			VCSNodePair pair = realIt.next();

			VCSNode commonChildMerge = findClosestCommonChild(myGraph, pair.first(), pair.second());
			VCSNode commonParentMerge = findClosestCommonParent(myGraph, pair.first(), pair.second());

			if (!pairInRange(pair, lowerBound, upperBound) || (commonChildMerge != null && commonChildMerge.getTime().getTime() > upperBound)
					|| (commonParentMerge != null && commonParentMerge.getTime().getTime() < lowerBound)) {
				realIt.remove();
			} else {

				if (pair.hasConflict()) {
					knownConflictsInRange++;
				} else {
					knownNonConflictsInRange++;
					System.out.println(commonChildMerge.getHex() + "\t" + pair.first().getHex() + "\t" + pair.second().getHex());
				}
			}
		}

		HashSet<VCSNodePair> mySpecMergePairs = myGraph.getSpeculativeMerges();
		System.out.println("# Speculative merges (total): \t\t\t" + mySpecMergePairs.size());
		Iterator<VCSNodePair> mySpecMergePairsIt = mySpecMergePairs.iterator();
		while (mySpecMergePairsIt.hasNext()) {
			VCSNodePair pair = mySpecMergePairsIt.next();

			VCSNode commonChildMerge = findClosestCommonChild(myGraph, pair.first(), pair.second());
			VCSNode commonParentMerge = findClosestCommonParent(myGraph, pair.first(), pair.second());

			if (!pairInRange(pair, lowerBound, upperBound) || (commonChildMerge != null && commonChildMerge.getTime().getTime() > upperBound)
					|| (commonParentMerge != null && commonParentMerge.getTime().getTime() < lowerBound)) {
				mySpecMergePairsIt.remove();
			}
		}
		System.out.println("# Speculative merges (in range): \t\t" + mySpecMergePairs.size());

		int knownSpecConflictsInRange = 0;
		int knownSpecNonConflictsInRange = 0;
		for (VCSNodePair merge : mySpecMergePairs) {
			if (merge.hasConflict()) {
				knownSpecConflictsInRange++;
			} else {
				knownSpecNonConflictsInRange++;
			}
		}

		System.out.println("===");

		NumberFormat nf = NumberFormat.getInstance();

		System.out.println("# Known merges (in range): \t\t\t" + (knownConflictsInRange + knownNonConflictsInRange));
		float knownConflictRate = (float) knownConflictsInRange / (knownNonConflictsInRange + knownConflictsInRange);
		System.out.println("\t Conflicting known merges: \t\t" + knownConflictsInRange + " ( " + nf.format(knownConflictRate) + " )");
		System.out.println("\t Clean known merges: \t\t\t" + knownNonConflictsInRange + " ( " + nf.format(1 - knownConflictRate) + " )");
		System.out.println("===");

		System.out.println("# Speculative merges (in range): \t\t" + (knownSpecConflictsInRange + knownSpecNonConflictsInRange));
		float specConflictRate = (float) knownSpecConflictsInRange / (knownSpecNonConflictsInRange + knownSpecConflictsInRange);
		System.out.println("\t Conflicting speculative merges: \t" + knownSpecConflictsInRange + " ( " + nf.format(specConflictRate) + " )");
		System.out.println("\t Clean speculative merges: \t\t" + knownSpecNonConflictsInRange + " ( " + nf.format(1 - specConflictRate) + " )");
		System.out.println("===");

		HashSet<String> developers = new HashSet<String>();
		for (VCSNode v : inRangeNodes) {
			developers.add(v.getCommitter());
		}

		System.out.println("In Range Node Summary:");
		System.out.println("From:\t\t" + inRangeNodes.firstElement().getTime() + " to: " + inRangeNodes.lastElement().getTime());
		long deltaDays = (inRangeNodes.lastElement().getTime().getTime() - inRangeNodes.firstElement().getTime().getTime()) / 1000 / 60 / 60 / 24;
		System.out.println("Total days:\t" + deltaDays);
		System.out.println("# changesets:\t" + inRangeNodes.size());
		System.out.println("# developers:\t" + developers.size());

		System.out.println("===");

		try {
			BufferedWriter outFile = new BufferedWriter(new FileWriter("/tmp/out.csv"));
			outFile.write("Kind,# Commits,Duration (ms), Duration (human)\n");

			for (VCSNodePair merge : myRealMergePairs) {
				try {
					DirectedSparseGraph<VCSNode, VCSEdge> graph = myGraph;

					VCSNode commonChildMerge = findClosestCommonChild(graph, merge.first(), merge.second());
					VCSNode commonParentMerge = findClosestCommonParent(graph, merge.first(), merge.second());

					if (merge.hasConflict()) {
						System.out.println("Conflict merge: " + merge);

						System.out.println("\tCommon Child (post conflict merge): \t\t\t" + commonChildMerge);

						// most recent parent

						System.out.println("\tCommon Parent (common parent before conflict): \t" + commonParentMerge);

						long latestConflictingCommit = Math.max(merge.first().getTime().getTime(), merge.second().getTime().getTime());

						long delta = commonChildMerge.getTime().getTime() - commonParentMerge.getTime().getTime();
						System.out.println("\tDays between split and join: \t\t\t\t\t" + (delta / 1000 / 60 / 60 / 24) + " (max length of time conflict could persist)");

						long delta2 = commonChildMerge.getTime().getTime() - latestConflictingCommit;
						// if the developer tried to push right after this commit they would have received a merge error
						// this time represents the duration required to 'fix' the error (of course, they could
						// have gotten the error and left it for the weekend)
						System.out.println("\tHours between  latest conflicting commit and join: \t" + (delta2 / 1000 / 60 / 60));

						evaluatePersistence(graph, commonParentMerge, commonChildMerge, mySpecMergePairs, myRealMergePairs, outFile, true);
					} else {
						// not conflicting evaluation
						if (commonParentMerge == null || commonChildMerge == null) {
							System.out.println("ERROR: Parent or child out of range for this merge");
						} else {
							evaluatePersistence(graph, commonParentMerge, commonChildMerge, mySpecMergePairs, myRealMergePairs, outFile, false);
						}
					}
				} catch (Exception e) {
					System.out.println("ERROR: " + e.getMessage());
					e.printStackTrace();
				}
			}
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// try {
		// // BufferedWriter outFile = new BufferedWriter(new FileWriter(outputPath));
		// // BufferedWriter outFile = new BufferedWriter(new Writer(System.out));
		// OutputStreamWriter outFile = new OutputStreamWriter(System.out);
		//
		// outFile.write("Start Conflicting\tFinish Conflicting\tConflicting Commit\tAt time\tcommits to merge\tand Commit\tAtTime\tcommits to merge\tMerged Into\tAt time\tLived for\tLived for human\n");
		// //
		// for (ConflictLife conflict : conflicts.values()) {
		// outFile.write(conflict.toString());
		// }
		// outFile.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

	private static void evaluatePersistence(DirectedSparseGraph<VCSNode, VCSEdge> graph, final VCSNode branchOrigin, final VCSNode branchMerge,
			HashSet<VCSNodePair> specMergePairs, HashSet<VCSNodePair> myRealMergePairs, BufferedWriter outFile, boolean conflictExpected) {

		// branchMerge always has two parents
		Assert.assertTrue(branchMerge.getParents().size() == 2, "ERROR: Branch has: " + branchMerge.getParents().size() + " parents (expected 2)");
		Iterator<VCSNode> mergeParentsIt = branchMerge.getParents().iterator();
		final VCSNode branchA = mergeParentsIt.next();
		final VCSNode branchB = mergeParentsIt.next();

		System.out.println("EvaluatePersistence");
		System.out.println("\tOrigin: " + branchOrigin);
		System.out.println("\tMerge: " + branchMerge);
		System.out.println("\t\tBranchA: " + branchA);
		System.out.println("\t\tBranchB: " + branchB);

		Set<VCSNode> nodeAset = new HashSet<VCSNode>();

		getAllPredeccessors(graph, branchA, nodeAset);
		nodeAset.add(branchA);

		Iterator<VCSNode> it = nodeAset.iterator();
		while (it.hasNext()) {
			// remove any parents that are older than the origin
			if (it.next().getTime().before(branchOrigin.getTime())) {
				it.remove();
			}
		}

		Set<VCSNode> nodeBset = new HashSet<VCSNode>();
		getAllPredeccessors(graph, branchB, nodeBset);
		nodeBset.add(branchB);

		it = nodeBset.iterator();
		while (it.hasNext()) {
			// remove any parents that are older than the origin
			if (it.next().getTime().before(branchOrigin.getTime())) {
				it.remove();
			}
		}

		// sort nodes most recent to least recent
		Vector<VCSNode> nodesA = new Vector<VCSNode>(nodeAset);
		Collections.sort(nodesA, new Comparator<VCSNode>() {

			@Override
			public int compare(VCSNode n0, VCSNode n1) {
				if (n0.equals(branchOrigin)) {
					return -1;
				}
				if (n1.equals(branchOrigin)) {
					return 1;
				}
				if (n0.equals(branchA) || n0.equals(branchB)) {
					return 1;
				}
				if (n1.equals(branchA) || n1.equals(branchB)) {
					return -1;
				}
				return n0.getTime().compareTo(n1.getTime());
			}
		});
		Collections.reverse(nodesA);

		// sort nodes most recent to least recent
		Vector<VCSNode> nodesB = new Vector<VCSNode>(nodeBset);
		Collections.sort(nodesB, new Comparator<VCSNode>() {

			@Override
			public int compare(VCSNode n0, VCSNode n1) {
				if (n0.equals(branchOrigin)) {
					return -1;
				}
				if (n1.equals(branchOrigin)) {
					return 1;
				}
				if (n0.equals(branchA) || n0.equals(branchB)) {
					return 1;
				}
				if (n1.equals(branchA) || n1.equals(branchB)) {
					return -1;
				}
				return n0.getTime().compareTo(n1.getTime());
			}
		});
		Collections.reverse(nodesB);

		System.out.println("\tA nodes");
		for (VCSNode n : nodesA) {
			System.out.println("\t\t" + n);
		}

		System.out.println("\tB nodes");
		for (VCSNode n : nodesB) {
			System.out.println("\t\t" + n);
		}

		// sanity check, the branches should start at the real merge predecessor
		// and end at the origin of the two branches

		// NOTE: these don't actually work (due to rebasing two nodes can have the same timestamp
		// which fiddles with collections.sort;

		try {
			// Assert.assertTrue(branchA.equals(nodesA.firstElement()));
			// Assert.assertTrue(branchB.equals(nodesB.firstElement()));
			// Assert.assertTrue(branchOrigin.equals(nodesA.lastElement()));
			// Assert.assertTrue(branchOrigin.equals(nodesB.lastElement()));

			// if both true, there is a known conflict
			boolean mergeConflicts = false;

			boolean realFound = false;
			// check the real pairs first
			for (VCSNodePair pair : myRealMergePairs) {
				if ((pair.first().equals(branchA) && pair.second().equals(branchB)) || pair.first().equals(branchB) && pair.second().equals(branchA)) {
					realFound = true;
					if (pair.conflictSet()) {
						mergeConflicts = pair.hasConflict();
					}
				}
			}

			if (!realFound) {
				for (VCSNodePair pair : specMergePairs) {
					if ((pair.first().equals(branchA) && pair.second().equals(branchB)) || pair.first().equals(branchB) && pair.second().equals(branchA)) {
						if (pair.conflictSet()) {
							mergeConflicts = pair.hasConflict();
						}
					}
				}
			}
			// Assert.assertTrue(mergeCalculated, "We should have always calculated the merge result for every true merge");

			long duration = 0;
			int numCommits = 0;

			if (mergeConflicts) {
				// we are dealing with a textualX

				VCSNodePair p = findOldestConflictingPair(nodesA, nodesB, myRealMergePairs, specMergePairs);

				long conflictStartedTS = Math.max(p.first().getTime().getTime(), p.second().getTime().getTime());
				for (VCSNode n : nodesA) {
					if (n.getTime().getTime() >= conflictStartedTS) {
						numCommits++;
					}
				}
				for (VCSNode n : nodesB) {
					if (n.getTime().getTime() >= conflictStartedTS) {
						numCommits++;
					}
				}
				// add one because the commit that actually fixed the merge isn't included in nodesA or nodesB
				numCommits++;

				duration = branchMerge.getTime().getTime() - conflictStartedTS;

				outFile.write("textualX," + numCommits + "," + duration + "," + TimeUtility.msToHumanReadable(duration) + "\n");
				System.out.println("RESULT: Conflicting merge; # commits: " + numCommits + " time: " + TimeUtility.msToHumanReadable(duration));

				if (!conflictExpected) {
					System.out.println("ERROR: conflict expected here");
				}

			} else {
				// we are dealing with a textualOK
				// -2 is removing the origin node from each of nodesA+nodesB
				numCommits = nodesA.size() + nodesB.size() - 2;

				// add one because the commit that actually fixed the merge isn't included in nodesA or nodesB
				numCommits++;
				duration = branchMerge.getTime().getTime() - branchOrigin.getTime().getTime();

				outFile.write("textualOK," + numCommits + "," + duration + "," + TimeUtility.msToHumanReadable(duration) + "\n");
				System.out.println("RESULT: Clean merge; # commits: " + numCommits + " time: " + TimeUtility.msToHumanReadable(duration));

				if (conflictExpected) {
					System.out.println("ERROR: conflict not expected here");
				}
			}
		} catch (Exception e) {
			// just eat crappy cases for now

			// I don't really know why this is happening, but whenever it does it looks like the git history is really
			// screwed up (e.g., my 'parent' is younger than i am (e.g., ddbba4 in Voldemort)
			// https://github.com/voldemort/voldemort/tree/ddbba4da458264f7bdcbae30443321ca86a283d1
			// git log --pretty -n1 ddbba4da458264f7bdcbae30443321ca86a283d1
			System.out.println("ERROR evaluating persistence for merge: " + branchMerge);
		}

	}

	private static VCSNodePair findOldestConflictingPair(Vector<VCSNode> nodesA, Vector<VCSNode> nodesB, HashSet<VCSNodePair> realMergePairs, HashSet<VCSNodePair> specMergePairs) {
		VCSNodePair oldestConflict = null;

		Vector<VCSNode> workNodesA = new Vector<VCSNode>(nodesA);
		Vector<VCSNode> workNodesB = new Vector<VCSNode>(nodesB);

		oldestConflict = new VCSNodePair(workNodesA.firstElement(), workNodesB.firstElement());

		boolean done = false;
		while (!done) {
			VCSNode nodeA = workNodesA.firstElement();
			VCSNode nodeB = workNodesB.firstElement();
			boolean isConflictSet = isConflictSet(nodeA, nodeB, realMergePairs, specMergePairs);
			boolean isConflict = false;
			if (isConflictSet) {
				isConflict = isConflict(nodeA, nodeB, realMergePairs, specMergePairs);
				if (isConflict) {
					oldestConflict = new VCSNodePair(nodeA, nodeB);
				} else {
					// conflict set and not in conflict so we're done
					done = true;
				}
			}

			if (workNodesA.firstElement().getTime().getTime() > workNodesB.firstElement().getTime().getTime()) {
				// nodeA older than nodeB; roll back the older of the pair
				workNodesA.remove(0);
			} else {
				workNodesB.remove(0);
			}
			if (workNodesA.size() < 1 || workNodesB.size() < 1) {
				done = true;
			}
		}
		return oldestConflict;
	}

	private static boolean isConflictSet(VCSNode elemA, VCSNode elemB, HashSet<VCSNodePair> knownPairs, HashSet<VCSNodePair> specPairs) {
		for (VCSNodePair pair : knownPairs) {
			if ((pair.first().equals(elemA) && pair.second().equals(elemB)) || pair.first().equals(elemB) && pair.second().equals(elemA)) {
				return pair.conflictSet();
			}
		}

		for (VCSNodePair pair : specPairs) {
			if ((pair.first().equals(elemA) && pair.second().equals(elemB)) || pair.first().equals(elemB) && pair.second().equals(elemA)) {
				return pair.conflictSet();
			}
		}
		return false;
	}

	private static boolean isConflict(VCSNode elemA, VCSNode elemB, HashSet<VCSNodePair> realPairs, HashSet<VCSNodePair> specPairs) {
		for (VCSNodePair pair : realPairs) {
			if ((pair.first().equals(elemA) && pair.second().equals(elemB)) || pair.first().equals(elemB) && pair.second().equals(elemA)) {
				if (pair.conflictSet()) {
					return pair.hasConflict();
				}
			}
		}

		for (VCSNodePair pair : specPairs) {
			if ((pair.first().equals(elemA) && pair.second().equals(elemB)) || pair.first().equals(elemB) && pair.second().equals(elemA)) {
				if (pair.conflictSet()) {
					return pair.hasConflict();
				}
			}
		}
		Assert.assertTrue(false, "Don't ask isConflict if isConflictSet not true");
		return false;
	}

	private static VCSNode findClosestCommonParent(DirectedSparseGraph<VCSNode, VCSEdge> graph, VCSNode first, VCSNode second) {
		Set<VCSNode> firstParents = new HashSet<VCSNode>();
		Set<VCSNode> secondParents = new HashSet<VCSNode>();

		getAllPredeccessors(graph, first, firstParents);
		getAllPredeccessors(graph, second, secondParents);

		SetView<VCSNode> allParents = Sets.intersection(firstParents, secondParents);

		Vector<VCSNode> sortedParents = new Vector<VCSNode>(allParents);

		Collections.sort(sortedParents, new Comparator<VCSNode>() {
			@Override
			public int compare(VCSNode arg0, VCSNode arg1) {
				return arg0.getTime().compareTo(arg1.getTime());
			}
		});

		if (sortedParents.size() > 0) {
			// for debugging
			// for (VCSNode n : sortedParents) {
			// if (n.getHex().startsWith("788a0")) {
			// System.out.print("");
			// }
			// }
			return sortedParents.lastElement();
		}

		return null;
	}

	/**
	 * Finds the origin node for two nodes on different paths
	 * 
	 * @param graph
	 * @param first
	 * @param second
	 * @return
	 */
	private static VCSNode findClosestCommonChild(DirectedSparseGraph<VCSNode, VCSEdge> graph, VCSNode first, VCSNode second) {

		// try doing this the dumb way first
		for (VCSNode node : graph.getVertices()) {
			if (node.getParents().contains(first) && node.getParents().contains(second)) {
				return node;
			}
		}

		Set<VCSNode> firstChildren = new HashSet<VCSNode>();
		Set<VCSNode> secondChildren = new HashSet<VCSNode>();

		getAllSuccessors(graph, first, firstChildren);
		getAllSuccessors(graph, second, secondChildren);

		SetView<VCSNode> allChildren = Sets.intersection(firstChildren, secondChildren);

		Vector<VCSNode> sortedChildren = new Vector<VCSNode>(allChildren);

		Collections.sort(sortedChildren, new Comparator<VCSNode>() {
			@Override
			public int compare(VCSNode arg0, VCSNode arg1) {
				return arg0.getTime().compareTo(arg1.getTime());
			}
		});

		if (sortedChildren.size() > 0) {
			// for debugging
			// for (int i = 0; i < sortedChildren.size(); i++) {
			// VCSNode n = sortedChildren.elementAt(i);
			// if (n.getHex().startsWith("ab977")) {
			// System.out.print("");
			// }
			// }
			return sortedChildren.firstElement();
		}

		return null;
	}

	private static void getAllPredeccessors(DirectedSparseGraph<VCSNode, VCSEdge> graph, VCSNode node, Set<VCSNode> results) {
		Collection<VCSNode> nodes = graph.getPredecessors(node);

		for (VCSNode n : nodes) {
			if (!results.contains(n)) {
				results.add(n);
				getAllPredeccessors(graph, n, results);
			}
		}
	}

	private static void getAllSuccessors(DirectedSparseGraph<VCSNode, VCSEdge> graph, VCSNode node, Set<VCSNode> results) {
		Collection<VCSNode> nodes = graph.getSuccessors(node);

		for (VCSNode n : nodes) {
			if (!results.contains(n) && n.getTime().getTime() >= node.getTime().getTime()) {
				results.add(n);
				getAllSuccessors(graph, n, results);
			}
		}
	}

	private static boolean pairInRange(VCSNodePair pair, long lowerBound, long upperBound) {
		return (pair.first().getTime().getTime() >= lowerBound && pair.first().getTime().getTime() <= upperBound && pair.second().getTime().getTime() >= lowerBound && pair
				.second().getTime().getTime() <= upperBound);
	}

	public static class ConflictLife {
		private VCSNode _merge;
		private VCSNode _first, _second;
		private long _length;
		private int _distanceFirst, _distanceSecond;
		private boolean _start, _finish;

		public ConflictLife(VCSNode merge, VCSNode first, VCSNode second, boolean start, boolean finish, DijkstraDistance<VCSNode, VCSEdge> dd) {
			_start = start;
			_finish = finish;
			if (first.getTime().getTime() < second.getTime().getTime()) {
				_first = first;
				_second = second;
			} else {
				_first = second;
				_second = first;
			}
			_merge = merge;

			_length = _merge.getTime().getTime() - _first.getTime().getTime();

			_distanceFirst = dd.getDistance(first, merge).intValue();
			_distanceSecond = dd.getDistance(second, merge).intValue();
		}

		public VCSNode getFirst() {
			return _first;
		}

		public VCSNode getSecond() {
			return _second;
		}

		public VCSNode getMerge() {
			return _merge;
		}

		public long getLength() {
			return _length;
		}

		public String getIDHex() {
			return _merge.getHex();
		}

		public String toString() {
			return "Start: " + _start + "\tFinish: " + _finish + "\t" + "\tFirst time: " + _first.getTime() + "\tFirst Dist: " + _distanceFirst + "\t Second Time: "
					+ _second.getTime() + "\tSecond Dist: " + _distanceSecond + "\tLength: " + _length + "\t" + TimeUtility.msToHumanReadable(_length);// + "\n";
		}
	}

}
