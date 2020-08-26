package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.RegionDetectionException;
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.function.Function;

import java.security.SecureRandom;
import java.sql.Timestamp;


/**
 * Can be used to fill database for simulations
 * can simulate x invocations of a certain function with a given availability (Ignores FT-Settings and Constraint Settings)
 */
public class DataBaseFiller {

    DataBaseFiller() {
    }

    public static void fillDatabase(Function func, int minRunningTime, int maxRunningTime, double successRate, int count) throws RegionDetectionException {
        SQLLiteDatabase database = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");
        for (int i = 0; i < count; i++) {
            int generatedRunningTime = new SecureRandom().nextInt((maxRunningTime + 1) - minRunningTime) + minRunningTime;
            Timestamp start = new Timestamp(System.currentTimeMillis());
            Timestamp end = new Timestamp(start.getTime() + generatedRunningTime);
            double d = new SecureRandom().nextDouble();
            if (d < successRate) {
                database.addInvocation(func.getUrl(), func.getType(), detectProvider(func.getUrl()), getRegion(func), start, end, "OK", null);
            } else {
                database.addInvocation(func.getUrl(), func.getType(), detectProvider(func.getUrl()), getRegion(func), start, end, "ERROR", "Execution failed");
            }
        }

    }

    private static String detectProvider(String functionURL) {
        if (functionURL.contains(".functions.cloud.ibm.com/") || functionURL.contains(".functions.appdomain.cloud/")) {
            return "ibm";
        }

        if (functionURL.contains("arn:aws:lambda:")) {
            return "aws";
        }

        // Inform Scheduler Provider Detection Failed
        return "fail";
    }

    private static String getRegion(Function function) throws RegionDetectionException {
        String regionName;
        switch (detectProvider(function.getUrl())) {
            case "ibm":
                int searchIndex = function.getUrl().indexOf("://");
                if (searchIndex != -1) {
                    regionName = function.getUrl().substring(searchIndex + "://".length());
                    regionName = regionName.split(".functions")[0];
                    return regionName;
                } else {
                    throw new RegionDetectionException("Failed to detect region of IBM Function.");
                }

            case "aws":
                int searchIndex2 = function.getUrl().indexOf("lambda:");
                if (searchIndex2 != -1) {
                    regionName = function.getUrl().substring(searchIndex2 + "lambda:".length());
                    regionName = regionName.split(":")[0];
                    return regionName;
                } else {
                    // Error Parsing arn
                    throw new RegionDetectionException("Region detection failed");
                }
            default:
                throw new RegionDetectionException("Failed to detect Region and Provider.");
        }
    }
}
