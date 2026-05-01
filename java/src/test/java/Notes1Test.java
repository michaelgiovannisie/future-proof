import org.junit.jupiter.api.*;
import java.nio.file.*;
import jdk.jfr.Timestamp;
import static org.junit.jupiter.api.Assertions.*;

public class Notes1Test {

    private Path tempDir;

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("notes-test");
        Files.createDirectories(tempDir.resolve("notes"));
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception ignored) {}
            });
    }

    @Test
    void testDeleteNote() throws Exception {
        Notes1.createNote(tempDir);
        Path notesdir = tempDir.resolve("notes");
        Path file = Files.list(notesSubdir).findFirst().get();
        String fileName = file.getFileName().toString();
        Notes1.deleteNote(tempDir, fileName);
        assertFalse(Files.exists(file),"File should be deleted");
    }

    @Time
    void testSearchNote()


}