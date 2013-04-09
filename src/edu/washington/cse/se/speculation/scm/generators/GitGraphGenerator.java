package edu.washington.cse.se.speculation.scm.generators;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;

import edu.washington.cse.se.speculation.scm.VCSEdge;
import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.git.GitNode;
import edu.washington.cse.se.speculation.util.TimeUtility;

/**
 * @author rtholmes
 * 
 */
public class GitGraphGenerator extends BaseGraphGenerator {

	/**
	 * hex -> node
	 */
	private Hashtable<String, GitNode> _nodes = new Hashtable<String, GitNode>();
	private HashSet<VCSEdge> _edges = new HashSet<VCSEdge>();

	private Repository _repo;

	private boolean _verbose = false;

	/**
	 * @param path
	 */
	public GitGraphGenerator(String path) {
		File gitPath = new File(path);

		if (!gitPath.exists()) {
			// ensure the gitPath exists or nothing will work and we might as well die here
			String msg = "GitGraphGenerator::GitGraphGenerator() - Path: " + path + " does not exist.";
			System.err.println(msg);
			throw new RuntimeException(msg);
		}

		try {

			@SuppressWarnings("rawtypes")
			BaseRepositoryBuilder builder = new BaseRepositoryBuilder();
			builder.setGitDir(gitPath);

			_repo = builder.build();

			String msg = "GitGraphGenerator::GitGraphGenerator() - Path: " + path;
			System.out.println(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public VCSGraph generateGraph() throws Exception {
		final long start = System.currentTimeMillis();
		System.out.println("GitGraphGenerator - Generating GIT graph.");

		final List<RevCommit> commits = new ArrayList<RevCommit>();

		RevWalk walk = new RevWalk(_repo);
		RevWalk argWalk = new RevWalk(_repo);

		if (commits.isEmpty()) {
			final ObjectId head = _repo.resolve(org.eclipse.jgit.lib.Constants.HEAD);
			if (head == null)
				throw new RuntimeException("Cannot resolve " + org.eclipse.jgit.lib.Constants.HEAD);

			commits.add(walk.parseCommit(head));
		}
		for (final RevCommit c : commits) {
			final RevCommit real = argWalk == walk ? c : walk.parseCommit(c);
			if (c.has(RevFlag.UNINTERESTING))
				walk.markUninteresting(real);
			else
				walk.markStart(real);
		}

		final int numNodes = walkLoop(walk);

		VCSGraph vcsGraph = new VCSGraph();
		// gitGraph.setNodes(nodes);
		for (VCSNode node : _nodes.values()) {
			vcsGraph.addVertex(node);
		}

		for (VCSEdge edge : _edges) {
			vcsGraph.addEdge(edge, edge.getParent(), edge.getChild());
		}

		setToTimes(vcsGraph);

		final long end = System.currentTimeMillis();
		System.out.println("GitGraphGenerator - Graph Generated. " + numNodes + " commits identified in: " + TimeUtility.msToHumanReadableDelta(start));

		// create graph
		// GitGraph gitGraph = new GitGraph();
		// gitGraph.setNodes(_nodes);
		// gitGraph.setKnownMerges(getActualMerges());
		// gitGraph.setSpeculativeMerges(getSpeculativeMerges());

		vcsGraph.setKnownMerges(getActualMerges(vcsGraph));
		vcsGraph.setSpeculativeMerges(getSpeculativeMerges(vcsGraph));

		return vcsGraph;
	}

	private GitNode getNode(RevCommit commit) {
		String hash = commit.getName();

		if (!_nodes.containsKey(hash)) {
			// _nodes.put(hash, new GitNode(commit));
			_nodes.put(hash, new GitNode(commit.getName(), commit.getCommitterIdent().getName(), commit.getCommitterIdent().getWhen()));
		}
		return _nodes.get(hash);
	}

	/**
	 * @param walk
	 * @return foo
	 * @throws Exception
	 */
	private int walkLoop(RevWalk walk) throws Exception {
		int n = 0;
		for (final RevCommit c : walk) {
			n++;

			if (_verbose) {
				System.out.println("Commit: " + c.getName());
				System.out.println("\tCommitter: " + c.getAuthorIdent().getName());
				System.out.println("\tDate: " + c.getAuthorIdent().getWhen());
			}

			GitNode child = getNode(c);

			RevCommit[] parents = c.getParents();
			// if (parents.length > 1){
			//
			// Map<String, Ref> foo = _repo.getAllRefs();
			// String br = _repo.getBranch();
			//
			// System.out.println("branch: "+_repo.mapCommit(parents[0].getName()).getRepository().getFullBranch());
			// System.out.println("branch: "+_repo.mapCommit(parents[1].getName()).getRepository().getFullBranch());
			//
			// System.out.println("");
			// }
			for (RevCommit parentCommit : parents) {
				if (_verbose) {
					System.out.println("\tParent Commit: " + parentCommit.getName());
				}
				GitNode parent = getNode(parentCommit);

				child.addParent(parent);

				VCSEdge edge = new VCSEdge(parent, child);
				_edges.add(edge);

			}
		}

		return n;
	}

	public static void main(String[] args) {

		String gitPath = "/Users/rtholmes/dev/git-repositories-for-speculation/git/.git";
		try {
			GitGraphGenerator graphGenerator = new GitGraphGenerator(gitPath);
			VCSGraph vcsGraph;

			vcsGraph = graphGenerator.generateGraph();

			String xmlPath = "git" + "_out_tmp.xml";
			VCSGraph.writeXML(xmlPath, vcsGraph);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
