package org.jugistanbul;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @author hakdogan (hakdogan75@gmail.com)
 * Created on 29.08.2025
 ***/
public class JollamaClient {

    private static final String OLLAMA_SERVER = "http://localhost:11434/api";
    private static final java.net.http.HttpClient HTTP_CLIENT = java.net.http.HttpClient.newHttpClient();

    public static HttpResponse<InputStream> sendRequest(final String model,
                                                        final String prompt,
                                                        final List<Integer> context) throws IOException, InterruptedException {
        String requestBody = requestInitiator(model, prompt, context);
        HttpRequest request = requestBuilder("generate", requestBody);

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    public static HttpResponse<InputStream> listModels() throws IOException, InterruptedException {
        HttpRequest request = requestBuilder("tags");
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private static HttpRequest requestBuilder(final String endpoint, String... requestBody){

        var builder = HttpRequest.newBuilder()
                .uri(generateUri(endpoint))
                .header("Content-Type", "application/json");

        return requestBody.length > 0
                ? builder.POST(HttpRequest.BodyPublishers.ofString(requestBody[0])).build()
                : builder.GET().build();
    }

    private static String requestInitiator(final String model,
                                           final String prompt,
                                           final List<Integer> context) {

        return context.isEmpty()
                ?
                """
                  {
                      "model": "%s",
                      "prompt": "%s"
                  }
                """.formatted(model, prompt)
                : """
                    {
                        "model": "%s",
                        "prompt": "%s",
                        "context": %s
            }
            """.formatted(model, prompt, context.toString());
    }

    private static URI generateUri(final String endpoint) {
        return URI.create(String.join("/", OLLAMA_SERVER, endpoint));
    }
}
