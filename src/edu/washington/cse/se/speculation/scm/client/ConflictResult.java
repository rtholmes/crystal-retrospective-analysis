package edu.washington.cse.se.speculation.scm.client;

public class ConflictResult {

	public enum ResultStatus {
		SAME, AHEAD, BEHIND, CONFLICT
	}

	private final DataSource _source;
	private final ResultStatus _status;

	ConflictResult(DataSource source, ResultStatus status) {
		_source = source;
		_status = status;
	}

	@Override
	public String toString() {
		return "ConflictResult - " + _source.getShortName() + " status: " + _status;
	}

	DataSource getDataSource() {
		return _source;
	}

	ResultStatus getStatus() {
		return _status;
	}
}
