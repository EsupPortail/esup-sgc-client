package org.esupportail.esupsgcclient;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

public final class LogLevelManager {

    public static final String ESUP_LOGGER_NAME = "org.esupportail";
    public static final String DEFAULT_LEVEL_NAME = "INFO";

    private LogLevelManager() {
    }

    public static Level parseLevel(String value, Level defaultLevel) {
        if (value == null || value.isBlank()) {
            return defaultLevel;
        }

        switch (value.trim().toUpperCase()) {
            case "TRACE":
                return Level.TRACE;
            case "DEBUG":
                return Level.DEBUG;
            case "INFO":
                return Level.INFO;
            case "WARN":
                return Level.WARN;
            case "ERROR":
                return Level.ERROR;
            default:
                return defaultLevel;
        }
    }

    public static String toName(Level level) {
        if (level == null) {
            return DEFAULT_LEVEL_NAME;
        }
        switch (level.toInt()) {
            case Level.TRACE_INT:
                return "TRACE";
            case Level.DEBUG_INT:
                return "DEBUG";
            case Level.WARN_INT:
                return "WARN";
            case Level.ERROR_INT:
                return "ERROR";
            default:
                return "INFO";
        }
    }

    public static void setLoggerLevel(String loggerName, Level level) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(level);
    }

    public static void setRootLoggerLevel(Level level) {
        setLoggerLevel(org.slf4j.Logger.ROOT_LOGGER_NAME, level);
    }
}
