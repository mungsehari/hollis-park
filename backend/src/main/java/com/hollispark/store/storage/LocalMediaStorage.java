package com.hollispark.store.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Development storage: files under store.media.local-dir, served by Spring at /media/**. */
public class LocalMediaStorage implements MediaStorage {
    private final Path root;

    public LocalMediaStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Bad media key");
        return p;
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        try {
            Path p = resolve(key);
            Files.createDirectories(p.getParent());
            Files.write(p, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public Stored get(String key) {
        try {
            Path p = resolve(key);
            if (!Files.isRegularFile(p)) return null;
            String name = p.getFileName().toString().toLowerCase();
            String type = name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp"
                    : name.endsWith(".avif") ? "image/avif" : "image/jpeg";
            return new Stored(Files.readAllBytes(p), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
