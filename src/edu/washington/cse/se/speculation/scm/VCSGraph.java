package edu.washington.cse.se.speculation.scm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.washington.cse.se.speculation.scm.git.Constants;
import edu.washington.cse.se.speculation.scm.git.GitNode;
import edu.washington.cse.se.speculation.scm.git.VCSNodePair;
import edu.washington.cse.se.speculation.util.TimeUtility;
import edu.washington.cse.se.speculation.util.XMLTools;

public class VCSGraph extends DirectedSparseGraph<VCSNode, VCSEdge> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 805320451233166642L;

	private HashSet<VCSNodePair> _knownMergePairs;

	private HashSet<VCSNodePair> _speculativeMergePairs;

	public HashSet<VCSNodePair> getKnownMerges() {
		return _knownMergePairs;
	}

	public HashSet<VCSNodePair> getSpeculativeMerges() {
		return _speculativeMergePairs;
	}

	public void setKnownMerges(HashSet<VCSNodePair> knownMergePairs) {
		_knownMergePairs = knownMergePairs;
	}

	public void setSpeculativeMerges(HashSet<VCSNodePair> speculativeMergePairs) {
		_speculativeMergePairs = speculativeMergePairs;
	}

	@SuppressWarnings("unchecked")
	public static VCSGraph readXML(String fName) {
		System.out.println("VCSGraph::readXML( " + fName + " )");

		Document doc = XMLTools.readXMLDocument(fName);

		Hashtable<String, VCSNode> nodes = new Hashtable<String, VCSNode>();
		HashSet<VCSEdge> edges = new HashSet<VCSEdge>();
		HashSet<VCSNodePair> speculativeMerges = new HashSet<VCSNodePair>();
		HashSet<VCSNodePair> actualMerges = new HashSet<VCSNodePair>();

		Element rootElement = doc.getRootElement();
		Element commitsElement = rootElement.getChild("commits");
		Element commitStructureElement = rootElement.getChild("commitStructure");
		Element speculativeMergesElement = rootElement.getChild("speculativeMerges");
		Element knownMergesElement = rootElement.getChild("knownMerges");

		for (Element commitElement : (List<Element>) commitsElement.getChildren()) {
			String hash = commitElement.getAttributeValue("hash");
			String committer = commitElement.getAttributeValue("dev");
			String time = commitElement.getAttributeValue("time");

			// XXX: should be base node or some such
			VCSNode node = new VCSBaseNode(hash, committer, new Date(Long.parseLong(time)));
			nodes.put(hash, node);
		}

		for (Element relationshipElement : (List<Element>) commitStructureElement.getChildren()) {
			String child = relationshipElement.getAttributeValue("child");
			String parent = relationshipElement.getAttributeValue("parent");

			VCSNode childNode = nodes.get(child);
			VCSNode parentNode = nodes.get(parent);
			childNode.addParent(parentNode);

			VCSEdge edge = new VCSEdge(parentNode, childNode);
			edges.add(edge);
		}

		for (Element mergeElement : (List<Element>) speculativeMergesElement.getChildren()) {
			String first = mergeElement.getAttributeValue("first");
			String second = mergeElement.getAttributeValue("second");
			String conflict = mergeElement.getAttributeValue("conflict");

			VCSNode firstNode = nodes.get(first);
			VCSNode secondNode = nodes.get(second);

			VCSNodePair pair = new VCSNodePair(firstNode, secondNode);

			if (conflict != null) {
				// if the conflict hasn't been set
				pair.setConflict(Boolean.parseBoolean(conflict));
			}

			speculativeMerges.add(pair);
		}

		for (Element mergeElement : (List<Element>) knownMergesElement.getChildren()) {
			String first = mergeElement.getAttributeValue("first");
			String second = mergeElement.getAttributeValue("second");
			String conflict = mergeElement.getAttributeValue("conflict");

			VCSNode firstNode = nodes.get(first);
			VCSNode secondNode = nodes.get(second);

			VCSNodePair pair = new VCSNodePair(firstNode, secondNode);
			if (conflict != null) {
				// if the conflict hasn't been set
				pair.setConflict(Boolean.parseBoolean(conflict));
			}

			actualMerges.add(pair);
		}

		VCSGraph vcsGraph = new VCSGraph();
		// gitGraph.setNodes(nodes);
		for (VCSNode node : nodes.values()) {
			vcsGraph.addVertex(node);
		}

		for (VCSEdge edge : edges) {
			vcsGraph.addEdge(edge, edge.getParent(), edge.getChild());
		}

		vcsGraph.setKnownMerges(actualMerges);
		vcsGraph.setSpeculativeMerges(speculativeMerges);

		return vcsGraph;
	}

	/**
	 * @param fName
	 * 
	 */
	public static void writeDot(String fName, VCSGraph vcsGraph) {

		System.out.println("VCSGraph::writeDOT( " + fName + " )");

		try {
			File outFile = new File(fName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}

			Hashtable<VCSNode, com.oy.shared.lm.graph.GraphNode> nodeMap = new Hashtable<VCSNode, com.oy.shared.lm.graph.GraphNode>();

			com.oy.shared.lm.graph.Graph graph = com.oy.shared.lm.graph.GraphFactory.newGraph();
			graph.getInfo().setCaption("Title");

			for (VCSNode vcsNode : vcsGraph.getVertices()) {

				if (!nodeMap.contains(vcsNode)) {
					com.oy.shared.lm.graph.GraphNode graphNode = graph.addNode();
					nodeMap.put(vcsNode, graphNode);
				} else {
					System.err.println("This should never happen");
				}
				com.oy.shared.lm.graph.GraphNode graphNode = nodeMap.get(vcsNode);

				// graphNode.getInfo().setCaption(gitNode.getCommitterName());
				graphNode.getInfo().setCaption(vcsNode.getHex().substring(0, 12));
				// graphNode.getInfo().setFooter("footer");

				Date d = vcsNode.getTime();
				DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
				DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

				// graphNode.getInfo().setFooter(timeFormat.format(d));
				graphNode.getInfo().setHeader(dateFormat.format(d) + " " + timeFormat.format(d));

				// graphNode.getInfo().setHeader(gitNode.getFromTime() + "");
				// graphNode.getInfo().setFooter(gitNode.getToTime() + "");

				if (vcsNode instanceof GitNode) {
					GitNode gitNode = (GitNode) vcsNode;
					if (gitNode.getColour() != null && !gitNode.getColour().equals(""))
						graphNode.getInfo().setFillColor(gitNode.getColour());
				}
			}

			for (VCSNode vcsChildNode : vcsGraph.getVertices()) {
				com.oy.shared.lm.graph.GraphNode childNode = nodeMap.get(vcsChildNode);

				for (VCSNode vcsParentNode : vcsChildNode.getParents()) {
					com.oy.shared.lm.graph.GraphNode parentNode = nodeMap.get(vcsParentNode);

					if (childNode == null || parentNode == null) {
						System.err.println("This should never happen");

					}

					// com.oy.shared.lm.graph.GraphEdge edge = graph.addEdge(parentNode, childNode);
					graph.addEdge(parentNode, childNode);
				}
			}

			com.oy.shared.lm.out.GRAPHtoDOT.transform(graph, new FileOutputStream(outFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void writeXML(String outXMLName, VCSGraph vcsGraph) {

		Document doc = XMLTools.newXMLDocument();
		Element rootElement = new Element("speculativeMerge");
		rootElement.setAttribute("date", TimeUtility.getCurrentLSMRDateString());
		rootElement.setAttribute("repoPath", Constants.REPOSITORY_PREFIX);
		rootElement.setAttribute("projectPath", Constants.PROJECT_PATH);

		doc.setRootElement(rootElement);

		Collection<VCSNode> nodes = vcsGraph.getVertices();
		Element nodesElement = new Element("commits");
		rootElement.addContent(nodesElement);
		nodesElement.setAttribute("count", +nodes.size() + "");

		HashSet<String> committers = new HashSet<String>();
		for (VCSNode node : vcsGraph.getVertices()) {
			committers.add(node.getCommitter());
		}

		nodesElement.setAttribute("committerCount", committers.size() + "");

		List<VCSNode> nodeList = new Vector<VCSNode>();
		nodeList.addAll(nodes);
		Collections.sort(nodeList, new Comparator<VCSNode>() {

			@Override
			public int compare(VCSNode arg0, VCSNode arg1) {
				return arg0.getTime().compareTo(arg1.getTime());
			}
		});

		for (VCSNode node : nodeList) {
			Element nodeElement = new Element("commit");
			nodeElement.setAttribute("hash", node.getHex());
			nodeElement.setAttribute("dev", node.getCommitter());
			nodeElement.setAttribute("time", node.getTime().getTime() + "");
			nodesElement.addContent(nodeElement);
		}

		Element parentsElement = new Element("commitStructure");
		rootElement.addContent(parentsElement);
		for (VCSNode node : nodeList) {
			for (VCSNode parentNode : node.getParents()) {
				Element nodeElement = new Element("structure");
				nodeElement.setAttribute("child", node.getHex());
				nodeElement.setAttribute("parent", parentNode.getHex());
				parentsElement.addContent(nodeElement);
			}
		}

		HashSet<VCSNodePair> speculativePairs = vcsGraph.getSpeculativeMerges();
		// System.out.println("Compare Speculative Pairs: " + speculativePairs.size());
		int pairNodeConflict = 0;
		int pairNodeClean = 0;
		for (VCSNodePair pair : speculativePairs) {
			if (pair.conflictSet()) {
				if (pair.hasConflict()) {
					pairNodeConflict++;
				} else {
					pairNodeClean++;
				}
			}
		}

		Element mergesElement = new Element("speculativeMerges");
		rootElement.addContent(mergesElement);
		mergesElement.setAttribute("numConflicts", pairNodeConflict + "");
		mergesElement.setAttribute("numClean", pairNodeClean + "");
		for (VCSNodePair pair : speculativePairs) {
			Element mergeElement = new Element("merge");
			mergeElement.setAttribute("first", pair.first().getHex());
			mergeElement.setAttribute("second", pair.second().getHex());

			if (pair.conflictSet()) {
				// if the conflict hasn't been set, don't write an attribute
				mergeElement.setAttribute("conflict", pair.hasConflict() + "");
			}

			mergesElement.addContent(mergeElement);
		}

		HashSet<VCSNodePair> mergePairs = vcsGraph.getKnownMerges();
		// System.out.println("Compare Merge Pairs: " + mergePairs.size());
		int mergeNodeConflict = 0;
		int mergeNodeClean = 0;
		for (VCSNodePair pair : mergePairs) {
			if (pair.conflictSet()) {
				if (pair.hasConflict()) {
					mergeNodeConflict++;
				} else {
					mergeNodeClean++;
				}
			}
		}

		Element knownMergesElement = new Element("knownMerges");
		rootElement.addContent(knownMergesElement);
		knownMergesElement.setAttribute("numConflicts", mergeNodeConflict + "");
		knownMergesElement.setAttribute("numClean", mergeNodeClean + "");
		for (VCSNodePair pair : mergePairs) {

			Element mergeElement = new Element("merge");
			mergeElement.setAttribute("first", pair.first().getHex());
			mergeElement.setAttribute("second", pair.second().getHex());

			if (pair.conflictSet()) {
				// if the conflict hasn't been set, don't write an attribute
				mergeElement.setAttribute("conflict", pair.hasConflict() + "");
			}

			knownMergesElement.addContent(mergeElement);
		}

		XMLTools.writeXMLDocument(doc, outXMLName);
	}

}
