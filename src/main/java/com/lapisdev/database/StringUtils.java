package com.lapisdev.database;

public class StringUtils {
    public static String toSnakeCase(String camelCaseString) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCaseString.length(); i++) {
            char c = camelCaseString.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String toCamelCase(String snakeCaseString) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < snakeCaseString.length(); i++) {
            char c = snakeCaseString.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
}
