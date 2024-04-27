package dev.roshin.tools.util;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class CommonUtils {

    // Private constructor to prevent instantiation
    private CommonUtils() {
    }

    /**
     * Returns the file name without the extension.
     *
     * @param path The path to the file.
     * @return The file name without the extension. If the file has no extension, the full file name is returned.
     */
    public static String getFileNameWithoutExtension(Path path) {
        Preconditions.checkNotNull(path, "Path cannot be null.");
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return fileName; // No extension found
        return fileName.substring(0, dotIndex);
    }

    public static void printLogLocation() {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        FileAppender fileAppender = configuration.getAppender("LogFile");
        if (fileAppender != null) {
            String logFilePath = fileAppender.getFileName();
            AnsiLogger.info("Currently logging to: {}", logFilePath);
        } else {
            LoggerFactory.getLogger(CommonUtils.class).warn("No file appender found in the configuration.");
            AnsiLogger.warning("No file appender found in the configuration.");
        }
    }
}
