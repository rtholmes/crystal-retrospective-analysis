package edu.washington.cse.se.speculation.scm.junit;

public class TestResult {

	private boolean _result;
	private String _name;

	public TestResult(String testName, boolean result) {
		_name = testName;
		_result = result;
	}

	public String getName() {
		return _name;
	}

	public boolean getResult() {
		return _result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TestResult) {
			return getName().equals(((TestResult) obj).getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
