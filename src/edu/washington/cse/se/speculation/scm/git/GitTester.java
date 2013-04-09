package edu.washington.cse.se.speculation.scm.git;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphEdge;
import com.oy.shared.lm.graph.GraphFactory;
import com.oy.shared.lm.graph.GraphNode;
import com.oy.shared.lm.out.GRAPHtoDOT;

/**
 * This class was a simple test routine to make sure our initial Git analysis code was working correctly.
 * 
 * @author rtholmes
 * 
 */
public class GitTester {

	Repository _repo;

	/**
	 * @param dotGitPath
	 */
	public GitTester(String dotGitPath) {
		File gitPath = new File(dotGitPath);
		try {

			@SuppressWarnings("rawtypes")
			BaseRepositoryBuilder builder = new BaseRepositoryBuilder();
			builder.setGitDir(gitPath);

			_repo = builder.build();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Graph _graph;

	private void getLog() throws Exception {

		_graph = GraphFactory.newGraph();

		final List<RevCommit> commits = new ArrayList<RevCommit>();

		Repository db = _repo;
		RevWalk walk = new RevWalk(db);
		RevWalk argWalk = new RevWalk(db);

		if (commits.isEmpty()) {
			final ObjectId head = db.resolve(Constants.HEAD);
			if (head == null)
				new RuntimeException("Cannot resolve " + Constants.HEAD);
			commits.add(walk.parseCommit(head));
		}
		for (final RevCommit c : commits) {
			final RevCommit real = argWalk == walk ? c : walk.parseCommit(c);
			if (c.has(RevFlag.UNINTERESTING))
				walk.markUninteresting(real);
			else
				walk.markStart(real);
		}

		final long start = System.currentTimeMillis();
		final int n = walkLoop(walk);
		// if (count) {
		if (true) {
			final long end = System.currentTimeMillis();
			System.err.print(n);
			System.err.print(' ');
			System.err.print(end - start);
			System.err.print(" ms");
			System.err.println();
		}

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

			boolean display = true;
			if (display) {
				System.out.println("Commit: " + c.getName());
				System.out.println("\tCommitter: " + c.getAuthorIdent().getName());
				System.out.println("\tDate: " + c.getAuthorIdent().getWhen());
			}
			// show(c);

			GitNode child = getNode(c);

			RevCommit[] parents = c.getParents();
			for (RevCommit parentCommit : parents) {
				if (display) {
					System.out.println("\tParent Commit: " + parentCommit.getName());
				}
				GitNode parent = getNode(parentCommit);

				GraphEdge ab = _graph.addEdge(parent._node, child._node);
				// ab.getInfo().setCaption("");
			}
		}

		// if (walk instanceof ObjectWalk) {
		// final ObjectWalk ow = (ObjectWalk) walk;
		// for (;;) {
		// final RevObject obj = ow.nextObject();
		// if (obj == null)
		// break;
		//
		// System.out.println("Show: " + ow);
		// // show(ow, obj);
		// }
		// }
		return n;
	}

	private GitNode getNode(RevCommit commit) {
		String hash = commit.getName();

		if (!_nodes.containsKey(hash)) {
			_nodes.put(hash, new GitNode(commit));
		}
		return _nodes.get(hash);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String jGitPath = "/Users/rtholmes/tmp/jgit/jgit/.git/";
		String voldPath = "/Users/rtholmes/tmp/git-test/voldemort/.git";
		String testPath = "/Users/rtholmes/tmp/git-test/TestProject/.git";
		String linuxPath = "/Users/rtholmes/tmp/git-test/linux-2.6/.git";

		String path = testPath;
		GitTester gt = new GitTester(path);
		try {
			gt.getLog();
			gt.writeDot();
			gt.testMerges();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void writeDot() {
		String outFolder = "/Users/rtholmes/tmp/";
		final String dotFileName = outFolder + "linuxNoLabels.dot";

		try {
			GRAPHtoDOT.transform(_graph, new FileOutputStream(new File(dotFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void draw() {
		Graph graph = GraphFactory.newGraph();
		graph.getInfo().setCaption("My test drawing");

		GraphNode f = graph.addNode();
		f.getInfo().setCaption("father");
		f.getInfo().setFillColor("cyan");

		GraphNode m = graph.addNode();
		m.getInfo().setCaption("mother");
		m.getInfo().setFillColor("pink");

		GraphNode c = graph.addNode();
		c.getInfo().setCaption("child");
		m.getInfo().setFillColor("yellow");

		GraphEdge ab = graph.addEdge(f, c);
		ab.getInfo().setCaption("parent");

		GraphEdge bc = graph.addEdge(m, c);
		bc.getInfo().setCaption("parent");

		// output graph to *.gif
		String outFolder = "/Users/rtholmes/tmp/";
		final String dotFileName = outFolder + "SimpleDraw.dot";
		final String gifFileName = outFolder + "SimpleDraw.gif";

		try {
			GRAPHtoDOT.transform(graph, new FileOutputStream(new File(dotFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	Hashtable<String, GitNode> _nodes = new Hashtable<String, GitNode>();

	void testMerges() {
		GitNode gn1 = _nodes.get("ddde063134b44cf3ff2d4765aae706c094c9aabf");
		GitNode gn2 = _nodes.get("ddde063134b44cf3ff2d4765aae706c094c9aabf");

		AnyObjectId[] tips = new AnyObjectId[2];

		tips[0] = gn1._commit;
		tips[1] = gn2._commit;

		MergeStrategy ms = MergeStrategy.get(MergeStrategy.OURS.getName());

		Merger m = ms.newMerger(_repo);

		try {
			boolean conflict = m.merge(tips);
			System.out.println("Conflict: " + conflict);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	class GitNode {

		String _hash;

		String _name;

		GraphNode _node;

		RevCommit _commit;

		protected GitNode(RevCommit commit) {

			_hash = commit.getName();
			_name = commit.getCommitterIdent().getName();

			_node = _graph.addNode();

			_commit = commit;

			boolean verboseOutput = true;

			if (verboseOutput) {
				_node.getInfo().setCaption(_name);
				_node.getInfo().setFooter("footer");

				Date d = commit.getCommitterIdent().getWhen();
				DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
				DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

				_node.getInfo().setFooter(timeFormat.format(d));
				_node.getInfo().setHeader(dateFormat.format(d));
				_node.getInfo().setFillColor("pink");
			}
		}

	}

}
