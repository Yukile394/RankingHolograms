package com.silvera.rankingholograms.util;

public final class TimeFormatter {

    private TimeFormatter() {}

    /**
     * Formats a duration in seconds into a human readable Turkish string,
     * e.g. "1 Gun 4 Saat", "22 Saat 35 Dakika", "45 Dakika".
     */
    public static String format(long totalSeconds, boolean showSeconds) {
        if (totalSeconds <= 0) {
            return showSeconds ? "0 Saniye" : "0 Dakika";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        int parts = 0;

        if (days > 0) {
            sb.append(days).append(" Gun");
            parts++;
        }
        if (hours > 0 && parts < 2) {
            if (parts > 0) sb.append(' ');
            sb.append(hours).append(" Saat");
            parts++;
        }
        if (minutes > 0 && parts < 2 && days == 0) {
            if (parts > 0) sb.append(' ');
            sb.append(minutes).append(" Dakika");
            parts++;
        }
        if (showSeconds && parts < 2 && days == 0 && hours == 0) {
            if (parts > 0) sb.append(' ');
            sb.append(seconds).append(" Saniye");
            parts++;
        }

        if (parts == 0) {
            sb.append(minutes).append(" Dakika");
        }

        return sb.toString();
    }
}
