package at.enactmentengine.serverless.main;
import dps.FTinvoker.database.SQLLiteDatabase;
import dps.FTinvoker.exception.InvalidResourceException;
import dps.FTinvoker.function.Function;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Can be used to fill database for simulations
 * can simulate x invcoations of a certain function with a given availability (Ignores FT-Settings and Constraint Settings)
 */
public class DataBaseFiller {
	public static void fillDatabase(Function func, int minRunningTime, int maxRunningTime, double successRate, int count) throws Exception{
		SQLLiteDatabase DB = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");
		for (int i = 0; i < count; i++) {
			int generatedRunningTime = ThreadLocalRandom.current().nextInt(minRunningTime, maxRunningTime + 1);
			Timestamp start = new Timestamp(System.currentTimeMillis());
			Timestamp end = new Timestamp(start.getTime() + generatedRunningTime);
			double d = Math.random();
			if (d < successRate) {
				DB.addInvocation(func.getUrl(), func.getType(), detectProvider(func.getUrl()), getRegion(func), start, end, "OK", null);
			} else {
				DB.addInvocation(func.getUrl(), func.getType(), detectProvider(func.getUrl()), getRegion(func), start, end, "ERROR", "Execution failed");
			}
		}
		
	}
	
	private static String detectProvider(String functionURL) {
		if (functionURL.contains(".functions.cloud.ibm.com/")) {
			return "ibm";
		}

		if (functionURL.contains("arn:aws:lambda:")) {
			return "aws";
		}

		// Inform Scheduler Provider Detection Failed
		return "fail";
	}
	
	private static String getRegion(Function function) throws Exception{
		String regionName;
		switch (detectProvider(function.getUrl())) {
		case "ibm":
			int searchIndex = function.getUrl().indexOf("://");
			if (searchIndex != -1) {
				regionName = function.getUrl().substring(searchIndex + "://".length());
				regionName = regionName.split(".functions")[0];
				return regionName;
			} else {
				throw new Exception("Failed to detect region of IBM Function.");
			}
		
		case "aws":
			int searchIndex2 = function.getUrl().indexOf("lambda:");
			if (searchIndex2 != -1) {
				regionName = function.getUrl().substring(searchIndex2 + "lambda:".length());
				regionName = regionName.split(":")[0];
				return regionName;
			} else {
				// Error Parsing arn
				throw new InvalidResourceException("Region detection failed");
			}
		default:
			throw new Exception("Failed to detect Provider.");
		}
	}
}
