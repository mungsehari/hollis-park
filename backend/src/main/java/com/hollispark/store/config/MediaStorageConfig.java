package com.hollispark.store.config;

import com.hollispark.store.storage.LocalMediaStorage;
import com.hollispark.store.storage.MediaStorage;
import com.hollispark.store.storage.S3MediaStorage;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class MediaStorageConfig {
    private final StoreProperties props;

    public MediaStorageConfig(StoreProperties props) {
        this.props = props;
    }

    @Bean
    MediaStorage mediaStorage() {
        var m = props.media();
        if ("s3".equalsIgnoreCase(m.storage())) {
            if (m.bucket() == null || m.bucket().isBlank()) {
                throw new IllegalStateException("MEDIA_STORAGE=s3 needs MEDIA_BUCKET");
            }
            return new S3MediaStorage(S3Client.builder().region(Region.of(m.region())).build(), m.bucket());
        }
        return new LocalMediaStorage(Path.of(m.localDir()));
    }
}
