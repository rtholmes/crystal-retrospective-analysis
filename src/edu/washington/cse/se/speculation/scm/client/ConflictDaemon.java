package edu.washington.cse.se.speculation.scm.client;

import edu.washington.cse.se.speculation.scm.client.ConflictResult.ResultStatus;

public class ConflictDaemon {
	public static void computeConflicts(ClientPreferences preferences, IConflictClient client) {

		for (DataSource source : preferences.getDataSources()) {
			calculateConflict(source, preferences, client);
		}

	}

	private static void calculateConflict(DataSource source, ClientPreferences preferences, IConflictClient client) {
		// TODO: actually write this code

		double val = Math.random();
		ResultStatus status = ConflictResult.ResultStatus.SAME;

		if (val < .25)
			status = ResultStatus.AHEAD;
		else if (val >= .25 && val < .5)
			status = ResultStatus.BEHIND;
		else if (val >= .5 && val < .75)
			status = ResultStatus.CONFLICT;

		client.setStatus(new ConflictResult(source, status));
	}
}
