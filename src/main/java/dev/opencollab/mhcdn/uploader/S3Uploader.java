package dev.opencollab.mhcdn.uploader;

import dev.opencollab.mhcdn.config.Config;
import org.geysermc.geyser.api.pack.PathPackCodec;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.HexFormat;

/**
 * Uploads resource packs to an S3 bucket.
 * Objects are keyed by the pack's SHA-256, so unchanged packs are never re-uploaded
 * and stale copies never need purging from the CDN cache.
 */
public class S3Uploader extends Uploader implements AutoCloseable {
    private final S3Client client;
    private final String bucket;

    public S3Uploader(Config config) {
        super(config);

        this.client = createS3Client(config);
        this.bucket = getBucket(config);
    }

    protected S3Client createS3Client(Config config) {
        Config.S3 s3 = config.s3();
        S3ClientBuilder builder = S3Client.builder()
                .endpointOverride(URI.create(s3.s3BucketUrl()))
                .region(Region.of("auto"))
                .httpClient(UrlConnectionHttpClient.create())
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.accessKeyId(), s3.secretAccessKey())
                ));

        return builder.build();
    }

    protected String getBucket(Config config) {
        return config.s3().bucket();
    }

    /**
     * Uploads the pack if it isn't already in the bucket.
     *
     * @return the public URL of the pack
     */
    public String upload0(PathPackCodec codec) throws IOException {
        createBucketIfRequired();
        String key = keyPrefix + HexFormat.of().formatHex(codec.sha256()) + ".zip";

        if (!exists(key)) {
            long size = codec.size();
            try (InputStream in = Channels.newInputStream(codec.serialize())) {
                client.putObject(builder -> builder
                                .bucket(bucket)
                                .key(key)
                                // Bedrock requires application/zip with a correct content length
                                .contentType("application/zip")
                                .contentLength(size)
                                .cacheControl("public, max-age=31536000, immutable"),
                        RequestBody.fromInputStream(in, size));
            }
        }

        return publicUrl + "/" + key;
    }

    private void createBucketIfRequired() {
        boolean bucketExists = false;
        try {
            client.getBucketAcl(r -> r.bucket(bucket));
            bucketExists = true;
        } catch (AwsServiceException ase) {
            if ((ase.statusCode() == HttpStatusCode.MOVED_PERMANENTLY) || "AccessDenied".equals(ase.awsErrorDetails().errorCode())) {
                bucketExists = true;
            } else if (ase.statusCode() != HttpStatusCode.NOT_FOUND) {
                throw ase;
            }
        }
        if (!bucketExists) {
            client.createBucket(CreateBucketRequest.builder()
                            .bucket(bucket)
                            .build());
        }
    }

    public String getExistingUrl(PathPackCodec codec) {
        String key = keyPrefix + HexFormat.of().formatHex(codec.sha256()) + ".zip";

        if (exists(key)) return publicUrl + "/" + key;
        return null;
    }

    private boolean exists(String key) {
        try {
            client.headObject(builder -> builder.bucket(bucket).key(key));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
