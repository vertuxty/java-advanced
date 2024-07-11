package info.kgeorgiy.ja.leshchev.walk;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Walk {
    public static void main(String[] args) {

        if (args == null || args.length != 2) {
            System.err.println("Error, args must contain two arguments.");
            return;
        }

        if (args[0] == null || args[1] == null) {
            System.err.println("Error, arguments must not be empty!");
            return;
        }
        Path outputPath;
        Path inputPath;
        try {
            outputPath = Path.of(args[1]);
        } catch (InvalidPathException e) {
            System.err.printf("Error, invalid path for < input_file >: %s%n", e.getMessage());
            return;
        }
        try {
            inputPath = Path.of(args[0]);
        } catch (InvalidPathException e) {
            System.err.printf("Error, invalid path for < output_file >: %s%n", e.getMessage());
            return;
        }
        action(inputPath, outputPath);
    }

    private static void action(Path inputPath, Path outputPath) {
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
        } catch (IOException e) {
            System.err.printf("Error, Input/Out unexpected error: %s%n", e.getMessage());
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
                String fileName;
                int hash = 0;
                while ((fileName = reader.readLine()) != null) {
                    try {
                        hash = controlSumByte(Path.of(fileName));
                    } catch (InvalidPathException | IOException e) {
//                        write(writer, hash, fileName);
                        try {
                            writer.write(String.format("%08x %s", hash, fileName));
                            writer.newLine();
                        } catch (IOException h) {
                            System.err.printf("Unexpected error, while writing hash in output file: %s%n", e.getMessage());
                            return;
                        }
                    }
                    writer.write(String.format("%08x %s", hash, fileName));
//                    write(writer, hash, fileName);
                    writer.newLine();
                }
            } catch (FileNotFoundException e) {
                System.err.println("Error, can't find file: " + inputPath + " " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Unexpected error while reading: " + e.getMessage());
            }
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error, encoding is not supported: " + StandardCharsets.UTF_8 + " " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Unexpected error, while write output file: " + e.getMessage());
        }
    }


    private static boolean write(BufferedWriter writer, String hash, String fileName) {
        try {
            writer.write(String.format("%s %s", hash, fileName));
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.printf("Unexpected error, while writing hash in output file: %s%n", e.getMessage());
            return false;
        }
    }

    private static int controlSumByte(Path filePath) throws
            SecurityException, IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return jenkinsHash(inputStream);
        }
    }

    private static int jenkinsHash(InputStream inputStream) throws IOException {
        int pointer;
        int hash = 0;
        byte[] buffer = new byte[1024];
        while ((pointer = inputStream.read(buffer)) != -1) {
            int i = 0;
            while (i != pointer) {
                hash += buffer[i] & 0xff;
                hash += hash << 10;
                hash ^= hash >>> 6;
                i++;
            }
        }
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        return hash;
    }
}
