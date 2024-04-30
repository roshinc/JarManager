package dev.roshin.tools.util;

import org.slf4j.Logger;

public class AnsiLogger {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    public static void info(String message) {
        System.out.println(BLUE + "[INFO] " + message + RESET);
    }

    public static void info(String template, Object... args) {
        System.out.println(BLUE + "[INFO] " + RESET + formatTemplate(template, BLUE, args));
    }

    public static void info(Logger logger, String template, Object... args) {
        logger.info(template, args);
        System.out.println(BLUE + "[INFO] " + RESET + formatTemplate(template, BLUE, args));
    }

    public static void success(String message) {
        System.out.println(GREEN + "[SUCCESS] " + message + RESET);
    }

    public static void success(String template, Object... args) {
        System.out.println(GREEN + "[SUCCESS] " + RESET + formatTemplate(template, GREEN, args));
    }

    public static void warning(String message) {
        System.out.println(YELLOW + "[WARNING] " + message + RESET);
    }

    public static void warning(String template, Object... args) {
        System.out.println(YELLOW + "[WARNING] " + RESET + formatTemplate(template, YELLOW, args));
    }

    public static void warning(Logger logger, String template, Object... args) {
        logger.warn(template, args);
        System.out.println(YELLOW + "[WARNING] " + RESET + formatTemplate(template, YELLOW, args));
    }

    public static void error(String message) {
        System.out.println(RED + "[ERROR] " + message + RESET);
    }

    public static void error(String template, Object... args) {
        System.out.println(RED + "[ERROR] " + RESET + formatTemplate(template, RED, args));
    }
    public static void error(Logger logger, String template, Object... args) {
       logger.error(template, args);
        System.out.println(RED + "[ERROR] " + RESET + formatTemplate(template, RED, args));
    }

    public static void debug(String message) {
        System.out.println(MAGENTA + "[DEBUG] " + message + RESET);
    }

    public static void debug(String template, Object... args) {
        System.out.println(MAGENTA + "[DEBUG] " + RESET + formatTemplate(template, MAGENTA, args));
    }

    public static void custom(String prefix, String message, String color) {
        System.out.println("[" + prefix + "] " + message);
    }

    public static void custom(String prefix, String template, String color, Object... args) {
        System.out.println("[" + prefix + "] " + formatTemplate(template, color, args));
    }

    private static String formatTemplate(String template, String color, Object... args) {
        String[] parts = template.split("\\{}");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < args.length) {
                sb.append(color).append(args[i]).append(RESET);
            }
        }

        return sb.toString();
    }
}