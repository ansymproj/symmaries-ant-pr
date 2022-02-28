package se.lnu.prosses.jimpleProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import se.lnu.prosses.general.Constants;
import se.lnu.prosses.general.SynthesisConfiguratons;
import se.lnu.prosses.general.Utils;
import se.lnu.prosses.jimpleProcessor.JimpleProjectHelper.SecuritySignature;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JCaughtExceptionRef;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceOfExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JThrowStmt;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;
import soot.util.cfgcmd.CFGToDotGraph;

public class SymmariesHelper {
	JimpleProjectHelper project;
	//public SynthesisConfiguratons configurations;
	public List<String> underProcessingMethods = new ArrayList<String>();
	public List<String> processedMethods = new ArrayList<String>();
	ThrowAnalysis throwAnalysis = soot.toolkits.exceptions.UnitThrowAnalysis.v();
	TypeProcessor typeProcessor;
	private Value renameToThisVariable;
	public List<Type> dummyLowSinkMethodsList = new ArrayList<Type>();
	public List<Type> dummyHighSourceMethodsList = new ArrayList<Type>();
	public List<Type> dummyHighSinkMethodsList = new ArrayList<Type>();
	public List<Type> dummyLowSourceMethodsList = new ArrayList<Type>();
	private List<SootClass> classInitializers = new ArrayList<SootClass>();


	public SymmariesHelper(JimpleProjectHelper jimpleProjectHelper, SynthesisConfiguratons configurations1,
			TypeProcessor tp, SourceSinkHelper sourceSinkHelper) {
		project = jimpleProjectHelper;
		typeProcessor = tp;
		//configurations = configurations1;

	}

	private void addDummyLowSinkMethod(Type type) {
		if (!dummyLowSinkMethodsList.contains(type))
			dummyLowSinkMethodsList.add(type);
	}

	private void addDummyHighSinkMethod(Type type) {
		if (!dummyHighSinkMethodsList.contains(type))
			dummyHighSinkMethodsList.add(type);
	}

	private void addDummyHighSourceMethod(Type type) {
		if (!dummyHighSourceMethodsList.contains(type))
			dummyHighSourceMethodsList.add(type);
	}

	private void addDummyLowSourceMethod(Type type) {
		if (!dummyLowSourceMethodsList.contains(type))
			dummyLowSourceMethodsList.add(type);
	}

	private void addToTheAccessedFieldsList(SootField field) {
		if (!typeProcessor.allAccessedFields.contains(field))
			typeProcessor.allAccessedFields.add(field);
	}

	// A method to find Object variables that can be rewritten:
	// a sequence `java.lang.Object $r1; $r1 = o.method(); C $r2; $r2 = (C) $r1;'
	// can be
	// rewritten `C $r2; $r2 = (C) o.method();'.

	private void buildLibrarySecurityAssumption(SootMethod method, List<String> update) throws TransformationException {
		if (project.getLibrarySecurityAssumptions().containsKey(method) && update.isEmpty())
			return;
		ArrayList<String> assumptions = new ArrayList<String>();
		if(method.isConstructor()) {
			assumptions.add("-<~");
			assumptions.add(".*<~@");
			/*for (SecuritySignature ss : project.securitySignatures.get(""))
					if (ss.matches(method) && (project.getLibrarySecurityAssumptions().get(method)==null ||
							project.getLibrarySecurityAssumptions().get(method)!=null && !project.getLibrarySecurityAssumptions().get(method).contains(ss.secSignature))&&
							!assumptions.contains(ss.secSignature)) {
						assumptions +=  ss.secSignature;
					}*/
		}
		else if(project.securitySignatures.containsKey(method.getName())) {
			for (SecuritySignature ss : project.securitySignatures.get(method.getName())) {
				if (ss.matches(method) && (project.getLibrarySecurityAssumptions().get(method)==null ||
							   project.getLibrarySecurityAssumptions().get(method)!=null)) {
					for(String tmp: ss.secSignature)
						if(!assumptions.contains(tmp))
							assumptions.add(tmp);
				}
			}
		}
		if (project.sourceSinkHelper.isHighSource(method))
			if(!method.isStatic()){
				assumptions.add("return .*");
			}
			else
				assumptions.add("return *");
		if(assumptions.isEmpty() && !project.getLibrarySecurityAssumptions().containsKey(method))
			assumptions.addAll(generateSecuritySummary(method));
		else
			assumptions.addAll(update);
		//+=  update + (update.trim().isEmpty()? "":";") + (project.getLibrarySecurityAssumptions().containsKey(method)? project.getLibrarySecurityAssumptions().get(method): "");

		/*if(method.isStatic() && assumptions.contains(".*")) {
			Utils.logErr(getClass(), method.getName() + " is static but its security siganture returns/uses .*. Replaced with return *.");
			assumptions = assumptions.replaceAll("\\.\\*", "\\*");
		}
		//assumptions = "  zzzz ; yhdn; ; uuu ;";
		if(!assumptions.trim().endsWith(";"))
			assumptions += ";";
		Matcher m = Pattern.compile("[^;]*;").matcher(assumptions);
		String assumptions1 = "";
		while (m.find()) {
			String tmp = m.group();
			if(!tmp.trim().equals(";"))
				assumptions1 += tmp;
		}*/

		project.getLibrarySecurityAssumptions().put(method, assumptions);
	}

	private ArrayList<String> generateSecuritySummary(SootMethod method) {
		// getter, setter, to, create, is, has, print, send, close, update
		//if(method.)
		if(!project.methodsWithAutoGeneratedSecSignature.contains(method))
			project.methodsWithAutoGeneratedSecSignature.add(method);

		// low sinks
		if((method.getName().contains("log") || method.getName().contains("print")|| method.getName().contains("write")
				|| method.getName().contains("send"))
				&& method.getReturnType().toString().equals("void"))
			return new ArrayList<String>(Arrays.asList("-<~" , "output_L"));

		if(method.getName().startsWith("set") && method.getReturnType().toString().equals("void"))
			if(!method.isStatic())
				return new ArrayList<String>(Arrays.asList(".*<~@"));
			else
				return new ArrayList<String>(Arrays.asList("*<~@"));
		if(method.getName().equals("put") && method.getReturnType().equals(method.getDeclaringClass().getType()))
			if(!method.isStatic())
				return new ArrayList<String>(Arrays.asList(".*<~@","return .*"));
			else
				return new ArrayList<String>(Arrays.asList("*<~@","return *"));
		if(method.getName().equals("put") && method.getReturnType().toString().equals("void"))
			if(!method.isStatic())
				return new ArrayList<String>(Arrays.asList(".*<~@"));
			else
				return new ArrayList<String>(Arrays.asList("*<~@"));
		if(method.getName().equals("close") && method.getReturnType().toString().equals("void"))
			return new ArrayList<String>(Arrays.asList("-<~"));

		if((method.getName().startsWith("is") || method.getName().startsWith("has")) &&
				method.getReturnType().toString().equals("boolean") && !method.isStatic())
			return new ArrayList<String>(Arrays.asList("-<~","return *"));

		//return something new that depends on args
		if(method.getName().startsWith("to") &&
				(!(method.getName().length()>2) || method.getName().substring(2,3).matches("[A-Z]")) &&
				method.getParameterCount()<=1)
			return new ArrayList<String>(Arrays.asList("-<~","return @"));
		// returns an existing object/field
		if((method.getName().startsWith("get") || method.getName().startsWith("find")))
			if(method.getParameterCount()==0)
				if(!method.isStatic())
					return new ArrayList<String>(Arrays.asList("-<~","return .*"));
				else
					return new ArrayList<String>(Arrays.asList("-<~","return *"));
		project.methodsWithAutoGeneratedSecSignature.remove(method);
		if(!project.methodsWithNoSecSignature.contains(method))
			project.methodsWithNoSecSignature.add(method);
		if(!method.getReturnType().toString().equals("void"))
			if(method.isStatic())
				return new ArrayList<String>(Arrays.asList("-<~","return *"));
			else
				return new ArrayList<String>(Arrays.asList("-<~","return .*"));
		else
			return new ArrayList<String>(Arrays.asList("-<~"));
	}

	public String constructHeader(SootMethod method, boolean library, String callerClass, List<Value> modifiedArgs) throws TransformationException {

		String ret = (method.isStatic() ? "static " : "");
		ret += (method.isConstructor() || method.isStaticInitializer() ? "" : method.getReturnType().toString() + " ");
		//ret += method.getDeclaringClass().getName();
		ret += callerClass;
		if(!method.isStaticInitializer()) {
			ret += (method.isConstructor() ? "" : ":" + project.escapeMethodName(method.getName())) + "(";
			String separator = "";
			if (!library)
				for (Value par : method.retrieveActiveBody().getParameterLocals()) {
					ret += separator + JimpleProjectHelper.escapeTypeName(par.getType()) + " "
							+ (modifiedArgs.contains(par) ? renameArg(par) : project.getSymmariesExpression(par, null, typeProcessor)) ;
					separator = ",";
				}
			else //if (args != null)
				for (int index = 0; index < method.getParameterCount(); index++) {
					ret += separator + method.getParameterType(index) + " ";
					separator = ",";
				}
			ret += ")";
		}
		return ret;
	}

	private String constructMethodCallExpr(Unit invoke) throws TransformationException {
		InvokeExpr invokeExpression = SootUtilities.getInvokeExpr(invoke);
		SootMethod method = invokeExpression.getMethod();
		boolean isLibrary = SootUtilities.isLibraryMethodCall(method, project.configurations.loadFromApk, project.configurations.thirdPartyMethods,
				project.getMethodUniqueName(method, SootUtilities.getInvokeBaseType(invoke)));
		String base = (SootUtilities.isStaticInvoke(invoke) ? SootUtilities.getClassName(method.getDeclaringClass())
				: project.getSymmariesExpression(SootUtilities.getInstanceInvkBase(invoke), this.renameToThisVariable,
						typeProcessor));
		if(SootUtilities.isStaticInvoke(invoke))
			typeProcessor.addsubtypeSupertypeRelation(method.getDeclaringClass().getType(), method.getDeclaringClass().getType());
		String ret = "", separator = "", sinkDefinition = "", sourceDefinition="";
		if (SootUtilities.isSpecialInvoke(invoke))
			ret = base + "#" + SootUtilities.getClassName(method.getDeclaringClass()) + ":"
					+ project.escapeMethodName(invokeExpression.getMethod().getName()) + "(";
		else
			ret = base + "." + project.escapeMethodName(invokeExpression.getMethod().getName()) + "(";

		for (int index = 0; index < invokeExpression.getArgs().size(); index++) {
			Value arg = invokeExpression.getArgs().get(index);
			// if(SootUtilities.isFieldRef(arg))
			// allAccessedFields.add(arg);
			String argName = project.getSymmariesExpression(arg, this.renameToThisVariable, typeProcessor).toString().trim();
			Type castedArgType = (SootUtilities.isNullConstant(arg)
					//|| SootUtilities.isPrimType(method.getParameterType(index))
					|| method.getParameterType(index).equals(invokeExpression.getArgs().get(index).getType()) ? null
							: method.getParameterType(index));
			typeProcessor.processTypes(method.getParameterType(index), arg);
			if (castedArgType != null)
				typeProcessor.addsubtypeSupertypeRelation(arg.getType(), castedArgType);
			if (argName.matches("class\\s\".*\"")) {
				Constant cst = ClassConstant.v(argName);
				argName = cst.getType().toString();
				// argName = argName.split("\"")[1];
			}
			ret += separator + (castedArgType == null ? "" : "(" + castedArgType + ")") + argName;
			sinkDefinition += this.getSinkParameterStatments(method, index, arg);
			sourceDefinition += this.getSourceParameterStatments(method, index, arg, isLibrary);

			separator = ",";
		}
		if(!SootUtilities.isStaticInvoke(invoke) ) {
			sinkDefinition += getSinkBaseStatments(method, SootUtilities.getInvokeBase(invoke));
			sourceDefinition += getSourceBaseStatments(method, SootUtilities.getInvokeBase(invoke), isLibrary);
		}

		if(isLibrary)
			updateLibraryMethodSummaries(method);

		ret += ")";

		String retSinkDefinition="", retSourceDefinition="";
		if (SootUtilities.isAssign(invoke)) {
			JAssignStmt as = (JAssignStmt) invoke;
			ret = project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + " = " + ret;
			if(project.sourceSinkHelper.returnsHighSink(method) || project.sourceSinkHelper.returnsLowSink(method))
				retSinkDefinition += getSinkReturnStatments(method, as.leftBox.getValue());
			if(project.sourceSinkHelper.returnsHighSource(method) || project.sourceSinkHelper.returnsLowSource(method))
				retSourceDefinition += getSourceReturnStatments(method, as.leftBox.getValue());

		}

		//	if(isLibrary)
		return  //checkpointDefinition + //(checkpointDefinition.trim().isEmpty()?"":";\n") +
				sinkDefinition + //(sinkDefinition.trim().isEmpty()?"":";\n") +
				sourceDefinition + // (sourceDefinition.trim().isEmpty()?"":";\n") +
				ret +
				(retSinkDefinition.trim().isEmpty()?"" : ";\n" + retSinkDefinition) +
				(retSourceDefinition.trim().isEmpty()?"" : ";\n" + retSourceDefinition);
		//	else
		//		return
		//				sinkDefinition + //(sinkDefinition.trim().isEmpty()?"":";\n") +
		//checkpointDefinition + //(checkpointDefinition.trim().isEmpty()?"":";\n") +
		//				sourceDefinition + //(sourceDefinition.trim().isEmpty()?"":";\n") +
		//				ret;
	}

	private void updateLibraryMethodSummaries(SootMethod method) throws TransformationException {
		if(project.sourceSinkHelper.returnsHighSource(method))
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("return *h?")));
		if( project.sourceSinkHelper.returnsLowSource(method))
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("return *l?")));

		if( project.sourceSinkHelper.isHighSource(method))
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("@<~@", "*<~@", "return *h")));

		if( project.sourceSinkHelper.isLowSink(method)) {
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("output_L")));
		}

		if( project.sourceSinkHelper.isHighSink(method)) {
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("@!")));
		}
		if( project.sourceSinkHelper.isLowSource(method)) {
			buildLibrarySecurityAssumption(method, new ArrayList<String> (Arrays.asList("-!", "return new_?")));//@<~@; *<~@;return new_?");
		}
	}

	private HashMap<String, Unit> getRemovableObjectVariablesFromBody(Body body) {
		HashMap<String, Unit> objectVariables = new HashMap<String, Unit>();
		List<String> toRemoveObjects = new ArrayList<String>();
		// get all java.lang.Objects first
		for (Local local : body.getLocals())
			if (local.getType().toString().equals(Constants.OBJECT))
				objectVariables.put(local.toString(), null);

		if (objectVariables.size() == 0)
			return null;

		// remove those java.lang.Object that are used only once in the left hand side
		for (String local : objectVariables.keySet())
			for (Unit unit : body.getUnits()) {
				boolean isLocalAssigned = false, isLocalUsed = false;
				for (ValueBox useBox : unit.getDefBoxes())
					if (useBox.getValue().toString().equals(local))
						isLocalAssigned = true;
				for (ValueBox useBox : unit.getUseBoxes())
					if (useBox.getValue().toString().equals(local))
						isLocalUsed = true;
				if (isLocalAssigned && objectVariables.get(local) == null)
					objectVariables.put(local, unit);
				else if (isLocalAssigned || isLocalUsed)// && SootUtilities.varUsed(unit,local)))
				{ // this means that the variable is assigned more than once; we support simple
					// cases for now
					toRemoveObjects.add(local);
					break;
				}
			}
		// Utils.log(this.getClass()"test");

		for (String local : toRemoveObjects)
			objectVariables.remove(local);

		return objectVariables;
	}



	private void postProcessMethod(SootMethod method, String methodUniqueName, String output, Body body) {
		// Utils.log(this.getClass() ,"Finished processing " +
		// method.getDeclaringClass() + "." + method.getName());
		Utils.writeTextFile(project.configurations.outputPath + "/Meth/" + methodUniqueName + ".meth", output);
		if (project.configurations.generateJimple)
			Utils.writeTextFile(project.configurations.outputPath + "/Jimple/" + methodUniqueName + ".jimple", body.toString());
		processedMethods.add(methodUniqueName);
		// toBeProcessedMethods.remove(methodUniqueName);
	}

	private Body preProcessAndGetMethodBody(SootMethod method, String declaringClass, String methodUniqueName)
			throws TransformationException, java.lang.RuntimeException {

		typeProcessor.constructTypeHierarchyFromClassDef(method.getDeclaringClass());
		for (Type type : method.getParameterTypes())
			//	typeProcessor.constructTypeHierarchyFromTypeDef(type, method.getDeclaringClass());
			typeProcessor.addsubtypeSupertypeRelation(type, type);

		if (SootUtilities.isLibraryMethodCall(method, project.configurations.loadFromApk, project.configurations.thirdPartyMethods,
				project.getMethodUniqueName(method, declaringClass))) {
			buildLibrarySecurityAssumption(method, new ArrayList<String>());
			return null;
		}

		if (method.isAbstract () ||
		    method.getDeclaringClass ().isInterface ())
			return null;

		if (this.processedMethods.contains(methodUniqueName) ||
		    this.underProcessingMethods.contains(methodUniqueName))
			return null;

		Body body = null;
		try {
			body = method.retrieveActiveBody();
		} catch (Exception ex) {
			Utils.log(this.getClass(), "The body of " + methodUniqueName + " could not be retrived!");
			return null;
		}
		typeProcessor.addsubtypeSupertypeRelation(method.getDeclaringClass().getType(),
							  method.getDeclaringClass().getType());
		//typeProcessor.constructTypeHierarchyFromClassDef(method.getDeclaringClass());
		for (Local value : method.retrieveActiveBody().getLocals())
			typeProcessor.addsubtypeSupertypeRelation(value.getType(), value.getType());
		//.constructTypeHierarchyFromTypeDef(value.getType(), method.getDeclaringClass());
		if(containRedeclaredVariable(method))
			// throw new TransformationException("The method " + project.getMethodUniqueName(method, declaringClass) + " redeclare a variable!");
			return null;

		underProcessingMethods.add(methodUniqueName);
		// Utils.log(this.getClass()"Extracting CFG for " + method.getDeclaringClass() +
		// "." +
		// method.getName() + " has started.");

		// ExceptionalUnitGraph cfg = new soot.toolkits.graph.ExceptionalUnitGraph(body, throwAnalysis);
		// CFGToDotGraph todot = new CFGToDotGraph();
		// try{
		// 	//	todot.drawCFG(cfg, body)
		// 	//	.plot(project.configurations.targetPath + "/CFG/" + File.separator + method.getName() + "cfg.dot");
		// } catch(RuntimeException ex){
		// 	Utils.logErr(getClass(), "Could not export the control flow graph of the method " +  method.getDeclaringClass() + "." + method.getName()
		// 	//+ ex.getMessage()
		// 			);
		// }
		return body;
	}

	private boolean containRedeclaredVariable(SootMethod method) {
		for (Local value1 : method.retrieveActiveBody().getLocals())
			for (Local value2 : method.retrieveActiveBody().getLocals())
				if((value1!=value2) && value1.getName()==value2.getName())
					return true;
		return false;
	}

	private String processAllInvokation(Unit unit) throws TransformationException {
		updateAccessedFields(unit);
		if (SootUtilities.isInitInvokation(unit))
			return processInstanceInvokationAssignment(unit); // constructors
		try {
			return processInvokation(unit);// non constructors
		} catch (TransformationException e) {
			throw new TransformationException(e.getLocalizedMessage());
		}
		catch(RuntimeException e) {
			throw new TransformationException("Could not transform the invocation " + unit);
		}
	}

	private String processAssigment(Unit unit, Body body) throws TransformationException {
		JAssignStmt as = (JAssignStmt) unit;
		typeProcessor.processTypes(as.leftBox.getValue().getType(), as.rightBox.getValue());
		String sourceDefinition = "", sinkDefinition = "";
		if (SootUtilities.isFieldLoad(as)) {
			addToTheAccessedFieldsList(SootUtilities.getField(as.rightBox.getValue()));
			sourceDefinition = getSourceFieldStatment(as);
			return (sourceDefinition.trim().isEmpty()? project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + "="
					+ project.getSymmariesExpression(as.rightBox.getValue(), renameToThisVariable, typeProcessor): sourceDefinition);
		} else if (SootUtilities.isFieldStore(as)) {
			addToTheAccessedFieldsList(SootUtilities.getField(as.leftBox.getValue()));
			sinkDefinition = getSinkFieldStatment(as);
			return project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + "="
			+ project.getSymmariesExpression(as.rightBox.getValue(), renameToThisVariable, typeProcessor)
			+ (sinkDefinition.trim().isEmpty()? "" : ";\n" +  sinkDefinition);

		} else if (as.rightBox.getValue() instanceof InvokeExpr)
			return processInvokation(as);
		else if (SootUtilities.isNewStmt(unit))
			return project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + "="
			+ (as.rightBox.getValue().toString().replaceAll("-", ""));
		else if (as.rightBox.getValue() instanceof JInstanceOfExpr) {
			JInstanceOfExpr expr = (JInstanceOfExpr) as.rightBox.getValue();
			typeProcessor.addsubtypeSupertypeRelation(expr.getOp().getType(), expr.getCheckType());
			return project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + "="
			+ project.getSymmariesExpression(expr.getOp(), renameToThisVariable, typeProcessor) + " instanceof "
			+ JimpleProjectHelper.escapeTypeName(expr.getCheckType());
		} else if (as.rightBox.getValue() instanceof ClassConstant) {
			ClassConstant expr = (ClassConstant) as.rightBox.getValue();
			Type classType = soot.Scene.v().getSootClass("java.lang.Class").getType ();
			typeProcessor.addsubtypeSupertypeRelation (as.leftBox.getValue().getType(), classType);
			return project.getSymmariesExpression (as.leftBox.getValue(), renameToThisVariable, typeProcessor) +
				"=" + expr.toSootType();
		}
		return //sinkDefinition
				//+ sourceDefinition +
				(!SootUtilities.isFieldLoad(as) || sourceDefinition.trim().isEmpty()? project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) + "="
						+ project.getSymmariesExpression(as.rightBox.getValue(), renameToThisVariable, typeProcessor) : "") ;
	}


	private String getSourceFieldStatment(JAssignStmt as) throws TransformationException {
		String sourceDefinition = "";
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.isLowSource(SootUtilities.getField(as.rightBox.getValue()))){
			// The right value will be leaked through the left hand side
			sourceDefinition =
					project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) +
					"=" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "()";
			this.addDummyLowSourceMethod((as.rightBox.getValue()).getType());
		}
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.isHighSource(SootUtilities.getField(as.rightBox.getValue()))){
			sourceDefinition =  project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor) +
					"=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "()";
			this.addDummyHighSourceMethod((as.rightBox.getValue()).getType());
		}
		return sourceDefinition;
	}

	private String getSinkFieldStatment(JAssignStmt as) throws TransformationException {
		String sinkDefinition = "";
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.isHighSink(SootUtilities.getField(as.leftBox.getValue()))) {
			sinkDefinition = Constants.dummyClass + "." + Constants.highSinkMethodName + "(" +
					project.getSymmariesExpression(as.leftBox.getValue(), renameToThisVariable, typeProcessor)+ ")";
			this.addDummyHighSinkMethod(as.leftBox.getValue().getType());

		}
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.isLowSink(SootUtilities.getField(as.leftBox.getValue()))) {
			sinkDefinition += Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" +
					project.getSymmariesExpression(as.rightBox.getValue(), renameToThisVariable, typeProcessor)+ ")";
			this.addDummyLowSinkMethod(as.leftBox.getValue().getType());
		}
		return sinkDefinition;
	}

	private String processInstanceInvokationAssignment(Unit invoke) throws TransformationException {
		SootMethod method = SootUtilities.getInvokeExpr(invoke).getMethod();
		boolean isLibrary = SootUtilities.isLibraryMethodCall(method, project.configurations.loadFromApk, project.configurations.thirdPartyMethods,
				project.getMethodUniqueName(method, SootUtilities.getInvokeBaseType(invoke)));
		if (isLibrary) {
			buildLibrarySecurityAssumption(method, new ArrayList<String>());//, SootUtilities.getInvokeArgs(invoke));
		} else {
			String type = "";
			if (invoke instanceof JDynamicInvokeExpr)
				type = JimpleProjectHelper
				.escapeTypeName(((JDynamicInvokeExpr) invoke).getMethod().getDeclaringClass().getType());
			else
				type = JimpleProjectHelper.escapeTypeName(SootUtilities.getInvokeBaseType(invoke));
			processMethod(method, type);
		}
		return constructInstanceInvokationExpr(invoke, method, isLibrary);
	}

	private String constructInstanceInvokationExpr(Unit invoke, SootMethod method, boolean isLibrary) throws TransformationException {
		InvokeExpr invokeExpression = ((JInvokeStmt) invoke).getInvokeExpr();
		Value base = SootUtilities.getInstanceInvkBase(invoke);
		typeProcessor.addsubtypeSupertypeRelation(base.getType(), method.getDeclaringClass().getType());
		String ret = project.getSymmariesExpression(base, this.renameToThisVariable, typeProcessor) + "#"
				+ JimpleProjectHelper.escapeTypeName(method.getDeclaringClass().getType()), separator = "";
		ret += "(";
		String sinkDefinition = "", checkpointDefinition = "", sourceDefinition = "";
		for (int index = 0; index < invokeExpression.getArgs().size(); index++) {
			Value arg = invokeExpression.getArgs().get(index);
			typeProcessor.processTypes(method.getParameterType(index), arg);
			String argName = project.getSymmariesExpression(arg, this.renameToThisVariable, typeProcessor).trim();
			if (argName.matches("class\\s\".*\"")) {
				Constant cst = ClassConstant.v(argName);
				argName = cst.getType().toString();
			}
			ret += separator
					+ (SootUtilities.isNullConstant(arg) || SootUtilities.isPrimType(method.getParameterType(index))
							|| method.getParameterType(index) == (arg.getType()) ? ""
									: "(" + method.getParameterType(index) + ")")
					+ argName;
			sinkDefinition += getSinkParameterStatments(method, index, arg);
			sourceDefinition += this.getSourceParameterStatments(method, index, arg, isLibrary);
			separator = ",";
		}
		sinkDefinition += getSinkBaseStatments(method, base);
		sourceDefinition += getSourceBaseStatments(method, base, isLibrary);
		if(isLibrary)
			updateLibraryMethodSummaries(method);
		//if (isSource(method))
		//checkpointDefinition = "\n checkpoint cp" + checkpointIndex++;
		ret += ")";
		if(isLibrary)
			return  checkpointDefinition + (checkpointDefinition.trim().isEmpty()? "": ";\n") +
					sinkDefinition       + //(sinkDefinition.trim().isEmpty()?       "": ";\n") +
					sourceDefinition     + (sourceDefinition.trim().isEmpty()?     "": ";\n") +
					ret ;
		else
			return
					sinkDefinition + //(sinkDefinition.trim().trim().isEmpty()?"":";\n") +
					checkpointDefinition + (checkpointDefinition.trim().isEmpty()?"":";\n") +
					sourceDefinition + (sourceDefinition.trim().trim().isEmpty()?"":";\n") +
					ret;
	}

	private String getSinkBaseStatments(SootMethod method, Value base) throws TransformationException {
		String out = "";
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.hasLowSinkBase(method)) {
			out += Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" + project.getSymmariesExpression(base, null, typeProcessor)  + ");\n";
			addDummyLowSinkMethod(base.getType());
		}
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.hasHighSinkBase(method)) {
			out += Constants.dummyClass + "." + Constants.highSinkMethodName + "(" + project.getSymmariesExpression(base, null, typeProcessor)  + ");\n";
			addDummyHighSinkMethod(base.getType());
		}
		return out;
	}

	private String getSinkReturnStatments(SootMethod method, Value value) throws TransformationException {
		String out = "";
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.returnsLowSink(method)) {
			out += Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" + project.getSymmariesExpression(value, null, typeProcessor) + ")";
			addDummyLowSinkMethod(method.getReturnType());
		}
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.hasHighSinkBase(method)) {
			out += Constants.dummyClass + "." + Constants.highSinkMethodName + "(" + project.getSymmariesExpression(value, null, typeProcessor)  + ")";
			addDummyHighSinkMethod(method.getReturnType());
		}
		return out;
	}

	private String getSourceReturnStatments(SootMethod method, Value value) throws TransformationException {
		String out = "";
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.returnsLowSource(method)) {
			out += project.getSymmariesExpression(value, null, typeProcessor)  + " =" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "()";
			addDummyLowSourceMethod(method.getReturnType());
		}
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.returnsHighSource(method)) {
			out += project.getSymmariesExpression(value, null, typeProcessor)  + " =" + Constants.dummyClass + "." + Constants.highSourceMethodName + "()";
			addDummyHighSourceMethod(method.getReturnType());
		}
		return out;
	}


	private String getSourceBaseStatments(SootMethod method, Value base, boolean isLibrary) throws TransformationException {
		String out = "";
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    isLibrary && project.sourceSinkHelper.hasLowSourceBase(method)) {
			out += project.getSymmariesExpression(base, null, typeProcessor) + "=" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "();\n";
			addDummyLowSourceMethod(base.getType());
		}
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    isLibrary && project.sourceSinkHelper.hasHighSourceBase(method)) {
			out += project.getSymmariesExpression(base, null, typeProcessor)  + "=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "();\n";
			addDummyHighSourceMethod(base.getType());
		}

		return out;
	}

	private String getSinkParameterStatments(SootMethod method, int index, Value arg) throws TransformationException {
		String out = "";
		if (project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.hasLowSinkParameter(method, index)) {
			out += Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" + project.getSymmariesExpression(arg, null, typeProcessor)  + ");\n";
			addDummyLowSinkMethod(arg.getType());
		}
		if (project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.hasHighSinkParameter(method, index)) {
			out += Constants.dummyClass + "." + Constants.highSinkMethodName + "(" + project.getSymmariesExpression(arg, null, typeProcessor)  + ");\n";
			addDummyHighSinkMethod(arg.getType());
		}
		return out;
	}

	private String getSourceParameterStatments(SootMethod method, int index, Value arg, boolean isLibrary) throws TransformationException {
		String out = "";
		if ( //isLibrary &&
		    project.configurations.analysis.taintCheckingEnabled () &&
		    project.sourceSinkHelper.hasLowSourceParameter(method, index)) {
			out += project.getSymmariesExpression(arg, null, typeProcessor)  + "=" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "();\n";
			addDummyLowSourceMethod(arg.getType());
		}
		if ( //isLibrary &&
		    project.configurations.analysis.confidentialityCheckingEnabled () &&
		    project.sourceSinkHelper.hasHighSourceParameter(method, index)) {
			out += project.getSymmariesExpression(arg, null, typeProcessor)  + "=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "();\n";
			addDummyHighSourceMethod(arg.getType());
		}

		return out;
	}


	private String processInvokation(Unit invoke) throws TransformationException {
		if (SootUtilities.getInvokeExpr(invoke) instanceof JDynamicInvokeExpr) {
			Utils.log(this.getClass(), "Symmaries does not support handling " + invoke);
			return null;
		}

		try {
			SootMethod method = SootUtilities.getInvokeExpr(invoke).getMethod();
			// Utils.log(this.getClass()invoke);
			if (!SootUtilities.isLibraryMethodCall(method, project.configurations.loadFromApk, project.configurations.thirdPartyMethods,
					project.getMethodUniqueName(method, SootUtilities.getInvokeType(invoke)))
					&& !project.configurations.excludedClasses.contains(method.getDeclaringClass().getName())
					&& !method.isSynchronized())
				if (method.getDeclaringClass().isInterface()) {
					// Utils.log(this.getClass()method.getDeclaringClass().getName());
					for (SootClass classs : soot.Scene.v().getActiveHierarchy()
							.getImplementersOf(method.getDeclaringClass())) {
						if (classs.getMethods().contains(method)) {
							SootMethod Implementedmethod = classs.getMethod(method.getName(),
									method.getParameterTypes(), method.getReturnType());
							processMethod(Implementedmethod, classs.getName());
						}
					}
				} else {
					String type;
					if (invoke instanceof JDynamicInvokeExpr)
						type = JimpleProjectHelper.escapeTypeName(
								((JDynamicInvokeExpr) invoke).getMethod().getDeclaringClass().getType());
					else
						type = JimpleProjectHelper.escapeTypeName(SootUtilities.getInvokeType(invoke));

					processMethod(method, type);
				}
			else
				buildLibrarySecurityAssumption(method, new ArrayList<String>());//, SootUtilities.getInvokeExpr(invoke).getArgs());
			return constructMethodCallExpr(invoke);
		}
		catch (RuntimeException e) {
			throw new TransformationException("Could not tranform " + invoke);
		}
	}

	private String processLocalVariables(Chain<Local> locals, List<Local> parameterLocals,
			Set<String> removableObjectVariables) throws TransformationException {
		String ret = "", separator = ";\n  ";
		for (Value var : locals) {
			if (removableObjectVariables == null || !removableObjectVariables.contains(var.toString())) {
				if (!parameterLocals.contains(var) && !var.toString().equals(Constants.This)
						&& !var.equals(renameToThisVariable))
					ret += JimpleProjectHelper.escapeTypeName(var.getType())+ " " + project.getSymmariesExpression(var,null,typeProcessor) + separator;
				Type varType = var.getType();
				while (varType instanceof ArrayType)
					varType = ((ArrayType) varType).baseType;
				if (!SootUtilities.isPrimType(varType))
					typeProcessor.addNewType(varType);
				if (SootUtilities.isSubtypeOf(soot.Scene.v().getSootClass(varType.toString()),
						soot.Scene.v().getSootClass("java.lang.Throwable")))
					typeProcessor.addsubtypeSupertypeRelation(varType, Constants.throwableType);
			}
		}
		return ret;
	}

	// Rename local version of instance variable, use `this' if constant
	static private boolean renameThisLocal (Body body) {
		Local thislocal = body.getThisLocal ();
		thislocal.setName ("this");
		int thismod = 0;
		for (Unit unit: body.getUnits ())
			if (SootUtilities.isAssign (unit) && ((JAssignStmt) unit).getLeftOp ().equals (thislocal))
				thismod ++;
		if (thismod > 0)
			// Keep modified local instance variable
			thislocal.setName ("$this");
		return thismod == 0;
	}

	public void processMethod(SootMethod method, String declaringClass) throws TransformationException {
			String methodUniqueName = project.getMethodUniqueName(method, declaringClass);//
			declaringClass = declaringClass.replaceAll("-", "");
			Body body = preProcessAndGetMethodBody(method, declaringClass, methodUniqueName);
			if (body == null) {
				// Utils.log(this.getClass()"The method " + methodUniqueName + "has been already
				// analyzed or is a third-party method");
				return;
			}
		try {
			Set<String> removableObjectVariables = tranformCodeByRemovingExtraObjectVariables(body);

			boolean keepThisAssign = true;
			if (!soot.Modifier.isStatic (method.getModifiers ())) {
				keepThisAssign = ! renameThisLocal (body);
			}
			// setRenameToThisVariable(body);

			Hashtable<Unit, String> statementLabels = new Hashtable<Unit, String>();
			String output = "{\n";
			output += processLocalVariables(body.getLocals(), body.getParameterLocals(), removableObjectVariables);
			List<Value> modifiedArgs = new ArrayList<Value>();
			String methodBody = processMethodBody(body, statementLabels, method, modifiedArgs,
							      keepThisAssign);
			if (methodBody == null) {
			  if (project.configurations.generateJimple)
				  Utils.writeTextFile(project.configurations.outputPath +
						      "/Jimple/" + methodUniqueName +
						      ".jimple", body.toString());
			  return;
			}
			output += methodBody;
			output += processTraps(body, statementLabels);
			output += "} \n";
			String header = constructHeader(method, false, declaringClass, modifiedArgs) + "\n";
			output = header + output;
			postProcessMethod(method, methodUniqueName, output, body);
			underProcessingMethods.remove(methodUniqueName);
		}
		catch (TransformationException e) {
			if(project.configurations.abortOnError)
				throw e;
			else {
				project.logErr("Error in method "+method.getSignature() +
					       ": " + e.getMessage());
				if (project.configurations.generateJimple)
					Utils.writeTextFile(project.configurations.outputPath +
							    "/Jimple/" + methodUniqueName +
							    ".jimple", body.toString());
				return;
			}
		}
		return;
	}

	private String processMethodBody(Body body, Hashtable<Unit, String> statementLabels, SootMethod method,
					 List<Value> modifiedArgs,
					 boolean keepThisAssign) throws TransformationException {
		String output = "", separator = ";\n ";

		for (Unit unit : body.getUnits()) {
			/*
			 * ThrowableSet exceptionsCausedByUnit = throwAnalysis.mightThrow(unit);
			 * Utils.log(this.getClass()"\nALL EXCEPTIONS THAT CAN BE RAISED BY " + unit +
			 * " ARE: \n            " + exceptionsCausedByUnit.toAbbreviatedString()); for
			 * (final Trap t : body.getTraps())
			 * if(exceptionsCausedByUnit.catchableAs(t.getException().getType()))
			 * Utils.log(this.getClass()"NON- GENERIC OR USER-RAISED ONES ARE:\n           "
			 * + t.getException().getType() ); for(Tag tag: unit.getTags())
			 * Utils.log(this.getClass()"TAGE: " + tag.getName() + " " + tag.getValue());
			 */
			String label = "", statement = null;
			if (statementLabels.containsKey(unit))
				label = statementLabels.get(unit) + ":";
			if (!unit.getBoxesPointingToThis().isEmpty() && !statementLabels.containsKey(unit)) {
				label = "label" + statementLabels.size () + ":";
				statementLabels.put(unit, "label" + statementLabels.size ());
			}
			statement = processStatement(unit, body, modifiedArgs, statementLabels, method,
						     keepThisAssign);
			if (statement == null)
				return null;
			if (!(label + statement).trim().isEmpty())
				output += label + statement + separator;
			// sourceLocation = processInvokation((JInvokeStmt) unit, sourceLocation);
		}
		// To rename the argument and define a new local variable with the arg's
		// original name
		String renamedArgsDef = "";
		for (Value arg : modifiedArgs)
			renamedArgsDef += arg.getType() + " " + project.getSymmariesExpression(arg,null,typeProcessor)  + ";\n";
		for (Value arg : modifiedArgs)
			output = project.getSymmariesExpression(arg,null,typeProcessor)  + "=" + renameArg(arg) + ";\n" + output;
		if(!classInitializers.contains(method.getDeclaringClass()))
			processClassInitilizer(method.getDeclaringClass());

		return renamedArgsDef + output;
		// return output;
	}

	private String processStatement(Unit unit, Body body, List<Value> modifiedArgs,
					Hashtable<Unit, String> statementLabels, SootMethod method,
					boolean keepThisAssign)
					throws TransformationException {
		String statement = "";

		if (SootUtilities.isIdentityStmt(unit)) {
		  if (((JIdentityStmt) unit).rightBox.getValue() instanceof ThisRef) {
		    if (keepThisAssign) {
		      String lhs = project.getSymmariesExpression(((JIdentityStmt) unit).leftBox.getValue(),
								  renameToThisVariable, typeProcessor);
		      statement = "this".equals(lhs) ? "" : (lhs + "= this");
		    } else {
		      statement = "";
		    }
			} else if ((((JIdentityStmt) unit).rightBox.getValue() instanceof ParameterRef)) {
				/*	int parameterIndex = ((ParameterRef)((JIdentityStmt) unit).rightBox.getValue()).getIndex();
				if(project.sourceSinkHelper.hasHighSourceParameter(method, parameterIndex)) {
					statement = project.getSymmariesExpression(((JIdentityStmt) unit).leftBox.getValue(), renameToThisVariable, typeProcessor) +
							"=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "()";
					this.addDummyHighSourceMethod(method.getParameterType(parameterIndex));
				}
				else if(project.sourceSinkHelper.hasLowSourceParameter(method, parameterIndex)) {
					statement = project.getSymmariesExpression((((JIdentityStmt) unit).leftBox.getValue()), renameToThisVariable, typeProcessor) +
							"=" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "()";
					this.addDummyLowSourceMethod(method.getParameterType(parameterIndex));
				}
				else*/
				statement = "";
			}
			else if ((((JIdentityStmt) unit).rightBox.getValue() instanceof JCaughtExceptionRef))
				statement = project.getSymmariesExpression(((JIdentityStmt) unit).leftBox.getValue(),null,typeProcessor) + " = catch";
			else
				throw new TransformationException("An error in translating " + unit.toString());
		} else if (SootUtilities.isAssign(unit)) {
			statement = processAssigment(unit, body);
			if (statement == null) {
				project.logErr("In method " + method.getSignature() +
					       ", statement " + unit + " cannot be tranformed.");
				return null;
			}

			if (!statement.trim().isEmpty())
				for (Local args : body.getParameterLocals())
					if (!SootUtilities.isPrimType(args.getType())
							&& ((JAssignStmt) unit).leftBox.getValue().toString().equals(args.getName().toString())
							&& !modifiedArgs.contains(args)) {
						modifiedArgs.add(args);
						break;
					}
		} else if (SootUtilities.isInvoke(unit)) {
			String methodDescr = processAllInvokation(unit);
			if (methodDescr == null)
				return null;
			SootMethod method1 = SootUtilities.getInvokeExpr(unit).getMethod();
			if (!underProcessingMethods.contains(project.getMethodUniqueName(method1, SootUtilities.getInvokeBaseType(unit))))
				statement = methodDescr;
			else
				if(SootUtilities.isInstanceInvoke(unit)) {
					boolean isLibrary = SootUtilities.isLibraryMethodCall(method, project.configurations.loadFromApk, project.configurations.thirdPartyMethods,project.getMethodUniqueName(method, SootUtilities.getInvokeBaseType(unit)));
					statement = constructInstanceInvokationExpr(unit, method1, isLibrary);
				} else
					statement = this.constructMethodCallExpr(unit);

		} else if (SootUtilities.isGotoStatment(unit)) {
			statementLabels.putIfAbsent(((JGotoStmt) unit).getTargetBox().getUnit(), "label" + statementLabels.size ());
			statement = "goto " + statementLabels.get(((JGotoStmt) unit).getTargetBox().getUnit());
		} else if (SootUtilities.isConditional(unit)) {
			statementLabels.putIfAbsent(((IfStmt) unit).getTarget(), "label" + statementLabels.size ());
			statement = "if " + project.getSymmariesExpression(((IfStmt) unit).getConditionBox().getValue(),
					renameToThisVariable, typeProcessor) + " goto "
					+ statementLabels.get(((IfStmt) unit).getTarget());
			if (((IfStmt) unit).getConditionBox().getValue().getUseBoxes().size() > 1)
				typeProcessor.processExpressionTypes(((IfStmt) unit).getConditionBox().getValue());
		} else if (SootUtilities.isReturn(unit)) {
			statement = processReturn(unit, method);
		} else if (SootUtilities.isNOP(unit)) {
			statement = ":;";
		} else if (SootUtilities.isVoidReturn(unit) || SootUtilities.isReturn(unit)) {
			statement = unit.toString();
		} else if (unit instanceof JThrowStmt) {
			statement = "throw " + project.getSymmariesExpression(((JThrowStmt)unit).getOp(),renameToThisVariable, typeProcessor);
		} else  if (unit instanceof JTableSwitchStmt) {
			statement = processTableSwitch((JTableSwitchStmt)unit, statementLabels, body, modifiedArgs, method);
		} else  if (unit instanceof JLookupSwitchStmt) {
			statement = processLookupSwitch((JLookupSwitchStmt)unit, statementLabels, body, modifiedArgs, method);
		}
		else  if (unit instanceof JEnterMonitorStmt) {
			statement = "monitorenter " + project.getSymmariesExpression(((JEnterMonitorStmt) unit).getOp(), renameToThisVariable, typeProcessor);

		}else  if (unit instanceof JExitMonitorStmt) {
			statement = "monitorexit " + project.getSymmariesExpression(((JExitMonitorStmt) unit).getOp(), renameToThisVariable, typeProcessor);

		}
		else {project.logErr("In method " + method.getSignature() +
				     ", statement " + unit + " cannot be tranformed.");
			return null;
		}
		return statement;
	}

	private String processSwitch(Value key, List<Unit> targets, Unit default_target, Hashtable<Unit, String> statementLabels, Body body, List<Value> modifiedArgs, SootMethod method) throws TransformationException {
		String statement = "switch [" + project.getSymmariesExpression(key, renameToThisVariable, typeProcessor) + "]\n";
		for(int i=0; i < targets.size();i++) {
			statementLabels.putIfAbsent(targets.get(i), "label" + statementLabels.size ());
			statement += "if " + i + " goto " + statementLabels.get(targets.get(i)) + ";\n";
		}
		statementLabels.putIfAbsent(default_target, "label" + statementLabels.size ());
		statement += "goto " + statementLabels.get(default_target);
		return statement;
	}

	private String processTableSwitch(JTableSwitchStmt unit, Hashtable<Unit, String> statementLabels, Body body, List<Value> modifiedArgs, SootMethod method) throws TransformationException {
		String statement = "switch [" + project.getSymmariesExpression(unit.getKey(), renameToThisVariable, typeProcessor) + "]\n";
		for(int i=0; i < unit.getTargets().size();i++) {
			statementLabels.putIfAbsent(unit.getTarget(i), "label" + statementLabels.size ());
			statement += "if " + i + " goto " + statementLabels.get(unit.getTarget(i)) + ";\n";
		}
		statementLabels.putIfAbsent(unit.getDefaultTarget(), "label" + statementLabels.size ());
		statement += "goto " + statementLabels.get(unit.getDefaultTarget());
		return statement;
	}

	private String processLookupSwitch(JLookupSwitchStmt unit, Hashtable<Unit, String> statementLabels, Body body, List<Value> modifiedArgs, SootMethod method) throws TransformationException {
	  return processSwitch (unit.getKey (), unit.getTargets (), unit.getDefaultTarget (), statementLabels, body, modifiedArgs, method);
	}

	private void processClassInitilizer(SootClass declaringClass) throws TransformationException {
		classInitializers.add(declaringClass);
		for(SootMethod method: declaringClass.getMethods())
			if(method.isStaticInitializer())
				processMethod(method,SootUtilities.getClassName(method.getDeclaringClass()));
	}

	private String processReturn(Unit unit, SootMethod method) throws TransformationException {
		this.typeProcessor.processTypes(method.getReturnType(), ((JReturnStmt) unit).getOp());
		String returnValue = "";
		String sink = "";
		String source = "";
		if (project.sourceSinkHelper.isLowSink(method)) {
			sink = Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" + returnValue + ");\n";
			addDummyLowSinkMethod(method.getReturnType());
		}
		if (project.sourceSinkHelper.isHighSink(method)) {
			sink += Constants.dummyClass + "." + Constants.highSinkMethodName + "(" + returnValue + ");\n";
			addDummyHighSinkMethod(method.getReturnType());
		}
		if (project.sourceSinkHelper.returnsHighSource(method)) {
			source = returnValue + "=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "();\n";
			this.addDummyHighSourceMethod(method.getReturnType());
		}
		if (project.sourceSinkHelper.returnsLowSource(method)) {
			source += returnValue + "=" + Constants.dummyClass + "." + Constants.lowSourceMethodName + "();\n";
			addDummyLowSourceMethod(method.getReturnType());
		}
		if (((JReturnStmt) unit).getOp() instanceof ClassConstant) {
			ClassConstant expr = (ClassConstant) ((JReturnStmt) unit).getOp();
			Type classType = soot.Scene.v().getSootClass("java.lang.Class").getType ();
			typeProcessor.addsubtypeSupertypeRelation(method.getReturnType(), classType);
			returnValue = JimpleProjectHelper.escapeTypeName(expr.toSootType());
		} else
			returnValue = project.getSymmariesExpression(((JReturnStmt) unit).getOp(), null, typeProcessor);
		return sink + source +
				//(sink ? Constants.dummyClass + "." + Constants.lowSinkMethodName + "(" + returnValue + ");\n" : "") +
				//(source ? returnValue + "=" + Constants.dummyClass + "." + Constants.highSourceMethodName + "();\n" : "") +
				"return  " + returnValue;
	}


	private String processTraps(Body body, Hashtable<Unit, String> statementLabels) throws TransformationException {
		String output = "";
		//if (body.getTraps().size() > 0)
		//	project.configurations.exceptionsEnabled = true;

		for (Trap trap : body.getTraps()) {
			output += "catch " + trap.getException() + ": " + statementLabels.get(trap.getBeginUnit()) + " - "
					+ statementLabels.get(trap.getEndUnit()) + " : " + statementLabels.get(trap.getHandlerUnit())
					+ ";\n";
			this.typeProcessor.addsubtypeSupertypeRelation(trap.getException().getType(), Constants.throwableType);
		}
		return output;
	}

	private String renameArg(Value arg) throws TransformationException {
		return ("@"+project.getSymmariesExpression(arg,null,typeProcessor)).replace('$', '@');
	}

	// private void setRenameToThisVariable(Body body) {
	// 	renameToThisVariable = null;
	// 	for (Unit unit : body.getUnits())
	// 		if (SootUtilities.isIdentityStmt(unit) && (((JIdentityStmt) unit).rightBox.getValue() instanceof ThisRef))
	// 			renameToThisVariable = ((JIdentityStmt) unit).leftBox.getValue();
	// 	for (Unit unit : body.getUnits())
	// 		for (ValueBox valuBox : unit.getDefBoxes())
	// 			if (valuBox.getValue().equals(renameToThisVariable)) {
	// 				renameToThisVariable = null;
	// 				return;
	// 			}
	// }

	private boolean tranformCode(Body body, HashMap<String, Unit> objectVariables) {
		Hashtable<Unit, Unit> toAddUnits = new Hashtable<Unit, Unit>();
		List<Unit> toRemoveUnits = new ArrayList<Unit>();

		boolean transformed = false;
		for (Unit unit : body.getUnits())
			if (SootUtilities.isAssign(unit)) {
				JAssignStmt as = (JAssignStmt) unit;
				if (SootUtilities.isCastExpr(as.rightBox.getValue())
						&& objectVariables.containsKey(((JCastExpr) (as.rightBox.getValue())).getOp().toString())) {
					Unit oldUnit = objectVariables.get(((JCastExpr) (as.rightBox.getValue())).getOp().toString());
					Value rhsValue;
					if (SootUtilities.isAssign(oldUnit))
						rhsValue = ((JAssignStmt) oldUnit).rightBox.getValue();
					else
						rhsValue = ((JIdentityStmt) oldUnit).rightBox.getValue();
					Unit toAdd1 = Jimple.v().newAssignStmt(as.leftBox.getValue(), rhsValue);
					toAddUnits.put(unit, toAdd1);
					toRemoveUnits.add(oldUnit);
				}
			}

		for (Unit unit : toAddUnits.keySet()) {
			body.getUnits().insertBefore(toAddUnits.get(unit), unit);
			body.getUnits().remove(unit);
			transformed = true;
		}

		for (Unit unit : toRemoveUnits) {
			body.getUnits().remove(unit);
			transformed = true;
		}

		return transformed;
	}

	private Set<String> tranformCodeByRemovingExtraObjectVariables(Body body) {
		HashMap<String, Unit> removableObjectVariables = getRemovableObjectVariablesFromBody(body);
		boolean transformed = false;
		// Utils.log(this.getClass()"test");
		if (removableObjectVariables == null)
			return null;

		if (removableObjectVariables.size() > 0) {
			transformed = tranformCode(body, removableObjectVariables);
		}

		if (transformed)
			return removableObjectVariables.keySet();
		return null;
	}

	private void updateAccessedFields(Unit unit) {
		InvokeExpr invokeExpression = SootUtilities.getInvokeExpr(unit);
		for (int index = 0; index < invokeExpression.getArgs().size(); index++) {
			Value arg = invokeExpression.getArgs().get(index);
			if (SootUtilities.isFieldRef(arg))
				addToTheAccessedFieldsList(SootUtilities.getField(arg));
		}
	}

}
