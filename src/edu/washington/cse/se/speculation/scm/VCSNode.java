package edu.washington.cse.se.speculation.scm;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

public interface VCSNode {

	public String getHex();

	public String getCommitter();

	public Date getTime();

	public void addParent(VCSNode node);

	public HashSet<VCSNode> getParents();

	public Hashtable<String, Integer> getDynamicRelationship();
	
	
}
