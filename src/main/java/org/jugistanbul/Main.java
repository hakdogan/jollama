package org.jugistanbul;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hakdogan (hakdogan75@gmail.com)
 * Created on 30.08.2025
 ***/
public class Main {

    private static final String SPECIFY_MODEL = "%nBefore you begin, please specify the model you want to use.%n(Type \"models\" to list local models)%n";
    private static final String ASK_ANYTHING = "%nAsk anything. Type \"exit\" to exit!%n";

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\":\"(.*?)\"");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("\"context\":\\[(.*?)\\]");
    private static final Pattern MODEL_PATTERN = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_PATTERN = Pattern.compile("\"error\":\"(.*?)\"");

    public static void main(String[] args) throws Exception {

        if (args.length > 1 && argumentValidation(args)){
            createModel(args[0], args[1]);
        }

        var scanner = new Scanner(System.in);
        List<Integer> context = new ArrayList<>();

        System.out.printf(SPECIFY_MODEL);
        String model;

        while (true) {

            model  = scanner.nextLine();
            isExitRequest(model);

            if("models".equals(model)) {
                HttpResponse<InputStream> response = JollamaClient.listModels();
                printModels(response);
                model = scanner.nextLine();
            }

            if(isModelAvailable(model)){
                break;
            }
        }

        System.out.printf(ASK_ANYTHING);

        while (true) {

            var prompt = scanner.nextLine();
            if(prompt.isBlank() || prompt.trim().length() == 0) {
                System.out.printf(ASK_ANYTHING);
                continue;
            }

            isExitRequest(prompt);

            HttpResponse<InputStream> response = JollamaClient.sendRequest(model, prompt, context);
            if(response.statusCode() != 200){
                errorHandler(response);
            }

            printResponse(response, context);
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

        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.printf("%nModel created successfully: %s", modelName);
        } else {
            System.err.printf("%nModel could not be created: %d", exitCode);
            System.exit(-1);
        }
    }

    private static boolean isModelAvailable(final String model) {

        HttpResponse<InputStream> response = JollamaClient.listModels();
        boolean found = findModel(model, response);

        if(!found){
            System.out.printf("model '%s' not found", model);
            System.out.printf(SPECIFY_MODEL);
        }

        return found;
    }

    private static boolean findModel(final String model,
                                     final HttpResponse<InputStream> response){
        return getModels(response).stream().anyMatch(model::equals);
    }

    private static void printModels(final HttpResponse<InputStream> response) throws IOException {
        getModels(response).forEach(System.out::println);
        System.out.printf("%nPlease specify the model you want to use.%n");
    }

    private static void printResponse(final HttpResponse<InputStream> response,
                                      final List<Integer> context) throws Exception {

        var rawJson = new StringBuilder();

        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
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

    private static void errorHandler(final HttpResponse<InputStream> response) {

        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher error = ERROR_PATTERN.matcher(line);
                if (error.find()) {
                    System.out.println(error.group(1));
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private static List<String> getModels(final HttpResponse<InputStream> response){

        var models = new ArrayList<String>();

        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            var rawJson = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                rawJson.append(line);
            }

            var jsonContent = rawJson.toString();
            Matcher matcher = MODEL_PATTERN.matcher(jsonContent);

            while (matcher.find()) {
                String fullModelName = matcher.group(1);
                String[] parts = fullModelName.split(":");
                String baseModelName = parts.length > 0 ? parts[0] : fullModelName;
                models.add(baseModelName);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return models;
    }

    private static void isExitRequest(final String prompt){
        if("exit".equals(prompt)){
            System.exit(0);
        }
    }
}
