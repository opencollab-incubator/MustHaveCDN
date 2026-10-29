package dev.opencollab.mhcdn.config;

import org.spongepowered.configurate.interfaces.meta.defaults.DefaultBoolean;
import org.spongepowered.configurate.interfaces.meta.defaults.DefaultString;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public interface Config {
    @Comment("The type of CDN to upload to")
    default Type type() {
        return Type.R2;
    }

    @Comment("Public base URL the bucket is served from (a custom domain attached to the bucket)")
    @DefaultString("")
    String publicUrl();

    @Comment("Prefix for uploaded object keys")
    @DefaultString("packs/")
    String keyPrefix();

    @Comment("Whether Geyser should send a pack directly if the client fails to download it from the CDN (Will cause ALL packs to be sent locally due to bedrock jank)")
    @DefaultBoolean(true)
    boolean fallback();

    @Comment("Cloudflare R2 bucket the resource packs are uploaded to")
    R2 r2();

    @Comment("Generic S3 bucket the resource packs are uploaded to")
    S3 s3();

    enum Type {
        R2, S3
    }

    @ConfigSerializable
    interface R2 {
        @DefaultString("")
        String accountId();

        @DefaultString("")
        String accessKeyId();

        @DefaultString("")
        String secretAccessKey();

        @DefaultString("")
        String bucket();
    }

    @ConfigSerializable
    interface S3 {
        @DefaultString("")
        String s3BucketUrl();

        @DefaultString("")
        String accessKeyId();

        @DefaultString("")
        String secretAccessKey();

        @DefaultString("")
        String bucket();
    }
}
