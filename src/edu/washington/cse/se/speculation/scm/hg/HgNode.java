package edu.washington.cse.se.speculation.scm.hg;

import java.util.Date;

import edu.washington.cse.se.speculation.scm.VCSExtendedNode;

public class HgNode extends VCSExtendedNode {

	public HgNode(String hex, String commiter, Date when) {
		super(hex, commiter, when);
	}

	@Override
	public String toString() {
		return "HgNode - " + getHex();
	}

}
