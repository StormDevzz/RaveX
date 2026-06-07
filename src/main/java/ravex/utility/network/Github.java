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
