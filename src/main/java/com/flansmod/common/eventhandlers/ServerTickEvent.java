package com.flansmod.common.eventhandlers;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.ItemGun;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ServerTickEvent {
    public static ArrayList<EntityPlayerMP> nightVisionPlayers = new ArrayList<>();
    public static HashMap<UUID, Integer> regenTimers = new HashMap<>();

    public ServerTickEvent() {
        FMLCommonHandler.instance().bus().register(this);
    }

    int tickCount = 0;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && tickCount >= 20) {
            ArrayList<EntityPlayerMP> playersToRemove = new ArrayList<>();
            for (EntityPlayerMP player : nightVisionPlayers) {
                ItemStack currentItem = player.getCurrentEquippedItem();
                if (currentItem != null && currentItem.getItem() instanceof ItemGun) {
                    ItemGun itemGun = (ItemGun) currentItem.getItem();
                    AttachmentType scope = itemGun.type.getScope(currentItem);
                    if ((scope == null && !itemGun.type.allowNightVision) || (scope != null && !scope.hasNightVision && !itemGun.type.allowNightVision)) {
                        player.removePotionEffect(Potion.nightVision.id);
                        playersToRemove.add(player);
                    }
                } else {
                    player.removePotionEffect(Potion.nightVision.id);
                    playersToRemove.add(player);
                }
            }
            tickCount = 0;
            nightVisionPlayers.removeAll(playersToRemove);
        }
        tickCount++;
    }

    /**
     * Handles regeneration bonuses per player.
     *
     * @see com.flansmod.common.CustomFoodStats Original implementation.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        int regenTimer = regenTimers.containsKey(player.getUniqueID()) ? regenTimers.get(player.getUniqueID()) : 0;
        if (regenTimer >= FlansMod.bonusRegenTickDelay) {
            if (player.getFoodStats().getFoodLevel() >= FlansMod.bonusRegenFoodLimit) {
                player.heal(FlansMod.bonusRegenAmount);
            }
            regenTimer = 0;
        }
        regenTimers.put(player.getUniqueID(), ++regenTimer);
    }
}
