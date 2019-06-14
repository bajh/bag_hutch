package server.bag_hutch_responses;

public class PutResponse {
    protected String status;
    protected String context;

    public PutResponse(String status, String context) {
        this.status = status;
        this.context = context;
    }

    public String getStatus() {
        return status;
    }

    public String getContext() {
        return context;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
