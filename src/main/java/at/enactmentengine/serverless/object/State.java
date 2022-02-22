package at.enactmentengine.serverless.object;

import at.enactmentengine.serverless.exception.MissingOutputDataException;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import com.google.gson.*;

import java.util.List;
import java.util.Locale;

/**
 * Class which holds the current state (all input and output data of the workflow and each function).
 *
 * @author andreasreheis
 */
public class State {
    private State(){}

    public JsonObject stateObject = new JsonObject();

    private static class StateHolder{
        private static final State INSTANCE = new State();
    }

    public static State getInstance(){
        return StateHolder.INSTANCE;
    }

    public JsonObject getStateObject() {
        return stateObject;
    }

    public void setStateObject(JsonObject stateObject) {
        this.stateObject = stateObject;
    }

    public synchronized void addParamToState(String result, String name, Integer id, String type) {
        type = type.toLowerCase(Locale.ROOT);

        JsonElement jsonElement;

        switch (type) {
            case "string":
                jsonElement = new JsonPrimitive(result.replace("\"", ""));
                break;
            case "number" :
                jsonElement = new JsonPrimitive(Double.parseDouble(result.replaceAll("\"", "").replaceAll("\\\\", "")));
                break;
            case "collection":
                jsonElement = new Gson().fromJson(result.replaceAll("\"", "").replaceAll("\\\\", ""), JsonElement.class);
                break;
            case "boolean":
                jsonElement = new JsonPrimitive(Boolean.parseBoolean(result));
                break;
            default:
                throw new IllegalStateException("Unexpected type: " + type);
        }

        State.getInstance().getStateObject().add(name + (id != 0 ? "/" + id : ""), jsonElement);
    }

    public synchronized void addResultToState(String result, String name, Integer id, List<DataOutsAtomic> output) throws MissingOutputDataException {

        JsonObject jsonObj = new Gson().fromJson(result, JsonElement.class).getAsJsonObject();

        for (DataOutsAtomic dataOutAtomic : output){
            if(jsonObj.get(dataOutAtomic.getName()) == null) {
                throw new MissingOutputDataException("Output " + dataOutAtomic.getName() + " could not be found");
            } else {
                addParamToState(jsonObj.get(dataOutAtomic.getName()).toString(), name + "/" + dataOutAtomic.getName(), id, dataOutAtomic.getType());
            }
        }
    }

    public synchronized String findJSONSubObject(String dataSource, String subKey, long count) {
        String retval = null;

        /**
         * Check if key 'dataSource' is accessible, could not be accessible if combinedSource in is used
         */
        if(State.getInstance().getStateObject().get(dataSource) != null){
            JsonObject jsonElement = new Gson().fromJson(State.getInstance().getStateObject().get(dataSource), JsonElement.class).getAsJsonObject();

            String[] subKeyList = subKey.split("/");

            for(int i = 0; i < subKeyList.length; i++){
                retval = jsonElement.get(subKeyList[i]) != null ? jsonElement.get(subKeyList[i]).toString() : null;
                if(i < count - 2){
                    jsonElement = new Gson().fromJson(retval, JsonElement.class).getAsJsonObject();
                }
            }
        }

        return retval;
    }
}
