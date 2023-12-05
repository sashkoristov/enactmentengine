package at.enactmentengine.serverless.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
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
            List<String> toRemove = List.of("aws_access_key_id", "aws_session_token", "aws_secret_key", "aws_secret_access_key",
                    "gcp_private_key", "gcp_project_id", "gcp_client_email", "credentials");

            printMap.replaceAll((key, value) ->
                    toRemove.stream().anyMatch(key::endsWith) ? "redacted" : value
            );
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