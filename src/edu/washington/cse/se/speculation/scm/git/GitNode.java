package edu.washington.cse.se.speculation.scm.git;

import java.util.Date;

import org.eclipse.jgit.revwalk.RevCommit;

import edu.washington.cse.se.speculation.scm.VCSExtendedNode;

public class GitNode extends VCSExtendedNode {

	private String _colour = "";

	public GitNode(RevCommit commit) {
		super(commit.getName(), commit.getCommitterIdent().getName(), commit.getAuthorIdent().getWhen());

	}

	public GitNode(String hash, String committerName, Date when) {
		super(hash, committerName, when);
	}

	public String getColour() {
		return _colour;
	}

	public void setColour(String colour) {
		_colour = colour;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GitNode) {
			return ((GitNode) obj).getHex().equals(getHex());
		}
		return false;
	}

	@Override
	public String toString() {
		return getHex();
	}

	@Override
	public int hashCode() {
		return getHex().hashCode();
	}
}