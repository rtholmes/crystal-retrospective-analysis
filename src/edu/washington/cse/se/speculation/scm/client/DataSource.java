package edu.washington.cse.se.speculation.scm.client;

public class DataSource {

	public enum RepoKind {
		GIT, HG
	}

	private String _shortName;
	private String _cloneString;
	private RepoKind _repoKind;

	public DataSource(String shortName, String cloneString, RepoKind repoKind) {
		_shortName = shortName;
		_cloneString = cloneString;
		_repoKind = repoKind;
	}

	String getShortName() {
		return _shortName;
	}

	String getCloneString() {
		return _cloneString;
	}

	public String getKind() {
		return _repoKind.toString();
	}

	public void setKind(RepoKind kind) {
		_repoKind = kind;
	}

	public void setShortName(String name) {
		_shortName = name;
	}

	public void setCloneString(String name) {
		_cloneString = name;
	}

	@Override
	public String toString() {
		return getShortName() + ";" + getCloneString();
	}
}
