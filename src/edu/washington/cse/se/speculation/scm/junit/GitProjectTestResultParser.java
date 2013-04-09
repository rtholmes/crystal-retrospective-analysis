/**
 * 
 */
package edu.washington.cse.se.speculation.scm.junit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author Yuriy
 *
 */
public class GitProjectTestResultParser {
	
	// parses a string representation of the output of the git project's "make test" command
	// this function will (1) print to stdout all unparsed lines
	// 					  (2) return an array of Strings where the first element contains a summary
	//						  and the rest of the elements uniquely identify each test
	public static String[] parseString(String testResults) {
		
		ArrayList<String> answer = new ArrayList<String>();

		StringTokenizer lines = new StringTokenizer(testResults, "\n");
		
		
		String currentTestScript = null;
		int passed = 0, fixed = 0, skipped = 0, failed = 0;

//		System.out.println("@@@" + lines.countTokens() + "\n\n\n");

		while (lines.hasMoreTokens()) {
			String line = lines.nextToken();
			
			if (line.startsWith("***")) {
				StringTokenizer tokens = new StringTokenizer(line);
				tokens.nextToken();
				currentTestScript = tokens.nextToken();
			}
			else if (line.startsWith("*   ok")) {				
				answer.add(currentTestScript + ":" + line);
				passed++;
			}
			else if (line.startsWith("*   FIXED")) {
				answer.add(currentTestScript + ":" + line);
				fixed++;
			}
			else if (line.startsWith("* skip")) {
				answer.add(currentTestScript + ":" + line);
				skipped++;
			}
			else if (line.startsWith("*   still broken")) {
				answer.add(currentTestScript + ":" + line);
				failed++;
			}
			else 
				System.out.println("Ignoring test-output line: " + line);
		}
		
		answer.add(0, "Passed: " + passed + "; Failed: " + failed + "; Fixed: " + fixed + "; Skipped:" + skipped);
		return answer.toArray(new String[1]);
	}
	
	// parses a File representation of the output of the git project's "make test" command
	// this function will (1) print to stdout all unparsed lines
	// 					  (2) return an array of Strings where the first element contains a summary
	//						  and the rest of the elements uniquely identify each test
	public static String[] parseFile(File testResults) throws IOException {
			String testResultsString = "";
		
	        BufferedReader reader = new BufferedReader(new FileReader(testResults));
	        String line = null;
	        while ((line = reader.readLine()) != null)
	            testResultsString += line + "\n";
	        reader.close();
	        
	        return parseString(testResultsString);
//	        System.out.println(testResultsString);
//	        return new String[1];
	}
	
	//reads a file and prints out the aggregated results
	public static void main(String[] args) throws IOException{	
		String fileLocation = "C:\\Users\\Yuriy\\Desktop\\GitTestOutput.txt";
		
		String[] answer = parseFile(new File(fileLocation));
		
		System.out.println("\n\n**********\n" + answer[0]);		
	}
}
