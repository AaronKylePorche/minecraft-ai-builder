package com.builderbot.ai;

import com.builderbot.BuilderBotPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Service for calling BuilderGPT microservice via HTTP.
 * Handles async requests to generate AI-powered structures.
 */
public class AiBuildService {

    private final BuilderBotPlugin plugin;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String microserviceUrl;

    public AiBuildService(BuilderBotPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1 (fixes body loss on upgrade failure)
                .build();
        this.gson = new Gson();
        this.microserviceUrl = plugin.getConfig().getString("ai.microservice-url", "http://127.0.0.1:8001");
    }

    /**
     * Request AI-generated structure from microservice.
     * Calls async and schedules paste on main thread when complete.
     *
     * @param player Player who requested the build
     * @param prompt Structure description
     */
    public void requestBuild(Player player, String prompt) {
        player.sendMessage("§e§lBuilderGPT: §7Summoning AI to generate your structure...");
        player.sendMessage("§7This may take 30-120 seconds. Please wait...");

        // Build request JSON
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("prompt", prompt);
        requestJson.addProperty("minecraft_version", "JE_1_21_1");
        requestJson.addProperty("output_format", "schem");

        String requestBody = gson.toJson(requestJson);

        // Debug logging
        plugin.getLogger().info("=== AI Build Request Debug ===");
        plugin.getLogger().info("Prompt: " + prompt);
        plugin.getLogger().info("Request Body: " + requestBody);
        plugin.getLogger().info("Request Body Length: " + requestBody.length());
        plugin.getLogger().info("Microservice URL: " + microserviceUrl + "/generate");

        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(microserviceUrl + "/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))  // 2 minute timeout for LLM generation
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        plugin.getLogger().info("HTTP Method: " + request.method());
        plugin.getLogger().info("Request Headers: " + request.headers().map());

        // Send async request
        CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Handle response
        futureResponse.thenAccept(response -> {
            if (response.statusCode() == 200) {
                handleSuccess(player, response.body());
            } else {
                handleError(player, response.statusCode(), response.body());
            }
        }).exceptionally(ex -> {
            handleException(player, ex);
            return null;
        });
    }

    /**
     * Handle successful microservice response.
     * Parses JSON and schedules WorldEdit paste on main thread.
     */
    private void handleSuccess(Player player, String responseBody) {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            String schemPath = response.get("schem_path").getAsString();
            JsonObject metadata = response.getAsJsonObject("metadata");

            int blockCount = metadata.has("approx_block_count")
                    ? metadata.get("approx_block_count").getAsInt()
                    : 0;
            double genTime = metadata.has("generation_time_seconds")
                    ? metadata.get("generation_time_seconds").getAsDouble()
                    : 0.0;

            plugin.getLogger().info(String.format(
                    "AI generation complete for %s: %s (%d blocks, %.1fs)",
                    player.getName(), schemPath, blockCount, genTime
            ));

            // Schedule paste on main thread (WorldEdit requires sync)
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(String.format(
                        "§a§lBuilderGPT: §7Structure generated! (%.1fs, ~%d blocks)",
                        genTime, blockCount
                ));
                player.sendMessage("§7Pasting structure in front of you...");

                // Call WorldEditPasteService
                plugin.getWorldEditPasteService().pasteSchematicInFrontOfPlayer(player, schemPath);
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to parse microservice response", e);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage("§c§lError: §7Failed to process AI response")
            );
        }
    }

    /**
     * Handle HTTP error responses from microservice.
     */
    private void handleError(Player player, int statusCode, String responseBody) {
        plugin.getLogger().warning(String.format(
                "Microservice error %d for %s: %s",
                statusCode, player.getName(), responseBody
        ));

        String errorMessage;
        try {
            JsonObject error = gson.fromJson(responseBody, JsonObject.class);
            errorMessage = error.has("message")
                    ? error.get("message").getAsString()
                    : "Unknown error";
        } catch (Exception e) {
            errorMessage = "Service returned error " + statusCode;
        }

        final String finalErrorMessage = errorMessage;
        Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage("§c§lBuilderGPT Error: §7" + finalErrorMessage)
        );
    }

    /**
     * Handle connection/timeout exceptions.
     */
    private void handleException(Player player, Throwable ex) {
        plugin.getLogger().log(Level.SEVERE, "Failed to connect to BuilderGPT microservice", ex);

        String userMessage;
        if (ex.getCause() instanceof java.net.ConnectException) {
            userMessage = "§c§lError: §7BuilderGPT service is not running. Please start the AI stack first.";
        } else if (ex.getCause() instanceof java.net.http.HttpTimeoutException) {
            userMessage = "§c§lError: §7AI generation timed out. Try a simpler structure or try again.";
        } else {
            userMessage = "§c§lError: §7Failed to connect to BuilderGPT service.";
        }

        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(userMessage));
    }

    /**
     * Test connection to microservice.
     * @return true if service is reachable
     */
    public boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(microserviceUrl + "/"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
