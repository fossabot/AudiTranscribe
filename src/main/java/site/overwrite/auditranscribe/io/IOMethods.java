/*
 * IOMethods.java
 *
 * Created on 2022-03-15
 * Updated on 2022-07-03
 *
 * Description: Input/Output methods that are used in the AudiTranscribe project.
 */

package site.overwrite.auditranscribe.io;

import site.overwrite.auditranscribe.MainApplication;
import site.overwrite.auditranscribe.system.OSMethods;
import site.overwrite.auditranscribe.system.OSType;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Input/Output methods that are used in the AudiTranscribe project.
 */
public final class IOMethods {
    private IOMethods() {
        // Private constructor to signal this is a utility class
    }

    // File path handling

    /**
     * Gets a file's URL.
     *
     * @param filePath Path to the file, with respect to the <b>resource path</b>.
     * @return A URL representing the absolute path to the file (which is in the target folder).
     */
    public static URL getFileURL(String filePath) {
        return MainApplication.class.getResource(filePath);
    }

    /**
     * Gets a file's URL as a string.
     *
     * @param filePath Path to the file, with respect to the <b>resource path</b>.
     * @return A string, representing a URL to the file (which is in the target folder).
     */
    public static String getFileURLAsString(String filePath) {
        return getFileURL(filePath).toString();
    }

    /**
     * Gets the absolute path of a file.
     *
     * @param filePath Path to the file, with respect to the <b>resource path</b>.
     * @return A string representing the absolute path to the file (which is in the target folder).
     */
    public static String getAbsoluteFilePath(String filePath) {
        // Get the raw path to the file
        String path = getFileURL(filePath).getPath();

        // Handle the treatment of the path
        path = treatPath(path);

        // Return path
        return path;
    }

    // IO Handling

    /**
     * Gets the input stream of a file, with respect to the <b>resource path</b>.
     *
     * @param filePath Path to the file, with respect to the <b>resource path</b>.
     * @return Input stream of the file.
     */
    public static InputStream getInputStream(String filePath) {
        // If the file path contains backslashes, replace with forward slashes
        filePath = filePath.replaceAll("\\\\", "/");
        return MainApplication.class.getResourceAsStream(filePath);
    }

    /**
     * Method that creates a file at the specified <code>absolutePath</code>.
     *
     * @param absolutePath <b>Absolute path</b> to the file.
     * @return An integer.
     * <ul>
     *     <li>Returns <code>0</code> if the file was successfully created.</li>
     *     <li>Returns <code>1</code> if the file already exists.</li>
     *     <li>Returns <code>-1</code> if the file failed to be created (<em>and does not already
     *     exist</em>).</li>
     * </ul>
     */
    public static int createFile(String absolutePath) {
        try {
            File myFile = new File(absolutePath);
            if (myFile.createNewFile()) {
                return 0;  // Success
            } else {
                return 1;  // File already exists
            }
        } catch (IOException e) {
            return -1;  // Failed to create file
        }
    }

    /**
     * Method that deletes a file at the specified <code>absolutePath</code>.
     *
     * @param absolutePath <b>Absolute path</b> to the file.
     * @return Boolean. Is <code>true</code> is the file was deleted and <code>false</code>
     * otherwise.
     */
    public static boolean deleteFile(String absolutePath) {
        try {
            return Files.deleteIfExists(Paths.get(absolutePath));
        } catch (IOException e) {
            new File(absolutePath).deleteOnExit();
            return false;
        }
    }

    /**
     * Method that creates a folder, if it does not already exist, at the specified
     * <code>absolutePath</code>.
     *
     * @param absolutePath <b>Absolute path</b> to the folder.
     * @return A boolean. Returns <code>true</code> if folder was created successfully, and
     * <code>false</code> otherwise.
     */
    public static boolean createFolder(String absolutePath) {
        try {
            Files.createDirectory(Paths.get(absolutePath));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // File location handling

    /**
     * Method that checks if a file is present at the specified absolute path.
     *
     * @param absolutePath <b>Absolute path</b> to check if a file exists at.
     * @return Boolean. Returns <code>true</code> if the file exists at the specified path, an
     * <code>false</code> otherwise.
     */
    public static boolean isFileAt(String absolutePath) {
        return (new File(absolutePath)).exists();
    }

    /**
     * Method that moves the file to a new location.
     *
     * @param originalAbsolutePath Original <b>absolute</b> path.
     * @param newAbsolutePath      New <b>absolute</b> path.
     * @throws IOException If movement of the file fails.
     */
    public static void moveFile(String originalAbsolutePath, String newAbsolutePath) throws IOException {
        // Check if the file exists first
        File origFile = new File(originalAbsolutePath);

        if (!origFile.exists()) {
            throw new IOException("A file does not exist at '" + originalAbsolutePath + "'.");
        }

        // Rename file to the new absolute path
        boolean success = origFile.renameTo(new File(newAbsolutePath));

        if (!success) {
            throw new IOException("Movement to the new location '" + newAbsolutePath + "' failed.");
        }
    }

    // Path handling

    /**
     * Method that builds a full path from the individual folders.
     *
     * @param elems The individual folders.
     * @return The full path.
     */
    public static String buildPath(String... elems) {
        StringBuilder buffer = new StringBuilder();
        String lastElem = null;
        for (String elem : elems) {
            if (elem == null) {  // Skip any empty/null paths
                continue;
            }

            if (lastElem == null) {  // Handle the first element
                buffer.append(elem);
            } else if (lastElem.endsWith(IOConstants.SEPARATOR)) {  // Check if the last path ends with the separator
                buffer.append(elem.startsWith(IOConstants.SEPARATOR) ? elem.substring(IOConstants.SEPARATOR.length()) : elem);
            } else {
                if (!elem.startsWith(IOConstants.SEPARATOR)) {
                    buffer.append(IOConstants.SEPARATOR);  // Append the separator first...
                }
                buffer.append(elem);                       // ...before appending the element
            }

            // Update the last element
            lastElem = elem;
        }

        return buffer.toString();
    }

    /**
     * Method that joins several paths together.
     *
     * @param paths The paths to join.
     * @return The joined path.
     */
    public static String joinPaths(String... paths) {
        StringBuilder buffer = new StringBuilder();
        for (String path : paths) {
            if (path == null) {  // Skip any empty/null paths
                continue;
            }

            if (buffer.length() > 0) {  // If there is something in the buffer
                buffer.append("/");     // Append the separator first...
            }
            buffer.append(path);        // ...before appending the path
        }

        return buffer.toString();
    }

    /**
     * Method that splits the paths.
     *
     * @param paths The paths to split.
     * @return The split paths.
     */
    public static String[] splitPaths(String paths) {
        // Replace backslashes with forward slashes
        paths = paths.replace("\\", "/");

        // Then actually split by separator
        return paths.split("/");
    }

    /**
     * Method that treats a path so that it is correctly parsed by the operating system.
     *
     * @param path Path to treat.
     * @return Treated path.
     */
    public static String treatPath(String path) {
        if (OSMethods.getOS() == OSType.WINDOWS) {
            // If the path starts with something like "/C:" or "\C:", remove the first slash
            if (path.matches("[/\\\\][A-Z]:.*")) {
                path = path.substring(1);
            }

            // Now treat all the escaped characters with percentage signs
            Pattern pattern = Pattern.compile("%(?<seq>[\\da-fA-F]{2})");
            Matcher matcher = pattern.matcher(path);
            while (matcher.find()) {
                // Get the sequence to convert
                String sequence = matcher.group("seq");

                // Replace the sequence with the character
                path = matcher.replaceFirst(Character.toString((char) Integer.parseInt(sequence, 16)));

                // Attempt to find next match
                matcher = pattern.matcher(path);
            }
        }

        return path;
    }
}
