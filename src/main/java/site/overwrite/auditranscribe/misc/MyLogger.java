/*
 * MyLogger.java
 *
 * Created on 2022-06-25
 * Updated on 2022-06-28
 *
 * Description: Class that handles the loggers.
 */

package site.overwrite.auditranscribe.misc;

import site.overwrite.auditranscribe.io.IOConstants;
import site.overwrite.auditranscribe.io.IOMethods;
import site.overwrite.auditranscribe.utils.MiscUtils;

import java.io.*;
import java.util.logging.*;

/**
 * Class that handles the loggers.
 */
public final class MyLogger {
    // Constants
    private static final long MAX_LOG_FILE_SIZE = 5_000_000;  // In bytes

    // Static attributes
    private static Logger logger;

    private MyLogger() {
        // Private constructor to signal this is a utility class
    }

    // Public methods

    /**
     * Log a message, with no arguments.<br>
     * If the logger is currently enabled for the given message level then the given message is
     * forwarded to all the registered output <code>Handler</code> objects.
     *
     * @param level            One of the message level identifiers, e.g., <code>SEVERE</code>.
     * @param msg              Message to log.
     * @param callingClassName Name of the class that called this method.
     */
    public static void log(Level level, String msg, String callingClassName) {
        getLogger().log(level, String.format("%1$-80s (%2$s)", msg, callingClassName));
    }

    /**
     * Method that logs an exception to file/console.
     *
     * @param e Exception to log.
     */
    public static void logException(Exception e) {
        // Get the stack trace message
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        // Log the error
        getLogger().log(Level.SEVERE, sw.toString());
    }

    // Private methods

    /**
     * Helper method that retrieves the <code>Logger</code> instance.
     *
     * @return The <code>Logger</code> instance.
     */
    private static Logger getLogger() {
        if (MyLogger.logger == null) {
            try {
                // Get initialization time
                int initTime = (int) (MiscUtils.getUnixTimestamp());

                // Determine logging folder path
                String loggingFolder;
                if (new File(IOConstants.APP_DATA_FOLDER_PATH).exists()) {
                    loggingFolder = IOMethods.joinPaths(IOConstants.APP_DATA_FOLDER_PATH, "logs");
                } else {
                    loggingFolder = IOConstants.USER_HOME_PATH;
                }

                // Create logging folder (if it doesn't already exist)
                IOMethods.createFolder(loggingFolder);

                // Try to read logging config from the logging properties file
                try (InputStream is = IOMethods.getInputStream("conf/logging.properties")) {
                    LogManager.getLogManager().readConfiguration(is);
                }

                // Set up logger
                logger = Logger.getLogger("AudiTranscribe");

                // Create file handler
                FileHandler fileHandler = new FileHandler(
                        IOMethods.joinPaths(loggingFolder, "Log-" + initTime + ".log"),
                        MAX_LOG_FILE_SIZE,
                        1,
                        true
                );

                // Update attributes of the file handler
                fileHandler.setLevel(Level.FINE);
                fileHandler.setFormatter(new SimpleFormatter());

                // Add the file handler to the logger
                logger.addHandler(fileHandler);

                // Make logger use parent handlers
                logger.setUseParentHandlers(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return logger;
    }
}
