package se.lnu.prosses.Benchmarks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import se.lnu.prosses.Statistics.SymmariesResultsHelper;
import se.lnu.prosses.general.SymmariesAnalysis;
import se.lnu.prosses.general.SymmariesHeapDom;
import se.lnu.prosses.general.Constants;
import se.lnu.prosses.general.Utils;
import se.lnu.prosses.tool.JARApplicationHelper;

public class IFSpecBenchmarkHelper extends SymmariesResultsHelper {

	// private HashMap<String, Boolean> groundTruthList = new HashMap<String,
	// Boolean>();
	//static String benchmarkRelativePath = "IFSpec/library/JavaBytecode/";//JavaBytecodePart1
	static boolean exceptionEnabeled = true;
	public static void main(String[] args) throws Exception {
		IFSpecBenchmarkHelper iFSpecBenchmarkHelper = new IFSpecBenchmarkHelper();
		//int methodSkipParameter = 120;
		methodSkipParameter = 300;
		analysis = SymmariesAnalysis.IMPLICIT_CONF;
		heapDom = SymmariesHeapDom.DEEPALIAS;
		exceptionEnabeled = true;
		enableSootOptimizations = true;
		benchmarkRelativePath = "IFSpec/library/JavaBytecodePart2";
		abortOnError = false;
		//extractVulenrabilityTypes(inputFolderPath + benchmarkRelativePath);
		iFSpecBenchmarkHelper.checkJARsInDirectory(true, methodSkipParameter);
		//iFSpecBenchmarkHelper.checkJARFilesInSheet(true, methodSkipParameter);
		//iFSpecBenchmarkHelper.checkSingleJARFile(true, methodSkipParameter,"ArrayIndexException-Insecure");

		String ReportName = "IFSpec" + new File(benchmarkRelativePath).getName();

		///*
		 iFSpecBenchmarkHelper.checkTheResults( "Misc/PerformanceReports/IFSPEC/SymmariesPerformance" + ReportName + ".txt",
							"Misc/PrecisionReports/IFSPEC/SymmariesVsExpectedResults" + ReportName + ".txt",
							"Misc/Datasets/" + ReportName + ".rtff",
							"");
		 // */

		extractOtherToolsStatistics();

	}

	private static void extractOtherToolsStatistics() {
		ArrayList<String> includedApplications = new ArrayList<String>();
		String[] fileContent = Utils.readStringTextFile(outputFolderPath + "/Misc/IFSPECAnalysisResults/AnalyzedSamples.csv").split("\n");
		for(String line: fileContent) {
			String[] elements = line.split(";");
			if(elements.length>0)
				includedApplications.add(elements[0].trim());
		}

		String[] samples = Utils.readStringTextFile(outputFolderPath + "/Misc/IFSPECAnalysisResults/key-shared.csv").split("\n");
		String out = "";
		for(String line: samples) {
			String[] elements = line.split(";");
			if(includedApplications.contains(elements[0].trim()))
				out +=line + "\n";
		}

		Utils.writeTextFile(outputFolderPath + "/Misc/IFSPECAnalysisResults/refined-key-shared.csv", out);

	}

	public void checkJARsInDirectory(boolean generateSCGSInputs, int methodSkipParameter) throws Exception {
		JARApplicationHelper tool = new JARApplicationHelper(// inputFolderPath + benchmarkRelativePath,
								     outputFolderPath, inputFolderPath);
		try {
			//String outputSubdirectory = jarfile.getAbsolutePath().replaceAll(this.inputPath, "");

			tool.checkJARsInDirectory(new File(tool.getInputPath() + java.io.File.separator + benchmarkRelativePath),
						  // applicationSubdirectory,
						  getExcludedApplications()
						  ,
						  "nice",
						  secstubsFiles,
						  inputFolderPath + "Secstubs/allSecStubs.xml",
						  null,
						  null,
						  exceptionEnabeled,
						  generateSCGSInputs,
						  Constants.symmariesPath,
						  outputFolderPath + File.separator + "Misc/CommandFiles/IFSpec.command",
						  analysis,
						  heapDom,
						  methodSkipParameter,
						  enableSootOptimizations,
						  abortOnError,
						  true,
						  generateJimple
						  );
		} catch (Exception e) {
			e.printStackTrace();
		}
		// new IFSpecBenchmarkHelper().checkTheResults();
	}

	public void checkSingleJARFile(boolean generateSCGSInputs, int methodSkipParameter, String app) throws Exception {
		ArrayList<File> includedApplications = new ArrayList<File>();
		includedApplications.add( new File(inputFolderPath + "/IFSpec/library/JavaBytecode/"+
				app.trim() + "/program/program.jar"));

		JARApplicationHelper tool = new JARApplicationHelper(//inputFolderPath + benchmarkRelativePath,
								     outputFolderPath + "/part1/", inputFolderPath);
		try {
			//String outputSubdirectory = jarfile.getAbsolutePath().replaceAll(this.inputPath, "");

			tool.checkJARFileCollections(includedApplications,
						     // applicationSubdirectory,
						     getExcludedApplications()
						     ,
						     "nice",
						     secstubsFiles,
						     inputFolderPath + "Secstubs/allSecStubs.xml",
						     null,
						     null,
						     exceptionEnabeled,
						     generateSCGSInputs,
						     Constants.symmariesPath,
						     outputFolderPath + File.separator + "Misc/CommandFiles/IFSpec.command",
						     analysis,
						     heapDom,
						     methodSkipParameter,
						     enableSootOptimizations,
						     abortOnError,true,
						     generateJimple
						     );
		} catch (Exception e) {
			e.printStackTrace();
		}
		// new IFSpecBenchmarkHelper().checkTheResults();
	}
	public void checkJARFilesInSheet(boolean generateSCGSInputs, int methodSkipParameter) throws Exception {
		ArrayList<File> includedApplications = new ArrayList<File>();
		String[] fileContent = Utils.readStringTextFile(outputFolderPath + "/Misc/PrecisionReports/IFSPEC/IncludedSamples.csv").split("\n");
		for(String line: fileContent) {
			String[] elements = line.split(";");
			includedApplications.add( new File(inputFolderPath + "/IFSpec/library/JavaBytecode/"+
							   elements[0].trim() + "/program/program.jar"));
		}

		JARApplicationHelper tool = new JARApplicationHelper(//inputFolderPath + benchmarkRelativePath,
								     outputFolderPath, inputFolderPath);
		try {
			//String outputSubdirectory = jarfile.getAbsolutePath().replaceAll(this.inputPath, "");

			tool.checkJARFileCollections(includedApplications,
						     null//	getExcludedApplications()
						     ,
						     "nice",
						     secstubsFiles,
						     inputFolderPath + "Secstubs/allSecStubs.xml",
						     null,
						     null,
						     exceptionEnabeled,
						     generateSCGSInputs,
						     Constants.symmariesPath,
						     outputFolderPath + File.separator + "Misc/CommandFiles/IFSpec.command",
						     analysis,
						     heapDom,
						     methodSkipParameter,
						     enableSootOptimizations,
						     abortOnError,
						     true,
						     generateJimple
						     );
		} catch (Exception e) {
			e.printStackTrace();
		}
		// new IFSpecBenchmarkHelper().checkTheResults();
	}

	private ArrayList<String> getExcludedApplications() {

		ArrayList<String> excludedApplications = new ArrayList<String>(
				Arrays.asList(

						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-HighAccess-Insecure/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-NoLeak/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-HighAccess-secure/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-ArrayAccess-Insecure/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-ArrayAccess-secure/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-Leak/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Inter6/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Inter7/program/program.jar", // static class initializer
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Static-Initializers-Not-Called/program/program.jar", // static class initializer

						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Refl1/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Refl2/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Refl3/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Refl4/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/simpleReflectionAccessPrivateField/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/simpleReflectionAccessPrivateField-secure/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/ReflectionSetSecretPrivateField-secure/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/ReflectionSetSecretPrivateField-Insecure/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Reflection-Accessibility-Modification-Secure/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Reflection-Accessibility-Modification/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/tools/program/program.jar", //reflection
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/simpleClassLoading/program/program.jar", //reflection



						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Arrays10/program/program.jar", // multi-arrays
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Arrays10-secure/program/program.jar", // multi-arrays
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Arrays9/program/program.jar", // multi-arrays

						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Deepcall1/program/program.jar", // JisymCompiler stack overflow
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Deepcall2/program/program.jar", // JisymCompiler stack overflow
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Basic4/program/program.jar", //Switch
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Deepalias1/program/program.jar", // stack overflow
						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/Deepalias2/program/program.jar", // stack overflow
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Aliasing5/program/program.jar", //Cannot generate the input; type mismatch
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Basic36/program/program.jar", //Cannot generate the input; type mismatch
						//	"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/SecuriBench-Basic4/program/program.jar", //Cannot generate the input; type mismatch

						"/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/StringIntern/program/program.jar", //Very specific to Java; Write h to the jvm string pool
						""
						));


		//excludedApplications.clear();
		ArrayList<String> includedApplications = new ArrayList<String>();
		String[] fileContent = Utils.readStringTextFile(outputFolderPath + "/Misc/IFSPECAnalysisResults/IncludedSamples.csv").split("\n");
		for(String line: fileContent) {
			String[] elements = line.split(";");
			includedApplications.add(elements[0].trim());
		}
		fileContent = Utils.readStringTextFile(outputFolderPath + "/Misc/IFSPECAnalysisResults/AllSamples.csv").split("\n");
		for(String line: fileContent) {
			String[] elements = line.split(";");
			//if(elements.length >= 4 && (elements[1].trim().toLowerCase().contains("secure")||elements[1].trim().toLowerCase().contains("insecure")) &&
			//		(elements[2].trim().toLowerCase().contains("secure")||elements[2].trim().toLowerCase().contains("insecure")) &&
			//		(elements[3].trim().toLowerCase().contains("yes")||elements[3].trim().toLowerCase().contains("no")));
			//else
			if(!includedApplications.contains(elements[0].trim()))
				excludedApplications.add("/Volumes/Academics/Workspaces/SymmariesExperiments/input/IFSpec/library/JavaBytecode/" + elements[0].trim() + "/program/program.jar");
		}



		return excludedApplications;
	}

	private String getApplicationName(String parent) {
		for (String applicationName : ApplicationsAnalysisResultsMap.keySet())
			if (applicationName.contains(parent))
				return applicationName;
		return parent;
	}

	private void setExpectedResult(String directoryPath) {
		for (File gtFile : Utils.getFilesOfTypes(directoryPath, new String[] {Constants.GroundTruthFile})) {
			try {
				@SuppressWarnings("resource")
				String line = new Scanner(new File(gtFile.getAbsolutePath())).next();
				String applicationName = getApplicationName(gtFile.getParent().replaceAll(inputFolderPath, ""));

				if (line.trim().toUpperCase().equals("SECURE"))
					groundTruthResultsMap.put(applicationName, true);
				else if (line.trim().toUpperCase().equals("INSECURE"))
					groundTruthResultsMap.put(applicationName, false);
				else
					Utils.logErr(this.getClass(), "Cannot read the ground truth result of " + applicationName);
			} catch (FileNotFoundException e) {
				Utils.logErr(this.getClass(), "Could not load the ground truth file: " + gtFile.getAbsolutePath());
			} catch (NumberFormatException e) {
				Utils.logErr(this.getClass(), "Bad statistics file format!");
			}
		}
	}

	void checkTheResults(String statisticsReportName, String symmariesResultsReportName, String sheetPath,String exprIDExtension) {
		extractSymmariesResultsAndStatistics(new File (outputFolderPath + benchmarkRelativePath), true);
		exportApplicationsStatistics(outputFolderPath, outputFolderPath + statisticsReportName,inputFolderPath);
		setExpectedResult(inputFolderPath + benchmarkRelativePath);
		analyzePrecision(inputFolderPath, outputFolderPath + symmariesResultsReportName, outputFolderPath);
		//WekaDatasetConstrcutor wekaDatasetConstrcutor = new WekaDatasetConstrcutor(this.ApplicationsAnalysisResultsMap);
		//wekaDatasetConstrcutor.constructDataSet(outputFolderPath + datasetFilePath);
		this.exportMethodStatisticsToSheet(outputFolderPath, sheetPath);
	}

	static void extractVulenrabilityTypes(String directoryPath) {
		File file = new File(directoryPath);
		String out = "";
		if (file.exists())
			for (File tagFile : Utils.getFilesOfTypes(directoryPath, new String[] {"tags.txt"}))
				//for (String tag : Utils.readStringTextFile(tagFile.getAbsolutePath()).split("\n")){
				out += tagFile.getParentFile().getName() + ";" + Utils.readStringTextFile(tagFile.getAbsolutePath()).replaceAll("\n","/") + "\n";
		//}
		Utils.writeTextFile( outputFolderPath + "IFSpec/library/tags.csv", out);
	}
}
