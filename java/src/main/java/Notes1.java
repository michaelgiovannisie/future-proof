import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 * Future Proof Notes Manager - Version One (CLI)
 * A personal notes manager using text files with YAML headers.
 * Command-line interface version with 'list' command.
 *
 * SETUP REMINDER:
 * Before running the 'list' command, copy the test notes to your notes directory:
 *     cp -r test-notes/* ~/.notes/
 * or create the directory structure:
 *     mkdir -p ~/.notes/notes
 *     cp test-notes/*.md ~/.notes/notes/
 */
public class Notes1 {

    private static final Path NOTES_DIR = Path.of(System.getProperty("user.home"), ".notes");

    /**
     * Initialize the notes application.
     */
    private static Path setup() {
        // Define the notes directory in HOME
        // Check if notes directory exists
        // For CLI version, we don't automatically create it
        return NOTES_DIR;
    }

    /**
     * Parse YAML front matter from a note file.
     * Returns a map with metadata.
     */
    private static Map<String, String> parseYamlHeader(Path filePath) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("file", filePath.getFileName().toString());
        try {
            List<String> lines = Files.readAllLines(filePath);
            // Check if file starts with YAML front matter
            if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
                metadata.put("title", filePath.getFileName().toString());
                return metadata;
            }
            // Find the closing ---
            int yamlEnd = -1;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("---")) {
                    yamlEnd = i;
                    break;
                }
            }
            if (yamlEnd == -1) {
                yamlEnd = lines.size();
            }
            // Parse YAML lines (simple parsing for basic key: value pairs)
            for (int i = 1; i < yamlEnd; i++) {
                String line = lines.get(i).trim();
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if(key.isEmpty()) {
                        continue;
                    }
                    metadata.put(key, value);
                }
            }
            if(!metadata.containsKey("title")){
                metadata.put("title", filePath.getFileName().toString());
            }
        } catch (IOException e) {
            metadata.put("error", e.getMessage());
        }
        return metadata;
    }

    /**
     * List all notes in the notes directory.
     */
    private static boolean listNotes(Path notesDir) {
        // Check if notes directory exists
        if (!Files.exists(notesDir)) {
            System.err.println("Error: Notes directory does not exist: " + notesDir);
            System.err.println("Create it with: mkdir -p ~/.notes/notes");
            System.err.println("Then copy test notes: cp test-notes/*.md ~/.notes/notes/");
            return false;
        }

        // Look for notes in the notes directory (or directly in .notes)
        Path notesSubdir = notesDir.resolve("notes");
        Path searchDir = Files.exists(notesSubdir) ? notesSubdir : notesDir;

        // Find all note files (*.md, *.note, *.txt)
        List<Path> noteFiles;
        try (Stream<Path> paths = Files.walk(searchDir, 1)) {
            noteFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".md") || name.endsWith(".note") || name.endsWith(".txt");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            System.err.println("Error reading notes directory: " + e.getMessage());
            return false;
        }
        if (noteFiles.isEmpty()) {
            System.out.println("No notes found in " + notesDir);
            System.err.println("Copy test notes with: cp test-notes/*.md ~/.notes/");
            return true;
        }
        // Parse and display notes
        System.out.println("Notes in " + notesDir + ":");
        System.out.println("=".repeat(60));
        for (Path noteFile : noteFiles) {
            // this should probably be a private method to be re-used
            Map<String, String> metadata = parseYamlHeader(noteFile);
            String title = metadata.getOrDefault("title", noteFile.getFileName().toString());
            String created = metadata.getOrDefault("created", "N/A");
            String last_updated = metadata.getOrDefault("last_updated", "N/A");
            String tags = metadata.getOrDefault("tags", "");
            System.out.println("\n" + noteFile.getFileName());
            System.out.println("  Title: " + title);
            if (!created.equals("N/A")) {
                System.out.println("  Created: " + created);
            }
            if (!tags.isEmpty()) {
                System.out.println("  Tags: " + tags);
            }
        }

        System.out.println("\n" + noteFiles.size() + " note(s) found.");
        return true;
    }

    /**
     * Display help information.
     */
    private static void showHelp() {
        String helpText = String.format("""
                Future Proof Notes Manager v0.1

                Usage: java Notes1 [command]

                Available commands:
                  help    - Display this help information
                  list    - List all notes in the notes directory

                Notes directory: %s

                Setup:
                  To test the 'list' command, copy sample notes:
                    mkdir -p ~/.notes/notes
                    cp test-notes/*.md ~/.notes/notes/
                """, NOTES_DIR);
        System.out.println(helpText.trim());
    }

    /**
     * Clean up and exit the application.
     */
    private static void finish(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Main entry point for the notes CLI application.
     */
    public static void main(String[] args) {
        // Setup
        Path notesDir = setup();

        // Parse command-line arguments
        if (args.length < 1) {
            // No command provided
            System.err.println("Error: No command provided.");
            System.err.println("Usage: java Notes1 [command]");
            System.err.println("Try 'java Notes1 help' for more information.");
            finish(1);
        }

        String command = args[0].toLowerCase();

        // Process command
        switch (command) {
            case "help":
                showHelp();
                finish(0);
                break;
            case "list":
                boolean success = listNotes(notesDir);
                finish(success ? 0 : 1);
                break;
            case "create":
                if (args.length < 3) {
                    System.err.println("Usage: java Notes1 create \"title\" \"tag1,tag2\"");
                    finish(1);
                }
                createNote(notesDir, args[1], args[2]);
                finish(0);
                break;
            case "read":
                if (args.length < 2) {
                    System.err.println("No filename provided.");
                    System.err.println("Usage: java Notes1 read <filename>");
                    finish(1);
                }
                readNote(notesDir, args[1]);
                finish(0);
                break;
            case "update":
                if (args.length < 2) {
                    System.err.println("No filename provided.");
                    System.err.println("Usage: java Notes1 update <filename>");
                    finish(1);
                }
                updateNote(notesDir, args[1]);
                finish(0);
                break;
            case "delete":
                if (args.length < 2) {
                    System.err.println("No filename provided.");
                    System.err.println("Usage: java Notes1 delete <filename>");
                    finish(1);
                }
                deleteNote(notesDir, args[1]);
                finish(0);
                break;
            case "search":
                try {
                    searchNote(notesDir, args[1]);
                    finish(0);
                } catch (IOException e) {
                    System.err.println("Search failed: " + e.getMessage());
                    finish(1);
                }
                break;
            default:
                System.err.println("Error: Unknown command '" + command + "'");
                System.err.println("Try 'java Notes1 help' for more information.");
                finish(1);
        }
    }

    public static void createNote(Path notesDir, String title, String tags) {
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Files.createDirectories(searchDir);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String timestamp = now.toString();
            String fileName = now.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
            ) + ".note";
            Path notePath = searchDir.resolve(fileName);
            Path tempFile = Files.createTempFile("note-content-", ".txt");
            ProcessBuilder pb = new ProcessBuilder("nano", tempFile.toString());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            String body = Files.readString(tempFile);
            String content = """
                ---
                title       : %s
                author      : Michael Giovanni Sie
                created     : %s
                last_updated: %s
                tags        : [%s]
                image       : %s
                ---

                %s
                """.formatted(title, timestamp, timestamp, tags, body);
            Files.writeString(notePath, content);
            Files.deleteIfExists(tempFile);
            System.out.println("Note created: " + fileName);
        }
        catch (Exception e) {
            System.out.println("Error creating note: " + e.getMessage());
        }
    }

    public static void readNote(Path notesDir, String fileName) {
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Path notePath = searchDir.resolve(fileName);
            if (!noteExistence(notePath)) return;
            String content = Files.readString(notePath);
            System.out.println("\n" + content);
        } catch (Exception e) {
            System.err.println("Error reading note: " + e.getMessage());
        }
    }

    public static void updateNote(Path notesDir, String fileName) {
        try {    
            Path searchDir = getNotesDirectory(notesDir);
            Path notePath = searchDir.resolve(fileName);
            if (!noteExistence(notePath)) return;
            String content = Files.readString(notePath);
            Path tempFile = Files.createTempFile("note-content-", ".txt");
            Files.writeString(tempFile, content);
            ProcessBuilder pb = new ProcessBuilder("nano", tempFile.toString());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            String newContent = Files.readString(tempFile);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String timestamp = now.toString();
            String[] lines = newContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("last_updated:")) {
                    lines[i] = "last_updated: " + timestamp;
                }
            }
            newContent = String.join("\n", lines);
            Files.writeString(notePath, newContent);
            Files.deleteIfExists(tempFile);
            System.out.println("\nNote updated: " + fileName + "\n");
        } catch (Exception e) {
            System.err.println("Error updating note: " + e.getMessage());
        }
    }

    public static void deleteNote(Path notesDir, String fileName){
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Path notePath = searchDir.resolve(fileName);
            if (!noteExistence(notePath)) return;
            Files.delete(notePath);
            System.out.println("Note successfully deleted: " + fileName);
        } catch (Exception e) {
            System.err.println("Error deleting note: " + e.getMessage());
        }
    }

    public static void searchNote(Path notesDir, String keyword) throws IOException {
        if (!Files.exists(notesDir)) {
            System.err.println("Error: Notes directory does not exist: " + notesDir);
            System.err.println("Create it with: mkdir -p ~/.notes/notes");
            System.err.println("Then copy test notes: cp test-notes/*.md ~/.notes/notes/");
            return;
        }
        Path searchDir = getNotesDirectory(notesDir);
        List<Path> noteFiles;
            try (Stream<Path> paths = Files.walk(searchDir, 1)) {
                noteFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                    return name.endsWith(".md") || name.endsWith(".note") || name.endsWith(".txt");
                    })
                    .sorted()
                    .toList();
                }
        if (noteFiles.isEmpty()) {
            System.out.println("No notes found in " + notesDir);
            System.err.println("Copy test notes with: cp test-notes/*.md ~/.notes/");
            return;
        }
        boolean found = false;
        System.out.println("Result: ");
        keyword = keyword.toLowerCase();
        for(Path noteFile : noteFiles) {
            try {
                Map<String, String> metadata = parseYamlHeader(noteFile);
                String title = metadata.getOrDefault("title", noteFile.getFileName().toString());
                String created = metadata.getOrDefault("created", "");
                String tags = metadata.getOrDefault("tags", "");
                String content = Files.readString(noteFile);
                if(title.toLowerCase().contains(keyword) || created.toLowerCase().contains(keyword) || 
                    tags.toLowerCase().contains(keyword) || content.toLowerCase().contains(keyword)) {
                    System.out.println("\n" + noteFile.getFileName() + " | " + title);
                    found = true;
                }
            } catch(IOException e) {}   
        }
        if(!found) {
                System.out.println("No file contains: " + keyword);
            }  
    }

    private static Path getNotesDirectory(Path notesDir) {
        Path notesSubdir = notesDir.resolve("notes");
        return Files.exists(notesSubdir) ? notesSubdir : notesDir;
    }

    private static boolean noteExistence(Path notePath) {
        if (!Files.exists(notePath)) {
                System.err.println("Note not found: " + notePath.getFileName());
                return false;
            }
            return true;
    }

    public static String listNotesAsString(Path notesDir) {
        StringBuilder result = new StringBuilder();

        try {
            Path searchDir = getNotesDirectory(notesDir);

            List<Path> files;
            try (Stream<Path> stream = Files.walk(searchDir, 1)) {
                files = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".note"))
                .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                .toList();
            }

            // 🔥 NO SORTING (remove bug source)

            for (Path path : files) {
                try {
                    String content = Files.readString(path);

                    String title = "(no title)";
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (line.startsWith("title")) {
                            title = line.split(":", 2)[1].trim();
                            break;
                        }
                    }

                    result.append(path.getFileName())
                        .append(" - ")
                        .append(title)
                        .append("\n");

                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 IMPORTANT
            return "Error: " + e.getMessage();
        }

        return result.toString();
    }
    
    public static String readNoteAsString(Path notesDir, String fileName) {
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Path notePath = searchDir.resolve(fileName);

            if (!Files.exists(notePath)) {
                return "Note not found";
            }

            return Files.readString(notePath);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String createNoteFromUI(Path notesDir, String fileName, String title, String content, String tags, String image) {
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Files.createDirectories(searchDir);

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String timestamp = now.toString();

            fileName = now.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
            ) + ".note";

            // ✅ ensure extension
            if (!fileName.endsWith(".note")) {
                fileName += ".note";
            }

            Path notePath = searchDir.resolve(fileName);

            String note = """
                ---
                title: %s
                created: %s
                last_updated: %s
                tags: [%s]
                image: %s
                ---

                %s
                """.formatted(title, timestamp, timestamp, tags, image, content);

            Files.writeString(notePath, note);

            return "Created: " + fileName;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String deleteNoteFromUI(Path notesDir, String fileName) {
        try {
            Path searchDir = getNotesDirectory(notesDir);
            Path notePath = searchDir.resolve(fileName);
            if (!Files.exists(notePath)) {
                return "Note not found";
            }
            Files.delete(notePath);
            return "Deleted: " + fileName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String updateNoteFromUI(Path notesDir, String oldFileName, String newFileName, String title, String tags, String image, String newContent) {
        try {
            Path searchDir = getNotesDirectory(notesDir);

            Path oldPath = searchDir.resolve(oldFileName);

            if (!Files.exists(oldPath)) {
                return "Note not found";
            }

            // ✅ ensure extension
            if (!newFileName.endsWith(".note")) {
                newFileName += ".note";
            }

            Path newPath = searchDir.resolve(newFileName);

            // ✅ rename if needed
            if (!oldFileName.equals(newFileName)) {
                Files.move(oldPath, newPath);
            }

            String existing = Files.readString(newPath);

            String created = "";

            // extract created timestamp
            for (String line : existing.split("\n")) {
                if (line.startsWith("created:")) {
                    created = line.replace("created:", "").trim();
                    break;
                }
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String updatedTime = now.toString();

            // ✅ preserve old image if new one is empty
            if (image == null || image.isBlank()) {
                for (String line : existing.split("\n")) {
                    if (line.startsWith("image:")) {
                        image = line.replace("image:", "").trim();
                        break;
                    }
                }
            }

            String updated = """
                ---
                title: %s
                created: %s
                last_updated: %s
                tags: [%s]
                image: %s
                ---

                %s
                """.formatted(title, created, updatedTime, tags, image, newContent);

            Files.writeString(newPath, updated);

            return "Updated: " + newFileName;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String getRandomNote(Path notesDir) {
        try {
            Path searchDir = getNotesDirectory(notesDir);

            List<Path> files = Files.list(searchDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".note"))
            .toList();

            if (files.isEmpty()) return "No memories found.";

            java.util.Random rand = new java.util.Random();
            Path random = files.get(rand.nextInt(files.size()));

            return random.getFileName().toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String searchNotes(Path notesDir, String keyword) {
        StringBuilder result = new StringBuilder();

        try {
            Path searchDir = getNotesDirectory(notesDir);

            Files.list(searchDir).forEach(path -> {
                try {
                    String content = Files.readString(path).toLowerCase();

                    if (content.contains(keyword)) {

                        Map<String, String> metadata = parseYamlHeader(path);
                        String title = metadata.getOrDefault("title", path.getFileName().toString());

                        result.append(path.getFileName())
                            .append(" - ")
                            .append(title)
                            .append("\n");
                    }

                } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        return result.toString();
    }

}



