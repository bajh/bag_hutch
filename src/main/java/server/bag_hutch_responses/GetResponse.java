package server.bag_hutch_responses;

import com.google.gson.Gson;
import data.Context;

public class GetResponse extends SuccessResponse {
    private String key;
    private String[] versions;
    private String context;

    public GetResponse(String key, String[] versions, Context context) {
        this.key = key;
        this.versions = versions;
        this.setContext(new Gson().toJson(context));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String[] getValues() {
        return versions;
    }

    public void setValues(String[] versions) {
        this.versions = versions;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
