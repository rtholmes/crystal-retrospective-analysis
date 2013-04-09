package edu.washington.cse.se.speculation.scm.git;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.generators.GitGraphGenerator;
import edu.washington.cse.se.speculation.util.TimeUtility;

/**
 * Collects data for conflict detection. This is somewhat more complex than it would need to be if
 * we could count on it working in one pass; unfortunately, collecting millions of conflict pairs in
 * a distributed manner is prone to failure and the controller has to be somewhat more robust to
 * interruption.
 * 
 * @author rtholmes
 * 
 */
public class GitController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final long start = System.currentTimeMillis();

		System.out.println("GitController - Starting up ( " + TimeUtility.getCurrentLSMRDateString() + " )");

		final String path;
		if (args.length > 0) {
			Constants.setProject(args[0]);
			System.out.println("\tGitController - project arg provided: " + args[0]);
		}

		path = Constants.PROJECT_PATH;

		System.out.println("\tGitController - project: " + Constants.PROJECT + " project path: " + Constants.PROJECT_PATH);

		String gitPath = path + ".git";
		final String repoPath = path;

		try {

			final VCSGraph vcsGraph;

			if (Constants.RECOVER_PREVIOUS) {
				// try to recover the old session
				if (new File(Constants.XML_FINAL).exists()) {
					vcsGraph = VCSGraph.readXML(Constants.XML_FINAL);
					System.out.println("GitController - Final XML recovered ( " + Constants.XML_FINAL + " )");
				} else if (new File(Constants.XML_TMP).exists()) {
					vcsGraph = VCSGraph.readXML(Constants.XML_TMP);
					System.out.println("GitController - in-progress XML recovered ( " + Constants.XML_TMP + " )");
				} else {
					System.out.println("GitController - XML not recovered ( " + Constants.XML_TMP + " ), starting from scratch");
					GitGraphGenerator graphGenerator = new GitGraphGenerator(gitPath);
					vcsGraph = graphGenerator.generateGraph();
					VCSGraph.writeXML(Constants.XML_TMP, vcsGraph);
				}
			} else {
				// start from scratch
				GitGraphGenerator graphGenerator = new GitGraphGenerator(gitPath);
				vcsGraph = graphGenerator.generateGraph();
				VCSGraph.writeXML(Constants.XML_TMP, vcsGraph);
			}

			final HashSet<VCSNodePair> mergePairs = vcsGraph.getKnownMerges();
			final HashSet<VCSNodePair> speculativePairs = vcsGraph.getSpeculativeMerges();

			System.out.println("Commits: " + vcsGraph.getVertexCount());
			HashSet<String> committers = new HashSet<String>();
			for (VCSNode node : vcsGraph.getVertices()) {
				committers.add(node.getCommitter());
			}
			System.out.println("Committers: " + committers.size());

			System.out.println("Speculative Pairs: " + speculativePairs.size());
			System.out.println("Merge Pairs: " + mergePairs.size());

			String tmpHex = null;
			long lastTime = 0;
			for (VCSNode node : vcsGraph.getVertices()) {
				if (node.getTime().getTime() > lastTime) {
					tmpHex = node.getHex();
					lastTime = node.getTime().getTime();
				}
				// if (node.getParents().size() == 0) {
				// if (tmpHex == null) {
				// tmpHex = node.getHex();
				// } else {
				// throw new RuntimeException("Graph shoudln't have two tips - first: " + tmpHex +
				// " second: " +
				// node.getHex());
				// }
				// }
			}

			final String tipHex = tmpHex;
			System.out.println("Tip Hex: " + tipHex);

			boolean execute = true;
			if (execute) {
				ExecutorService service = Executors.newFixedThreadPool(Constants.POOL_SIZE);

				final Vector<VCSNodePair> allPairs = new Vector<VCSNodePair>();
				allPairs.addAll(mergePairs);
				allPairs.addAll(speculativePairs);
				// if (speculativePairs.size() < 20000) {
				// // don't calculate the speculative pairs if it's going to take more than a day
				// // 24h * 60m * 60s / 5 seconds per run == 17,280 comparisons / day
				// allPairs.addAll(speculativePairs);
				// } else {
				// System.out.println("GitController - too many speculative pairs ( " +
				// speculativePairs.size()
				// + " ); computing actual merges only.");
				// }

				final AtomicInteger counter = new AtomicInteger(0);

				// we would like this to be here rather than below, but it just doesn't work
				// final GitConflictDetector gcd = new GitConflictDetector(repoPath, workingPath);

				final Stack<GitConflictDetector> gcds = new Stack<GitConflictDetector>();
				String wpDateStamp = TimeUtility.getCurrentLSMRDateString();
				for (int i = 0; i < Constants.POOL_SIZE; i++) {
					final String workingPath = Constants.REPOSITORY_PREFIX + "tmp_" + Constants.PROJECT + "_" + wpDateStamp + "_" + i + File.separator;
					gcds.push(new GitConflictDetector(repoPath, workingPath));
				}

				System.out.println("# of conflict pairs to check: " + allPairs.size());
				for (final VCSNodePair pair : allPairs) {
					if (pair.conflictSet()) {
						System.out.println(counter.get() + " of " + allPairs.size() + ": " + TimeUtility.getCurrentLSMRDateString() + " - node: " + pair.first().getHex()
								+ " hasConflict: " + pair.hasConflict() + " recovered from XML");
						counter.incrementAndGet();
					} else {
						// only compute the conflict if it hasn't already been computed

						// If we're over some threshold value we might want to think about how we
						// tackle these (e.g.,
						// sampling from the speculative pool)

						service.execute(new Runnable() {

							@Override
							public void run() {
								GitConflictDetector gcd = gcds.pop();
								gcd.reset(tipHex);

								VCSNode first = pair.first();
								VCSNode second = pair.second();

								long runStart = System.currentTimeMillis();

								// GitConflictDetector gcd = new GitConflictDetector(repoPath,
								// workingPath);

								String c1 = first.getHex();
								String c2 = second.getHex();

								try {
									boolean hasConflict = gcd.hasConflict(c1, c2);
									System.out.println(counter.get() + " of " + allPairs.size() + ": " + TimeUtility.getCurrentLSMRDateString() + " - node: " + first.getHex()
											+ " hasConflict: " + hasConflict + " took: " + TimeUtility.msToHumanReadableDelta(runStart));

									if (hasConflict) {
										if (mergePairs.contains(pair)) {
											// TODO: attributes on git / hg nodes
											// first.setColour("red");
										}
										pair.setConflict(true);
									} else {
										if (mergePairs.contains(pair)) {
											// TODO: attributes on git / hg nodes
											// second.setColour("green3");
										}
										pair.setConflict(false);
									}

									int localCounter = counter.incrementAndGet();
									if (localCounter % Constants.TMP_WRITE_FREQ == 0) {
										// write a tmp xml ever N entries

										GitGraph.writeXML(Constants.XML_TMP, vcsGraph);
									}

								} catch (IOException e) {
									e.printStackTrace();
									gcds.push(gcd);
								}
								gcds.push(gcd);
							}
						});
					}
				}
				// tell the service to finish all queued jobs and exit
				service.shutdown();

				while (!service.isTerminated()) {
					// check every XXX ms to see if the service is done shutting down
					// essentially spins until the queue is empty so the exit message happens at the
					// end.
					// Thread.sleep(250);
				}

				System.out.println("Merge Pairs: " + mergePairs.size());
				int mergeNodeConflict = 0;
				int mergeNodeClean = 0;
				int mergeNodeNotSet = 0;
				for (VCSNodePair pair : mergePairs) {
					if (!pair.conflictSet()) {
						mergeNodeNotSet++;
					} else {
						if (pair.hasConflict()) {
							mergeNodeConflict++;
						} else {
							mergeNodeClean++;
						}
					}
				}
				System.out.println("\tConflicts: " + mergeNodeConflict);
				System.out.println("\tClean: " + mergeNodeClean);
				System.out.println("\tNot Set: " + mergeNodeNotSet);

				System.out.println("Compare Pairs: " + speculativePairs.size());
				int pairNodeConflict = 0;
				int pairNodeClean = 0;
				int pairNodeNotSet = 0;
				for (VCSNodePair pair : speculativePairs) {
					if (!pair.conflictSet()) {
						pairNodeNotSet++;
					} else {
						if (pair.hasConflict()) {
							pairNodeConflict++;
						} else {
							pairNodeClean++;
						}
					}
				}
				System.out.println("\tConflicts: " + pairNodeConflict);
				System.out.println("\tClean: " + pairNodeClean);
				System.out.println("\tNot Set: " + pairNodeNotSet);
			}

			GitGraph.writeXML(Constants.XML_FINAL, vcsGraph);

			GitGraph.writeDot(Constants.DOT_FINAL, vcsGraph);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("GitController major failure: " + e);
			System.err.println("GitController major failure: " + e);
		}

		System.out.println("GitController - done in: " + TimeUtility.msToHumanReadableDelta(start));
	}
}
