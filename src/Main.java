import mpi.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Main {

    static final int MASTER = 0;

    static final int WORK_TAG = 1;   // These tags will indicate
    static final int DONE_TAG = 2;   // the type of message or "instruction"
    static final int RESULT_TAG = 3; //

    static final int BUFFER_SIZE = 100000;
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
        int limit = 30;
        String allowedHost = "www.famnit.upr.si";

        Queue<String> toVisit = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        Logger logger = new Logger("crawl_distributed.txt");
        toVisit.add(startUrl);

        int busyWorkers = 0;

        for (int rank = 1; rank < size; rank++) {
            if (!toVisit.isEmpty()) {
                String url = toVisit.poll();
                visited.add(url);
                char[] msg = url.toCharArray();
                MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.CHAR, rank, WORK_TAG);
                // P1: the array we are sending
                // P2: starting position in the array (always 0)
                // P3: how many characters we are sending
                // P4: the type we are sending (characters)
                // P5: which worker to send to
                // P6: the tag — TAG_WORK means "go crawl this URL"
                busyWorkers++;
            }
        }

        while (busyWorkers > 0) {

            char[] buffer = new char[BUFFER_SIZE];
            Status status = MPI.COMM_WORLD.Recv(buffer, 0, buffer.length, MPI.CHAR, MPI.ANY_SOURCE, RESULT_TAG);

            int workerRank = status.source; // ANY_SOURCE means that we dont care who sent the message
                                            // But then we check status.source to figure out who it was
            String received = new String(buffer, 0, status.count).trim();
            // status count tells me how many characters were sent and how big the buffer needs to be
            String[] parts = received.split("\\" + SEPARATOR); // split the message
            String crawledUrl = parts[0]; // Position 0 is the name of the page,
                                          // the rest are the links from that page
            logger.log("VISIT: " + crawledUrl);

            for (int i = 1; i < parts.length; i++) {
                String link = parts[i].trim();
                if (link.isEmpty()) continue;
                if (!visited.contains(link) && link.startsWith("https://" + allowedHost)) {
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
    }
}
