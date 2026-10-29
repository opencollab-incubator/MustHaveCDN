package dev.opencollab.mhcdn.uploader;

import dev.opencollab.mhcdn.config.Config;
import org.geysermc.geyser.api.pack.PathPackCodec;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.HexFormat;

/**
 * Uploads resource packs to a Cloudflare R2 bucket through its S3-compatible API.
 */
public class R2Uploader extends S3Uploader {
    public R2Uploader(Config config) {
        super(config);
    }

    @Override
    protected S3Client createS3Client(Config config) {
        Config.R2 r2 = config.r2();
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + r2.accountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKeyId(), r2.secretAccessKey())))
                .httpClient(UrlConnectionHttpClient.create())
                .forcePathStyle(true)
                // R2 doesn't support all of the SDK's default checksum algorithms
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Override
    protected String getBucket(Config config) {
        return config.r2().bucket();
    }
}
