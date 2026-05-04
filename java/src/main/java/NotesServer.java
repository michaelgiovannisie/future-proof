import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class NotesServer {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        Path notesDir = Path.of(System.getProperty("user.home"), ".notes");

        // 🔥 LIST NOTES
        server.createContext("/api/notes", exchange -> {
            String response = Notes1.listNotesAsString(notesDir);
            send(exchange, response);
        });

        // 🔥 CREATE NOTE
        server.createContext("/api/create", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // ✅ ADD THIS LINE (this is the fix)
            String body = new String(exchange.getRequestBody().readAllBytes());

            String[] parts = body.split("\\|", 5);

            String fileName = parts.length > 0 ? parts[0] : "";
            String title = parts.length > 1 ? parts[1] : "";
            String content = parts.length > 2 ? parts[2] : "";
            String tags = parts.length > 3 ? parts[3] : "";
            String image = parts.length > 4 ? parts[4] : "";

            String response = Notes1.createNoteFromUI(notesDir, fileName, title, content, tags, image);

            send(exchange, response);
        });

        // 🔥 READ NOTE
        server.createContext("/api/note", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String fileName = "";

            if (query != null && query.startsWith("file=")) {
                fileName = query.substring(5);
            }

            String response = Notes1.readNoteAsString(notesDir, fileName);

            send(exchange, response);
        });

        // 🔥 UPDATE NOTE
        server.createContext("/api/update", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            String[] parts = body.split("\\|", 6);

            String oldFileName = parts.length > 0 ? parts[0] : "";
            String newFileName = parts.length > 1 ? parts[1] : "";
            String title = parts.length > 2 ? parts[2] : "";
            String tags = parts.length > 3 ? parts[3] : "";
            String image = parts.length > 4 ? parts[4] : "";
            String content = parts.length > 5 ? parts[5] : "";

            String response = Notes1.updateNoteFromUI(notesDir, oldFileName, newFileName, title, tags, image, content);
            send(exchange, response);
        });

        // 🔥 DELETE NOTE (FIXED)
        server.createContext("/api/delete", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String fileName = new String(exchange.getRequestBody().readAllBytes());

            String response = Notes1.deleteNoteFromUI(notesDir, fileName);

            send(exchange, response);
        });

        // 🔥 SEARCH
        server.createContext("/api/search", exchange -> {
            String query = exchange.getRequestURI().getQuery();

            final String keyword = (query != null && query.startsWith("q="))
                    ? query.substring(2).toLowerCase()
                    : "";

            String response = Notes1.searchNotes(notesDir, keyword);

            send(exchange, response);
        });

        // 🔥 RANDOM MEMORY
        server.createContext("/api/random", exchange -> {
            String response = Notes1.getRandomNote(notesDir);
            send(exchange, response);
        });

        server.createContext("/", exchange -> {

        if (!exchange.getRequestURI().getPath().equals("/")) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        try {
            String html = java.nio.file.Files.readString(java.nio.file.Path.of("index.html"));

            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();

        } catch (Exception e) {
            String error = "Error loading index.html: " + e.getMessage();
            exchange.sendResponseHeaders(500, error.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(error.getBytes());
            os.close();
        }
    });
        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    // 🔥 HELPER
    private static void send(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "text/plain");

        exchange.sendResponseHeaders(200, response.getBytes().length);

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}