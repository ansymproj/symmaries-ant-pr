package se.lnu.prosses.Benchmarks;

import java.io.File;
import java.util.ArrayList;

import se.lnu.prosses.general.SymmariesAnalysis;
import se.lnu.prosses.general.SymmariesHeapDom;
import se.lnu.prosses.general.SynthesisConfiguratons;
import se.lnu.prosses.general.Constants;
import se.lnu.prosses.tool.APKApplicationHelper;
import se.lnu.prosses.tool.JavaApplicationHelper;

public class MainNormal extends BenchmarkExperimentConfig {
	public static void main(String[] args) throws Exception {
		benchmarkRelativePath = "/WebApplications/Final/";
		ReportName = new File(benchmarkRelativePath).getName();
		methodSkipParameter = 0;
		analysis = SymmariesAnalysis.EXPLICIT_CONF;
		heapDom = SymmariesHeapDom.DEEPALIAS;
		exceptionEnabeled = false;
		abortOnError = false;
		generateJimple = false;
		String targetSubfolder = analysis.toString ();

		//JimpleProjectHelper project = new JimpleProjectHelper();
		//		project.configurations.xmlSecsstubsPath = inputFolderPath+ "Secstubs/allSecStubs.xml";
		//		project.loadSecstubsFromXMLFile();
		//		String output = "";
		/*for(ArrayList<SecuritySignature> methods: project.securitySignatures.values())
			for(SecuritySignature ss: methods)
				output += ss.toString();
		Utils.writeTextFile(inputFolderPath+ "Secstubs/allSecStubs.secstubs", output);*/

		new JavaApplicationHelper().processSingleJavaApplication(inputFolderPath + "/academy-web-target/classes/",
									 outputFolderPath + "/academy-web-target/" + targetSubfolder,
									 "",
									 inputFolderPath + "Secstubs/allSecStubs.xml",
									 secstubsFiles,
									 exceptionEnabeled,
									 true,
									 Constants.symmariesPath,
									 outputFolderPath + "/Misc/CommandFiles/Omegapoint" + targetSubfolder + "Command.command",
									 analysis,
									 heapDom,
									 120,
									 false, // no soot optim
									 false,
									 false, generateJimple);
		//processJavaApplicationsFromClassFiles();
		//processJavaApplications();
		//processAndroidApplications(false);
	}

	private static void processAndroidApplications(boolean generateSCGSInputs) {
		ArrayList<String> excludedApplications = new ArrayList<String>();
		String targetSubdirectory = "/Android/FDroid/";
		// targetSubdirectory = "IFSpec";//"/Android";///ICC-Bench-master";
		File apkDirectory = new File(inputFolderPath + targetSubdirectory);
		APKApplicationHelper tool = new APKApplicationHelper(// inputFolderPath + targetSubdirectory,
								     outputFolderPath, inputFolderPath);
		try {
			tool.checkAPKsInDirectory(apkDirectory, outputFolderPath, excludedApplications, targetSubdirectory, "nice", exceptionEnabeled,
					outputFolderPath + File.separator + "Misc/CommandFiles/" + new File(targetSubdirectory).getName() + ".command", generateSCGSInputs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// inputSubdirectory =
		// "/Volumes/Academics/Dropbox/Workspaces/SCGSWorkspace/ScgsInputGenertaor/input/Android/FDroid/NotWorkingSCGS";
		// processSingleAPKFile("me.kuehle.carreport_79.apk",inputPath,"FDroid",
		// "bad");// line 12, character 11: syntax error at token `.'

	}

}
