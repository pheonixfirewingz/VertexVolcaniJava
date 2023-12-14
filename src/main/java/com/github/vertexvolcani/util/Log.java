package com.github.vertexvolcani.util;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
/**
 * Handles logging and debug messages for Vulkan applications using SLF4J.
 * This class provides methods to log messages with different severity levels and handles Vulkan debug messages.
 *
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class Log {
    private static final Logger LOGGER = LoggerFactory.getLogger(Log.class);
    /**
     * empty constructor
     */
    public Log() {}
    /**
     * Enumeration representing severity levels for logging.
     */
    public enum Severity {
        /**
         * Informational messages.
         */
        INFO,
        /**
         * Debugging messages.
         */
        DEBUG,
        /**
         * Warning messages.
         */
        WARNING,
        /**
         * Error messages.
         */
        ERROR
    }

    /**
     * Logs a Vulkan debug message based on its type and severity.
     *
     * @param type Vulkan debug message type.
     * @param msg  Message to log.
     */
    public static void logVkDebugMessage(int type, String msg) {
        if ((type & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            LOGGER.debug(msg);
        } else if ((type & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            LOGGER.warn(msg);
        } else if ((type & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            LOGGER.error(msg);
        } else {
            LOGGER.info(msg);
        }
    }

    /**
     * Logs a message with the specified severity level.
     *
     * @param type Severity level of the message.
     * @param msg  Message to log.
     */
    public static void print(Severity type, String msg) {
        switch (type) {
            case INFO:
                LOGGER.info(msg);
                break;
            case DEBUG:
                LOGGER.debug(msg);
                break;
            case WARNING:
                LOGGER.warn(msg);
                break;
            case ERROR:
                LOGGER.error(msg);
                break;
        }
    }
}