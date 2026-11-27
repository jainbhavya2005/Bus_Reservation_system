package util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Weekly schedule utility for bus run days (e.g. "DAILY", "MON,WED,FRI", "TUE,THU,SAT").
 */
public class RunDays {

    /**
     * Checks if a bus scheduled on {@code runDays} operates on the given {@code date}.
     *
     * @param runDays comma-delimited days of week (e.g. "MON,WED,FRI") or "DAILY" / null
     * @param date the journey date to check
     * @return true if the bus operates on that date, false otherwise
     */
    public static boolean runsOn(String runDays, LocalDate date) {
        if (date == null) return true;
        if (runDays == null || runDays.isBlank() || runDays.trim().equalsIgnoreCase("DAILY")) {
            return true;
        }

        String targetDay = date.getDayOfWeek().name().substring(0, 3).toUpperCase(Locale.ROOT);
        String[] parts = runDays.split(",");
        for (String part : parts) {
            String p = part.trim().toUpperCase(Locale.ROOT);
            if (p.equals(targetDay) || p.startsWith(targetDay)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a human-friendly label for a schedule string.
     * E.g. "DAILY" -> "Daily", "MON,WED,FRI" -> "Mon, Wed, Fri"
     */
    public static String label(String runDays) {
        if (runDays == null || runDays.isBlank() || runDays.trim().equalsIgnoreCase("DAILY")) {
            return "Daily";
        }

        String cleaned = Arrays.stream(runDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));

        if ("Mon, Tue, Wed, Thu, Fri".equalsIgnoreCase(cleaned)) {
            return "Mon - Fri (Weekdays)";
        }
        if ("Sat, Sun".equalsIgnoreCase(cleaned) || "Sun, Sat".equalsIgnoreCase(cleaned)) {
            return "Sat, Sun (Weekends)";
        }

        return cleaned.isEmpty() ? "Daily" : cleaned;
    }

    /**
     * Normalizes a user-entered run_days string (e.g. "mon, wed" -> "MON,WED").
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank() || input.trim().equalsIgnoreCase("DAILY")) {
            return "DAILY";
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
