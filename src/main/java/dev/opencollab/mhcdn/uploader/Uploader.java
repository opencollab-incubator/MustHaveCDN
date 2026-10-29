package dev.opencollab.mhcdn.uploader;

import dev.opencollab.mhcdn.config.Config;
import org.geysermc.geyser.api.pack.PathPackCodec;

import java.io.IOException;

public abstract class Uploader implements AutoCloseable {
    protected final String keyPrefix;
    protected final String publicUrl;

    public Uploader(Config config) {
        this.keyPrefix = config.keyPrefix();
        this.publicUrl = config.publicUrl().replaceAll("/+$", "");
    }

    public String upload(PathPackCodec codec) throws IOException {
        String existingUrl = getExistingUrl(codec);
        if (existingUrl != null) return existingUrl;
        return upload0(codec);
    }

    public abstract String upload0(PathPackCodec codec) throws IOException;

    public abstract String getExistingUrl(PathPackCodec codec);
}
