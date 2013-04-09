package edu.washington.cse.se.speculation.scm;

import java.util.Date;

public class VCSExtendedNode extends VCSBaseNode {

	private long _toTime;

	public VCSExtendedNode(String hex, String commiter, Date when) {
		super(hex, commiter, when);
	}

	public long getFromTime() {
		return getTime().getTime();
	}

	public long getToTime() {
		return _toTime;
	}

	public void setToTime(long toTime) {
		_toTime = toTime;
	}

	public boolean during(long atTime) {
		if (_toTime == 0)
			throw new RuntimeException("The _toTime is not set for this commit: " + getHex());
		return (getFromTime() <= atTime) && (atTime < _toTime);
	}
}
