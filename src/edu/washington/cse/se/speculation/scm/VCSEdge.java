package edu.washington.cse.se.speculation.scm;

public class VCSEdge {

	private VCSNode _parent;
	private VCSNode _child;

	public VCSEdge(VCSNode parent, VCSNode child) {
		_parent = parent;
		_child = child;
	}

	public VCSNode getParent() {
		return _parent;
	}

	public VCSNode getChild() {
		return _child;
	}

	// NOTE: What VCSNodePair even represents when we have VCSEdge? Should VCSNodePair just go away? I think the original intent of VCSNodePair was for there
	// to be an unordered pairing of elements, but really these should be date ordered anyways so there is an explicit parent/child relationship. Or am I wrong?
}
