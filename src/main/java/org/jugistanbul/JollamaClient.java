package org.jugistanbul;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hakdogan (hakdogan75@gmail.com)
 * Created on 29.08.2025
 ***/
public class JollamaClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String OLLAMA_SERVER = "http://localhost:11434/api/generate";

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\":\"(.*?)\"");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("\"context\":\\[(.*?)\\]");
    private static final Pattern ERROR_PATTERN = Pattern.compile("\"error\":\"(.*?)\"");

    public static void main(String[] args) throws Exception {

        if (args.length > 1 && argumentValidation(args)){
            createModel(args[0], args[1]);
        }

        var scanner = new Scanner(System.in);
        List<Integer> context = new ArrayList<>();

        System.out.printf("%nBefore you begin, please specify the model you want to use.%n");
        var model = scanner.nextLine();

        System.out.printf("%nAsk anything. Type exit to exit!%n");

        while (true) {
            var prompt = scanner.nextLine();

            if("exit".equals(prompt)){
                break;
            }

            sendMessage(model, prompt, context);
            System.out.printf("%nGot a new question?%n");

        }
    }

    private static boolean argumentValidation(final String[] args){
        return !args[0].isBlank() && !args[1].isBlank();
    }

    private static void createModel(final String modelName, final String path)
            throws IOException, InterruptedException {

        Path modelfilePath = Path.of(path);
        ProcessBuilder pb = new ProcessBuilder(
                "ollama", "create", modelName, "-f", modelfilePath.toString()
        );

        pb.inheritIO(); // Konsola çıktıları bas
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.printf("%nModel created successfully: %s", modelName);
        } else {
            System.err.printf("%nModel could not be created: %d", exitCode);
            System.exit(-1);
        }
    }

    private static void sendMessage(final String model,
                                    final String prompt,
                                    final List<Integer> context) throws Exception {

        var requestBody = requestInitiator(model, prompt, context);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_SERVER))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        var rawJson = new StringBuilder();

        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorHandler(line);
                rawJson.append(line);
                Matcher matcher = RESPONSE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String chunk = matcher.group(1);
                    System.out.print(chunk.replace("\\n", "\n").replace("\\\"", "\""));
                }
            }
        }

        contextAppender(rawJson.toString(), context);
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

    private static void errorHandler(final String line){

        Matcher error = ERROR_PATTERN.matcher(line);
        if (error.find()) {
            System.out.println(error.group(1));
            System.exit(-1);
        }
    }

    private static void contextAppender(final String response, final List<Integer> context) {

        Matcher ctxMatcher = CONTEXT_PATTERN.matcher(response);
        if (ctxMatcher.find()) {
            String[] tokens = ctxMatcher.group(1).split(",");
            context.clear();
            for (String token : tokens) {
                if (!token.isBlank()) {
                    context.add(Integer.parseInt(token.trim()));
                }
            }
        }
    }
}
