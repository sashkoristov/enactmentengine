package at.enactmentengine.serverless.scheduler;

import java.io.IOException;
import java.util.*;

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
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.function.Function;
import jdk.jshell.spi.ExecutionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Part of Future scheduler
 * Proposes Alternative Strategy and Changes AFCL before it will be run by EE
 *
 * TODO actually this should not be part of the EE?!
 *
 */
public class AlternativePlanScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlternativePlanScheduler.class);
    private SQLLiteDatabase database = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.database");


    /**
     * Has to be called before EE executes the Workflow
     * parses YAML file and adds Alternative Strategy to each function if "FT-AltStrat-requiredAvailability" is set
     * outputs new YAML file to output location
     *
     * @throws Exception
     */
    public void addAlternativePlansToYAML(String yamlFile, String outputFile) throws ExecutionControl.NotImplementedException, IOException {
        Map<String, Object> functionInputs = new HashMap<>(); //needed to create temp dummy Func
        functionInputs.put("null", "null");
        Workflow workflow = at.uibk.dps.afcl.utils.Utils.readYAMLNoValidation(yamlFile);
        List<AtomicFunction> allFunctionsInWorkflowNew = null;
        allFunctionsInWorkflowNew = getAllFunctionsInWorkflow(workflow);
        for (AtomicFunction each : allFunctionsInWorkflowNew) {
            List<PropertyConstraint> tmpList = new LinkedList<>();
            AtomicFunction casted = each;
            for (PropertyConstraint constraint : casted.getConstraints()) {
                if ("FT-AltStrat-requiredAvailability".equals(constraint.getName())) { // Has Availability for AltStrat Set
                    tmpList.addAll(manageAvailability(casted, constraint, functionInputs));

                }
            }
            each.setConstraints(tmpList);
        }
        at.uibk.dps.afcl.utils.Utils.writeYamlNoValidation(workflow, outputFile);
    }

    private List<PropertyConstraint> manageAvailability(AtomicFunction casted, PropertyConstraint constraint, Map<String, Object> functionInputs) throws ExecutionControl.NotImplementedException {
        List<PropertyConstraint> tmpList = new LinkedList<>();
        double requiredAvailability = Double.parseDouble(constraint.getValue());
        for (PropertyConstraint property : casted.getProperties()) {
            if ("resource".equals(property.getName())) { // Found a Function URL
                Function tempFunc = new Function(property.getValue(), casted.getType(), functionInputs);
                List<String> tempList = proposeAlternativeStrategy(tempFunc, requiredAvailability);
                if (tempList != null) {
                    int i = 0;
                    for (String altPlanString : tempList) {
                        PropertyConstraint tmpConstraint = new PropertyConstraint("FT-AltPlan-" + i, altPlanString);
                        tmpList.add(tmpConstraint);
                        i++;
                    }
                }
            }
        }
        return tmpList;
    }


    /**
     * returns success rate of first X functions
     */
    public double getSuccessRateOfFirstXFuncs(List<Function> functionAlternativeList, int x) {
        if (functionAlternativeList.size() < x) {
            return 0;
        } else {
            double availabilityProduct = 1;
            double reachedAvailability = 0;
            for (int i = 0; i < x; i++) {
                availabilityProduct = availabilityProduct * (1 - functionAlternativeList.get(i).getSuccessRate());
            }
            reachedAvailability = 1 - availabilityProduct;
            return reachedAvailability;
        }
    }


    /**
     * Used to recursivly add all AtomicFunctions in a Workflow to a list
     * Called by "getAllFunctionsInWorkflow()"
     */
    private void recursiveSolver(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        switch (function.getClass().getSimpleName()) {
            case "AtomicFunction":
                solveAtmomicFunction(function, listToSaveTo);
                break;
            case "Switch":
                solveSwitch(function, listToSaveTo);
                break;
            case "SequentialWhile":
                solveSequentialWhile(function, listToSaveTo);
                break;
            case "SequentialFor":
                solveSequentialFor(function, listToSaveTo);
                break;
            case "Sequence":
                solveSequence(function, listToSaveTo);
                break;
            case "ParallelFor":
                solveParallelFor(function, listToSaveTo);
                break;
            case "Parallel":
                solveParallel(function, listToSaveTo);
                break;
            case "IfThenElse":
                solveIfThenElse(function, listToSaveTo);
                break;
            default:
                LOGGER.warn("Could not find construct {}", function.getClass().getSimpleName());
        }
    }

    private void solveIfThenElse(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        IfThenElse castedIfThenElse = (IfThenElse) function;
        for (at.uibk.dps.afcl.Function funcs : castedIfThenElse.getThen()) {
            recursiveSolver(funcs, listToSaveTo);
        }
        for (at.uibk.dps.afcl.Function funcs : castedIfThenElse.getElse()) {
            recursiveSolver(funcs, listToSaveTo);
        }
    }

    private void solveParallel(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        Parallel castedParallel = (Parallel) function;
        List<Section> sectionList = castedParallel.getParallelBody();
        for (Section section : sectionList) {
            for (at.uibk.dps.afcl.Function functionInSection : section.getSection()) {
                recursiveSolver(functionInSection, listToSaveTo);
            }
        }
    }

    private void solveParallelFor(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        ParallelFor castedParallelFor = (ParallelFor) function;
        List<at.uibk.dps.afcl.Function> loopBody = castedParallelFor.getLoopBody();
        for (at.uibk.dps.afcl.Function each : loopBody) {
            recursiveSolver(each, listToSaveTo);
        }
    }

    private void solveSequence(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        Sequence castedSequence = (Sequence) function;
        List<at.uibk.dps.afcl.Function> sequenceBody = castedSequence.getSequenceBody();
        for (at.uibk.dps.afcl.Function each : sequenceBody) {
            recursiveSolver(each, listToSaveTo);
        }
    }

    private void solveSequentialFor(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        SequentialFor castedSF = (SequentialFor) function;
        List<at.uibk.dps.afcl.Function> loopBodySF = castedSF.getLoopBody();
        for (at.uibk.dps.afcl.Function each : loopBodySF) {
            recursiveSolver(each, listToSaveTo);
        }
    }

    private void solveSequentialWhile(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        SequentialWhile castedSW = (SequentialWhile) function;
        List<at.uibk.dps.afcl.Function> loopBodySW = castedSW.getLoopBody();
        for (at.uibk.dps.afcl.Function each : loopBodySW) {
            recursiveSolver(each, listToSaveTo);
        }
    }

    private void solveSwitch(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        Switch castedSwitch = (Switch) function;
        List<at.uibk.dps.afcl.Function> switchDefault = castedSwitch.getDefault();
        if (switchDefault != null) {
            for (at.uibk.dps.afcl.Function funcs : castedSwitch.getDefault()) {
                recursiveSolver(funcs, listToSaveTo);
            }
        }
        List<Case> cases = castedSwitch.getCases();
        if (cases != null) {
            for (Case cases1 : cases) {
                for (at.uibk.dps.afcl.Function functionsInCase : cases1.getFunctions()) {
                    recursiveSolver(functionsInCase, listToSaveTo);
                }
            }
        }
    }

    private void solveAtmomicFunction(at.uibk.dps.afcl.Function function, List<AtomicFunction> listToSaveTo) {
        AtomicFunction castedToAtomicFunction = (AtomicFunction) function;
        List<PropertyConstraint> properties = castedToAtomicFunction.getProperties();
        if (properties != null) {
            for (PropertyConstraint property : properties) {
                if ("resource".equals(property.getName())) { // Found a Function
                    // URL
                    LOGGER.warn("Function Name:  "+castedToAtomicFunction.getName()+" " +
                            "Type: "+castedToAtomicFunction.getType()+"  URL: "+property.getValue()+"");
                    listToSaveTo.add(castedToAtomicFunction);
                }
            }
        }
    }

    /**
     * Returns all AtomicFunctions in a Workflow
     */
    public List<AtomicFunction> getAllFunctionsInWorkflow(Workflow workflow) {
        List<at.uibk.dps.afcl.Function> workflowFunctionObjectList = workflow.getWorkflowBody();
        List<AtomicFunction> returnList = new LinkedList<>();
        for (at.uibk.dps.afcl.Function function : workflowFunctionObjectList) {
            recursiveSolver(function, returnList);
        }
        return returnList;
    }

    /**
     * Returns a List of Strings that each represent an Alternative Possibility that reaches the required availability
     *
     * @throws ExecutionControl.NotImplementedException on unsupported call
     */
    public List<String> proposeAlternativeStrategy(Function function, double wantedAvailability) throws ExecutionControl.NotImplementedException {
        List<String> proposedAltStrategy = new ArrayList<>();
        List<Function> functionAlternativeList = database.getFunctionAlternatives(function);
        int i = 1;
        while (i <= functionAlternativeList.size()) {
            if (getSuccessRateOfFirstXFuncs(functionAlternativeList, i) > wantedAvailability) {
                LinkedList<Function> alternativePlan = new LinkedList<>();
                StringBuilder stringForOneAlternative = new StringBuilder();
                for (int c = 0; c < i; c++) {
                    Function fun = functionAlternativeList.get(0);
                    alternativePlan.add(fun);
                    stringForOneAlternative.append(fun.getUrl());
                    stringForOneAlternative.append(";");
                    functionAlternativeList.remove(fun);
                }
                stringForOneAlternative.insert(0,
                        getSuccessRateOfFirstXFuncs(alternativePlan, alternativePlan.size()) + ";");
                proposedAltStrategy.add(stringForOneAlternative.toString());
            } else {
                i++;
            }
        }
        if (proposedAltStrategy.isEmpty()) {
            throw new ExecutionControl.NotImplementedException("No Alternative Strategy Could Be Found");
        } else {
            return proposedAltStrategy;
        }
    }

}
