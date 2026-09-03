package com.liphium.snowsplash.util;

import com.liphium.snowsplash.Snowsplash;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class TaskManager {

    private final ArrayList<Runnable> runnables = new ArrayList<>();
    private final ArrayList<Runnable> toRemove = new ArrayList<>();

    public void initTask() {

        new BukkitRunnable() {
            @Override
            public void run() {

                for (Runnable rem : toRemove) {
                    runnables.remove(rem);
                }

                for (Runnable runnable : runnables) {
                    runnable.run();
                }
            }
        }.runTaskTimer(Snowsplash.getInstance(), 0, 1);

    }

    public void inject(Runnable runnable) {
        runnables.add(runnable);
    }

    public void uninject(Runnable runnable) {
        toRemove.add(runnable);
    }

}
