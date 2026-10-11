package com.hollispark.store.media;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Production storage: objects in an S3 bucket under "media/", so a CloudFront behaviour for
 * /media/* can serve them directly. Credentials come from the ECS task role.
 */
public class S3MediaStorage implements MediaStorage {
    private static final String PREFIX = "media/";
    private final S3Client s3;
    private final String bucket;

    public S3MediaStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        s3.putObject(b -> b.bucket(bucket).key(PREFIX + key).contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable"), RequestBody.fromBytes(bytes));
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(b -> b.bucket(bucket).key(PREFIX + key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    public Stored get(String key) {
        try {
            var res = s3.getObjectAsBytes(b -> b.bucket(bucket).key(PREFIX + key));
            return new Stored(res.asByteArray(), res.response().contentType());
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(b -> b.bucket(bucket).key(PREFIX + key));
    }
}
