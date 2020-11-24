package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.RegionDetectionException;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.function.Function;

import java.security.SecureRandom;
import java.sql.Timestamp;



/**
 * Can be used to fill database for simulations
 * can simulate x invocations of a certain function with a given availability (Ignores FT-Settings and Constraint Settings)
 *
 * @author matteobattaglin, stefanpedratscher
 */
public class DataBaseFiller {

    /**
     * Default empty constructor
     */
    DataBaseFiller() { }

    /**
     * Fill the database with randomly simulated invocations.
     *
     * @param func which invocation should be simulated.
     * @param minRunningTime minimum running time of the function.
     * @param maxRunningTime maximum running time of the function.
     * @param successRate to determine if the function should fail or not.
     * @param count the number of invocations that should be added to the database.
     *
     * @throws RegionDetectionException on failure detecting the region in which the function is executed.
     */
    public static void fillDatabase(Function func, int minRunningTime, int maxRunningTime, double successRate, int count) throws RegionDetectionException {

        /* Connect to local sqlite database */
        SQLLiteDatabase database = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");

        /* Add invocations to the database */
        for (int i = 0; i < count; i++) {

            /* Generate a random runtime for the function */
            int generatedRunningTime = new SecureRandom().nextInt((maxRunningTime + 1) - minRunningTime) + minRunningTime;
            Timestamp start = new Timestamp(System.currentTimeMillis());
            Timestamp end = new Timestamp(start.getTime() + generatedRunningTime);
            double d = new SecureRandom().nextDouble();
            if (d < successRate) {
                /* Add successful invocation */
                database.addInvocation(func.getUrl(), func.getType(), String.valueOf(Utils.detectProvider(func.getUrl())), Utils.detectRegion(func.getUrl()), start, end, "OK", null);
            } else {
                /* Add failed invocation */
                database.addInvocation(func.getUrl(), func.getType(), String.valueOf(Utils.detectProvider(func.getUrl())), Utils.detectRegion(func.getUrl()), start, end, "ERROR", "Execution failed");
            }
        }
    }
}
