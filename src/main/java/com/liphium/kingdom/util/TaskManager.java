package com.liphium.kingdom.util;

import com.liphium.kingdom.Kingdom;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class TaskManager {

    private final ArrayList<Runnable> runnables = new ArrayList<>();
    private final ArrayList<Runnable> toRemove = new ArrayList<>();
    private final ArrayList<Runnable> toAdd = new ArrayList<>();

    public void initTask() {

        new BukkitRunnable() {
            @Override
            public void run() {
                runnables.addAll(toAdd);
                toAdd.clear();

                for (Runnable runnable : runnables) {
                    runnable.run();
                }

                toRemove.forEach(runnables::remove);
                toRemove.clear();
            }
        }.runTaskTimer(Kingdom.getInstance(), 0, 1);

    }

    public void inject(Runnable runnable) {
        toAdd.add(runnable);
    }

    public void uninject(Runnable runnable) {
        toRemove.add(runnable);
    }

}
