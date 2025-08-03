package com.example.spawnguard;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class SpawnGuardPlugin extends JavaPlugin implements Listener {

    private WorldGuardPlugin wgPlugin;
    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();

    @Override
    public void onEnable() {
        wgPlugin = getWorldGuard();

        if (wgPlugin == null) {
            getLogger().severe("WorldGuard plugin غير موجود! تعطيل البلوقن.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SpawnGuardPlugin اشتغل تمام!");
    }

    private WorldGuardPlugin getWorldGuard() {
        return (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
    }

    private boolean isInSpawnRegion(Player player) {
        RegionManager regionManager = wgPlugin.getRegionContainer().get(player.getWorld());
        if (regionManager == null) return false;

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        for (ProtectedRegion region : regions) {
            if (region.getId().equalsIgnoreCase("spawn")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean wasIn = savedInventories.containsKey(uuid);
        boolean nowIn = isInSpawnRegion(player);

        if (!wasIn && nowIn) {
            savedInventories.put(uuid, player.getInventory().getContents());
            player.getInventory().clear();
            player.sendMessage("§aدخلت منطقة السباون - تم تفريغ الإنفنتوري مؤقتاً.");
        } else if (wasIn && !nowIn) {
            player.getInventory().clear();
            player.getInventory().setContents(savedInventories.get(uuid));
            savedInventories.remove(uuid);
            player.sendMessage("§aخرجت من السباون - تمت استعادة الإنفنتوري.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isInSpawnRegion(player)) {
            event.setCancelled(true);
            player.sendMessage("§cممنوع تكسر بلوكات بالسباون.");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            if (isInSpawnRegion(damaged)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isInSpawnRegion(player)) {
            event.setCancelled(true);
            player.sendMessage("§cممنوع ترمي أغراض بالسباون.");
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInSpawnRegion(player)) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }
}
