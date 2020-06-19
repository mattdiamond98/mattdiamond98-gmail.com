package com.gmail.mattdiamond98.coronacraft.abilities.Engineer;

import com.gmail.mattdiamond98.coronacraft.abilities.Ability;
import com.gmail.mattdiamond98.coronacraft.CoronaCraft;
import com.gmail.mattdiamond98.coronacraft.event.CoolDownTickEvent;
import com.gmail.mattdiamond98.coronacraft.util.AbilityKey;
import com.gmail.mattdiamond98.coronacraft.util.AbilityUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class Stockpile extends Ability {

    public Stockpile() {
        super("Engineer Tool", Material.IRON_PICKAXE);
    }

    @Override
    public void initialize() {
        // TODO: Other material styles
    }

    @EventHandler
    public void onCoolDownTick(CoolDownTickEvent e) {
        if (e.getItem() != item) return;
        if (e.getTicksRemaining() % (CoronaCraft.ABILITY_TICK_PER_SECOND * 8) != 0) return; // Once per eight seconds
        if (AbilityUtil.getTotalCount(e.getPlayer(), item) == 0) return;
        AbilityUtil.regenerateItem(e.getPlayer(), Material.COBBLESTONE, 32, 4);
        AbilityUtil.regenerateItem(e.getPlayer(), Material.OAK_PLANKS, 64, 8);
        AbilityUtil.regenerateItem(e.getPlayer(), Material.COBWEB, 8, 1);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        AbilityKey key = new AbilityKey(e.getPlayer(), item);
        if (e.getPlayer().getInventory().contains(item)) {
            if (!CoronaCraft.isOnCooldown(e.getPlayer(), item))
                CoronaCraft.getPlayerCoolDowns().put(key, 10_000);
        } else if (CoronaCraft.getPlayerCoolDowns().containsKey(key)){
            CoronaCraft.getPlayerCoolDowns().remove(key);
        }
    }
}
