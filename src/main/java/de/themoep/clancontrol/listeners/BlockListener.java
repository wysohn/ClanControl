package de.themoep.clancontrol.listeners;

import de.themoep.clancontrol.ClanControl;
import de.themoep.clancontrol.OccupiedChunk;
import de.themoep.clancontrol.Region;
import de.themoep.clancontrol.RegionStatus;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClanControl
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BlockListener implements Listener {

    private ClanControl plugin;
    
    private static List<Material> beaconBaseMaterial = Arrays.asList(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK);

    public BlockListener(ClanControl plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(!event.isCancelled()) {
            if(event.getBlock().getWorld().equals(plugin.getRegionManager().getWorld())) {
                OccupiedChunk chunk = plugin.getRegionManager().getChunk(event.getBlock().getLocation());
                String clan = plugin.getClan(event.getPlayer());
                boolean blockIsAboveChunkBeacon = chunk != null && chunk.getBeacon() != null && chunk.getBeacon().getY() < event.getBlock().getY() && chunk.getBeacon().getX() == event.getBlock().getX() && chunk.getBeacon().getZ() == event.getBlock().getZ();
                if(plugin.protectBlocks && !plugin.getRegionManager().canBuild(event.getPlayer(), event.getBlockPlaced().getLocation())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You can't place blocks here!");
                    event.setCancelled(true);
                } else if(event.getBlock().getType().isOccluding() && blockIsAboveChunkBeacon) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You can't place beam obstructing blocks above a chunks beacon!");
                    event.setCancelled(true);
                } else if(clan != null && (event.getBlock().getType() == Material.BEACON || beaconBaseMaterial.contains(event.getBlock().getType())) && event.getPlayer().hasPermission("clancontrol.chunks.claim")) {
                    List<Block> beacons = getCompletedBeacons(event.getBlock());
                    beaconloop: for (Block b : beacons) {
                        Block highest = b.getWorld().getHighestBlockAt(b.getLocation());
                        if(!highest.getType().isOccluding()) {
                            Block above = b;
                            while(b.getY() < highest.getY()) {
                                b = b.getRelative(BlockFace.UP);
                                if(b.getType().isOccluding()) {
                                    event.getPlayer().sendMessage(ChatColor.RED + "There is a block above the beacon that is obstructing the beam!");
                                    continue beaconloop;
                                }
                            }
                            boolean success = plugin.getRegionManager().registerBeacon(clan, b.getLocation());
                            if(success) {
                                event.getPlayer().sendMessage(ChatColor.YELLOW + "Du hast den Chunk " + b.getChunk().getX() + "/" + b.getChunk().getZ() + " für " + plugin.getClanDisplay(clan) + ChatColor.YELLOW + " eingenommen!");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the Beacon from a block which could be contained in a beacon structure
     * @param block The block to get a potential beacon from (the base or the beacon itself)
     * @return The a list of beacon blocks (empty list if none found)
     */
    private List<Block> getCompletedBeacons(Block block) {
        List<Block> beacons = new ArrayList<Block>();
        if(block != null) {
            if(block.getType() == Material.BEACON) {
                int x = block.getX() - 1;
                int y = block.getY() - 1;
                int z = block.getZ() - 1;
                for(int i = 0; i < 3; i++) {
                    for(int j = 0; j < 3; j++) {
                        if(!beaconBaseMaterial.contains(block.getWorld().getBlockAt(x + i, y, z + j).getType())) {
                            return beacons;
                        }
                    }
                }
                beacons.add(block);
            } else if(beaconBaseMaterial.contains(block.getType())) {
                List<Block> foundBeacons = new ArrayList<Block>();
                int x = block.getX() - 1;
                int y = block.getY() + 1;
                int z = block.getZ() - 1;
                for(int i = 0; i < 3; i++) {
                    for(int j = 0; j < 3; j++) {
                        Block check = block.getWorld().getBlockAt(x + i, y, z + j);
                        if(check.getType() == Material.BEACON) {
                            foundBeacons.add(check);
                        }
                    }
                }
                for(Block b : foundBeacons) {
                    List<Block> completedBeacons = getCompletedBeacons(b);
                    if(completedBeacons != null) {
                        beacons.addAll(completedBeacons);
                    }
                }
            }
        }
        return beacons;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if(!event.isCancelled() && event.getBlock().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(plugin.protectBlocks && !plugin.getRegionManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler 
    public void onBlockDestroy(BlockBreakEvent event) {
        if(!event.isCancelled() && event.getBlock().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(plugin.protectBlocks && !plugin.getRegionManager().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You can't break blocks here!");
                event.setCancelled(true);
            } else {
                OccupiedChunk chunk = plugin.getRegionManager().getChunk(event.getBlock().getLocation());
                String clan = plugin.getClan(event.getPlayer());
                if(chunk != null && chunk.getClan().equals(clan)) {
                    if(event.getBlock().getType() == Material.BEACON && event.getBlock().equals(chunk.getBeacon())) {
                        if(event.getPlayer().hasPermission("clancontrol.chunks.unclaim")) {
                            event.getPlayer().sendMessage(ChatColor.YELLOW + "You unclaimed this chunk for " + plugin.getClanDisplay(clan));
                            plugin.getRegionManager().unregisterChunk(chunk);
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You do not have the permission to unclaim chunks for " + plugin.getClanDisplay(clan));
                            event.setCancelled(true);
                        }
                    } else if(beaconBaseMaterial.contains(event.getBlock().getType())) {
                        List<Block> beacons = getCompletedBeacons(event.getBlock());
                        for(Block b : beacons) {
                            if(b.equals(chunk.getBeacon())) {
                                if(event.getPlayer().hasPermission("clancontrol.chunks.unclaim")) {
                                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Du hast die Sicherung des Chunks " + b.getChunk().getX() + "/" + b.getChunk().getZ() + " für deinen Clan " + plugin.getClanDisplay(clan) + ChatColor.YELLOW + " entfernt!");
                                    plugin.getRegionManager().unregisterChunk(chunk);
                                } else {
                                    event.getPlayer().sendMessage(ChatColor.RED + "You do not have the permission to unclaim chunks for " + plugin.getClanDisplay(clan));
                                    event.setCancelled(true);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event) {
        if(!event.isCancelled() && event.getLocation().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(plugin.protectBlocks) {
                Region region = plugin.getRegionManager().getRegion(event.getLocation());
                if(region != null && region.getStatus() == RegionStatus.CENTER || plugin.getRegionManager().getChunk(event.getLocation()) != null) {
                    event.setCancelled(true);
                } else {
                    for(Block b : event.blockList()) {
                        Region r = plugin.getRegionManager().getRegion(b.getLocation());
                        if(region != null && r.getStatus() == RegionStatus.CENTER || plugin.getRegionManager().getChunk(b.getLocation()) != null) {
                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        }        
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if(!event.isCancelled() && event.getBlock().getLocation().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(plugin.protectBlocks) {
                Region region = plugin.getRegionManager().getRegion(event.getBlock().getLocation());
                if(region != null && region.getStatus() == RegionStatus.CENTER || plugin.getRegionManager().getChunk(event.getBlock().getLocation()) != null) {
                    event.setCancelled(true);
                } else {
                    for(Block b : event.blockList()) {
                        Region r = plugin.getRegionManager().getRegion(b.getLocation());
                        if(region != null && r.getStatus() == RegionStatus.CENTER || plugin.getRegionManager().getChunk(b.getLocation()) != null) {
                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPistonExtendBlock(BlockPistonExtendEvent event) {
        if(!event.isCancelled() && event.getBlock().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(event.getDirection() != BlockFace.DOWN && event.getDirection() != BlockFace.UP) {
                for(Block b : event.getBlocks()) {
                    if(b.getType().isOccluding()) {
                        OccupiedChunk oldChunk = plugin.getRegionManager().getChunk(b.getLocation());
                        boolean oldBlockAboveBeacon = oldChunk != null && oldChunk.getBeacon() != null && oldChunk.getBeacon().getY() < b.getY() && oldChunk.getBeacon().getX() == b.getX() && oldChunk.getBeacon().getZ() == b.getZ();
                        if(!oldBlockAboveBeacon) {
                            Location resultlocation = b.getRelative(event.getDirection()).getLocation();
                            OccupiedChunk newChunk = plugin.getRegionManager().getChunk(resultlocation);
                            boolean newBlockAboveBeacon = newChunk != null && newChunk.getBeacon() != null && newChunk.getBeacon().getY() < resultlocation.getY() && newChunk.getBeacon().getX() == resultlocation.getX() && newChunk.getBeacon().getZ() == resultlocation.getZ();
                            if (newBlockAboveBeacon) {
                                event.setCancelled(true);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtendBlock(BlockPistonRetractEvent event) {
        if(!event.isCancelled() && event.isSticky() && event.getBlock().getWorld().equals(plugin.getRegionManager().getWorld())) {
            if(event.getDirection() != BlockFace.DOWN && event.getDirection() != BlockFace.UP) {
                for(Block b : event.getBlocks()) {
                    if(b.getType().isOccluding()) {
                        OccupiedChunk oldChunk = plugin.getRegionManager().getChunk(b.getLocation());
                        boolean oldBlockAboveBeacon = oldChunk != null && oldChunk.getBeacon() != null && oldChunk.getBeacon().getY() < b.getY() && oldChunk.getBeacon().getX() == b.getX() && oldChunk.getBeacon().getZ() == b.getZ();
                        if(!oldBlockAboveBeacon) {
                            Location resultlocation = b.getRelative(event.getDirection()).getLocation();
                            OccupiedChunk newChunk = plugin.getRegionManager().getChunk(resultlocation);
                            boolean newBlockAboveBeacon = newChunk != null && newChunk.getBeacon() != null && newChunk.getBeacon().getY() < resultlocation.getY() && newChunk.getBeacon().getX() == resultlocation.getX() && newChunk.getBeacon().getZ() == resultlocation.getZ();
                            if (newBlockAboveBeacon) {
                                event.setCancelled(true);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
