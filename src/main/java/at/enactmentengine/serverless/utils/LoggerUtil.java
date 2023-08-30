package at.enactmentengine.serverless.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public final class LoggerUtil {

    /**
     * Flag that determines whether the credentials should be hidden from the console.
     */
    public static boolean HIDE_CREDENTIALS = false;

    /**
     * Clears the credentials from the given map.
     *
     * @param map the map to clear the credentials from
     *
     * @return the map without credentials
     */
    public static Map<String, Object> clearCredentials(Map<String, Object> map) {
        HashMap<String, Object> printMap = new HashMap<>(map);

        if (HIDE_CREDENTIALS) {
            printMap.remove("aws_access_key_id");
            printMap.remove("aws_session_token");
            printMap.remove("aws_secret_key");
            printMap.remove("gcp_private_key");
            printMap.remove("gcp_project_id");
            printMap.remove("gcp_client_email");
            printMap.remove("credentials");
        }

        return printMap;
    }

    /**
     * Clears the credentials from the given string.
     *
     * @param jsonString the string to clear the credentials from
     *
     * @return the string without credentials
     */
    public static String clearCredentials(String jsonString) {
        Gson gson = new Gson();
        Map<String, Object> retMap = gson.fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {}.getType());

        return gson.toJson(clearCredentials(retMap));
    }

}