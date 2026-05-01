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
            Path notesSubdir = notesDir.resolve("notes");
            Files.createDirectories(notesSubdir);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String timestamp = now.toString();
            String fileName = now.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
            ) + ".note";
            Path notePath = notesSubdir.resolve(fileName);
            Path tempFile = Files.createTempFile("note-content-", ".txt");
            ProcessBuilder pb = new ProcessBuilder("nano", tempFile.toString());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            String body = Files.readString(tempFile);
            String content = """
                ---
                title: %s
                author: Michael Sie
                created: %s
                last_updated: %s
                tags: [%s]
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
            Path notesSubdir = notesDir.resolve("notes");
            Path notePath = notesSubdir.resolve(fileName);
            if (!Files.exists(notePath)) {
                System.err.println("Note not found: " + fileName);
                return;
            }
            String content = Files.readString(notePath);
            System.out.println("\n" + content);
        } catch (Exception e) {
            System.err.println("Error reading note: " + e.getMessage());
        }
    }

    public static void updateNote(Path notesDir, String fileName) {
        try {    
            Path notesSubdir = notesDir.resolve("notes");
            Path notePath = notesSubdir.resolve(fileName);
            if(!Files.exists(notePath)) {
                System.err.println("Note not found: " + fileName);
                return;
            }
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
            Path notesSubdir = notesDir.resolve("notes");
            Path notePath = notesSubdir.resolve(fileName);
            if(!Files.exists(notePath)) {
                System.err.println("Note not found: " + fileName);
                return;
            }
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
        Path notesSubdir = notesDir.resolve("notes");
        Path searchDir = Files.exists(notesSubdir) ? notesSubdir : notesDir;
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
}



