package edu.washington.cse.se.speculation.scm;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

public class VCSBaseNode implements VCSNode {

	private HashSet<VCSNode> _parents = new HashSet<VCSNode>();
	protected String _committer;
	protected String _hex;
	protected Date _date;

	public VCSBaseNode(String hex, String commiter, Date when) {
		_hex = hex;
		_committer = commiter;
		_date = when;
	}

	@Override
	public void addParent(VCSNode node) {
		_parents.add(node);
	}

	@Override
	public String getCommitter() {
		return _committer;
	}

	@Override
	public String getHex() {
		return _hex;
	}

	@Override
	public HashSet<VCSNode> getParents() {
		return _parents;
	}

	@Override
	public Date getTime() {
		return _date;
	}

	private Hashtable<String, Integer> dynResults = new Hashtable<String, Integer>();

	public Hashtable<String, Integer> getDynamicRelationship() {
		return dynResults;
	}

	@Override
	public String toString() {
		return "BaseNode( " + getHex() + ", " + getTime() + " )";
	}

}
