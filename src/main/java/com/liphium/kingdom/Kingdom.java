package com.liphium.kingdom;

import com.liphium.core.Core;
import com.liphium.kingdom.command.SetCommand;
import com.liphium.kingdom.command.TimerCommand;
import com.liphium.kingdom.game.GameManager;
import com.liphium.kingdom.listener.ChatListener;
import com.liphium.kingdom.listener.GameListener;
import com.liphium.kingdom.listener.JoinQuitListener;
import com.liphium.kingdom.listener.machines.MachineManager;
import com.liphium.kingdom.screens.ItemShopScreen;
import com.liphium.kingdom.screens.TeamSelectionScreen;
import com.liphium.kingdom.util.TaskManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public final class Kingdom extends JavaPlugin {

    public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY).append(Component.text("Kingdom", NamedTextColor.AQUA)).append(Component.text("]", NamedTextColor.DARK_GRAY)).append(Component.text(" "));

    private static Kingdom instance;

    private TaskManager taskManager;

    private GameManager gameManager;

    private MachineManager machineManager;

    public static String GAME_WORLD = "game";

    @Override
    public void onEnable() {
        instance = this;
        Core.init();

        prepareGameWorld();

        taskManager = new TaskManager();
        taskManager.initTask();

        machineManager = new MachineManager();

        gameManager = new GameManager();

        Listener[] listeners = new Listener[]{new GameListener(), new ChatListener(), new JoinQuitListener()};
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        getCommand("set").setExecutor(new SetCommand());
        getCommand("timer").setExecutor(new TimerCommand());

        Core.getInstance().getScreens().register(new TeamSelectionScreen(), new ItemShopScreen());
    }

    @Override
    public void onDisable() {
        deleteWorld(GAME_WORLD);
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public static Kingdom getInstance() {
        return instance;
    }

    private void prepareGameWorld() {
        deleteWorld(GAME_WORLD);

        World sourceWorld = Bukkit.getWorld("world");
        if (sourceWorld == null) {
            sourceWorld = Bukkit.createWorld(WorldCreator.name("world"));
        }
        if (sourceWorld == null) {
            getLogger().warning("Could not load source world 'world'.");
            return;
        }

        Path sourcePath = sourceWorld.getWorldFolder().toPath();
        Path targetPath = sourcePath.getParent().resolve(GAME_WORLD);

        try {
            copyDirectory(sourcePath, targetPath);
            Bukkit.createWorld(WorldCreator.name(GAME_WORLD));
            getLogger().info("Successfully prepared the game world.");
        } catch (IOException exception) {
            getLogger().warning("Failed to prepare the game world: " + exception.getMessage());
        }
    }

    private void deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        Path worldPath = Bukkit.getWorldContainer().toPath().resolve(worldName);
        try {
            if (Files.notExists(worldPath)) {
                return;
            }
            Files.walk(worldPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
            getLogger().info("Successfully deleted world '" + worldName + "'.");
        } catch (IOException exception) {
            getLogger().warning("Failed to delete world '" + worldName + "': " + exception.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Set<String> ignoredEntries = Set.of("uid.dat", "session.lock");
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                if (relative.toString().isEmpty()) {
                    continue;
                }

                Path destination = target.resolve(relative.toString());
                if (ignoredEntries.contains(path.getFileName().toString())) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
