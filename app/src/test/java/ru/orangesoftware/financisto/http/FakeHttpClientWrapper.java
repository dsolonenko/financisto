package ru.orangesoftware.financisto.http;

import java.util.HashMap;
import java.util.Map;

public class FakeHttpClientWrapper extends HttpClientWrapper {

    public final Map<String, String> responses = new HashMap<String, String>();
    public Exception error;

    public FakeHttpClientWrapper() {
        super(null);
    }

    @Override
    public String getAsString(String url) throws Exception {
        if (error != null) {
            throw error;
        }
        String response = responses.get(url);
        if (response == null) {
            response = responses.get("*");
        }
        return response;
    }

    public void givenResponse(String url, String response) {
        responses.put(url, response);
    }
}
