package com.hollispark.store.media;

/** Where uploaded image bytes live. Keys look like "uploads/2026/10/3f2c….webp". */
public interface MediaStorage {
    void put(String key, byte[] bytes, String contentType);

    boolean exists(String key);

    void delete(String key);

    /** Object bytes and type, or null if there is no such object. Used when the API itself serves /media. */
    default Stored get(String key) {
        return null;
    }

    record Stored(byte[] bytes, String contentType) {}
}
