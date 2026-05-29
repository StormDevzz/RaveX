package ravex.utility.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Github {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Asynchronously fetches the raw text content of a file from a GitHub repository.
     * Can be used to load dynamic features like splash texts, configurations, or update checks.
     *
     * @param repoOwner Owner/organization of the repository
     * @param repoName  Name of the repository
     * @param branch    Branch to fetch from (e.g., "main" or "master")
     * @param filePath  Relative path to the file in the repository
     * @return CompletableFuture containing the raw text content of the file
     */
    public static CompletableFuture<String> fetchRawContent(String repoOwner, String repoName, String branch, String filePath) {
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s", repoOwner, repoName, branch, filePath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RaveX-Client")
                .timeout(Duration.ofSeconds(5))
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        throw new RuntimeException("GitHub returned HTTP status " + response.statusCode());
                    }
                });
    }

    /**
     * Asynchronously checks if a GitHub repository is reachable (returns HTTP 200).
     *
     * @param repoOwner Owner of the repository
     * @param repoName  Name of the repository
     * @return CompletableFuture containing true if reachable, false otherwise
     */
    public static CompletableFuture<Boolean> isReachable(String repoOwner, String repoName) {
        String url = String.format("https://api.github.com/repos/%s/%s", repoOwner, repoName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RaveX-Client")
                .timeout(Duration.ofSeconds(5))
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }
}
