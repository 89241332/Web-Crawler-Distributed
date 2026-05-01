import mpi.*;
import java.util.*;

public class Main {

    static final int MASTER = 0;

    static final int WORK_TAG = 1;   // These tags will indicate
    static final int DONE_TAG = 2;   // the type of message or "instruction"
    static final int RESULT_TAG = 3; //

    static final int BUFFER_SIZE = 500000;
    static final String SEPARATOR = "|"; // This will be used to unpack the links
                                         // which will be sent in a single message

    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (me == MASTER) {
            master(size);
        } else {
            worker(me);
        }

        MPI.Finalize();
    }

    private static void master(int size) throws Exception {
        String startUrl = "https://www.famnit.upr.si/sl/";
        int limit = 20;
        String allowedHost = "www.famnit.upr.si";

        long start = System.currentTimeMillis();

        Queue<String> toVisit = new LinkedList<>();
        Set<String> queued = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Logger logger = new Logger("crawl_distributed.txt");
        toVisit.add(startUrl);

        int busyWorkers = 0;

        for (int rank = 1; rank < size; rank++) {
            if (!toVisit.isEmpty()) {
                String url = toVisit.poll();
                visited.add(url);
                queued.add(url);
                char[] msg = url.toCharArray();
                MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, rank, WORK_TAG);
                // P1: the array we are sending
                // P2: starting position in the array (always 0)
                // P3: how many characters we are sending
                // P4: the type we are sending (characters)
                // P5: which worker to send to
                // P6: the tag — TAG_WORK means "go crawl this URL"
                busyWorkers++;
            } else {
            // No work for this worker — shut it down immediately
            char[] done = "DONE".toCharArray();
            MPI.COMM_WORLD.Send(done, 0, done.length, MPI.CHAR, rank, DONE_TAG);
            }
        }

        while (busyWorkers > 0) {

            char[] buffer = new char[BUFFER_SIZE];
            Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.CHAR, MPI.ANY_SOURCE, RESULT_TAG);

            int workerRank = status.source; // ANY_SOURCE means that we dont care who sent the message
                                            // But then we check status.source to figure out who it was
            String received = new String(buffer, 0, status.count).trim();

            String[] sections = received.split("\\|\\|\\|");
            String logSection = sections[0];
            String urlSection = sections.length > 1 ? sections[1] : "";

            logger.log(logSection);

            for (String line : urlSection.split("\n")) {
                String link = line.trim();
                if (!link.isEmpty() && !visited.contains(link)) {
                    queued.add(link);
                    toVisit.add(link);
                }
            }

            if (!toVisit.isEmpty() && visited.size() < limit) {

                String next = toVisit.poll();
                visited.add(next);
                char[] msg = next.toCharArray();
                MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, workerRank, WORK_TAG);

            } else {

                char[] done = "DONE".toCharArray();
                MPI.COMM_WORLD.Send(done, 0, done.length, MPI.CHAR, workerRank, DONE_TAG);
                busyWorkers--;

            }
        }

        logger.log("DONE. Visited " + visited.size() + " pages.");
        logger.close();
        long end = System.currentTimeMillis(); // stop timer
        long seconds = (end - start) / 1000;
        long miliseconds = (end - start) % 1000;
        System.out.println("Total time: " + seconds + "." + miliseconds + " secs");
    }

    private static void worker(int rank) throws Exception {

        HttpsFetcher fetcher = new HttpsFetcher();
        Extractor extractor = new Extractor();

        while (true) {

            char[] buffer = new char[BUFFER_SIZE];
            Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.CHAR, MASTER, MPI.ANY_TAG);

            if (status.tag == DONE_TAG) break;

            String url = new String(buffer, 0, status.count).trim();
            String allowedHost = Helpers.extractHost(url);

            StringBuilder logSection = new StringBuilder();
            StringBuilder urlSection = new StringBuilder();

            try {
                Helpers.FetchResult pageFetch = Helpers.fetchWithRedirects(url, allowedHost, 5, fetcher);

                logSection.append("\nVISIT: ").append(url)
                        .append(" STATUS: ").append(pageFetch.statusText)
                        .append(pageFetch.finalUrl.equals(url) ? "" : (" REDIRECT: " + pageFetch.finalUrl))
                        .append("\n");

                List<String> links = extractor.extractLinks(pageFetch.response);

                for (String rawLink : links) {
                    if (rawLink == null) continue;
                    String link = rawLink.trim();
                    if (link.isEmpty()) continue;

                    String absolute = Helpers.toAbsoluteUrl(pageFetch.finalUrl, link, allowedHost);
                    if (absolute == null) continue;

                    Helpers.FetchResult linkFetch = Helpers.fetchWithRedirects(absolute, allowedHost, 5, fetcher);

                    logSection.append("LINK: ").append(absolute)
                            .append(" STATUS: ").append(linkFetch.statusText)
                            .append(" FOUND ON: ").append(pageFetch.finalUrl)
                            .append("\n");

                    urlSection.append(absolute).append("\n");
                }

            } catch (Exception e) {
                logSection.append("\nVISIT: ").append(url)
                        .append(" STATUS: ERROR (").append(e.getMessage()).append(")\n");
            }

            String message = logSection.toString() + "|||" + urlSection.toString();
            char[] msg = message.toCharArray();
            MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, MASTER, RESULT_TAG);
        }
    }

}
