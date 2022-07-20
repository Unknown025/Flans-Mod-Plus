package com.flansmod.common.eventhandlers;

import com.flansmod.common.driveables.EntityDriveable;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Cancelable
public class GunFiredEvent extends Event {
	
	private Entity shooter;
	
	
	public GunFiredEvent(Entity shooter) {
		this.shooter = shooter;
	}
	
	public Entity getShooter() {
		return shooter;
	}
	
}
