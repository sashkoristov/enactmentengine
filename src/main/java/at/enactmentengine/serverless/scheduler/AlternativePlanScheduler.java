package at.enactmentengine.serverless.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.AtomicFunction;
import at.uibk.dps.afcl.functions.IfThenElse;
import at.uibk.dps.afcl.functions.Parallel;
import at.uibk.dps.afcl.functions.ParallelFor;
import at.uibk.dps.afcl.functions.Sequence;
import at.uibk.dps.afcl.functions.SequentialFor;
import at.uibk.dps.afcl.functions.SequentialWhile;
import at.uibk.dps.afcl.functions.Switch;
import at.uibk.dps.afcl.functions.objects.Case;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.afcl.functions.objects.Section;
import dps.FTinvoker.database.SQLLiteDatabase;
import dps.FTinvoker.function.Function;

/**
 *  Part of Future scheduler
 *  Proposes Alternative Strategy and Changes AFCL before it will be run by EE
 */
public class AlternativePlanScheduler {
	private SQLLiteDatabase DB = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");

	
	
	/**
	 *  Has to be called before EE executes the Workflow
	 *  parses YAML file and adds Alternative Strategy to each function if "FT-AltStrat-requiredAvailability" is set
	 *  outputs new YAML file to output location
	 * @throws Exception 
	 */
	public void addAlternativePlansToYAML(String yamlFile,String outputFile) throws Exception{
		Map<String, Object> functionInputs = new HashMap<>(); //needed to create temp dummy Func
    	functionInputs.put("null", "null"); 
		Workflow workflow = at.uibk.dps.afcl.utils.Utils.readYAMLNoValidation(yamlFile);
		List<AtomicFunction> AllFunctionsInWorkflowNew = null;
		AllFunctionsInWorkflowNew = getAllFunctionsInWorkflow(workflow);
		for (at.uibk.dps.afcl.Function each:AllFunctionsInWorkflowNew){
			List<PropertyConstraint> tmpList = new LinkedList<PropertyConstraint>();
			AtomicFunction casted = (AtomicFunction) each;
			for (PropertyConstraint constraint:casted.getConstraints()){
				if(constraint.getName().equals("FT-AltStrat-requiredAvailability")){ // Has Availability for AltStrat Set
					double requiredAvailability = Double.parseDouble(constraint.getValue());
					for (PropertyConstraint property:casted.getProperties()){
						if(property.getName().equals("resource")){ // Found a Function URL
							Function tempFunc = new Function(property.getValue(), casted.getType(), functionInputs);
							List<String> tempList = proposeAlternativeStrategy(tempFunc, requiredAvailability);
							if(tempList != null){
							int i = 0;
							for(String altPlanString: tempList){
								PropertyConstraint tmpConstraint = new PropertyConstraint("FT-AltPlan-"+i,altPlanString);
								tmpList.add(tmpConstraint);
								i++;
							}
							}
						}	
					}
				}	
			}
			each.setConstraints(tmpList);
		}
		at.uibk.dps.afcl.utils.Utils.writeYamlNoValidation(workflow, outputFile);
	}
	
	
	/**
	 *  returns success rate of first X functions 
	 */
	public double getSuccessRateOfFirstXFuncs(List<Function> functionAlternativeList, int x) {
		if (functionAlternativeList.size() < x) {
			return 0;
		} else {
			double AvailabilityProduct = 1;
			double ReachedAvailability = 0;
			for (int i = 0; i < x; i++) {
				AvailabilityProduct = AvailabilityProduct * (1 - functionAlternativeList.get(i).getSuccessRate());
			}
			ReachedAvailability = 1 - AvailabilityProduct;
			return ReachedAvailability;
		}
	}



	/**
	 *  Used to recursivly add all AtomicFunctions in a Workflow to a list
	 *  Called by "getAllFunctionsInWorkflow()"
	 */
	private void recursiveSolver(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
		switch (function.getClass().getSimpleName()) {
		case "AtomicFunction":
			AtomicFunction castedToAtomicFunction = (AtomicFunction) function;
			List<PropertyConstraint> properties = castedToAtomicFunction.getProperties();
			if (properties != null){
			for (PropertyConstraint property : properties) {
				if (property.getName().equals("resource")) { // Found a Function
																// URL
					System.out.println("Function Name: " + castedToAtomicFunction.getName() + " Type: "
							+ castedToAtomicFunction.getType() + " URL: " + property.getValue());
					listToSaveTo.add(castedToAtomicFunction);
				}
			}
			}
			break;
		case "Switch":
			Switch castedSwitch = (Switch) function;
			List<at.uibk.dps.afcl.Function> switchDefault = castedSwitch.getDefault();
			if (switchDefault != null) {
				for (at.uibk.dps.afcl.Function funcs : castedSwitch.getDefault()) {
					recursiveSolver(funcs, listToSaveTo);
				}
			}
			List<Case> cases = castedSwitch.getCases();
			if(cases != null){
			for (Case cases1 : cases) {
				for (at.uibk.dps.afcl.Function functionsInCase : cases1.getFunctions()) {
					recursiveSolver(functionsInCase, listToSaveTo);
				}
			}
			}
			break;
		case "SequentialWhile":
			SequentialWhile castedSW = (SequentialWhile) function;
			List<at.uibk.dps.afcl.Function> loopBodySW = castedSW.getLoopBody();
			for (at.uibk.dps.afcl.Function each : loopBodySW) {
				recursiveSolver(each, listToSaveTo);
			}
			break;
		case "SequentialFor":
			SequentialFor castedSF = (SequentialFor) function;
			List<at.uibk.dps.afcl.Function> loopBodySF = castedSF.getLoopBody();
			for (at.uibk.dps.afcl.Function each : loopBodySF) {
				recursiveSolver(each, listToSaveTo);
			}
			break;
		case "Sequence":
			Sequence castedSequence = (Sequence) function;
			List<at.uibk.dps.afcl.Function> sequenceBody = castedSequence.getSequenceBody();
			for (at.uibk.dps.afcl.Function each : sequenceBody) {
				recursiveSolver(each, listToSaveTo);
			}
			break;
		case "ParallelFor":
			ParallelFor castedParallelFor = (ParallelFor) function;
			List<at.uibk.dps.afcl.Function> loopBody = castedParallelFor.getLoopBody();
			for (at.uibk.dps.afcl.Function each : loopBody) {
				recursiveSolver(each, listToSaveTo);
			}
			break;
		case "Parallel":
			Parallel castedParallel = (Parallel) function;
			List<Section> sectionList = castedParallel.getParallelBody();
			for (Section section : sectionList) {
				for (at.uibk.dps.afcl.Function functionInSection : section.getSection()) {
					recursiveSolver(functionInSection, listToSaveTo);
				}
			}
			break;
		case "IfThenElse":
			IfThenElse castedIfThenElse = (IfThenElse) function;
			for (at.uibk.dps.afcl.Function funcs : castedIfThenElse.getThen()) {
				recursiveSolver(funcs, listToSaveTo);
			}
			for (at.uibk.dps.afcl.Function funcs : castedIfThenElse.getElse()) {
				recursiveSolver(funcs, listToSaveTo);
			}
			break;
		}
	}

	/**
	 *  Returns all AtomicFunctions in a Workflow
	 */
	public List<AtomicFunction> getAllFunctionsInWorkflow(Workflow workflow) {
		List<at.uibk.dps.afcl.Function> workflowFunctionObjectList = workflow.getWorkflowBody();
		List<AtomicFunction> returnList = new LinkedList<AtomicFunction>();
		for (at.uibk.dps.afcl.Function function : workflowFunctionObjectList) {
			recursiveSolver(function, returnList);
		}
		return returnList;
	};

	/**
	 *  Returns a List of Strings that each represent an Alternative Possibility that reaches the required availability
	 * @throws Exception 
	 */
	public List<String> proposeAlternativeStrategy(Function function, double wantedAvailability) throws Exception {
		List<String> proposedAltStrategy = new ArrayList<String>();
		List<Function> functionAlternativeList = DB.getFunctionAlternatives(function);
		int i = 1;
		while (i <= functionAlternativeList.size()) {
			if (getSuccessRateOfFirstXFuncs(functionAlternativeList, i) > wantedAvailability) {
				LinkedList<Function> alternativePlan = new LinkedList<Function>();
				StringBuilder stringForOneAlternative = new StringBuilder("");
				for (int c = 0; c < i; c++) {
					alternativePlan.add(functionAlternativeList.get(0));
					stringForOneAlternative.append(functionAlternativeList.get(0).getUrl());
					stringForOneAlternative.append(";");
					functionAlternativeList.remove(0);
				}
				stringForOneAlternative.insert(0,
						getSuccessRateOfFirstXFuncs(alternativePlan, alternativePlan.size()) + ";");
				// System.out.println(getSuccessRateOfFirstXFuncs(alternativePlan,alternativePlan.size()));
				proposedAltStrategy.add(stringForOneAlternative.toString());
			} else {
				i++;
			}
		}
		if (proposedAltStrategy.size() == 0) {
			throw new Exception("No Alternative Strategy Could Be Found");
		} else {
			return proposedAltStrategy;
		}
	}

}
