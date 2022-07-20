package com.flansmod.common.eventhandlers;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Cancelable
public class PlayerEnterSeatEvent extends Event {
	
	private EntitySeat seat;
	
	private EntityPlayer player;
	
	
	public PlayerEnterSeatEvent(EntitySeat seat, EntityPlayer player) {
		this.seat = seat;
		this.player = player;
	}
	
	public EntitySeat getSeat() {
		return seat;
	}
	
	public EntityPlayer getPlayer() {
		return player;
	}
	
}
