package com.flansmod.common.eventhandlers;

import com.flansmod.common.driveables.EntityDriveable;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.item.ItemStack;

@Cancelable
public class DriveableDeathByHandEvent extends Event {
	
	private EntityDriveable driveable;
	
	private ItemStack driveableStack;
	
	
	public DriveableDeathByHandEvent(EntityDriveable driveable, ItemStack driveableStack) {
		this.driveable = driveable;
		this.driveableStack = driveableStack;
	}
	
	public EntityDriveable getEntity() {
		return driveable;
	}
	
	public ItemStack getItemStack() {
		return driveableStack;
	}
	
}
