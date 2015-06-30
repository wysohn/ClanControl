package de.themoep.clancontrol.listeners;

import de.themoep.clancontrol.ClanControl;
import de.themoep.clancontrol.OccupiedChunk;
import de.themoep.clancontrol.Region;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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
public class MoveListener implements Listener {
    private final ClanControl plugin;

    public MoveListener(ClanControl plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!event.isCancelled()) {
            if(event.getFrom().getChunk() != event.getTo().getChunk()) {
                if(event.getTo().getWorld().equals(plugin.getRegionManager().getWorld())) {
                    OccupiedChunk chunkTo = plugin.getRegionManager().getChunk(event.getTo());
                    if(chunkTo != null) {
                        OccupiedChunk chunkFrom = plugin.getRegionManager().getChunk(event.getFrom());
                        if(chunkFrom == null || !chunkFrom.getClan().equals(chunkTo.getClan())) {
                            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du befindest dich nun im Chunk von " + plugin.getClanDisplay(chunkTo.getClan()) + ChatColor.YELLOW + "!");
                        }
                    }
                    Region regionTo = plugin.getRegionManager().getRegion(event.getTo());
                    if(regionTo != null && !regionTo.getController().isEmpty()) {
                        Region regionFrom = plugin.getRegionManager().getRegion(event.getFrom());
                        if(regionFrom == null || !regionFrom.getController().equals(regionTo.getController())) {
                            String clanDisplay = plugin.getClanDisplay(regionTo.getController());
                            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du befindest dich nun in der " + regionTo.getStatus().toString() + " Region von " + clanDisplay + ChatColor.YELLOW + "!");
                            String playerclan = plugin.getClan(event.getPlayer());
                            if(!plugin.areAllied(playerclan, regionTo.getController())) {
                                plugin.notifyClan(regionTo.getController(), clanDisplay + " " + ChatColor.RED + event.getPlayer().getName() + " hat die " + regionTo.getStatus().toString() + " Region " + regionTo.getX() + "/" + regionTo.getZ() + " deines Clans betreten!");
                            }
                        }
                    }
                }
            }
        }        
    }
}