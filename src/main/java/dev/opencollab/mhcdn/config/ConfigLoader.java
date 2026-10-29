package dev.opencollab.mhcdn.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public final class ConfigLoader {
    public static final String VERSION_KEY = "config-version";

    private static final ConfigurationTransformation.Versioned MIGRATIONS = ConfigurationTransformation.versionedBuilder()
            .versionKey(VERSION_KEY)
            // 0 -> 1: introduced config-version, layout unchanged
            .addVersion(1, ConfigurationTransformation.empty())
            // 1 -> 2: add session-packs
            .addVersion(2, ConfigurationTransformation.empty())
            .build();

    private ConfigLoader() {
    }

    public static Config load(Path dataFolder) throws ConfigurateException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(dataFolder.resolve("config.yml"))
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            .defaultOptions(options -> InterfaceDefaultOptions.addTo(options).shouldCopyDefaults(true))
            .build();

        CommentedConfigurationNode node = loader.load();
        if (node.empty()) {
            // Fresh config, nothing to migrate
            node.node(VERSION_KEY).set(MIGRATIONS.latestVersion());
        } else {
            int version = MIGRATIONS.version(node);
            if (version > MIGRATIONS.latestVersion()) {
                throw new ConfigurateException(node, "Config version " + version
                    + " is newer than this version of MustHaveCDN supports (" + MIGRATIONS.latestVersion() + ")");
            }
            MIGRATIONS.apply(node);
        }
        node.node(VERSION_KEY).comment("Used to migrate the config between versions, do not change");

        Config config = node.get(Config.class);
        // Writes out migrations and any missing defaults
        loader.save(node);
        return config;
    }

    public static boolean isConfigured(Config config) {
        return !config.publicUrl().isBlank();
    }
}
