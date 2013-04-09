package edu.washington.cse.se.speculation.scm.generators;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import edu.washington.cse.se.speculation.scm.VCSExtendedNode;
import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.git.VCSNodePair;
import edu.washington.cse.se.speculation.util.TimeUtility;

public class BaseGraphGenerator {

	protected HashSet<VCSNodePair> getActualMerges(VCSGraph graph) {
		HashSet<VCSNode> merges = new HashSet<VCSNode>();
		final long start = System.currentTimeMillis();
		System.out.println("BaseGraphGenerator::getActualMerges() - starting");

		for (VCSNode node : graph.getVertices()) {
			Collection<VCSNode> parents = node.getParents();
			if (parents.size() > 1) {
				// System.out.println("\tMerge found: " + node);
				merges.add(node);
			}
		}
		System.out.println("BaseGraphGenerator::getActualMerges() - done. " + merges.size() + " merges found; took: " + TimeUtility.msToHumanReadableDelta(start));

		HashSet<VCSNodePair> mergePairs = new HashSet<VCSNodePair>();
		for (final VCSNode node : merges) {
			VCSNode[] nodes = new VCSNode[2];
			nodes = node.getParents().toArray(nodes);

			VCSNodePair mergePair = new VCSNodePair(nodes[0], nodes[1]);
			mergePairs.add(mergePair);
		}

		return mergePairs;
	}

	protected HashSet<VCSNodePair> getSpeculativeMerges(VCSGraph graph) {
		HashSet<VCSNodePair> answer = new HashSet<VCSNodePair>();
		final long start = System.currentTimeMillis();
		System.out.println("BaseGraphGenerator::getSpeculativeMerges() - starting");

		HashMap<Long, VCSExtendedNode> timeToNodeMap = new HashMap<Long, VCSExtendedNode>();
		TreeSet<Long> commitTimes = new TreeSet<Long>();
		for (VCSNode vcsNode : graph.getVertices()) {
			VCSExtendedNode node = (VCSExtendedNode) vcsNode;
			timeToNodeMap.put(node.getFromTime(), node);
			commitTimes.add(node.getFromTime());
		}

		System.out.println("\tBaseGraphGenerator::getSpeculativeMerges() - timeToNodeMap generated in: " + TimeUtility.msToHumanReadableDelta(start));

		System.out.println("\tBaseGraphGenerator::getSpeculativeMerges() - first timestamp: " + TimeUtility.formatLSMRDate(new Date(commitTimes.first())));
		System.out.println("\tBaseGraphGenerator::getSpeculativeMerges() - last timestamp: " + TimeUtility.formatLSMRDate(new Date(commitTimes.last())));

		int totalNodes = commitTimes.size();
		int processedNodes = 0;
		boolean overThreshold = false;
		for (Iterator<Long> i = commitTimes.iterator(); i.hasNext();) {

			if (overThreshold) {
				break;
			} else {
				long currentCommitTime = i.next();
				VCSExtendedNode currentNode = timeToNodeMap.get(currentCommitTime);
				for (VCSExtendedNode node : nodesAtTime(currentCommitTime, graph)) {
					if (node != currentNode) {
						answer.add(new VCSNodePair(currentNode, node));

						if (answer.size() % 10000 == 0) {
							System.out.println("\tCalculating speculative merges: " + answer.size() + ". Currently at: " + TimeUtility.formatLSMRDate(new Date(currentCommitTime))
									+ " ( " + processedNodes + " of " + totalNodes + " )");
						}

						// the kill switch
						// final int KILL_SWITCH = 250000;
						final int KILL_SWITCH = 100000000;
						if (answer.size() > KILL_SWITCH) {
							System.out.println("BaseGraphGenerator::getSpeculativeMerges() - Exceeded threshold; speculative merges dropped.");
							answer.clear();
							overThreshold = true;
							break;
						}
					}
				}
				processedNodes++;
			}
		}

		if (!overThreshold) {
			System.out.println("BaseGraphGenerator::getSpeculativeMerges() - done. " + answer.size() + " merges found; took: " + TimeUtility.msToHumanReadableDelta(start));
		}
		return answer;
	}

	private HashSet<VCSExtendedNode> nodesAtTime(long atTime, VCSGraph graph) {
		HashSet<VCSExtendedNode> answer = new HashSet<VCSExtendedNode>();
		for (VCSNode vcsNode : graph.getVertices()) {
			VCSExtendedNode node = (VCSExtendedNode) vcsNode;
			if (node.during(atTime))
				answer.add(node);
		}
		return answer;
	}

	protected void setToTimes(VCSGraph graph) {

		// First go through all the nodes and set each parent's
		// toTime to the fromTime of the latest child
		for (VCSNode vcsNode : graph.getVertices()) {

			VCSExtendedNode node = (VCSExtendedNode) vcsNode;

			for (VCSNode parent : vcsNode.getParents()) {

				// HgNode parentHg = (HgNode) parent;
				VCSExtendedNode parentNode = (VCSExtendedNode) parent;
				if (parentNode.getToTime() < node.getFromTime())
					parentNode.setToTime(node.getFromTime());
			}
		}

		for (VCSNode vcsNode : graph.getVertices()) {
			if (vcsNode.getParents().size() == 0) {
				System.out.println("Head Node: " + vcsNode);
			}

		}

		// Now find the one node that had no children (head) and
		// set its toTime to the current time.
		// Throw an exception if you find more than one head.
		// XXX: had to change this for Hg but shouldn't have!
		boolean foundHead = false;
		for (VCSNode vcsNode : graph.getVertices()) {
			VCSExtendedNode node = (VCSExtendedNode) vcsNode;
			if (node.getToTime() == 0) {
				System.out.println("head? " + node);
				node.setToTime(System.currentTimeMillis());
				if (foundHead)
					throw new RuntimeException("Found more than one head (node without any children)");
				foundHead = true;
			}
		}

		// boolean foundHead = false;
		// for (VCSNode vcsNode : graph.getVertices()) {
		// HgNode node = (HgNode) vcsNode;
		// if (node.getParents().size() == 0) {
		// node.setToTime(System.currentTimeMillis());
		// if (foundHead)
		// throw new RuntimeException("Found more than one head (node without any children)");
		// foundHead = true;
		// }
		// }
	}

}
