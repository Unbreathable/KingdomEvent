package com.liphium.kingdom;

import com.liphium.core.Core;
import com.liphium.kingdom.command.SetCommand;
import com.liphium.kingdom.command.TimerCommand;
import com.liphium.kingdom.game.flag.FlagManager;
import com.liphium.kingdom.game.horse.HorseManager;
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
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.CloneWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

public final class Kingdom extends JavaPlugin {

    public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY).append(Component.text("Kingdom", NamedTextColor.AQUA)).append(Component.text("]", NamedTextColor.DARK_GRAY)).append(Component.text(" "));

    private static Kingdom instance;

    private MultiverseCoreApi core;

    private TaskManager taskManager;

    private GameManager gameManager;

    private MachineManager machineManager;

    private final FlagManager flagManager = new FlagManager();

    private final HorseManager horseManager = new HorseManager();

    public static String GAME_WORLD = "game";

    public static NamespacedKey TNT_BOW_KEY;

    @Override
    public void onEnable() {
        instance = this;
        Core.init();
        TNT_BOW_KEY = new NamespacedKey(this, "tnt-bow");

        // Initialize multiverse core and stuff
        core = MultiverseCoreApi.get();
        assert core != null;
        getLogger().info("Deleting game world...");
        core.getWorldManager().getWorld(GAME_WORLD).peek(world -> {
            core.getWorldManager().deleteWorld(DeleteWorldOptions.world(world)).onSuccess(() -> {
                getLogger().info("Successfully deleted the game world.");
            });
        });
        getLogger().info("Creating world for the game...");
        core.getWorldManager().getWorld("world").peek(world -> {
            core.getWorldManager().cloneWorld(CloneWorldOptions.fromTo(world, GAME_WORLD)).onSuccess(() -> {
                getLogger().info("Successfully created the game world.");
            });
        });

        taskManager = new TaskManager();
        taskManager.initTask();

        gameManager = new GameManager();

        machineManager = new MachineManager();

        Listener[] listeners = new Listener[]{new GameListener(), new ChatListener(), new JoinQuitListener(), flagManager, horseManager};
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        getCommand("set").setExecutor(new SetCommand());
        getCommand("timer").setExecutor(new TimerCommand());

        Core.getInstance().getScreens().register(new TeamSelectionScreen(), new ItemShopScreen());
    }

    @Override
    public void onDisable() {
        core.getWorldManager().getWorld(GAME_WORLD).peek(world -> {
            core.getWorldManager().deleteWorld(DeleteWorldOptions.world(world)).onSuccess(() -> {
                getLogger().info("Successfully deleted the game world.");
            });
        });
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    public HorseManager getHorseManager() {
        return horseManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public static Kingdom getInstance() {
        return instance;
    }
}
