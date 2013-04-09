package edu.washington.cse.se.speculation.scm.junit;

import java.util.HashSet;

public class ResultSet {

	public ResultSet(String pHex, boolean pDidCompile, boolean pDidGoInfinite, HashSet<TestResult> pResults) {
		hex = pHex;
		didCompile = pDidCompile;
		didGoInfinite = pDidGoInfinite;
		results = pResults;
	}

	HashSet<TestResult> results = new HashSet<TestResult>();

	boolean didCompile = true;

	boolean didGoInfinite = false;

	String hex = "";

}
