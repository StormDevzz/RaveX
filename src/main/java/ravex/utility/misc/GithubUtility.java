package ravex.utility.misc;

import ravex.RaveX;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubUtility {
    public static void checkUpdates() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/StormDevzz/RaveX/releases/latest"))
                .header("User-Agent", "RaveX-Client")
                .build();
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        RaveX.LOGGER.info("Connected to GitHub successfully! Latest version is online.");
                    } else {
                        RaveX.LOGGER.warn("GitHub API returned status code: " + response.statusCode());
                    }
                });
        } catch (Exception e) {
            RaveX.LOGGER.warn("Failed to check GitHub updates: " + e.getMessage());
        }
    }
}
