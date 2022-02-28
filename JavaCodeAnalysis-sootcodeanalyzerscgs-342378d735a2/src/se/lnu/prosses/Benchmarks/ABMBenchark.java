package se.lnu.prosses.Benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.io.FileUtils;

import se.lnu.prosses.Statistics.SourcesStatsHelper;
import se.lnu.prosses.Statistics.ApplicationStatsHelper;
import se.lnu.prosses.Statistics.SymmariesResultsHelper;
import se.lnu.prosses.general.SynthesisConfiguratons;
import se.lnu.prosses.general.SymmariesAnalysis;
import se.lnu.prosses.general.SymmariesHeapDom;
import se.lnu.prosses.general.Constants;
import se.lnu.prosses.general.Utils;
import se.lnu.prosses.tool.Tool;
import se.lnu.prosses.tool.JARApplicationHelper;

public class ABMBenchark extends SymmariesResultsHelper {

	public static void main (String[] args) throws Exception {
		ABMBenchark bench = new ABMBenchark();
		// benchmarkRelativePath = "/WebApplications/test/TaintExpr";
		//benchmarkRelativePath = "/academy-web-target/Results/";
		benchmarkRelativePath = "WebApplications";

		BenchmarkExperimentConfig.methodSkipParameter = 1000;
		BenchmarkExperimentConfig.exceptionEnabeled = false;
		BenchmarkExperimentConfig.abortOnError = false;
		BenchmarkExperimentConfig.heapDom = SymmariesHeapDom.DEEPALIAS;

		boolean unzip = true;
		boolean generateSymmariesInput = false;
		boolean generateStatisticsReport = false;

		ReportName = new File(benchmarkRelativePath).getName();

		int split = ArrayUtils.indexOf(args, "--");
		final String[] include =
			(split < 0) ? args : ArrayUtils.subarray (args, 0, split);
		final String[] exclude =
			ArrayUtils.addAll
			((split < 0) ? new String[] {} :
			 ArrayUtils.subarray (args, split + 1, args.length)
			 // , "48324834_erp"
			 // , "47632829_spring-social-dynamics-crm"
			 // , "28852213_cms_1.0"
			 //
			 // does not compile... (missing javafx dependency?)
			 , "51217809_CRMAppliction"
			 //
			 // unable to compile (with gradle, on zinn)
			 , "45204620_cassandra-cms"
			 //
			 // unable to compile (with gradle, on zinn)
			 , "47907539_Travel_ERP"
			 //
			 // ----------------------------------------------------
			 //
			 // Those below may be "more" fixable...
			 //
			 // Multi-pom with intra-dependencies: "The POM for
			 // ca.webvue.cms:common-model:jar:0.0.1-SNAPSHOT is
			 // missing, no dependency information available"... But
			 // all cp.txt's seem ok otherwise, so binary version
			 // might be doable? (Well, binary file seems empty
			 // though)
			 , "39234370_webvue-cms"
			 //
			 // Could not find artifact
			 // com.oracle:ojdbc6:jar:11.1.0.7.0 in oracle
			 // (http://maven.jahia.org/maven2)
			 , "38675591_erp_insa"
			 //
			 // No method at all in generated meth_file
			 , "46423777_repo-valtaks"
			 );
		Arrays.sort (include);
		Arrays.sort (exclude);

		// File appDirsFile = new File (outputFolderPath, "app-dirs");
		File appInfosFile = new File (outputFolderPath, "app-infos");
		Map<String,AppInfos> appInfos = loadAppInfos (appInfosFile);

		// Manually specified classpath apps that come in binary for.
		Map<String, String[]> appClassPaths = new HashMap<>();

		if(generateSymmariesInput) {
			if (unzip) {
				File appsDir = new File (inputFolderPath, benchmarkRelativePath);
				appInfos = bench.unzipAllJarfilesInDir (appsDir, appClassPaths, appInfos);
				appInfos = bench.unzipAllZipfilesInDir (appsDir, appInfos);
				saveAppInfos (appInfosFile, appInfos);
			}

			for (AppInfos ai: appInfos.values ()) {
				if (include.length > 0 &&
				    Arrays.binarySearch (include, ai.getName ()) < 0)
					continue;
				Utils.log (ABMBenchark.class, "--- "+ ai +" ---");
				if (Arrays.binarySearch (exclude, ai.getName ()) >= 0) {
					Utils.log (ABMBenchark.class, "Skipping explicitly excluded application "+ ai);
					continue;
				}
				if (ai.isExcluded ()) {
					Utils.log (ABMBenchark.class, "Skipping "+ ai +" since: "+ ai.causeForExclusion ());
					continue;
				}
				boolean genSyrsInput = true;
				for (SymmariesAnalysis a: new SymmariesAnalysis[]
						{ SymmariesAnalysis.HEAP_ONLY,
						  SymmariesAnalysis.TAINT,
						  SymmariesAnalysis.EXPLICIT_CONF,
						  SymmariesAnalysis.IMPLICIT_CONF }) {
					BenchmarkExperimentConfig.analysis = a;
					bench.compileAppInDirToSymmariesFromClassFiles
						(ai,
						 genSyrsInput, secstubsFiles,
						 inputFolderPath + File.separator + "Secstubs/allSecStubs.xml",
						 a.toString () // <- targetSubfolder
						 );
					// Generate inputs only once: remaining
					// iterations will only create command
					// files:
					genSyrsInput = false;
				}
			}

		}

		// for (AppInfos ai: appInfos.values ())
		// 	ai.recap ();

		File statsDir = new File (outputFolderPath, "Misc" + File.separator + "PerformanceReports");
		if(generateStatisticsReport)
			bench.generatXlsxStatisticsReport(statsDir,
							  // "statistics" + ReportName + ".txt",
							  // "scgsResults" + ReportName + ".txt",
							  // "Misc/PerformanceReports/ABM/statistics" + ReportName + ".txt",
							  // "Misc/PerformanceReports/ABM/scgsResults" + ReportName + ".txt",
							  // "Misc/Datasets/ABM" + ReportName + ".rtff", // the path for storing the dataset used for learning
							  "ABM" + ReportName, // the path for storing the xlsx file with statistics results
							  // "Misc/PerformanceReports/ABM" + ReportName + ".csv", // the path for storing the xlsx file with statistics results
							  // "450"
							  null);
		//findNumberOfAnalyzedMethods();
		//GenerateStatisticTable(ReportName);
		//categorizeSecstubs();
	}

	public static class LaTeX {
		public static void main (String[] args) {

			File statsDir = new File (outputFolderPath, "Misc" + File.separator + "PerformanceReports");

			Map<String, String> selectedApps = new HashMap<> ();
			selectedApps.put ("27823030_WEB_SHOP-CMS", "\\webshopapp");
			selectedApps.put ("44409894_crm", "\\crmapp");
			selectedApps.put ("9783982_CMStudio", "\\CMStudioapp");
			selectedApps.put ("2114204_RadioCRM", "\\RadioCRMapp");
			selectedApps.put ("9592899_taglibs", "\\taglibsapp");
			selectedApps.put ("32859973_exc-crm", "\\exccrmapp");
			selectedApps.put ("32396294_Java-CMS", "\\JavaCMSapp");
			selectedApps.put ("34873434_cms", "\\cmsapp");
			selectedApps.put ("50269397_cms", "\\cmsXapp");
			selectedApps.put ("46690913_plate", "\\plateapp");
			// \\TravelERPname{\texttt{47907539\_Travel\_ERP

			Map<String, Integer> appLocs = new HashMap<> ();
			appLocs.put ("27823030_WEB_SHOP-CMS", 25_825);
			appLocs.put ("44409894_crm",           6_369);
			appLocs.put ("9783982_CMStudio",      15_886);
			appLocs.put ("2114204_RadioCRM",      12_644);
			appLocs.put ("9592899_taglibs",        9_672);
			appLocs.put ("32859973_exc-crm",       9_327);
			appLocs.put ("32396294_Java-CMS",      8_788);
			appLocs.put ("34873434_cms",           6_211);
			appLocs.put ("50269397_cms",           3_425);
			appLocs.put ("46690913_plate",         2_960);

			String cmd = (args.length == 0) ? "app-table" : args[0];
			switch (cmd) {
			case "app-table":
				generateLatexStatisticTable(new File (statsDir, "ABM" + ReportName + ".csv"),
							    selectedApps, appLocs);
				break;
			default:
				Utils.logErr (ABMBenchark.class, "Unknown generation command " + cmd);
			}
		}
	}

	private static class AppInfos implements java.io.Serializable {
		public final String appDir;
		public final String classesDir;
		public final String[] classpath;
		public String cause4exclusion = null;
		AppInfos (String appDir,
			  String classesDir,
			  String[] classpath) {
			this.appDir = appDir;
			this.classesDir = classesDir;
			this.classpath = classpath;
		}
		public String getName () {
			return new File (this.appDir).getName ();
		}
		public String toString () {
			return this.getName ();
		}
		public void recap () {
			System.out.println ("---" + this + "---");
			System.out.println ("- appdir: "+appDir);
			System.out.println ("- classes: "+classesDir);
			System.out.println ("- classpath: "+Arrays.toString(classpath));
			if (this.isExcluded ())
				System.out.println ("- cause for exclusion: "+cause4exclusion);
		}
		public void exclude (String cause) { this.cause4exclusion = cause; }
		public boolean isExcluded () { return this.cause4exclusion != null; }
		public String causeForExclusion () { return this.cause4exclusion; }
	}


	private static class FullAppInfos extends AppInfos {
		public final String zipFile;
		public final String baseDir;
		public final SourcesStatsHelper srcStats;
		FullAppInfos (String zipFile,
			      String appDir,
			      String baseDir,
			      String classesDir,
			      String[] classpath,
			      SourcesStatsHelper srcStats) {
			super (appDir, classesDir, classpath);
			this.zipFile = zipFile;
			this.baseDir = baseDir;
			this.srcStats = srcStats;
		}
		public void recap () {
			super.recap ();
			System.out.println ("- zip: "+zipFile);
			System.out.println ("- basedir: "+baseDir);
			srcStats.recap ();
			if (this.isExcluded ())
				System.out.println ("- cause for exclusion: "+cause4exclusion);
		}
	}

	private static
	void saveAppInfos (File appInfosFile, Map<String, AppInfos> appInfos)
		throws java.io.IOException
	{
		java.io.ObjectOutputStream f = new java.io.ObjectOutputStream
			(new java.io.FileOutputStream (appInfosFile));
		Utils.log (ABMBenchark.class, "Saving app-specific infos into "+ appInfosFile);
		f.writeObject (appInfos);
		f.flush ();
		f.close ();
	}

	private static
	Map<String, AppInfos> loadAppInfos (File appInfosFile)
		throws java.io.IOException, java.lang.ClassNotFoundException
	{
		try {
			java.io.ObjectInputStream f = new java.io.ObjectInputStream
				(new java.io.FileInputStream (appInfosFile));
			Utils.log (ABMBenchark.class, "Loading app-specific infos from "+ appInfosFile);
			Map<String, AppInfos> c = (Map<String, AppInfos>) f.readObject ();
			f.close ();
			return c;
		} catch (java.io.FileNotFoundException _) {
			return new HashMap<String, AppInfos>();
		}
	}

	private static void findNumberOfAnalyzedMethods() {
		ArrayList<File> methFiles = Utils.getFilesOfTypes(outputFolderPath + benchmarkRelativePath, new String[] {".meth"});
		ArrayList<File> secsumFiles = Utils.getFilesOfTypes(outputFolderPath + benchmarkRelativePath, new String[] {".secsum"});
		int exMeths = 0, imMeths=0, tntMeths=0, exSecsum=0, imSecsums=0, tntSecsums=0;
		for(File file: methFiles)
			if(file.getAbsolutePath().toUpperCase().contains("EXPLICIT"))
				exMeths++;
			else
				if(file.getAbsolutePath().toUpperCase().contains("IMPLICIT"))
					imMeths++;
				else
					if(file.getAbsolutePath().toUpperCase().contains("TAINT"))
						tntMeths++;
					else
						Utils.logErr(null, file.getAbsolutePath() + " belongs to no category");

		for(File file: secsumFiles)
			if(file.getAbsolutePath().toUpperCase().contains("EXPLICIT"))
				exSecsum++;
			else
				if(file.getAbsolutePath().toUpperCase().contains("IMPLICIT"))
					imSecsums++;
				else
					if(file.getAbsolutePath().toUpperCase().contains("TAINT"))
						tntSecsums++;
					else
						Utils.logErr(null, file.getAbsolutePath() + " belongs to no category");

		System.out.print("Explicit Rate: " + (float) exSecsum/exMeths + " Implicit Rate: " + (float)imSecsums /imMeths +
				"Taint Rate: " + (float)tntSecsums /tntMeths + "\n");
		System.out.print("exSecsum: " + exSecsum + " exMeths:" +exMeths +
				"imSecsums: " + imSecsums + " imMeths: " + imMeths +
				"tntSecsums: " + tntSecsums + " tntMeths: " +tntMeths);
	}

	private static String[] trimAll (final String[] s) {
		for (int i = 0; i < s.length; i++)
			s[i] = s[i].trim ();
		return s;
	}

	private static void generateLatexStatisticTable(File csvFile) {
		generateLatexStatisticTable (csvFile, null, null);
	}

	private static void generateLatexStatisticTable(File csvFile,
							Map<String, String> selectedApps,
							Map<String, Integer> appLocs) {
		String[] lines = Utils.readStringTextFile(csvFile.getAbsolutePath ()).split("\n");
		Hashtable<String, Integer> attMap = new Hashtable<String,Integer>();
		String[] header = trimAll (lines[0].split (";"));
		for(int index=0; index < header.length; index++)
			attMap.put(header[index], index);
		// ApplicationName; JavaLOC; ByteCodeLOC; #Methods;
		// #MethodsInMethsFile; Mean/Max #Bytecode/Meth; Average
		// #Bytecode/Meth; #Jisymb Errors; Analysis; %Processed Methods;
		// %Processed Methods (excl. Missed by Symmaries);
		// #SkippedBySymmaries; #Missed; #TotalSkipped; #Unsat Guards;
		// #Secure; Average Method Analysis Time; Wall clock time
		// (analysis); System time (analysis); User time (analysis); Max
		// memory (analysis); Exit status (analysis); Mean/Max SCFG
		// Locations; Min/Mean/Max SCFG Locations (Summarized);
		// Min/Mean/Max SCFG Locations (Skipped); Mean/Max SCFG
		// Transitions; Min/Mean/Max SCFG Transitions (Summarized);
		// Min/Mean/Max SCFG Transitions (Skipped); Mean/Max SCFG
		// Variables; Min/Mean/Max SCFG Variables (Summarized);
		// Min/Mean/Max SCFG Variables (Skipped); Min/Mean/Max Support
		// Size; Min/Mean/Max Footprint Size; Mean/Max #Summarizations
		// Per Meth; Wall clock time (types); System time (types); User
		// time (types); Max memory (types); Exit status (types);
		// #Classes; #Interfaces; Size(AliasRel); Density(AliasRel);
		// Size(FieldAliasRel); Density(FieldAliasRel);
		// delta-_FA(java.lang.Object); delta-_FA(java.lang.Object[]);
		// delta-_FA(java.util.Collection)
		Map<String, String> analysisName = new HashMap<>();
		for (SymmariesAnalysis a: SymmariesAnalysis.values ()) {
			if (a == SymmariesAnalysis.HEAP_ONLY) // filter out
				continue;
			analysisName.put
				(Tool.getVariantCode (a,
						      SymmariesHeapDom.DEEPALIAS,
						      BenchmarkExperimentConfig.methodSkipParameter,
						      BenchmarkExperimentConfig.exceptionEnabeled,
						      SynthesisConfiguratons.REALIB_STATIC_REORDER,
						      "2m",
						      "4GiB"),
				 a.toString ());
		}

		// for (String k: analysisName.keySet ())
		// 	System.out.println (k + ": " + analysisName.get(k));


		Map<String, HashMap<String, String[]>> res = new HashMap<>();
		for (String fullline: Arrays.copyOfRange(lines, 1, lines.length)) {
			String[] line = trimAll (fullline.split (";"));
			String analysis = analysisName.get (line[attMap.get ("Analysis")]);
			if (analysis == null)
				continue;
			String app = line[attMap.get("ApplicationName")];
			if (selectedApps != null && ! selectedApps.containsKey (app))
				continue;
			HashMap appm = res.containsKey (app) ? res.get (app) : new HashMap<String, String[]>();
			appm.put (analysis, line);
			res.put (app, appm);
		}

		List<String> appEntries = new LinkedList<> ();
		for (String app: res.keySet ()) {
			boolean headDone = false;
			String out = "";
			for (SymmariesAnalysis a: SymmariesAnalysis.values ()) {
				String[] line = res.get (app).get (a.toString ());
				if (line == null) continue;
				if (!headDone) {
					String appKey = (selectedApps == null || !selectedApps.containsKey (app))
						? app.replaceAll("_", "\\\\_")
						: selectedApps.get (app);
					int sz = res.get (app).size ();
					out += "\\multirow{"+sz+"}{*}{" + appKey + "} &";
					out += "\\multirow{"+sz+"}{*}{" + f1 (((float)appLocs.get (app)) / 1000.f) + "K} & ";
					out += "\\multirow{"+sz+"}{*}{" + is (line[attMap.get("#MethodsInMethsFile")]) + "} & ";
					out += "\\multirow{"+sz+"}{*}{" +(line[attMap.get("Average #Bytecode/Meth")] + "/" +
								     is (line[attMap.get("Mean/Max #Bytecode/Meth")].split("/")[1])) + "}";
					out += "\n       & ";
					headDone = true;
				} else {
					out += " & & & & ";
				}
				out += "\\" + a.toString () + " & ";
				out += is (line [attMap.get ("#SkippedBySymmaries")]) + " & ";
				out += line [attMap.get ("%Processed Methods (excl. Missed by Symmaries)")] + " & ";
				out += f0s (line [attMap.get ("Wall clock time (analysis)")]) + " & ";
				out += f2s (line [attMap.get ("Average Method Analysis Time")]) + " & ";
				if (a.isAlwaysSafe ()) {
					out += " - & - & ";
				} else {
					out += is (line [attMap.get ("#Unsat Guards")]) + " & ";
					out += is (line [attMap.get ("#Secure")]) + " & ";
				}
				out += isa (line [attMap.get ("Mean/Max SCFG Variables")]) + " & ";
				String[] fps = line [attMap.get ("Min/Mean/Max Footprint Size")].split("/");
				out += f2s (fps[1]) + "/" + is (fps[2]);
				out += "\\\\" + "\n";
			}
			appEntries.add (out.replace ("/", "\\,/\\,"));
		}

		System.out.println(String.join ("\\hline\n", appEntries));
	}

	// Enforce thousands separator:
	private static String i(int i) { return String.format ("%,d", i); }
	private static String f0(float f) { return String.format ("%,.0f", f); }
	private static String f1(float f) { return String.format ("%,.1f", f); }
	private static String f2(float f) { return String.format ("%,.2f", f); }
	private static String f3(float f) { return String.format ("%,.3f", f); }
	private static String is(String s) { return i (Integer.parseInt (s)); }
	private static String f0s(String s) { return f0 (Float.parseFloat (s)); }
	private static String f1s(String s) { return f1 (Float.parseFloat (s)); }
	private static String f2s(String s) { return f2 (Float.parseFloat (s)); }
	private static String f3s(String s) { return f3 (Float.parseFloat (s)); }
	private static String isa(String s) {
		String[] x = s.split ("/");
		for (int i = 0; i < x.length; i++)
			try { x[i] = is (x[i]); }
			catch (NumberFormatException _) {
				try { x[i] = f2s (x[i]); }
				catch (NumberFormatException __) {};
			};
		return String.join ("/", x);
	}

	// private static void copySelectedApps() {
	// 	String[] lines = Utils.readStringTextFile(outputFolderPath + "/Misc/selectedApps.csv").split("\n");
	// 	for(int index=1; index < lines.length; index++) {
	// 		String fileName = new File(lines[index].split(";")[0]).getParent().replace("output", "input");
	// 		if(new File(fileName+".jar").exists())
	// 			Utils.copy(fileName+".jar", inputFolderPath + "/WebApplications/Selected/" +  new File(fileName).getName() + ".jar");
	// 		else
	// 			Utils.copy(fileName+".war", inputFolderPath + "/WebApplications/Selected/" + new File(fileName).getName() + ".war");

	// 	}
	// }

	// public class StringComparator implements Comparator {
	// 	@Override
	// 	public int compare(Object o1, Object o2) {
	// 		//			String str1 = (String) o1,  str2 = (String) o2;
	// 		//			if(str1.contains(" ") && str2.contains(" "))
	// 		//				return str1.substring(str1.indexOf(" ")).compareTo(str2.substring(str2.indexOf(" ")));
	// 		//			else
	// 		//				return str1.compareTo(str2);
	// 		return ((String)o1).compareTo((String)o2);

	// 	}
	// }

	// private static void categorizeSecstubs() {
	// 	ArrayList<File> secstubs = Utils.getFilesOfTypes(outputFolderPath + benchmarkRelativePath, new String[] {".secstubs"});
	// 	String[] libraryNames = new String[] {"javax\\.","java\\.", "org\\.apache", "org\\.hibernate", "org\\.springframework","org\\.json", "Misc"};
	// 	HashMap<String,ArrayList<String>> libraryMethods = new HashMap<String,ArrayList<String>>();
	// 	for(int i=0;i <7;i++)
	// 		libraryMethods.put(libraryNames[i], new ArrayList<String>());
	// 	for(File file: secstubs) {
	// 		String[] content = Utils.readStringTextFile(file.getAbsolutePath()).split("\n");
	// 		for(String secstub:content) {
	// 			secstub = secstub.replaceAll("static","").trim();
	// 			boolean flag = false;
	// 			for(int i=0;i <6;i++)
	// 				if(secstub.matches("^\\S+\\s*" + libraryNames[i] + ".*") || secstub.matches(libraryNames[i] + "\\S+\\(.*")) {
	// 					if(!libraryMethods.get(libraryNames[i]).contains(secstub))
	// 						libraryMethods.get(libraryNames[i]).add(secstub);
	// 					flag = true;
	// 					break;
	// 				}
	// 			if(!flag)
	// 				if(!libraryMethods.get(libraryNames[6]).contains(secstub))
	// 					libraryMethods.get(libraryNames[6]).add(secstub);
	// 		}
	// 	}
	// 	for(int i=0;i <7;i++)
	// 		libraryMethods.get(libraryNames[i]).sort(new ABMBenchark().new StringComparator());
	// 	//Collections.sort(sortedKeys, new SootMethodsComparator());

	// 	for(int i=0; i< 7; i++) {
	// 		String fileContent = "";
	// 		for(String str: libraryMethods.get(libraryNames[i]))
	// 			fileContent += str + "\n";
	// 		Utils.writeTextFile(outputFolderPath + benchmarkRelativePath + File.separator + libraryNames[i].replaceAll("\\.", "") + ".secstubs", fileContent);
	// 	}
	// }


	// private static void copySelectedApps1() {
	// 	List<String> apps = Utils.getDirectories(outputFolderPath + "/WebApplications/Selected/Realistic/");
	// 	for(String app: apps) {
	// 		String fileName = inputFolderPath + "/WebApplications/Selected/" +  new File(app).getName();
	// 		if(new File(fileName+".jar").exists())
	// 			Utils.copy(fileName+".jar", inputFolderPath + "/WebApplications/Final/" +  new File(fileName).getName() + ".jar");
	// 		else
	// 			Utils.copy(fileName+".war", inputFolderPath + "/WebApplications/Final/" + new File(fileName).getName() + ".war");

	// 	}
	// }

	// private static void copySelectedApps3() {
	// 	List<String> apps = Utils.getDirectories(outputFolderPath + "/WebApplications/Selected/Realistic/");
	// 	for(String app: apps) {
	// 		String directory = outputFolderPath + "/WebApplications/Selected/Realistic/" +  new File(app).getName();
	// 		Utils.copy(directory, outputFolderPath + "/WebApplications/Final/" +  new File(directory).getName());
	// 	}
	// }


	// private static void copyAnalyzedResults() {
	// 	String[] selectedApps = {
	// 			//"9783982_CMStudio",
	// 			"27823030_WEB_SHOP-CMS",
	// 			"44409894_crm",
	// 			//"32859973_exc-crm","34873434_cms",
	// 			"32396294_Java-CMS","2114204_RadioCRM","9592899_taglibs",
	// 			"47907539_Travel_ERP","46690913_plate","50269397_cms",
	// 			"29078043_totalprint_crm","49790037_crm"};
	// 	String[] exprType = {"ExplicitConf","TaintExpr"};
	// 	for(String config: exprType) {
	// 		for(String app: selectedApps) {
	// 			String source = outputFolderPath + "/WebApplications/FinalJune/" + config + "/" + app;
	// 			String target = outputFolderPath + "/WebApplications/Paper/"  + config + "/" + app;
	// 			Utils.copy(source, target);
	// 			Utils.log(null, "Copied " + source);
	// 		}
	// 	}
	// }

	public Map<String,AppInfos> unzipAllJarfilesInDir(File jarRootDirectory,
							  Map<String,String[]> appClassPaths,
							  Map<String,AppInfos> appInfos)
		throws java.io.IOException
	{
		if (jarRootDirectory.exists()) {
			for (File jarfile : Utils.getFilesOfTypes(jarRootDirectory.getAbsolutePath(), new String[] {".jar",".war"})) {
				String outputSubdirectory = jarfile.getAbsolutePath().replaceAll(inputFolderPath, outputFolderPath);
				String appDir = outputSubdirectory.substring(0, outputSubdirectory.lastIndexOf("."));
				String classesDir = appDir + File.separator + "JarContent";

				File dest = new File (classesDir);
				if (dest.exists () &&
				    org.apache.commons.io.FileUtils.isFileNewer (dest, jarfile)) {
					Utils.log (getClass (), "Skipping unmodified "+ jarfile.getAbsolutePath ());
					continue;
				}

				Utils.uncompressZipFile(jarfile.getAbsolutePath(), classesDir, ".class");

				boolean ok = Utils.getFilesOfTypes (classesDir, new String[] {".class"}).size () > 0;
				File cp = new File (org.apache.commons.io.FilenameUtils.removeExtension
						    (jarfile.getAbsolutePath ()) + "-cp.txt");
				String[] classPath =
					(cp.exists ())
					? new String (Files.readAllBytes (cp.toPath ())).split(":\n")
					: appClassPaths.get (new File (appDir).getName ());
				AppInfos ai = new AppInfos (appDir, classesDir, classPath);
				if (!ok)
					ai.exclude ("No class found in "+ classesDir);
				appInfos.put (appDir, ai);
			}
		}
		return appInfos;
	}

	private static String appClassPathFileFromAppDir (String appDir) {
		return appDir + File.separator + "cp.txt";
	}

	private static String[] appClassPathFromAppDir (String appDir)
		throws java.io.IOException
	{
		String cpf = appClassPathFileFromAppDir (appDir);
		String cp = new String (Files.readAllBytes (Paths.get (cpf)));
		return cp.split (":");
	}

	public Map<String,AppInfos> unzipAllZipfilesInDir (File zipRootDirectory)
		throws java.io.IOException
	{
		return unzipAllZipfilesInDir (zipRootDirectory, new HashMap<>());
	}

	public Map<String,AppInfos> unzipAllZipfilesInDir (File zipRootDirectory,
							   Map<String,AppInfos> appInfos)
		throws java.io.IOException
	{

		if (! zipRootDirectory.exists())
			return appInfos;

		for (File zipfile : Utils.getFilesOfTypes(zipRootDirectory.getAbsolutePath(), new String[] {".zip"})) {
			String outputSubdirectory = zipfile.getAbsolutePath().replaceAll(inputFolderPath, outputFolderPath);
			String appDir = outputSubdirectory.substring(0, outputSubdirectory.lastIndexOf("."));
			String zipContentPath = appDir + File.separator + "ZipContent";

			File dest = new File (zipContentPath);
			if (dest.exists () && FileUtils.isFileNewer (dest, zipfile)) {
				Utils.log (getClass (), "Skipping unmodified "+ zipfile.getAbsolutePath ());
				continue;
			}

			AppInfos ai = unzipAndCompileAppInDir (zipfile, zipContentPath, appDir);
			if (ai != null)
				appInfos.put (appDir, ai);
		}
		return appInfos;
	}

	private AppInfos unzipAndCompileAppInDir (File zipfile,
						  String zipContentPath,
						  String appDir)
		throws java.io.IOException
	{
		Utils.uncompressZipFile(zipfile.getAbsolutePath(), zipContentPath);
		List<Path> poms =
			Files.walk (Paths.get (zipContentPath), Integer.MAX_VALUE)
			.filter (Files::isRegularFile)
			.filter (f -> "pom.xml".equals (f.getFileName ().toString ()))
			.collect (Collectors.toList());

		if (poms.isEmpty ()) {
			Utils.log (getClass (), "Warning: pom.xml not found in " + zipfile.getName ());
			return null;
		} else if (poms.size () > 1) {
			Utils.log (getClass (), "Warning: Multiple pom.xml files found in " + zipfile.getName ());
			Utils.log (getClass (), "Warning: Arbitrarily choosing one pom.xml to build the app.");
		}

		// Totally arbitrarilty take shortest path (note it's string
		// length compare, not path length compare):
		Path pom = poms.stream().min((a, b) -> (a.toString ().length () -
							b.toString ().length ())).get();
		Utils.log (getClass (), "Found " + pom.toString ());

		String appSrcDir = pom.getParent ().toString ();
		java.util.Map<String,String> env = new java.util.HashMap<>();
		env.put ("maven.multiModuleProjectDirectory", appSrcDir);

		// File srcDir = new File (appSrcDir + File.separator + "src" + File.separator + "main" +
		// 			File.separator + "java");
		// if (! srcDir.exists () && poms.size () == 1) {
		// 	Utils.log (getClass (),
		// 		   "Warning: src/main/java does not seem to exist: attempting fix...");
		// 	Utils.createDirectory (srcDir.getAbsolutePath ());
		// 	Utils.relocate (new File (appSrcDir).toPath (), srcDir.toPath (),
		// 			new String[] {".java"});
		// } else if (! srcDir.exists ()) {
		// 	// TODO? maybe some apps need better fixes (e.g
		// 	// attempting to find a pom.xml that comes with a
		// 	// src/main/java directory.
		// 	Utils.logErr (getClass (),
		// 		      "Error: src/main/java does not seem to exist and multiple pom.xml "+
		// 		      "files found: aborting.");
		// 	// Utils.deleteDirectory (zipContentPath);
		// 	return null;
		// }

		Utils.log (getClass (), "Setting up "+ appSrcDir +"...");

		try {
			org.apache.maven.it.Verifier v =
				new org.apache.maven.it.Verifier (appSrcDir, true);
			v.setSystemProperty ("mdep.outputFile", appClassPathFileFromAppDir (appDir));
			String baseDir = v.getBasedir ();
			String classesDir = appDir + File.separator + "JarContent";

			// int attempt = 1;
			// boolean ok = false;
			// do {
			// v.setSystemProperty("jar.finalName", "output");
			v.executeGoals (Arrays.asList("compile", "jar:jar",
						      "dependency:build-classpath"),
					env);
			for (File jarFile : Utils.getFilesOfTypes (baseDir + File.separator + "target",
								   new String[] {".jar",".war"}))
				Utils.uncompressZipFile (jarFile.getAbsolutePath (), classesDir, ".class");

			boolean ok = Utils.getFilesOfTypes (classesDir, new String[] {".class"}).size () > 0;
			// 	if (!ok && attempt == 1) {
			// 		Utils.log(getClass(), "No class file found in "+ classesDir +": attempting fix.");
			// 		tryFixAppSrcDir4Maven (appSrcDir);
			// 	}
			// 	if (!ok) attempt++;
			// } while (! ok && attempt < 2);
			// if (! ok)
			// 	return null;
			AppInfos ai = new FullAppInfos (zipfile.getAbsolutePath (),
							appDir, baseDir, classesDir,
							appClassPathFromAppDir (appDir),
							new SourcesStatsHelper (new File (appSrcDir).toPath ()));
			if (!ok)
				ai.exclude ("No class found in "+ classesDir);
			return ai;
		} catch (org.apache.maven.it.VerificationException e) {
			Utils.log (getClass (), "Exception caught during compilation in "+ appSrcDir);
			e.printStackTrace();
			// Utils.deleteDirectory (zipContentPath);
			return null;
		}
	}


	// private void tryFixAppSrcDir4Maven (String appSrcDir)
	// 	throws java.io.IOException
	// {
	// 	File srcDir = new File (appSrcDir + File.separator + "src" + File.separator + "main" +
	// 				File.separator + "java");
	// 	if (! srcDir.exists () // && poms.size () == 1
	// 	    ) {
	// 		Utils.log (getClass (),
	// 			   "Warning: src/main/java does not seem to exist: attempting fix...");
	// 		Utils.createDirectory (srcDir.getAbsolutePath ());
	// 		Utils.relocate (new File (appSrcDir).toPath (), srcDir.toPath (),
	// 				new String[] {".java"});
	// 	}
	// }

	public void writeApplicationSizeStatistics(File jarRootDirectory) {
		if (jarRootDirectory.exists()) {
			String applicationSizeInfo = "Application;Size;number Of classes;number Of ThirdParty Methods;Number Of Method; Number Of Processed Methods;Average Method Size; Max Method Size, Method Sizes\n";
			for (String outputSubdirectory : Utils.getDirectories(jarRootDirectory.getAbsolutePath())){//, new String[] {".jar",".war"})) {
				//String outputSubdirectory = jarfile.replaceAll(inputPath, "");
				String jarContentPath = jarRootDirectory
						//outputPath
						+ File.separator + outputSubdirectory//.substring(0, outputSubdirectory)
						+ File.separator + "JarContent";
				if(new File(jarContentPath).exists()) {
					String[] methodSize = ApplicationStatsHelper.getMethodSizeStatistics(new File(jarContentPath).getParent()+ "/syrsInput/Meth/");
					applicationSizeInfo += jarContentPath + "; " + Utils.folderSize(new File(jarContentPath)) +
							"; " + Utils.numberOfFiles(new File(jarContentPath)) +
							"; " + Utils.readStringTextFile(new File(jarContentPath).getParent()+ "/syrsInput/Meth/all.secstubs").split("\n").length +
							"; " + Utils.getFilesOfTypes(new File(jarContentPath).getParent()+ "/syrsInput/Meth/", new String[] {".meth"}).size() +
							"; " + Utils.getFilesOfTypes(new File(jarContentPath).getParent()+ "/syrsInput/Meth/", new String[] {".secsum"}).size() +
							"; " + methodSize[0] +
							"; " + methodSize[1] +
							"; " + methodSize[2] +
							"\n";
				}
			}
			//Utils.updateTextFile(outputFolderPath + java.io.File.separator + "WebApplications/statistics.csv", applicationSizeInfo);
			System.out.println(applicationSizeInfo);
		} else
			Utils.log(getClass(), "Directory " + jarRootDirectory + " does not exist!");
	}



	public void compileAppsInDirsToSymmariesFromJarFiles(boolean generateSCGSInputs,
							     int methodSkipParameter,
							     String xmlSecstubsFileName,
							     String[] extraClassPath) throws Exception {

		JARApplicationHelper tool = new JARApplicationHelper(outputFolderPath, inputFolderPath);
		try {
			tool.checkJARsInDirectory(new File(tool.getInputPath() + java.io.File.separator + benchmarkRelativePath),
						  new ArrayList<String>(Arrays.asList("")),
						  "nice",
						  secstubsFiles,
						  xmlSecstubsFileName,
						  getClassesFolderMap(),
						  extraClassPath,
						  exceptionEnabeled,
						  generateSCGSInputs,
						  Constants.symmariesPath,
						  outputFolderPath + File.separator + "Misc/CommandFiles/MacroBench" + ReportName + ".command",
						  BenchmarkExperimentConfig.analysis,
						  BenchmarkExperimentConfig.heapDom,
						  methodSkipParameter,
						  BenchmarkExperimentConfig.enableSootOptimizations,
						  BenchmarkExperimentConfig.abortOnError,
						  false,
						  generateJimple);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// new IFSpecBenchmarkHelper().checkTheResults();
	}

	public void compileAppsInDirsToSymmariesFromClassFiles(boolean generateSymmariesInputs,
							       String[] secstubsPath,
							       String xmlSecstubsFileName,
							       String targetSubFolder,
							       String[] extraClassPath) throws Exception {

		JARApplicationHelper tool = new JARApplicationHelper(outputFolderPath, inputFolderPath);
		try {
			tool.checkApplicationsInDirectory(new File(tool.getOutputPath() + java.io.File.separator + benchmarkRelativePath),
							  targetSubFolder,
							  new ArrayList<String>(// Arrays.asList("protected","34611071_cmsPuzzle")
										),
							  "",
							  xmlSecstubsFileName,
							  secstubsPath,
							  getClassesFolderMap(),
							  extraClassPath,
							  exceptionEnabeled,
							  generateSymmariesInputs,
							  Constants.symmariesPath,
							  outputFolderPath + File.separator + "Misc/CommandFiles/ABC" + ReportName + targetSubFolder + ".command",
							  analysis, heapDom,
							  methodSkipParameter,
							  BenchmarkExperimentConfig.enableSootOptimizations,
							  BenchmarkExperimentConfig.abortOnError,
							  false,
							  generateJimple);

		} catch (Exception e) {
			e.printStackTrace();
		}
		// new IFSpecBenchmarkHelper().checkTheResults();
	}

	public void compileAppInDirToSymmariesFromClassFiles(AppInfos ai,
							     boolean generateSymmariesInputs,
							     String[] secstubsPath,
							     String xmlSecstubsFileName,
							     String commandVariant)
	{
		JARApplicationHelper tool = new JARApplicationHelper(outputFolderPath, inputFolderPath);
		File dest = new File (outputFolderPath + File.separator + "SyrsWorks", ai.getName ());
		File src = new File (ai.classesDir);
		String cmdFileNameBase = outputFolderPath + File.separator + "Misc/CommandFiles/" + ReportName + ai + ".command";
		if (dest.exists () && FileUtils.isFileNewer (dest, src)) {
			Utils.log (getClass (), commandVariant + ": Skipping full generation from unmodified "+ src.getAbsolutePath ());
			generateSymmariesInputs = false;
		} else {
			Utils.log (ABMBenchark.class,
				   "Generating (remaining) Symmaries inputs for "+ ai +" ("+ commandVariant +")");
		}

		tool.specializeCommandFileName = true;
		tool.checkApplicationInDirectory(ai.appDir,
						 ai.classesDir,
						 dest.getAbsolutePath (),
						 new ArrayList<String>(// Arrays.asList("protected","34611071_cmsPuzzle")
										),
						 "",
						 xmlSecstubsFileName,
						 secstubsPath,
						 getClassesFolderMap(),
						 ai.classpath,
						 exceptionEnabeled,
						 generateSymmariesInputs,
						 Constants.symmariesPath,
						 cmdFileNameBase,
						 analysis, heapDom,
						 methodSkipParameter,
						 BenchmarkExperimentConfig.enableSootOptimizations,
						 BenchmarkExperimentConfig.abortOnError,
						 false,
						 generateJimple);
	}

	private HashMap<String, String> getClassesFolderMap() {
		HashMap<String,String> results = new HashMap<String,String>();
		String fileContent =
			Utils.readStringTextFile(inputFolderPath + // java.io.File.separator + "WebApplications"
						 java.io.File.separator + benchmarkRelativePath + File.separator + "classesFolderPath.txt");
		if (! fileContent.equals (""))
			for(String line: fileContent.split ("\n"))
				results.put(line.split(",")[0].trim(), line.split(",")[1].trim());
		return results;
	}

	protected void generatXlsxStatisticsReport(File statsDir,
						   // String statisticsReportName,
						   // String symmariesResultsReportName,
						   // String datasetFilePath,
						   String sheetFileBase,// ,
						   Collection<String> algoIds) {
		Utils.createDirectory (statsDir.getAbsolutePath ());
		generatXlsxStatisticsReport (new File (outputFolderPath + File.separator + "SyrsWorks"),
					     new File (statsDir, sheetFileBase + ".csv"),
					     algoIds);
	}

	private static Hashtable<String, Integer> getAPplicationLOCs() {
		return getAPplicationLOCs (outputFolderPath + "/Misc/PerformanceReports/ABM/ApplicationSize.csv");
	}

	private static Hashtable<String, Integer> getAPplicationLOCs(String csvPath) {
		String[] lines = Utils.readStringTextFile(csvPath).split("\n");
		Hashtable<String, Integer> applicationLocs = new Hashtable<String,Integer>();
		for(int index=1; index < lines.length; index++) {
			String[] line = lines[index].split(";");
			applicationLocs.put(new File(line[0].trim()).getParentFile().getName(), Integer.parseInt(line[1].trim()));
		}
		return applicationLocs;
	}
}
