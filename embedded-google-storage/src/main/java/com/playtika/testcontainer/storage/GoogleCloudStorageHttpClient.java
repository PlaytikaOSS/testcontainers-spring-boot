package com.playtika.testcontainer.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@Slf4j
class GoogleCloudStorageHttpClient {

    //TODO: replace with HttpRequest from Java 11, when migrated
    public void sendUpdateConfigRequest(String containerEndpoint) throws IOException {
        HttpURLConnection connection = null;
        try {

            String requestBody = "{"
                    + "\"externalUrl\": \"" + containerEndpoint + "\""
                    + "}";

            URL url = new URL(containerEndpoint + "/_internal/config");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            try (OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream())) {
                osw.write(requestBody);
                osw.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                String response = getResponseBody(connection);
                log.error(
                        "error updating Google Cloud Fake Storage Server with external url, response status code {} != 200 message {}",
                        responseCode,
                        response);
            }
        } catch (Exception e) {
            log.error("error updating Google Cloud Fake Storage Server with external host", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getResponseBody(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = getStream(connection)) {
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            Scanner s = new Scanner(streamReader).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private InputStream getStream(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        return  errorStream != null ? errorStream : connection.getInputStream();
    }
}
