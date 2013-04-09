==============================
crystal-retrospective-analysis
==============================

This repository contains a collection of fairly rough code that can be used to analyze conflicts in Git and Mercurial repositories. The code was used to analyze the data behind:

* Proactive detection of collaboration conflicts (FSE 2011) [http://dx.doi.org/10.1145/2025113.2025139]
* Early Detection of Collaboration Conflicts and Risks (TSE 2013, TO APPEAR)

Using the code
==============================

* System constants must be set in accordance with your analysis paths, sample projects, and environment variables (e.g., modify `Constants.java`.
* The metadata of the repository to be analyzed must extracted. Either `GitGraphGenerator.java` or `HgGraphGenerator.java` can be used for this purpose.
* `GitController.java` controls the collection of commit conflicts.
* `JUnitDriver.java` controls the collection and analysis of JUnit conflicts.
* `HigherOrderDriver.java` calculates data about different conflict types for individual projects.
* `TableGenerator.java` calculates conflict persistence data for individual projects. 





