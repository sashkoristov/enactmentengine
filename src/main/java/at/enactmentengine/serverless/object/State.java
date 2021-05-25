package at.enactmentengine.serverless.object;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    public void addResultToState(String result, String name){

        JsonObject jsonObj = new Gson().fromJson(result, JsonElement.class).getAsJsonObject();

        JsonObject state = State.getInstance().getStateObject();

        for (String key : jsonObj.keySet()) {
            state.add(name + "/" + key, jsonObj.get(key));
        }
    }
}
