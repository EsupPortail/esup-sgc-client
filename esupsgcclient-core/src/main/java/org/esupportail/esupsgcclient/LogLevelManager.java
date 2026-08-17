package org.esupportail.esupsgcclient;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

public final class LogLevelManager {

    public static final String ESUP_LOGGER_NAME = "org.esupportail";
    public static final String DEFAULT_LEVEL_NAME = "INFO";

    public enum LogLevelChoice {
        TRACE("TRACE", Level.TRACE),
        DEBUG("DEBUG", Level.DEBUG),
        INFO("INFO", Level.INFO),
        WARN("WARN", Level.WARN),
        ERROR("ERROR", Level.ERROR);

        private final String label;
        private final Level level;

        LogLevelChoice(String label, Level level) {
            this.label = label;
            this.level = level;
        }

        public String getLabel() {
            return label;
        }

        public Level getLevel() {
            return level;
        }

        public static LogLevelChoice fromName(String value) {
            if (value == null || value.isBlank()) {
                return INFO;
            }
            for (LogLevelChoice choice : values()) {
                if (choice.label.equalsIgnoreCase(value.trim())) {
                    return choice;
                }
            }
            return INFO;
        }

        public static LogLevelChoice fromLevel(Level level) {
            if (level == null) {
                return INFO;
            }
            for (LogLevelChoice choice : values()) {
                if (choice.level.equals(level)) {
                    return choice;
                }
            }
            return INFO;
        }
    }

    private LogLevelManager() {
    }

    public static Level parseLevel(String value, Level defaultLevel) {
        LogLevelChoice choice = LogLevelChoice.fromName(value);
        return choice.getLevel() != null ? choice.getLevel() : defaultLevel;
    }

    public static String toName(Level level) {
        return LogLevelChoice.fromLevel(level).getLabel();
    }

    public static void setLoggerLevel(String loggerName, Level level) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(level);
    }

    public static void setRootLoggerLevel(Level level) {
        setLoggerLevel(org.slf4j.Logger.ROOT_LOGGER_NAME, level);
    }
}
