package com.flansmod.common.eventhandlers;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.Entity;

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
