package at.enactmentengine.serverless.object;

import at.enactmentengine.serverless.exception.RegionDetectionException;


/**
 * Utility class for the enactment-engine.
 *
 * @author stefanpedratscher
 */
public class Utils {

    /**
     * Path to the credentials properties file
     */
    public static String PATH_TO_CREDENTIALS = "credentials.properties";

    /**
     * Determine if the availability should be simulated.
     */
    public static boolean SIMULATE_AVAILABILITY = false;

    /**
     * The protocol for the resource links.
     */
    private static final String PROTOCOL = "https://";

    /**
     * Detect the provider with given function url.
     *
     * @param resourceLink the resource of the function.
     * @return the detected provider.
     */
    public static Provider detectProvider(String resourceLink) {

        /* Check for providers */
        if (resourceLink.contains(".functions.cloud.ibm.com/") || resourceLink.contains(".functions.appdomain.cloud/")) {
            return Provider.IBM;
        } else if (resourceLink.contains("arn:aws:lambda:")) {
            return Provider.AWS;
        } else if (resourceLink.contains("cloudfunctions.net")) {
            return Provider.GOOGLE;
        } else if (resourceLink.contains("azure")) {
            return Provider.AZURE;
        } else if (resourceLink.contains("fc.aliyuncs")) {
            return Provider.ALIBABA;
        }

        // Inform Scheduler Provider Detection Failed
        return Provider.FAIL;
    }

    /**
     * Detect the provider with given function url.
     *
     * @param resourceLink the resource of the function.
     * @return detected region.
     * @throws RegionDetectionException on region detection failure.
     */
    public static String detectRegion(String resourceLink) throws RegionDetectionException {
        String regionName;
        switch (Utils.detectProvider(resourceLink)) {
            case IBM:
                if(resourceLink.contains("functions.cloud.ibm")){
                    return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.cloud\\.ibm")[0];
                } else if(resourceLink.contains("functions.appdomain.cloud")) {
                    return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.appdomain\\.cloud")[0];
                }
                return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.cloud\\.ibm")[0];
            case AWS:
                return resourceLink.split("lambda:")[1].split(":")[0];
            case GOOGLE:
                return resourceLink.split(PROTOCOL)[1].split("\\.cloudfunctions\\.net")[0];
            case AZURE:
                throw new RegionDetectionException("Azure currently not supported.");
            case ALIBABA:
                throw new RegionDetectionException("Alibaba currently not supported.");
            default:
                throw new RegionDetectionException("Failed to detect Region and Provider.");
        }
    }
}
