package com.jkcdarunday.linkcleaner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LinkCleaner {
    private static final int MAX_REDIRECTS = 10;
    private static final Pattern WEB_URL =
            Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36";

    private LinkCleaner() {}

    static String extractUrl(String sharedText) {
        if (sharedText == null) {
            return null;
        }

        Matcher matcher = WEB_URL.matcher(sharedText);
        if (!matcher.find()) {
            return null;
        }

        String url = matcher.group();
        while (!url.isEmpty() && ".,;!)]}".indexOf(url.charAt(url.length() - 1)) >= 0) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    static String resolveAndClean(String originalUrl) throws IOException {
        URI current = parseWebUri(originalUrl);
        Set<URI> visited = new HashSet<>();

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            if (!visited.add(current)) {
                throw new IOException("The link contains a redirect loop.");
            }

            HttpURLConnection connection = open(current.toURL());
            try {
                int status = connection.getResponseCode();
                if (isRedirect(status)) {
                    if (redirectCount == MAX_REDIRECTS) {
                        throw new IOException("The link has too many redirects.");
                    }

                    String location = connection.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        throw new IOException("The server redirected without a destination.");
                    }
                    current = parseWebUri(current.resolve(location.trim()).toString());
                    continue;
                }

                if (status < 200 || status >= 300) {
                    throw new IOException("The server returned HTTP " + status + ".");
                }
                return removeTracking(current).toString();
            } finally {
                connection.disconnect();
            }
        }

        throw new IOException("The link could not be resolved.");
    }

    static URI removeTracking(URI uri) throws IOException {
        String host = uri.getHost();
        if (host == null || !isTikTokHost(host)) {
            return uri;
        }

        try {
            return new URI(
                    uri.getScheme(),
                    null,
                    host.toLowerCase(Locale.US),
                    uri.getPort(),
                    uri.getPath(),
                    null,
                    null);
        } catch (URISyntaxException exception) {
            throw new IOException("The resolved link is invalid.", exception);
        }
    }

    private static HttpURLConnection open(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty(
                "Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8");
        return connection;
    }

    private static URI parseWebUri(String value) throws IOException {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || scheme == null
                    || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
                throw new IOException("Only HTTP and HTTPS links are supported.");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new IOException("The shared link is invalid.", exception);
        }
    }

    private static boolean isTikTokHost(String host) {
        String normalized = host.toLowerCase(Locale.US);
        return normalized.equals("tiktok.com") || normalized.endsWith(".tiktok.com");
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MULT_CHOICE
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }
}
