package com.shafting.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shafting.json");

    public boolean littlefootEsp = true;
    public boolean corpseHighlight = true;
    public boolean hideOpened = true;
    public int lapisColor = 0x5555FFFF;
    public int mineralColor = 0xAAAAAAFF;
    public int yogColor = 0xFFAA00FF;
    public int vanguardColor = 0xFF55FFFF;

    public static Config load() {
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Config c = GSON.fromJson(reader, Config.class);
            return c == null ? new Config() : c;
        } catch (Exception ignored) {
            return new Config();
        }
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {
        }
    }
}
