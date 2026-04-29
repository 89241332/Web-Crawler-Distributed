import java.util.ArrayList;
import java.util.List;

public class Extractor {                            // Purpose: scan the HTML for hrefs and store the links

    public List<String> extractLinks(String html) {
        List<String> links = new ArrayList<>();

        if (html == null || html.isEmpty()) {
            return links;
        }

        String href = "href=\"";
        int index = 0;

        while (true) {

            int start = html.indexOf(href, index);
            if (start == -1) {
                break;
            }

            start = start + href.length();

            int end = html.indexOf("\"", start);
            if (end == -1) {
                break;
            }

            String link = html.substring(start, end);

            links.add(link);

            index = end + 1;
        }

        return links;
    }
}

