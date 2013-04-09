/**
 * 
 */
package edu.washington.cse.se.speculation.scm.git;

import edu.washington.cse.se.speculation.scm.VCSNode;

/**
 * Tracks whether a pair of VCS nodes (either from git or hg right now) are in conflict.
 * 
 * @author rtholmes - change to VCSNodePair from GitNodePair
 * @author Yuriy
 */
public class VCSNodePair {

	private VCSNode _first, _second;

	private VCSNodePair() {
	}

	public VCSNodePair(VCSNode first, VCSNode second) {
		_first = first;
		_second = second;
	}

	public VCSNode first() {
		return _first;
	}

	public VCSNode second() {
		return _second;
	}

	private boolean _hasConflict = false;
	private boolean _conflictSet = false;

	public void setConflict(boolean conflict) {
		_conflictSet = true;

		_hasConflict = conflict;
	}

	public boolean conflictSet() {
		return _conflictSet;
	}

	public boolean hasConflict() {
		if (_conflictSet) {
			return _hasConflict;
		} else {
			throw new RuntimeException("VCSNodePair::hasConflict() - conflicts not set for: " + this);
		}
	}

	@Override
	public int hashCode() {
		return _first.hashCode() * _second.hashCode() * 7;
	}

	@Override
	public String toString() {
		return first().getHex() + " _:_ " + second().getHex();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VCSNodePair) {
			VCSNodePair o = (VCSNodePair) obj;
			return first().equals(o.first()) && second().equals(o.second());
		}
		return false;
	}

}
