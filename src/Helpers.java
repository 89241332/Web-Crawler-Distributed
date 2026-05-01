public class Helpers {

    public static class FetchResult {
        final String finalUrl;
        final String response;
        final String statusText;

        FetchResult(String finalUrl, String response, String statusText) {
            this.finalUrl = finalUrl;
            this.response = response;
            this.statusText = statusText;
        }
    }

    public static FetchResult fetchWithRedirects(String url, String allowedHost, int maxRedirects, HttpsFetcher fetcher) {
        String current = url;
        for (int i = 0; i <= maxRedirects; i++) {
            try {
                String host = extractHost(current);
                String path = extractPath(current);
                if (host == null || path == null)
                    return new FetchResult(current, "", "SKIP (null url)");
                if (!host.equalsIgnoreCase(allowedHost))
                    return new FetchResult(current, "", "SKIP (external domain)");
                String response = fetcher.fetch(host, path);
                int status = getStatusCode(response);
                if (!isRedirect(status))
                    return new FetchResult(current, response, String.valueOf(status));
                String location = getHeaderValue(response, "Location");
                if (location == null || location.isBlank())
                    return new FetchResult(current, response, status + " (redirect but no Location)");
                String next = toAbsoluteUrl(current, location.trim(), allowedHost);
                if (next == null)
                    return new FetchResult(current, response, status + " (redirect to external/invalid)");
                current = next;
            } catch (Exception e) {
                return new FetchResult(current, "", "ERROR (" + e.getMessage() + ")");
            }
        }
        return new FetchResult(current, "", "ERROR (too many redirects)");
    }

    public static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    public static String getHeaderValue(String response, String headerName) {
        if (response == null) return null;
        String[] lines = response.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) break;
            if (trimmed.toLowerCase().startsWith(headerName.toLowerCase() + ":"))
                return trimmed.substring(headerName.length() + 1).trim();
        }
        return null;
    }

    public static int getStatusCode(String response) {
        if (response == null || response.isEmpty()) return -1;
        int newline = response.indexOf('\n');
        String firstLine = (newline == -1) ? response.trim() : response.substring(0, newline).trim();
        int firstSpace = firstLine.indexOf(' ');
        if (firstSpace == -1) return -1;
        int secondSpace = firstLine.indexOf(' ', firstSpace + 1);
        if (secondSpace == -1) return -1;
        String codeStr = firstLine.substring(firstSpace + 1, secondSpace).trim();
        try {
            return Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String toAbsoluteUrl(String currentUrl, String link, String allowedHost) {
        if (link.startsWith("#")) return null;
        if (link.startsWith("mailto:")) return null;
        if (link.startsWith("javascript:")) return null;
        int hash = link.indexOf('#');
        if (hash != -1) {
            link = link.substring(0, hash).trim();
            if (link.isEmpty()) return null;
        }
        if (link.startsWith("https://")) {
            String host = extractHost(link);
            if (host == null || !host.equalsIgnoreCase(allowedHost)) return null;
            return link;
        }                                                              // Again I had to turn to
        if (link.startsWith("http://")) return null;                   // ChatGPT to assist with writing
        if (link.startsWith("//")) {                                   // the URL conversion because
            String abs = "https:" + link;                              // i simply could not do it on my own
            String host = extractHost(abs);
            if (host == null || !host.equalsIgnoreCase(allowedHost)) return null;
            return abs;
        }
        if (link.startsWith("/")) return "https://" + allowedHost + link;
        String currentPath = extractPath(currentUrl);
        if (currentPath == null) currentPath = "/";
        int lastSlash = currentPath.lastIndexOf('/');
        String dir = (lastSlash == -1) ? "/" : currentPath.substring(0, lastSlash + 1);
        return "https://" + allowedHost + dir + link;
    }

    public static String extractHost(String url) {
        String rest = url.substring("https://".length());
        int slash = rest.indexOf('/');
        return slash == -1 ? rest : rest.substring(0, slash);
    }

    public static String extractPath(String url) {
        String rest = url.substring("https://".length());
        int slash = rest.indexOf('/');
        return slash == -1 ? "/" : rest.substring(slash);
    }
}