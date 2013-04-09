package edu.washington.cse.se.speculation.scm.generators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.freehg.hgkit.core.ChangeLog;
import org.freehg.hgkit.core.NodeId;
import org.freehg.hgkit.core.Repository;
import org.freehg.hgkit.core.RevlogEntry;
import org.freehg.hgkit.core.ChangeLog.ChangeSet;

import edu.washington.cse.se.speculation.scm.VCSEdge;
import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.git.Constants;
import edu.washington.cse.se.speculation.scm.git.VCSNodePair;
import edu.washington.cse.se.speculation.scm.hg.HgNode;
import edu.washington.cse.se.speculation.util.Assert;

public class HgGraphGenerator extends BaseGraphGenerator {

	String _repoDir;

	public HgGraphGenerator(String repoDir) {
		_repoDir = repoDir;
	}

	public VCSGraph generateGraph() {
		System.out.println("HgGraphGenerator::generateGraph() - start");

		Repository repo = new Repository(_repoDir);
		ChangeLog subject = repo.getChangeLog();

		List<ChangeSet> revisions = subject.getLog();

		final ChangeSet initialChangeSet = revisions.get(0);

		ArrayList<RevlogEntry> index = repo.getChangeLog().getIndex();
		System.out.println("revlog index size: " + index.size());

		System.out.println("# change sets: " + revisions.size());

		VCSGraph graph = new VCSGraph();

		Hashtable<String, VCSNode> nodeMap = new Hashtable<String, VCSNode>();

		// verticies first
		for (ChangeSet cs : revisions) {
			int revision = cs.getRevision();

			System.out.println("\tCS: " + cs.getChangeId().asFull() + " " + cs.getWhen() + " " + cs.getAuthor());
			VCSNode node = new HgNode(cs.getChangeId().asFull(), cs.getAuthor(), cs.getWhen());

			graph.addVertex(node);
			nodeMap.put(node.getHex(), node);
		}

		// edges second
		for (ChangeSet cs : revisions) {
			VCSNode childNode = nodeMap.get(cs.getChangeId().asFull());
			Assert.assertTrue(childNode != null);

			int revision = cs.getRevision();
			RevlogEntry rle = index.get(revision);
			// System.out.println("\t" + rle);

			if (rle != null) {
				System.out.println("\tCommit: " + childNode.getHex() + " Parent 1: " + rle.getFirstParent().getId().asShort() + "\tParent 2: "
						+ rle.getSecondParent().getId().asShort());
			} else {
				System.err.println("RLE NULL");
			}

			NodeId firstParentId = rle.getFirstParent().getId();
			NodeId secondParentId = rle.getSecondParent().getId();

			if (!firstParentId.equals(RevlogEntry.nullInstance.getId())) {
				VCSNode parentNode = nodeMap.get(firstParentId.asFull());
				Assert.assertTrue(parentNode != null, "parentHex: " + firstParentId.asFull());

				childNode.addParent(parentNode);
				VCSEdge edge = new VCSEdge(parentNode, childNode);
				graph.addEdge(edge, edge.getParent(), edge.getChild());
			}

			if (!secondParentId.equals(RevlogEntry.nullInstance.getId())) {
				VCSNode parentNode = nodeMap.get(secondParentId.asFull());
				Assert.assertTrue(parentNode != null, "parentHex: " + firstParentId.asFull());

				childNode.addParent(parentNode);
				VCSEdge edge = new VCSEdge(parentNode, childNode);
				graph.addEdge(edge, edge.getParent(), edge.getChild());
			}
		}

		setToTimes(graph);

		graph.setKnownMerges(getActualMerges(graph));
		graph.setSpeculativeMerges(getSpeculativeMerges(graph));

		System.out.println("HgGraphGenerator::generateGraph() - done.");

		return graph;
	}

//	private void setToTimes(VCSGraph graph) {
//
//		// First go through all the nodes and set each parent's
//		// toTime to the fromTime of the latest child
//		for (VCSNode vcsNode : graph.getVertices()) {
//			HgNode node = (HgNode) vcsNode;
//			for (VCSNode parent : node.getParents()) {
//				HgNode parentHg = (HgNode) parent;
//				if (parentHg.getToTime() < node.getFromTime())
//					parentHg.setToTime(node.getFromTime());
//			}
//		}
//
//		for (VCSNode vcsNode : graph.getVertices()) {
//			if (vcsNode.getParents().size() == 0) {
//				System.out.println("Head Node: " + vcsNode);
//			}
//
//		}
//
//		// Now find the one node that had no children (head) and
//		// set its toTime to the current time.
//		// Throw an exception if you find more than one head.
//		// XXX: had to change this for Hg but shouldn't have!
//		boolean foundHead = false;
//		for (VCSNode vcsNode : graph.getVertices()) {
//			HgNode node = (HgNode) vcsNode;
//			if (node.getToTime() == 0) {
//				System.out.println("head? " + node);
//				node.setToTime(System.currentTimeMillis());
//				if (foundHead)
//					throw new RuntimeException("Found more than one head (node without any children)");
//				foundHead = true;
//			}
//		}
//
//		// boolean foundHead = false;
//		// for (VCSNode vcsNode : graph.getVertices()) {
//		// HgNode node = (HgNode) vcsNode;
//		// if (node.getParents().size() == 0) {
//		// node.setToTime(System.currentTimeMillis());
//		// if (foundHead)
//		// throw new RuntimeException("Found more than one head (node without any children)");
//		// foundHead = true;
//		// }
//		// }
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// String project = "hgkit";
//		String project = "checker-framework";
		String project = "jdk";

		String repoDir = "/Users/rtholmes/tmp/hg-tmp/" + project;
		HgGraphGenerator hggg = new HgGraphGenerator(repoDir);

		VCSGraph graph = hggg.generateGraph();

		HashSet<String> committers = new HashSet<String>();
		for (VCSNode node : graph.getVertices()) {
			committers.add(node.getCommitter());
		}
		System.out.println("Committers: " + committers.size());

		System.out.println("Commits: " + graph.getVertexCount());

		final HashSet<VCSNodePair> mergePairs = graph.getKnownMerges();
		final HashSet<VCSNodePair> speculativePairs = graph.getSpeculativeMerges();
		System.out.println("Speculative Pairs: " + speculativePairs.size());
		// graph.setSpeculativeMerges(new HashSet<VCSNodePair>());
		System.out.println("Merge Pairs: " + mergePairs.size());

		String outXML = Constants.REPOSITORY_PREFIX + project + "_out_final.xml";
		String outDOT = Constants.REPOSITORY_PREFIX + project + "_out_final.dot";
		VCSGraph.writeXML(outXML, graph);
		VCSGraph.writeDot(outDOT, graph);
	}

}
