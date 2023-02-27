package com.flansmod.common.guns;

public class PenetrationLoss {

	public enum PenetrationLossType {
		PLAYER, ENTITY, BLOCK, DECAY;
		
		public float getEffectOnDamage(BulletType bulletType) {
			switch(this) {
			case BLOCK:
				return bulletType.blockPenetrationEffectOnDamage;
			case DECAY:
				return bulletType.penetrationDecayEffectOnDamage;
			case ENTITY:
				return bulletType.entityPenetrationEffectOnDamage;
			case PLAYER:
				return bulletType.playerPenetrationEffectOnDamage;
			default:
				return 1F;
			}
		}
		
	}
	
	private final PenetrationLossType type;
	
	private final float loss;
	
	public PenetrationLoss(float loss, PenetrationLossType type) {
		this.loss = loss;
		this.type = type;
	}
	
	public float getLoss() {
		return loss;
	}
	
	public PenetrationLossType getType() {
		return type;
	}
	
}
