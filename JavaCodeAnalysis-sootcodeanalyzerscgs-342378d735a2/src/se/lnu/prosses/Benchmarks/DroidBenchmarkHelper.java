package se.lnu.prosses.Benchmarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import se.lnu.prosses.Statistics.SymmariesResultsHelper;
import se.lnu.prosses.general.Utils;
import se.lnu.prosses.tool.Tool;

public class DroidBenchmarkHelper extends SymmariesResultsHelper {
	public class TestCase {
		String applicatioName;
		String descriptionTest;
		int number_of_leaks;
		String challenges;

		public TestCase() {

		}

		public void print() {
			Utils.log(this.getClass(), this.applicatioName + "\n " + this.descriptionTest + "\n" + this.number_of_leaks
					+ "\n" + this.challenges + "\n\n");

		}
	}

	private static HashMap<String, TestCase> microbenchmarkExpectedResults = new HashMap<String, TestCase>();
	private static ArrayList<String> excludedApplications = new ArrayList<String>();
	private static Tool tool = new Tool(outputFolderPath, inputFolderPath);
	static SymmariesResultsHelper scgsResultsHelper = new SymmariesResultsHelper();

	private void extractExpectedResultFromATestCase(File javaFile) {
		TestCase testCase = null;
		String applicatioName = javaFile.getAbsolutePath().replaceAll(inputFolderPath, "");
		applicatioName = applicatioName.split("/")[1] + "/" + applicatioName.split("/")[2];
		try {
			Scanner sc = new Scanner(javaFile);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("@testcase_name")) {
					testCase = new DroidBenchmarkHelper().new TestCase();

					testCase.applicatioName = applicatioName;// + "/" + line.split("@testcase_name")[1].trim();
				} else if (line.contains("@challenges"))
					testCase.challenges = line.split("@challenges")[1].trim();
				else if (line.contains("@number_of_leaks"))
					testCase.number_of_leaks = Integer.parseInt(line.split("@number_of_leaks")[1].trim());
				else if (line.contains("@description"))
					testCase.descriptionTest = line.split("@description")[1].trim();
			}
			if (testCase != null) {
				microbenchmarkExpectedResults.put(testCase.applicatioName, testCase);
				testCase.print();
			}
			sc.close();
		} catch (FileNotFoundException e) {
			Utils.logErr(this.getClass(), "Could not open the file " + javaFile);
		}

	}

	private void extractExpectedResultsFromMicrobenchmark(String directoryPath) throws FileNotFoundException {
		for (File javaFile : Utils.getFilesOfTypes(directoryPath, new String[] {".java"}))
			extractExpectedResultFromATestCase(javaFile);
	}

	public static void main(String[] args) throws Exception {
		/*
		 * excludedApplications.add("Parcel1.apk");
		 * excludedApplications.add("PlayStore1.apk");
		 * excludedApplications.add("PrivateDataLeak3.apk");
		 * excludedApplications.add("Exceptions1.apk");
		 * excludedApplications.add("Exceptions2.apk");
		 * excludedApplications.add("Exceptions3.apk");
		 * excludedApplications.add("Exceptions4.apk");
		 */
		// tool.checkAPKsInDirectory(new File(InputPath +
		// "Android/DroidBench-master/apk/"), excludedApplications, "DroidBench-master",
		// "nice");


		// scgsResultsHelper.exportListOfUsedJavaLibrariesMethods(new
		// File(experimentsDirectory), "longList");
		new DroidBenchmarkHelper().processSCGSResults();

	}

	@SuppressWarnings({ "static-access", "unused" })
	private void processSCGSResults() throws FileNotFoundException {
		String experimentsDirectory = outputFolderPath + "Android/";
		extractSymmariesResultsAndStatistics(new File (experimentsDirectory), false);
		exportApplicationsStatistics(outputFolderPath, outputFolderPath + "Misc/DroidBench.txt", inputFolderPath);

		extractExpectedResultsFromMicrobenchmark(inputFolderPath + "Android/DroidBench-master/eclipse-project");
		// scgsResultsAnalyzer.extractSecurityGuardsFromDirectory( OutputPath +
		// "Dataset/");
		tool.project.configurations.xmlSourcesAndSinks = "/Volumes/Academics/Dropbox/Workspaces/SymmariesExperiments/ScgsInputGenertaor/input/Android/SourcesAndSinks.txt";
		tool.project.extractSinkAndSources();

		int totalNumberOfAnalyzedResults = 0, insecureConsistents = 0, secureConsisstents = 0, unsounds = 0,
				restrictives = 0;
		for (String applicationName : scgsResultsHelper.ApplicationsAnalysisResultsMap.keySet()) {
			TestCase testCase = microbenchmarkExpectedResults.get(applicationName.replaceAll(".apk", ""));
			// Utils.log(this.getClass(),);
			if (testCase != null) {
				totalNumberOfAnalyzedResults++;
				boolean isSecureApplication = scgsResultsHelper.ApplicationsAnalysisResultsMap
						.get(applicationName).isSecureApplicationBasedonSCGS();
				if (isSecureApplication)
					if (!scgsResultsHelper.ApplicationsAnalysisResultsMap.get(applicationName)
							.isSecureApplicationBasedonSCGS())
						if (testCase.number_of_leaks == 0) {
							restrictives++;
							Utils.logErr(this.getClass(),
									"Restrictive results for " + testCase.applicatioName + "\n	SCGSResults: "
											+ isSecureApplication + "\n	Expected Results: " + testCase.challenges);
						} else {
							insecureConsistents++;
							Utils.log(this.getClass(),
									"Consistent insecure results for " + testCase.applicatioName + "\n	SCGSResults: "
											+ isSecureApplication + "\n	Expected Results: number of leaks is "
											+ testCase.number_of_leaks + " and  " + testCase.challenges);
						}
					else if (testCase.number_of_leaks > 0) {
						unsounds++;
						Utils.logErr(this.getClass(),
								"Unsound results for " + testCase.applicatioName + "\n	SCGSResults: "
										+ isSecureApplication + "\n	Expected Results: number of leaks is "
										+ testCase.number_of_leaks + " and  " + testCase.challenges);
					} else {
						secureConsisstents++;
						Utils.log(this.getClass(),
								"Consistent secure results for " + testCase.applicatioName + "\n	SCGSResults: "
										+ isSecureApplication + "\n	Expected Results: " + testCase.challenges + " and "
										+ testCase.number_of_leaks);
					}
			}
		}
		Utils.log(this.getClass(),
				"\n**************************************************************\n"
						+ "The number of applications processed by SCGS is: "
						+ scgsResultsHelper.ApplicationsAnalysisResultsMap.size());
		Utils.log(this.getClass(),
				"The number of micro-benchmark appliactions is: " + microbenchmarkExpectedResults.size());
		Utils.log(this.getClass(),
				"The total number of appliactions whose results are compared: " + totalNumberOfAnalyzedResults);
		Utils.log(this.getClass(), "The number of restrictive results: " + restrictives);
		Utils.log(this.getClass(), "The number of insecureConsistents: " + insecureConsistents);
		Utils.log(this.getClass(), "The number of secureConsisstents: " + secureConsisstents);
		Utils.log(this.getClass(), "The number of unsounds: " + unsounds);
	}

}
