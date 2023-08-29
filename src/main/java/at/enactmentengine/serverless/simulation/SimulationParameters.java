package at.enactmentengine.serverless.simulation;

import java.util.*;

/**
 * Class that holds some parameters for the simulation.
 */
public final class SimulationParameters {
    /**
     * Flag that determines whether all functions to simulate will be successful.
     */
    public static boolean IGNORE_FT;

    /**
     * Flag that determines whether no normal distribution should be applied for the RTT of functions.
     */
    public static boolean NO_DISTRIBUTION = false;

    /**
     * Variable that sums up the whole cost of the workflow.
     */
    public static double workflowCost = 0;

    /**
     * A list containing the finishing times of all iterations of a parallelFor loop.
     */
    public static List<Long> iterationFinishTimes = Collections.synchronizedList(new ArrayList<>());

    /**
     * A helper map that stores a time if multiple functions need to access the same starting time.
     */
    private static HashMap<Long, List<Long>> parallelPerTime = new HashMap<>();

    public static synchronized List<Long> getIterationFinishTimes() {
        return iterationFinishTimes;
    }

    public static synchronized void setIterationFinishTimes(List<Long> iterationFinishTimes) {
        if (iterationFinishTimes.size() > 0) {
            SimulationParameters.iterationFinishTimes.add(iterationFinishTimes.get(iterationFinishTimes.size() - 1));
        }
    }

    /**
     * Gets the starting time of a function if the concurrency limit is exceeded.
     *
     * @param amountParallelFunctions the amount of functions that need to access the same starting time (e.g. from a
     *                                parallel construct)
     * @param loopCounter             the current loop counter of the function
     *
     * @return the starting time or -1 if no matching start date could be found
     */
    public static synchronized long getStartTime(long amountParallelFunctions, int loopCounter, List<Long> ignoreTimes) {
        long startTime = System.currentTimeMillis();
        // wait until a time is found or 100ms are exceeded
        while (iterationFinishTimes.isEmpty() && (System.currentTimeMillis() - startTime) < 100) {
        }

        if (iterationFinishTimes.isEmpty()) {
            return -1;
        }

        long time = -1;
        // find the earliest finish time
        if (ignoreTimes == null || ignoreTimes.isEmpty()) {
            time = Collections.min(iterationFinishTimes);
        } else {
            List<Long> copy = new ArrayList<>(iterationFinishTimes);
            copy.removeAll(ignoreTimes);
            if (copy.isEmpty()) {
                return -1;
            }
            time = Collections.min(copy);
        }

        // if only one function needs to use the time, remove it from the list
        if (amountParallelFunctions == -1) {
            iterationFinishTimes.remove(time);
        } else {
            // otherwise, add it to the hashmap or update the hashmap with the remaining functions that need to use the same time
            if (parallelPerTime.containsKey(time) && loopCounter == parallelPerTime.get(time).get(1)) {
                parallelPerTime.put(time, Arrays.asList(parallelPerTime.get(time).get(0) - 1, parallelPerTime.get(time).get(1)));
            } else if (!parallelPerTime.containsKey(time)) {
                parallelPerTime.put(time, Arrays.asList(amountParallelFunctions - 1, (long) loopCounter));
            } else if (parallelPerTime.containsKey(time) && loopCounter != parallelPerTime.get(time).get(1)) {
                if (ignoreTimes == null) {
                    ignoreTimes = new ArrayList<>();
                }
                ignoreTimes.add(time);

                return getStartTime(amountParallelFunctions, loopCounter, ignoreTimes);
            }
            // if all functions used the time, remove it
            if (parallelPerTime.get(time).get(0) == 0) {
                iterationFinishTimes.remove(time);
                parallelPerTime.remove(time);
            }
        }

        return time;
    }

    public static void reset() {
        iterationFinishTimes = Collections.synchronizedList(new ArrayList<>());
    }
}
