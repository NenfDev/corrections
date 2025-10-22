package dev.lsdmc.edenCorrections.utils;

import java.util.logging.Logger;


public final class LoggingUtils {

    private static final String PREFIX = "[Corrections]";

    private LoggingUtils() { }

    public static void info(Logger logger, String message) {
        if (logger != null && message != null) {
            logger.info(format("INFO", message));
        }
    }

    public static void warn(Logger logger, String message) {
        if (logger != null && message != null) {
            logger.warning(format("WARN", message));
        }
    }

    public static void error(Logger logger, String message) {
        if (logger != null && message != null) {
            logger.severe(format("ERROR", message));
        }
    }

    public static void debug(Logger logger, boolean debugEnabled, String message) {
        if (debugEnabled && logger != null && message != null) {
            logger.info(format("DEBUG", message));
        }
    }

    private static String format(String level, String message) {
        
        return PREFIX + " [" + level + "] " + message;
    }
}


