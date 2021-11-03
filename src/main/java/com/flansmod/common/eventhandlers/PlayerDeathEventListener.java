package com.flansmod.common.eventhandlers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.network.PacketKillMessage;
import com.flansmod.common.teams.Team;
import com.flansmod.common.types.InfoType;

import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@SuppressWarnings("unused")
public class PlayerDeathEventListener {

	public PlayerDeathEventListener()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	@EventHandler
	@SubscribeEvent
	public void PlayerDied(LivingDeathEvent DamageEvent)
	{
		Entity souceEntity = DamageEvent.source.getSourceOfDamage();
		
		if( (DamageEvent.source.getDamageType().equalsIgnoreCase("explosion") &&
			((souceEntity instanceof EntityGrenade) || (souceEntity instanceof EntityBullet))) &&
			DamageEvent.entityLiving instanceof EntityPlayer)
		{
			InfoType info;
			EntityPlayer killer;
			EntityPlayer killed = (EntityPlayer) DamageEvent.entityLiving;

			if(souceEntity instanceof EntityGrenade)
			{
				killer = (EntityPlayer) ((EntityGrenade) souceEntity).thrower;
				info = ((EntityGrenade) souceEntity).type;
			}
			else
			{
				killer = (EntityPlayer) ((EntityBullet) souceEntity).owner;
				info = ((EntityBullet) souceEntity).type;
			}

			Team killerTeam = null;
			Team killedTeam = null;
			killerTeam = PlayerHandler.getPlayerData(killer).team;
			killedTeam = PlayerHandler.getPlayerData(killed).team;

			if(killer.getHeldItem()!=null){
				FlansMod.getPacketHandler().sendToDimension(
						new PacketKillMessage(false,
								info,
								killer.getHeldItem().getItemDamage(),
								(killedTeam == null ? "f" : killedTeam.textColour) + killed.getDisplayName(),
								(killerTeam == null ? "f" : killedTeam.textColour) + killer.getDisplayName(),
								killed.getDistanceToEntity(killer)),
						DamageEvent.entityLiving.dimension);
			} else {
				FlansMod.getPacketHandler().sendToDimension(
						new PacketKillMessage(false,
								info,
								0,
								(killedTeam == null ? "f" : killedTeam.textColour) + killed.getDisplayName(),
								(killerTeam == null ? "f" : killedTeam.textColour) + killer.getDisplayName(),
								killed.getDistanceToEntity(killer)),
						DamageEvent.entityLiving.dimension);
			}
			// FlansMod.log("Weapon:"+((EntityBullet)souceEntity).type+" victim:"+killed.getDisplayName()+" Murderer:"+killer.getDisplayName());
			// FlansMod.log("Weapon:"+((EntityBullet)souceEntity).type+" victim:"+killed.getDisplayName()+" Murderer:"+killer.getDisplayName());
			FlansMod.log(killer.getDisplayName() + " has killed " + killed.getDisplayName() + " with " + info.name + ". TickExisted:" + killed.ticksExisted / 20 +
				" KilledPos(X:" + (int)(killed.posX) + " Y:" + (int)(killed.posY) + " Z:" + (int)(killed.posZ) + ")" +
				" KillerPos(X:" + (int)(killer.posX) + " Y:" + (int)(killer.posY) + " Z:" + (int)(killer.posZ) + ")");

			if(killed.getCurrentArmor(2) != null) {
				FlansMod.log("KilledPlayer:" + killed + " wore " + killed.getCurrentArmor(2).getUnlocalizedName());
			} else {
				FlansMod.log("KilledPlayer:" + killed + " wears nothing.");
			}

			if(killer.getCurrentArmor(2) != null) {
				FlansMod.log("Killer:" + killer + " wore " + killer.getCurrentArmor(2).getUnlocalizedName());
			} else {
				FlansMod.log("Killer:" + killer + " wears nothing.");
			}

			if(killed.ticksExisted / 20 < FlansMod.noticeSpawnKillTime) {
				FlansMod.log("Warning! PlayerName:" + killer.getCommandSenderName() + " may have SPAWN KILLED. Time:" + killed.ticksExisted / 20 + " " + killed.getCommandSenderName() + " was killed.");
			}
			// FlansMod.log(killed.getCurrentArmor(2));
		}
	}
}
