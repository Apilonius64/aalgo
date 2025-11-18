package tea.cs.ui.config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

public record OSMAPIImport(
    Double from_lat,
    Double from_lon,
    Double to_lat,
    Double to_lon
) {
    public String genOSMRequest() {
        return String.format("""
[out:json][timeout:25];
(
  way["highway"](%f, %f, %f, %f);
);
out body;
>;
out skel qt;
        """, from_lat, from_lon, to_lat, to_lon);
    }

    public Optional<String> getData() {
        // https://openjdk.org/groups/net/httpclient/recipes.html#post
        String body = genOSMRequest();
        System.out.println(body);
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://overpass-api.de/api/interpreter"))
        .POST(BodyPublishers.ofString(body))
        .build();

        try {
            HttpResponse<?> response = client.send(request, BodyHandlers.ofString());
            return Optional.of(response.body().toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
