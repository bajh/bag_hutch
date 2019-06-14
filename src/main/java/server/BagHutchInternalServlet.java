package server;

import client.Client;
import client.PutRequest;
import com.google.gson.Gson;
import data.Context;
import data.Record;
import server.bag_hutch_responses.ErrorResponse;
import server.bag_hutch_responses.GetResponse;
import server.bag_hutch_responses.PutResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BagHutchInternalServlet extends HttpServlet {
    Client client;

    BagHutchInternalServlet(Client client) {
        this.client = client;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {

        response.setContentType("application/json");

        String []keys = request.getParameterMap().get("key");

        if (keys == null || keys.length != 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(new Gson().toJson(
                    new ErrorResponse("exactly one key must be provided")));
            return;
        }

        String key = keys[0];
        try {
            Record record = client.get(key).join();
            // TODO: Not Found?

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(new Gson().toJson(
                    new GetResponse(key, record.getVersions(), new Context(record.getClock()))));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {

        response.setContentType("application/json");

        PutRequest putRequest = new Gson().fromJson(request.getReader(), PutRequest.class);

        String key = putRequest.getKey();
        String value = putRequest.getValue();
        if (key == null || key == "" || value == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(
                    new Gson().toJson(new ErrorResponse("a key and value must be provided")));
        }

        try {
            Context context = client.put(putRequest.getKey(), putRequest.getValue(), putRequest.getContext())
                    .join();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(new Gson().toJson(
                    new PutResponse(putRequest.getValue(), new Gson().toJson(context))));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

}
