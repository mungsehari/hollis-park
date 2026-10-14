package com.hollispark.store.storage;

/** Detects the image type from the file's first bytes, never from its name or the browser's claim. */
public final class ImageTypes {
    private ImageTypes() {}

    public record Type(String contentType, String extension) {}

    public static Type detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return new Type("image/jpeg", "jpg");
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return new Type("image/png", "png");
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return new Type("image/webp", "webp");
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p'
                && ((b[8] == 'a' && b[9] == 'v' && b[10] == 'i' && (b[11] == 'f' || b[11] == 's')))) return new Type("image/avif", "avif");
        return null;
    }
}
