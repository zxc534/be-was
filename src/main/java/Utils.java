public final class Utils {

    private Utils() {}

    public static String[] splitOnce(String text, char delimiter) {
        int idx = text.indexOf(delimiter);
        if (idx < 0) return new String[] { text, ""};

        return new String[] { text.substring(0, idx), text.substring(idx + 1) };
    }
}
