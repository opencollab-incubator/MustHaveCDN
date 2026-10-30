package dev.opencollab.mhcdn;

import dev.opencollab.mhcdn.config.Config;
import dev.opencollab.mhcdn.config.ConfigLoader;
import dev.opencollab.mhcdn.uploader.R2Uploader;
import dev.opencollab.mhcdn.uploader.S3Uploader;
import dev.opencollab.mhcdn.uploader.Uploader;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;
import org.geysermc.geyser.api.pack.option.UrlFallbackOption;

import java.util.ArrayList;
import java.util.List;

public class MHCDNExtension implements Extension {
    private Config config;
    private Uploader uploader;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        init();
    }

    @Subscribe
    public void onReload(GeyserPreReloadEvent event) throws Exception {
        closeUploader();
        init();
    }

    private void init() {
        try {
            this.config = ConfigLoader.load(this.dataFolder());
        } catch (Exception e) {
            this.logger().error("Failed to load config! Packs will be sent directly.", e);
            return;
        }

        if (!ConfigLoader.isConfigured(this.config)) {
            this.logger().warning("Public URL isn't configured yet, fill in config.yml. Packs will be sent directly.");
            return;
        }

        this.uploader = switch (this.config.type()) {
            case R2 -> new R2Uploader(this.config);
            case S3 -> new S3Uploader(this.config);
        };

        if (this.config.sessionPacks()) {
            logger().warning("Uploading of session packs has been enabled, this will block session join until packs are uploaded.");
        }
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger().info("Loading MustHaveCDN...");
    }

    @Subscribe(postOrder = PostOrder.LAST)
    public void onSessionPacks(GeyserDefineResourcePacksEvent event) {
        if (this.uploader == null) {
            return;
        }

        for (ResourcePack pack : new ArrayList<>(event.resourcePacks())) {
            if (!(pack.codec() instanceof PathPackCodec pathPackCodec)) {
                continue;
            }

            String name = pathPackCodec.path().getFileName().toString();
            try {
                String url = this.uploader.upload(pathPackCodec);

                ResourcePack urlPack = ResourcePack.builder(PackCodec.url(url))
                        .contentKey(pack.contentKey())
                        .build();

                List<ResourcePackOption<?>> options = new ArrayList<>(event.options(pack.uuid()));
                options.add(this.config.fallback() ? UrlFallbackOption.TRUE : UrlFallbackOption.FALSE);

                event.unregister(pack.uuid());
                event.register(urlPack, options.toArray(ResourcePackOption[]::new));
            } catch (Exception e) {
                this.logger().error("Failed to upload " + name + ", it will be sent directly.", e);
            }
        }
    }

    @Subscribe(postOrder = PostOrder.LAST)
    public void onSessionPacks(SessionLoadResourcePacksEvent event) {
        if (!this.config.sessionPacks()) return;

        if (this.uploader == null) {
            return;
        }

        for (ResourcePack pack : new ArrayList<>(event.resourcePacks())) {
            if (!(pack.codec() instanceof PathPackCodec pathPackCodec)) {
                continue;
            }

            String name = pathPackCodec.path().getFileName().toString();
            try {
                String url = this.uploader.upload(pathPackCodec);

                ResourcePack urlPack = ResourcePack.builder(PackCodec.url(url))
                    .contentKey(pack.contentKey())
                    .build();

                List<ResourcePackOption<?>> options = new ArrayList<>(event.options(pack.uuid()));
                options.add(this.config.fallback() ? UrlFallbackOption.TRUE : UrlFallbackOption.FALSE);

                event.unregister(pack.uuid());
                event.register(urlPack, options.toArray(ResourcePackOption[]::new));
            } catch (Exception e) {
                this.logger().error("Failed to upload " + name + ", it will be sent directly.", e);
            }
        }
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) throws Exception {
        closeUploader();
    }

    private void closeUploader() throws Exception {
        if (this.uploader != null) {
            this.uploader.close();
            this.uploader = null;
        }
    }
}
