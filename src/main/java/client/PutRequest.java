package client;

import data.Context;

public class PutRequest {
    private Context context;
    private String key;
    private String value;

    public PutRequest(String key, String value, Context context) {
        this.setKey(key);
        this.setValue(value);
        this.setContext(context);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
