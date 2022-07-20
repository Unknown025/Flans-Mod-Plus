package com.flansmod.common.eventhandlers;

import com.flansmod.common.driveables.EntityDriveable;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Cancelable
public class DriveableDeathEvent extends Event {
	
	private EntityDriveable driveable;
	
	private boolean byHand;
	
	private ItemStack driveableStack;
	
	
	public DriveableDeathEvent(EntityDriveable driveable, ItemStack driveableStack, boolean byHand) {
		this.driveable = driveable;
		this.driveableStack = driveableStack;
		this.byHand = byHand;
	}
	
	public EntityDriveable getEntity() {
		return driveable;
	}
	
	public ItemStack getItemStack() {
		return driveableStack;
	}
	
	public boolean isDestroyedByHand() {
		return byHand;
	}
	
}
