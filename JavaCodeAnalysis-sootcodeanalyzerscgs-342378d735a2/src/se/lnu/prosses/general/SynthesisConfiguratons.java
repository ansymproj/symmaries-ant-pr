package se.lnu.prosses.general;

import java.util.ArrayList;
import java.util.List;

public class SynthesisConfiguratons {
	//public String androidJarPath;
	public String callbacks;

	public SymmariesAnalysis analysis = SymmariesAnalysis.HEAP_ONLY;
	public SymmariesHeapDom heapDom = SymmariesHeapDom.DEEPALIAS;

	public boolean generateJimple = true;
	public boolean generateCfg = false;
	public boolean generateSymmScfg = false;
	public String[] requiredClassesPaths;
	public boolean ignoreCheckPoints;
	public boolean monitorType = true;
	public boolean loadFromApk;
	public boolean enableSootOptimizations = false; // default?

	public static final String REALIB_NO_REORDER = "-r";
	public static final String REALIB_STATIC_REORDER = "-dr";
	public static final String REALIB_DYNAMIC_REORDER = "+dr";
	public String realibReorder = REALIB_STATIC_REORDER;

	public int methodSkipParameter = 0;
	public String timeout = "2m";
	public String cuddMem = "4GiB";

	//public String scgsPath;
	public boolean exceptionsEnabled = false;
	public boolean clearOldGeneratedfiles = false;
	public List<String> excludedMathods = new ArrayList<String>();
	public List<String> excludedClasses = new ArrayList<String>();
	public String outputPath;
	public String inputPath;

	public String defaultSecuritySummary = "-<~;";

	public String monitorHelperPath;
	public List<String> thirdPartyMethods = new ArrayList<String>();

	public String SymmariesCommandFile = "";
	public String symmariesPath = "";

	public String[] assumedSecSigsFilePath;
	public String xmlSecsstubsPath = "";
	public String xmlSourcesAndSinks;
	public boolean abortOnError = false;
	public boolean allowInconsistentTypes = false;

	public SynthesisConfiguratons() {}

	public SynthesisConfiguratons(SymmariesAnalysis analysis, boolean e) {
		this.analysis = analysis;
		ignoreCheckPoints = false;
	}

	/*	public boolean isLocalReax() {
		if (reaxPath == null || isValidIP(reaxPath))
			return false;
		return true;
	}*/

	// Check if the IP is valid
	/*	private boolean isValidIP(String IP) {
		String[] IpParts = IP.split("\\.");
		if (IpParts.length == 4) {
			for (int i = 0; i < 4; i++) {
				try {
					int intIpParts = Integer.parseInt(IpParts[i]);
					if (intIpParts < 0 || intIpParts > 255) {
						return false;
					}

				} catch (NumberFormatException e) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}*/

}
