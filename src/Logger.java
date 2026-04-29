import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {             // create output file

    private PrintWriter writer;

    public Logger(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename, true); // true = append mode
        writer = new PrintWriter(fileWriter);
    }

    public synchronized void log(String message) { //not sure if i need syncrhonized here
        writer.println(message);
        writer.flush();
    }

    public void close() {
        writer.close();
    }
}
