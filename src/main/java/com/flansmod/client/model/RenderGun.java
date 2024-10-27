package com.flansmod.client.model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.IScope;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.paintjob.Paintjob;
import com.flansmod.common.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.IItemRenderer;

public class RenderGun implements IItemRenderer {
	private static TextureManager renderEngine;
	public static float smoothing;

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		switch (type) {
		case ENTITY:
			if (!Minecraft.getMinecraft().gameSettings.fancyGraphics)
				return false;
		case EQUIPPED:
		case EQUIPPED_FIRST_PERSON:
			return item != null && item.getItem() instanceof ItemGun
					&& ((ItemGun) item.getItem()).type.model != null;
		default:
			return false;
		}
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return false;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// Avoid any broken cases by returning
		if (!(item.getItem() instanceof ItemGun))
			return;
		GunType gunType = ((ItemGun) item.getItem()).type;
		if (gunType == null)
			return;
		ModelGun model = gunType.model;
		if (model == null)
			return;

		// Render main hand gun
		GunAnimations animations;
		if (type == ItemRenderType.ENTITY) {
			animations = new GunAnimations();
		} else {
			animations = FlansModClient.getGunAnimations((EntityLivingBase) data[1], false);
		}

		//Sets up translations and rotations then calls renderGunModel
		setupGunRender(type, item, gunType, animations, false, data);

		// Render off-hand gun
		if (gunType.getOneHanded() && type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			EntityLivingBase entity = (EntityLivingBase) data[1];
			if (entity instanceof EntityPlayer) {
				EntityPlayer player = (EntityPlayer) entity;
				PlayerData playerData = PlayerHandler.getPlayerData(player, Side.CLIENT);
				if (playerData.offHandGunSlot != 0) {
					animations = FlansModClient.gunAnimationsLeft.get(data[1]);
					if (animations == null) {
						animations = new GunAnimations();
						FlansModClient.gunAnimationsLeft.put((EntityLivingBase) data[1], animations);
					}
					ItemStack offHandItem = player.inventory.getStackInSlot(playerData.offHandGunSlot - 1);
					if (offHandItem == null || !(offHandItem.getItem() instanceof ItemGun))
						return;
					GunType offHandGunType = ((ItemGun) offHandItem.getItem()).type;
					if (!offHandGunType.getOneHanded())
						return;

					//Sets up translations and rotations then calls renderGunModel
					setupGunRender(type, offHandItem, offHandGunType, animations, true, data);
				}

			}
		}
	}

	// Render off-hand gun in 3rd person
	public void renderOffHandGun(EntityPlayer player, ItemStack offHandItemStack) {
		GunAnimations animations = FlansModClient.gunAnimationsLeft.get(player);
		if (animations == null) {
			animations = new GunAnimations();
			FlansModClient.gunAnimationsLeft.put(player, animations);
		}
		GunType offHandGunType = ((ItemGun) offHandItemStack.getItem()).type;
		if (!offHandGunType.getOneHanded())
			return;

		//Sets up translations and rotations then calls renderGunModel
		setupGunRender(ItemRenderType.EQUIPPED, offHandItemStack, offHandGunType, animations, true, player);
	}

	private void setupGunRender(ItemRenderType type, ItemStack item, GunType gunType, GunAnimations animations, boolean offHand, Object... data) {
		// The model scale
		float f = 1F / 16F;
		ModelGun model = gunType.model;

		GL11.glPushMatrix();
		{
			// Do transforms and rotations depending on how the gun is being rendered/viewed
			//Ground, item frame, etc
			if (type == ItemRenderType.ENTITY) {
				renderEntityMovement(model, data);
			//Third person
			} else if (type == ItemRenderType.EQUIPPED) {
				renderEquippedMovement(model, offHand);
			//First person
			} else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
				renderEquippedFirstPersonMovement(gunType, item, model, offHand, animations);
			}

			//Now that the matrix is set up, render the models
			renderGunModel(item, gunType, f, model, animations, type);
		}
		GL11.glPopMatrix();
	}

	private void renderEntityMovement(ModelGun model, Object... data) {
		EntityItem entity = (EntityItem) data[1];
		GL11.glRotatef(entity.age + (entity.age == 0 ? 0 : smoothing), 0F, 1F, 0F);
		GL11.glTranslatef(-0.2F + model.itemFrameOffset.x, 0.2F + model.itemFrameOffset.y,
				0.1F + model.itemFrameOffset.z);
	}

	private void renderEquippedMovement(ModelGun model, boolean offHand) {
		if (offHand) {
			GL11.glRotatef(-70F, 1F, 0F, 0F);
			GL11.glRotatef(48F, 0F, 0F, 1F);
			GL11.glRotatef(105F, 0F, 1F, 0F);
			GL11.glTranslatef(-0.1F, -0.22F, -0.15F);
		} else {
			GL11.glRotatef(35F, 0F, 0F, 1F);
			GL11.glRotatef(-5F, 0F, 1F, 0F);
			GL11.glTranslatef(0.75F, -0.22F, -0.08F);
			GL11.glScalef(1F, 1F, -1F);
		}
		GL11.glTranslatef(model.thirdPersonOffset.x, model.thirdPersonOffset.y, model.thirdPersonOffset.z);
	}

	private void renderEquippedFirstPersonMovement(GunType gunType, ItemStack item, ModelGun model, boolean offHand, GunAnimations animations) {
		int flip = offHand ? -1 : 1;
		IScope scope = gunType.getCurrentScope(item);
		float adsSwitch = FlansModClient.lastZoomProgress
				+ (FlansModClient.zoomProgress - FlansModClient.lastZoomProgress) * smoothing;
		adsSwitch *= model.adsEffectMultiplier;
		boolean crouching = FlansModClient.zoomProgress + 0.1F > 0.9F && ItemGun.crouching && !animations.reloading;
		boolean sprinting = FlansModClient.zoomProgress + 0.1F < 0.2F && ItemGun.sprinting && !animations.reloading
				&& !ItemGun.shooting && model.fancyStance;

		//If in scope, dont render the gun unless its forced in the gun settings
		if (FlansModClient.zoomProgress > 0.9F && scope.hasZoomOverlay() && !model.stillRenderGunWhenScopedOverlay) {
			GL11.glPopMatrix();
			return;


		}

		//Resting positions
		if (offHand) {
			GL11.glTranslatef(0F, 0.03F, -0.76F);
			GL11.glRotatef(23F, 0F, 0F, 1F);
			GL11.glRotatef(-4F, 0F, 1F, 0F);
			GL11.glTranslatef(0.15F, 0.2F, -0.6F);
		} else if (crouching) {
			GL11.glRotatef(25F - 5F * adsSwitch, 0F, 0F, 1F);
			GL11.glRotatef(-5F, 0F, 1F, 0F);
			GL11.glTranslatef(0.15F, 0.2F + 0.175F * adsSwitch, -0.6F - 0.405F * adsSwitch);
			if (gunType.hasScopeOverlay && !model.stillRenderGunWhenScopedOverlay) {
				GL11.glTranslatef(-0.3F * adsSwitch, 0F, 0F);
			}
			GL11.glRotatef(4.5F * adsSwitch, 0F, 0F, 1F);
			// forward, up, sideways
			GL11.glTranslatef(model.crouchZoom, -0.03F * adsSwitch, 0F);
		} else {
			// Angle down slightly
			GL11.glRotatef(25F - 5F * adsSwitch, 0F, 0F, 1F); // Angle nose down slightly -> angle nose up slightly
			GL11.glRotatef(-5F, 0F, 1F, 0F); // Rotate Z nose inward
			GL11.glTranslatef(0.15F, 0.2F + 0.175F * adsSwitch, -0.6F - 0.405F * adsSwitch); // Slightly forward, slightly up -> more up, to left -> more towards middle
			if (gunType.hasScopeOverlay && !model.stillRenderGunWhenScopedOverlay) {
				GL11.glTranslatef(-0.3F * adsSwitch, 0F, 0F);
			}
			// Rotate nose up
			GL11.glRotatef(4.5F * adsSwitch, 0F, 0F, 1F);
			// Move gun down as ADS progresses
			GL11.glTranslatef(-0.0F, -0.03F * adsSwitch, 0F);
		}

		//Weapon switch animation
		if (animations.switchAnimationProgress > 0 && animations.switchAnimationLength > 0) {
			renderWeaponSwitchMovement(animations);
		}

		//Weapon sprinting animation
		if (sprinting && animations.stanceTimer == 0 && FlansMod.enableWeaponSprintStance) {
			if (animations.runningStanceAnimationProgress == 0)
				animations.runningStanceAnimationProgress = 1;
			renderWeaponSprintMovement(animations, model, gunType);
		} else {
			animations.runningStanceAnimationProgress = 0;
		}

		//Melee animations
		if (animations.meleeAnimationProgress > 0 && animations.meleeAnimationProgress < gunType.meleePath.size())
			renderMeleeMovement(gunType, animations);

		if (model.spinningCocking) {
			GL11.glTranslatef(model.spinPoint.x, model.spinPoint.y, model.spinPoint.z);
			float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing);
			GL11.glRotatef(pumped * 180F + 180F, 0F, 0F, 1F);
			GL11.glTranslatef(-model.spinPoint.x, -model.spinPoint.y, -model.spinPoint.z);
		}

		//Handle reload animations
		if (animations.reloading)
			renderReloadMovement(animations, model, gunType, item, flip);
	}

	//TODO, Allow per weapon "resting" positions
	private void renderWeaponSwitchMovement(GunAnimations animations) {
		Vector3f pos1 = new Vector3f(0, -0.4f, 0);
		Vector3f pos2 = new Vector3f(0, 0, 0);
		Vector3f startAngles = new Vector3f(90, 30, -40);
		Vector3f endAngles = new Vector3f(0, 0, 0);
		float interp =
				(animations.switchAnimationProgress + smoothing) / animations.switchAnimationLength;

		GL11.glTranslatef(pos2.x + (pos2.x - pos1.x) * interp,
				pos1.y + (pos2.y - pos1.y) * interp, pos1.z + (pos2.z - pos1.z) * interp);

		GL11.glRotatef(startAngles.y + (endAngles.y - startAngles.y) * interp, 0f, 1f, 0f);
		GL11.glRotatef(startAngles.z + (endAngles.z - startAngles.z) * interp, 0f, 0f, 1f);
	}

	private void renderWeaponSprintMovement(GunAnimations animations, ModelGun model, GunType gunType) {
		Vector3f defaultTranslate = new Vector3f(0, 0F, -0.2);
		Vector3f defaultRotation = new Vector3f(-15F, 45F, -10F);

		Vector3f configuredTranslate = model.sprintStanceTranslate;
		Vector3f configuredRotation = model.sprintStanceRotate;

		float progress = (animations.runningStanceAnimationProgress + smoothing) / animations.runningStanceAnimationLength;
		if (animations.runningStanceAnimationProgress == animations.runningStanceAnimationLength)
			progress = 1;

		if (FlansMod.enableRandomSprintStance) {
			animations.updateSprintStance(gunType.getShortName());
			defaultRotation = animations.sprintingStance;
		}

		if (!Objects.equals(model.sprintStanceTranslate, new Vector3f(0F, 0F, 0F))) {
			GL11.glTranslatef(configuredTranslate.x * progress, configuredTranslate.y * progress, configuredTranslate.z * progress);
		} else {
			GL11.glTranslatef(defaultTranslate.x * progress, defaultTranslate.y * progress, defaultTranslate.z * progress);
		}

		if (!Objects.equals(model.sprintStanceRotate, new Vector3f(0F, 0F, 0F))) {
			GL11.glRotatef(configuredRotation.x * progress, 1f, 0f, 0f);
			GL11.glRotatef(configuredRotation.y * progress, 0f, 1f, 0f);
			GL11.glRotatef(configuredRotation.z * progress, 0f, 0f, 1f);
		} else {
			GL11.glRotatef(defaultRotation.x * progress, 1f, 0f, 0f);
			GL11.glRotatef(defaultRotation.y * progress, 0f, 1f, 0f);
			GL11.glRotatef(defaultRotation.z * progress, 0f, 0f, 1f);
		}
	}

	private void renderMeleeMovement(GunType gunType, GunAnimations animations) {
		Vector3f meleePos = gunType.meleePath.get(animations.meleeAnimationProgress);
		Vector3f nextMeleePos = animations.meleeAnimationProgress + 1 < gunType.meleePath.size() ? gunType.meleePath.get(animations.meleeAnimationProgress + 1) : new Vector3f();
		GL11.glTranslatef(meleePos.x + (nextMeleePos.x - meleePos.x) * smoothing, meleePos.y + (nextMeleePos.y - meleePos.y) * smoothing, meleePos.z + (nextMeleePos.z - meleePos.z) * smoothing);
		Vector3f meleeAngles = gunType.meleePathAngles.get(animations.meleeAnimationProgress);
		Vector3f nextMeleeAngles = animations.meleeAnimationProgress + 1 < gunType.meleePathAngles.size() ? gunType.meleePathAngles.get(animations.meleeAnimationProgress + 1) : new Vector3f();
		GL11.glRotatef(meleeAngles.y + (nextMeleeAngles.y - meleeAngles.y) * smoothing, 0F, 1F, 0F);
		GL11.glRotatef(meleeAngles.z + (nextMeleeAngles.z - meleeAngles.z) * smoothing, 0F, 0F, 1F);
		GL11.glRotatef(meleeAngles.x + (nextMeleeAngles.x - meleeAngles.x) * smoothing, 1F, 0F, 0F);
	}

	private void renderReloadMovement(GunAnimations animations, ModelGun model, GunType gunType, ItemStack item, int flip) {
		EnumAnimationType anim = model.animationType;
		if (gunType.getGrip(item) != null && gunType.getSecondaryFire(item))
			anim = gunType.getGrip(item).model.secondaryAnimType;

		// Calculate the amount of tilt required for the reloading animation
		float reloadRotate = getReloadRotate(animations, model);

		// Rotate/translate the GUN dependent on the animation type
		switch (anim) {
			case BOTTOM_CLIP:
			case PISTOL_CLIP:
			case SHOTGUN:
			case END_LOADED: {
				GL11.glRotatef(60F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(30F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.25F * reloadRotate, 0F, 0F);
				break;
			}
			//TODO Investigate if all this can be deleted
			case CUSTOMBOTTOM_CLIP:
			case CUSTOMPISTOL_CLIP:
			case CUSTOMSHOTGUN:
			case CUSTOMEND_LOADED:
			case CUSTOMBACK_LOADED:
			case CUSTOMBULLPUP:
				//allows you to customize gun tilt & rotation while maintaining the specialized reload
			case CUSTOMRIFLE:
			case CUSTOMRIFLE_TOP:
			case CUSTOMREVOLVER:
			case CUSTOMREVOLVER2:
			case CUSTOMALT_PISTOL_CLIP:
			case CUSTOMSTRIKER:
			case CUSTOMGENERIC:
			case CUSTOM: {
				GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
				GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
				GL11.glTranslatef(model.translateGun.x * reloadRotate, model.translateGun.y * reloadRotate,
						model.translateGun.z * reloadRotate);
				break;
			}
			case BACK_LOADED: {
				GL11.glRotatef(-75F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(-30F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.5F * reloadRotate, 0F, 0F);
				break;
			}
			case BULLPUP: {
				GL11.glRotatef(70F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(10F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.5F * reloadRotate, -0.2F * reloadRotate, 0F);
				break;
			}
			case RIFLE: {
				GL11.glRotatef(30F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(-30F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.5F * reloadRotate, 0F, -0.5F * reloadRotate);
				break;
			}
			case RIFLE_TOP:
			case REVOLVER: {
				GL11.glRotatef(30F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(10F * reloadRotate, 0F, 1F, 0F);
				GL11.glRotatef(-10F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.1F * reloadRotate, -0.2F * reloadRotate, -0.1F * reloadRotate);
				break;
			}
			case REVOLVER2: {
				GL11.glRotatef(20F * reloadRotate, 0F, 0F, 1F);
				GL11.glRotatef(-10F * reloadRotate * flip, 1F, 0F, 0F);
				break;
			}
			case ALT_PISTOL_CLIP: {
				GL11.glRotatef(60F * reloadRotate * flip, 0F, 1F, 0F);
				GL11.glTranslatef(0.15F * reloadRotate, 0.25F * reloadRotate, 0F);
				break;
			}
			case STRIKER: {
				GL11.glRotatef(-35F * reloadRotate * flip, 1F, 0F, 0F);
				GL11.glTranslatef(0.2F * reloadRotate, 0F, -0.1F * reloadRotate);
				break;
			}
			case GENERIC: {
				// Gun reloads partly or completely off-screen.
				GL11.glRotatef(45F * reloadRotate, 0F, 0F, 1F);
				GL11.glTranslatef(-0.2F * reloadRotate, -0.5F * reloadRotate, 0F);
				break;
			}
			default:
				break;
		}
	}

	private static float getReloadRotate(GunAnimations animations, ModelGun model) {
		float reloadRotate = 1F;

		// Snap to zero if reload is finished. Otherwise, weird behaviour.
		if (!animations.reloading) {
			return 0F;
		}

		float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress
				+ (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;

		if (effectiveReloadAnimationProgress < model.tiltGunTime)
			reloadRotate = effectiveReloadAnimationProgress / model.tiltGunTime;
		if (effectiveReloadAnimationProgress > model.tiltGunTime + model.unloadClipTime + model.loadClipTime)
			reloadRotate = 1F - (effectiveReloadAnimationProgress - (model.tiltGunTime + model.unloadClipTime + model.loadClipTime)) / model.untiltGunTime;

		return reloadRotate;
	}

	/**
	 * Gun render method, seperated from transforms so that mecha renderer may also
	 * call this
	 */
	public void renderGunModel(ItemStack item, GunType type, float f, ModelGun model, GunAnimations animations, ItemRenderType rtype) {
		// Make sure we actually have the renderEngine
		if (renderEngine == null)
			renderEngine = Minecraft.getMinecraft().renderEngine;

		// If we have no animation variables, use defaults
		if (animations == null)
			animations = GunAnimations.defaults;

		// Get all the attachments that we may need to render
		AttachmentType scopeAttachment = type.getScope(item);
		AttachmentType gripAttachment = type.getGrip(item);
		animations.recoilAmount = model.recoilAmount;

		ItemStack[] bulletStacks = new ItemStack[type.getNumAmmoItemsInGun(item)];
		boolean empty = true;
		int numRounds = 0;

		for (int i = 0; i < type.getNumAmmoItemsInGun(item); i++) {
			bulletStacks[i] = ((ItemGun) item.getItem()).getBulletItemStack(item, i);
			if (bulletStacks[i] != null && bulletStacks[i].getItem() instanceof ItemShootable) {
				if (bulletStacks[i].getItemDamage() < bulletStacks[i].getMaxDamage()) {
					empty = false;
					numRounds += bulletStacks[i].getMaxDamage() - bulletStacks[i].getItemDamage();
				}
			}
		}

		// Sanity check for empty guns
		if (model.slideLockOnEmpty) {
			if (empty)
				animations.onGunEmpty(true);
			else if (!empty && !animations.reloading)
				animations.onGunEmpty(false);
		}

		GL11.glPushMatrix();
		{
			//First person only effects
			if (rtype == ItemRenderType.EQUIPPED_FIRST_PERSON) {
				//Render arms
				if (FlansMod.armsEnable && model.hasArms) {
					Minecraft mc = Minecraft.getMinecraft();
					renderFirstPersonArm(mc.thePlayer, model, animations);
				}
				//Apply model recoil when shooting
				handleGunRecoil(
						animations,
						model,
						getRecoilDistance(gripAttachment, type, item),
						getRecoilAngle(gripAttachment, type, item)
				);
			}

			//Gun Texture
			renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));

			//This allows you to offset your gun with a sight attached to properly align the aiming reticle
			if (scopeAttachment != null && model.gunOffset != 0 && FlansModClient.zoomProgress >= 0.5F)
				GL11.glTranslatef(0F, -scopeAttachment.model.renderOffset + model.gunOffset / 16F, 0F);

			// Render the gun and default attachment models
			renderGunAndComponents(model, type, item, f, animations, numRounds, empty, rtype);
			// Render custom attachments
			renderCustomAttachments(model, type, item, f, animations);
		}
		GL11.glPopMatrix();
	}

	private void renderCustomAttachments(ModelGun model, GunType type, ItemStack item, float f, GunAnimations animations) {
		ItemStack scopeItemStack = type.getScopeItemStack(item);
		ItemStack barrelItemStack = type.getBarrelItemStack(item);
		ItemStack stockItemStack = type.getStockItemStack(item);
		ItemStack gripItemStack = type.getGripItemStack(item);
		ItemStack gadgetItemStack = type.getGadgetItemStack(item);
		ItemStack slideItemStack = type.getSlideItemStack(item);
		ItemStack pumpItemStack = type.getPumpItemStack(item);
		ItemStack accessoryItemStack = type.getAccessoryItemStack(item);

		List<AttachmentType> attachments = type.getCurrentAttachments(item);
		// Get all the attachments that we may need to render
		for (AttachmentType attachment : attachments) {
			GL11.glPushMatrix();

			switch(attachment.type.name()) {
				case "sights":
					preRenderAttachment(attachment, scopeItemStack, model.scopeAttachPoint, type);
					if (model.scopeIsOnBreakAction) {
						GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
						GL11.glRotatef(getReloadRotate(animations, model) * -model.breakAngle, 0F, 0F, 1F);
						GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
					}
					if (model.scopeIsOnSlide) {
						GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
					}
					postRenderAttachment(attachment, scopeItemStack, f);
					break;
				case "grip":
					preRenderAttachment(attachment, gripItemStack, model.gripAttachPoint, type);
					if (model.gripIsOnPump)
						GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
					postRenderAttachment(attachment, gripItemStack, f);
					break;
				case "barrel":
					preRenderAttachment(attachment, barrelItemStack, model.barrelAttachPoint, type);
					postRenderAttachment(attachment, barrelItemStack, f);
					break;
				case "stock":
					preRenderAttachment(attachment, stockItemStack, model.stockAttachPoint, type);
					postRenderAttachment(attachment, stockItemStack, f);
					break;
				case "slide":
					preRenderAttachment(attachment, slideItemStack, model.slideAttachPoint, type);
					GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
					postRenderAttachment(attachment, slideItemStack, f);
					break;
				case "gadget":
					preRenderAttachment(attachment, gadgetItemStack, model.gadgetAttachPoint, type);
					if (model.gadgetIsOnPump)
						GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
					postRenderAttachment(attachment, gadgetItemStack, f);
					break;
				case "accessory":
					preRenderAttachment(attachment, accessoryItemStack, model.accessoryAttachPoint, type);
					postRenderAttachment(attachment, accessoryItemStack, f);
					break;
				case "pump":
					preRenderAttachment(attachment, pumpItemStack, model.pumpAttachPoint, type);
					GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
					postRenderAttachment(attachment, pumpItemStack, f);
					break;
				default:
					break;

			}
			GL11.glPopMatrix();
		}
	}

	private void renderGunAndComponents(ModelGun model, GunType type, ItemStack item, float f, GunAnimations animations, int numRounds, boolean empty, ItemRenderType rtype) {
		// Get all the attachments that we may need to render
		AttachmentType scopeAttachment = type.getScope(item);
		AttachmentType barrelAttachment = type.getBarrel(item);
		AttachmentType stockAttachment = type.getStock(item);
		AttachmentType gripAttachment = type.getGrip(item);
		AttachmentType gadgetAttachment = type.getGadget(item);
		AttachmentType slideAttachment = type.getSlide(item);
		AttachmentType pumpAttachment = type.getPump(item);
		AttachmentType accessoryAttachment = type.getAccessory(item);

		ItemStack gripItemStack = type.getGripItemStack(item);

		GL11.glPushMatrix();
		{
			GL11.glScalef(type.modelScale, type.modelScale, type.modelScale);

			//Handle muzzle flash stuffs
			renderMuzzleFlash(animations, model, type, barrelAttachment, item, f);

			model.renderGun(f);

			// Render the guns default parts if no attachment is installed
			if (scopeAttachment == null && !model.scopeIsOnSlide && !model.scopeIsOnBreakAction)
				model.renderDefaultScope(f);
			if (barrelAttachment == null)
				model.renderDefaultBarrel(f);
			if (stockAttachment == null)
				model.renderDefaultStock(f);
			if (gripAttachment == null && !model.gripIsOnPump)
				model.renderDefaultGrip(f);
			if (gadgetAttachment == null && !model.gadgetIsOnPump)
				model.renderDefaultGadget(f);

			//Render the bullet counter
			GL11.glPushMatrix();
			if(model.isBulletCounterActive)
				model.renderBulletCounter(f, numRounds);
			GL11.glPopMatrix();

			GL11.glPushMatrix();
			if(model.isAdvBulletCounterActive)
				model.renderAdvBulletCounter(f, numRounds, model.countOnRightHandSide);
			GL11.glPopMatrix();



			// Render the slide and charge action
			if (slideAttachment == null)
				renderSlideModels(scopeAttachment, type, animations, model, f, item);

			//Render break action(s)
			renderBreakAction(model, scopeAttachment, getReloadRotate(animations, model), f);
			//Render hammer action(s)
			renderHammer(model, animations, f);
			//Render pump action(s)
			renderPumpAction(model, animations, pumpAttachment, f, gripAttachment, type, gadgetAttachment);
			//Render bolt action(s)
			renderBoltAction(model, animations, f, type);
			//Render charge handle(s)
			renderChargeHandle(model, animations, f);
			//Render minigun barrels
			renderMinigunBarrels(type, model, animations, f);
			// Render the revolver barrel
			renderRevolverBarrel(model, getReloadRotate(animations, model), f);
			//Render gun ammo
			renderAmmo(model, type, gripAttachment, item, empty, animations, getReloadRotate(animations, model), rtype, f, gripItemStack);
			// Render a static model of the ammo NOT being reloaded, for underbarrels??
			renderStaticAmmo(type, item, model, f, gripAttachment, gripItemStack);

			//Render casing ejection
			if (rtype == ItemRenderType.EQUIPPED_FIRST_PERSON && FlansMod.casingEnable && type.casingModel != null && !type.getSecondaryFire(item))
				renderCasingEjection(type, animations, model, f, item);


		}
		GL11.glPopMatrix();

	}

	private void renderRevolverBarrel(ModelGun model, float reloadRotate, float f) {
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
			GL11.glRotatef(reloadRotate * model.revolverFlipAngle, 1F, 0F, 0F);
			GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
			model.renderRevolverBarrel(f);
		}
		GL11.glPopMatrix();

		// Render the revolver2 barrel
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
			GL11.glRotatef(reloadRotate * model.revolverFlipAngle, -1F, 0F, 0F);
			GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
			model.renderRevolver2Barrel(f);
		}
		GL11.glPopMatrix();
	}

	private void renderMinigunBarrels(GunType type, ModelGun model, GunAnimations animations, float f) {
		// Render the minigun barrels
		if (type.mode == EnumFireMode.MINIGUN)
		{
			GL11.glPushMatrix();
			GL11.glTranslatef(model.minigunBarrelOrigin.x, model.minigunBarrelOrigin.y, model.minigunBarrelOrigin.z);
			GL11.glRotatef(animations.minigunBarrelRotation, type.spinX, type.spinY, type.spinZ);
			GL11.glTranslatef(-model.minigunBarrelOrigin.x, -model.minigunBarrelOrigin.y, -model.minigunBarrelOrigin.z);
			model.renderMinigunBarrel(f);
			GL11.glPopMatrix();
		}
	}

	private void renderChargeHandle(ModelGun model, GunAnimations animations, float f) {
		// Render the charge handle
		if (model.chargeHandleDistance != 0F)
		{
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * smoothing)) * model.chargeHandleDistance, 0F, 0F);
				model.renderCharge(f);
			}
			GL11.glPopMatrix();
		}
	}

	private void renderPumpAction(ModelGun model, GunAnimations animations, AttachmentType pumpAttachment, float f, AttachmentType gripAttachment, GunType type, AttachmentType gadgetAttachment) {
		// Render the pump-action handle
		if (pumpAttachment == null)
		{
		        GL11.glPushMatrix();
			{
				GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				model.renderPump(f);
				if (gripAttachment == null && model.gripIsOnPump)
					model.renderDefaultGrip(f);
				if (gadgetAttachment == null && model.gadgetIsOnPump)
					model.renderDefaultGadget(f);
				if(FlansModClient.shotState != -1 && -(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance != -0.0)
				{
					FlansModClient.shotState = -1;
					if(type.actionSound != null)
					{
						Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.actionSound), 1.0F));
					}
				}
			}
			GL11.glPopMatrix();
		}

		// Render the alternate pump-action handle
		if (pumpAttachment == null)
		{
			GL11.glPushMatrix();
			{

				GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing);
				model.renderaltPump(f);
				if (gripAttachment == null && model.gripIsOnPump)
					model.renderDefaultGrip(f);
				if (gadgetAttachment == null && model.gadgetIsOnPump)
					model.renderDefaultGadget(f);
			}
			GL11.glPopMatrix();
		}
	}

	private void renderBoltAction(ModelGun model, GunAnimations animations, float f, GunType type) {
		// Render the bolt action
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.boltCycleDistance, 0F, 0F);
			//injectRotationTool(f, model);
			GL11.glTranslatef(model.boltRotationOffset.x, model.boltRotationOffset.y, model.boltRotationOffset.z);
			GL11.glRotatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.boltRotationAngle, 1F, 0F, 0F);
			GL11.glTranslatef(-model.boltRotationOffset.x, -model.boltRotationOffset.y, -model.boltRotationOffset.z);
			model.renderBoltAction(f);
		}
		GL11.glPopMatrix();

		if(FlansModClient.shotState != -1 && -(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.boltCycleDistance != -0.0) {
			FlansModClient.shotState = -1;
			if(type.actionSound != null) {
				Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.actionSound), 1.0F));
			}
		}
	}

	private void renderHammer(ModelGun model, GunAnimations animations, float f) {
		// Render the hammer
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.hammerSpinPoint.x, model.hammerSpinPoint.y, model.hammerSpinPoint.z);
			GL11.glRotatef(-animations.hammerRotation, 0F, 0F, 1F);
			GL11.glTranslatef(-model.hammerSpinPoint.x, -model.hammerSpinPoint.y, -model.hammerSpinPoint.z);
			model.renderHammer(f);
		}
		GL11.glPopMatrix();

		// Render the alternate hammer
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.althammerSpinPoint.x, model.althammerSpinPoint.y, model.althammerSpinPoint.z);
			GL11.glRotatef(-animations.althammerRotation, 0F, 0F, 1F);
			GL11.glTranslatef(-model.althammerSpinPoint.x, -model.althammerSpinPoint.y, -model.althammerSpinPoint.z);
			model.renderaltHammer(f);
		}
		GL11.glPopMatrix();
	}

	private void renderBreakAction(ModelGun model, AttachmentType scopeAttachment, float reloadRotate, float f) {
		// Render the break action
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
			GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
			GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
			model.renderBreakAction(f);
			if (scopeAttachment == null && model.scopeIsOnBreakAction)
				model.renderDefaultScope(f);
		}
		GL11.glPopMatrix();

		// Render the alternate break action
		GL11.glPushMatrix();
		{
			GL11.glTranslatef(model.altbarrelBreakPoint.x, model.altbarrelBreakPoint.y, model.altbarrelBreakPoint.z);
			GL11.glRotatef(reloadRotate * -model.altbreakAngle, 0F, 0F, 1F);
			GL11.glTranslatef(-model.altbarrelBreakPoint.x, -model.altbarrelBreakPoint.y, -model.altbarrelBreakPoint.z);
			model.renderaltBreakAction(f);
		}
		GL11.glPopMatrix();
	}

	private void renderCasingEjection(GunType type, GunAnimations animations, ModelGun model, float f, ItemStack item) {
		ModelCasing casing = type.casingModel;
		GL11.glPushMatrix();
		{
			float casingProg = (animations.lastCasingStage + (animations.casingStage - animations.lastCasingStage) * smoothing) / model.casingAnimTime;
			if (casingProg >= 1)
				casingProg = 0;
			float moveX = model.casingAnimDistance.x + (animations.casingRandom.x * model.casingAnimSpread.x);
			float moveY = model.casingAnimDistance.y + (animations.casingRandom.y * model.casingAnimSpread.y);
			float moveZ = model.casingAnimDistance.z + (animations.casingRandom.z * model.casingAnimSpread.z);
			GL11.glScalef(model.caseScale, model.caseScale, model.caseScale);
			GL11.glTranslatef(model.casingAttachPoint.x + (casingProg * moveX), model.casingAttachPoint.y + (casingProg * moveY), model.casingAttachPoint.z + (casingProg * moveZ));
			GL11.glRotatef(casingProg * 180, model.casingRotateVector.x, model.casingRotateVector.y, model.casingRotateVector.z);
			renderEngine.bindTexture(FlansModResourceHandler.getAuxiliaryTexture(type.casingTexture));
			casing.renderCasing(f);
			renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));
		}
		GL11.glPopMatrix();
	}

	private void renderStaticAmmo(GunType type, ItemStack item, ModelGun model, float f, AttachmentType gripAttachment, ItemStack gripItemStack) {
		GL11.glPushMatrix();
		{
			if (type.getSecondaryFire(item))
				model.renderAmmo(f);
			else if (gripAttachment != null && !type.getSecondaryFire(item))
				renderAttachmentAmmo(f, gripAttachment, model, gripAttachment.getPaintjob(gripItemStack.getItemDamage()), type.getPaintjob(item.getItemDamage()));
		}
		GL11.glPopMatrix();
	}

	private void renderAmmo(ModelGun model, GunType type, AttachmentType gripAttachment, ItemStack item,
							   boolean empty, GunAnimations animations, float reloadRotate, ItemRenderType rtype,
							   float f, ItemStack gripItemStack) {
		GL11.glPushMatrix();
		{
			boolean shouldRender = true;

			EnumAnimationType anim = model.animationType;
			if (gripAttachment != null && type.getSecondaryFire(item))
				anim = gripAttachment.model.secondaryAnimType;

			float tiltGunTime = model.tiltGunTime, unloadClipTime = model.unloadClipTime, loadClipTime = model.loadClipTime;
			if (gripAttachment != null && type.getSecondaryFire(item))
			{
				tiltGunTime = gripAttachment.model.tiltGunTime;
				unloadClipTime = gripAttachment.model.unloadClipTime;
				loadClipTime = gripAttachment.model.loadClipTime;
			}

			// Check to see if the ammo should be rendered first
			if ((anim == EnumAnimationType.END_LOADED || anim == EnumAnimationType.BACK_LOADED) && empty && !animations.reloading) {
				shouldRender = false;
			}
			
			// If it should be rendered, do the transformations required
			if (shouldRender && animations.reloading && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)
			{
				// Calculate the amount of tilt required for the reloading animation
				float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
				float clipPosition = 0F;
				if (effectiveReloadAnimationProgress > tiltGunTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime)
					clipPosition = (effectiveReloadAnimationProgress - tiltGunTime) / unloadClipTime;
				if (effectiveReloadAnimationProgress >= tiltGunTime + unloadClipTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime + loadClipTime)
					clipPosition = 1F - (effectiveReloadAnimationProgress - (tiltGunTime + unloadClipTime)) / loadClipTime;
				float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - tiltGunTime) / (unloadClipTime + loadClipTime))));

				// Rotate/translate the AMMO dependent on the animation type
				switch (anim)
				{
					case BREAK_ACTION: case CUSTOMBREAK_ACTION:
					{
						GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
						GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
						GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
						GL11.glTranslatef(-model.breakActionAmmoDistance * clipPosition * 1 / type.modelScale, 0F, 0F);
						break;
					}

					case REVOLVER: case CUSTOMREVOLVER:
					{
						GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
						GL11.glRotatef(reloadRotate * model.revolverFlipAngle, 1F, 0F, 0F);
						GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
						GL11.glTranslatef(-1F * clipPosition * 1 / type.modelScale, 0F, 0F);
						break;
					}

					case REVOLVER2: case CUSTOMREVOLVER2:
					{
						GL11.glTranslatef(model.revolver2FlipPoint.x, model.revolver2FlipPoint.y, model.revolver2FlipPoint.z);
						GL11.glRotatef(reloadRotate * model.revolver2FlipAngle, -1F, 0F, 0F);
						GL11.glTranslatef(-model.revolver2FlipPoint.x, -model.revolver2FlipPoint.y, -model.revolver2FlipPoint.z);
						GL11.glTranslatef(-1F * clipPosition * 1 / type.modelScale, 0F, 0F);
						break;
					}

					case BOTTOM_CLIP: case CUSTOMBOTTOM_CLIP:
					{
						GL11.glRotatef(-180F * clipPosition, 0F, 0F, 1F);
						GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
						GL11.glTranslatef(0.5F * clipPosition * 1 / type.modelScale, 0F, 0F);
						break;
					}

					case PISTOL_CLIP: case CUSTOMPISTOL_CLIP:
					{
						GL11.glRotatef(-90F * clipPosition * clipPosition, 0F, 0F, 1F);
						GL11.glTranslatef(0F, -1F * clipPosition * 1 / type.modelScale, 0F);
						break;
					}

					case ALT_PISTOL_CLIP: case CUSTOMALT_PISTOL_CLIP:
					{
						GL11.glRotatef(5F * clipPosition, 0F, 0F, 1F);
						GL11.glTranslatef(0F, -3F * clipPosition * 1 / type.modelScale, 0F);
						break;
					}

					case SIDE_CLIP: case CUSTOMSIDE_CLIP:
					{
						GL11.glRotatef(180F * clipPosition, 0F, 1F, 0F);
						GL11.glRotatef(60F * clipPosition, 0F, 1F, 0F);
						GL11.glTranslatef(0.5F * clipPosition * 1 / type.modelScale, 0F, 0F);
						break;
					}

					case BULLPUP: case CUSTOMBULLPUP:
					{
						GL11.glRotatef(-150F * clipPosition, 0F, 0F, 1F);
						GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
						GL11.glTranslatef(1F * clipPosition * 1 / type.modelScale,
								-0.5F * clipPosition * 1 / type.modelScale, 0F);
						break;
					}

					case P90: case CUSTOMP90:
					{
						GL11.glRotatef(-15F * reloadRotate * reloadRotate, 0F, 0F, 1F);
						GL11.glTranslatef(0F, 0.075F * reloadRotate, 0F);
						GL11.glTranslatef(-2F * clipPosition * 1 / type.modelScale, -0.3F * clipPosition * 1 / type.modelScale, 0.5F * clipPosition * 1 / type.modelScale);
						break;
					}

					case RIFLE:
					{
						float ammoPosition = clipPosition * getNumBulletsInReload(animations, gripAttachment, type, item);
						int bulletNum = MathHelper.floor_float(ammoPosition);
						float bulletProgress = ammoPosition - bulletNum;

						GL11.glRotatef(bulletProgress * 15F, 0F, 1F, 0F);
						GL11.glRotatef(bulletProgress * 15F, 0F, 0F, 1F);
						GL11.glTranslatef(bulletProgress * -1F * 1 / type.modelScale, 0F, bulletProgress * 0.5F * 1 / type.modelScale);
						break;
					}

					case CUSTOMRIFLE:
					{
						float maxBullets = getNumBulletsInReload(animations, gripAttachment, type, item);
						float ammoPosition = clipPosition * maxBullets;
						int bulletNum = MathHelper.floor_float(ammoPosition);
						float bulletProgress = ammoPosition - bulletNum;

						if(type.getNumAmmoItemsInGun(item) > 1 && type.bulletInsert != null && FlansModClient.lastBulletReload != -2)
						{
							if(maxBullets == 2 && FlansModClient.lastBulletReload != -1)
							{
								int time = (int) (animations.reloadAnimationTime / maxBullets);
								Minecraft.getMinecraft().getSoundHandler().playDelayedSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F), time);
								FlansModClient.lastBulletReload = -1;
							} else if((bulletNum == (int) maxBullets || bulletNum == FlansModClient.lastBulletReload-1))
							{
								FlansModClient.lastBulletReload = bulletNum;
								Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F));
							}

							if((ammoPosition < 0.03 && bulletProgress > 0))
							{
								FlansModClient.lastBulletReload = -2;
								Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F));
							}
						}

						GL11.glRotatef(bulletProgress * model.rotateClipVertical, 0F, 1F, 0F);
						GL11.glRotatef(bulletProgress * model.rotateClipHorizontal, 0F, 0F, 1F);
						GL11.glRotatef(bulletProgress * model.tiltClip, 1F, 0F, 0F);
						GL11.glTranslatef(bulletProgress * model.translateClip.x / type.modelScale, bulletProgress * model.translateClip.y / type.modelScale, bulletProgress * model.translateClip.z / type.modelScale);
						break;
					}

					case RIFLE_TOP: case CUSTOMRIFLE_TOP:
					{
						float ammoPosition = clipPosition * getNumBulletsInReload(animations, gripAttachment, type, item);
						int bulletNum = MathHelper.floor_float(ammoPosition);
						float bulletProgress = ammoPosition - bulletNum;

						GL11.glRotatef(bulletProgress * 55F, 0F, 1F, 0F);
						GL11.glRotatef(bulletProgress * 95F, 0F, 0F, 1F);
						GL11.glTranslatef(bulletProgress * -0.1F * 1 / type.modelScale, bulletProgress * 1F * 1 / type.modelScale, bulletProgress * 0.5F * 1 / type.modelScale);

						break;
					}
					case SHOTGUN: case STRIKER: case CUSTOMSHOTGUN: case CUSTOMSTRIKER:
					{
						float maxBullets = getNumBulletsInReload(animations, gripAttachment, type, item);
						float ammoPosition = clipPosition * maxBullets;
						int bulletNum = MathHelper.floor_float(ammoPosition);
						float bulletProgress = ammoPosition - bulletNum;

						if (maxBullets > 1) {
							if (type.getNumAmmoItemsInGun(item) > 1 && type.bulletInsert != null && FlansModClient.lastBulletReload != -2) {
								if (maxBullets == 2 && FlansModClient.lastBulletReload != -1) {
									int time = (int) (animations.reloadAnimationTime / maxBullets);
									Minecraft.getMinecraft().getSoundHandler() .playDelayedSound(PositionedSoundRecord.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F), time);
									FlansModClient.lastBulletReload = -1;
								} else if ((bulletNum == (int) maxBullets || bulletNum == FlansModClient.lastBulletReload - 1)) {
									FlansModClient.lastBulletReload = bulletNum;
									Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord
											.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F));
								}

								if ((ammoPosition < 0.03 && bulletProgress > 0)) {
									FlansModClient.lastBulletReload = -2;
									Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord
											.func_147674_a(FlansModResourceHandler.getSound(type.bulletInsert), 1.0F));
								}
							}
						}


						GL11.glRotatef(bulletProgress * -30F, 0F, 0F, 1F);
						GL11.glTranslatef(bulletProgress * -0.5F * 1 / type.modelScale, bulletProgress * -1F * 1 / type.modelScale, 0F);

						break;
					}
					case CUSTOM:
					{
						// Staged reload allows you to change the animation route half way through
						if (effectiveReloadAnimationProgress < 0.5 && model.stagedReload)
						{
							GL11.glRotatef(model.rotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.rotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.tiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.translateClip.x * clipPosition * 1 / type.modelScale, model.translateClip.y * clipPosition * 1 / type.modelScale, model.translateClip.z * clipPosition * 1 / type.modelScale);
							break;
						}
						else if (effectiveReloadAnimationProgress > 0.5 && model.stagedReload)
						{
							GL11.glRotatef(model.stagedrotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.stagedrotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.stagedtiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.stagedtranslateClip.x * clipPosition * 1 / type.modelScale, model.stagedtranslateClip.y * clipPosition * 1 / type.modelScale, model.stagedtranslateClip.z * clipPosition * 1 / type.modelScale);
							break;
						}
						else
						{
							GL11.glRotatef(model.rotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.rotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.tiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.translateClip.x * clipPosition * 1 / type.modelScale, model.translateClip.y * clipPosition * 1 / type.modelScale, model.translateClip.z * clipPosition * 1 / type.modelScale);
							break;
						}
					}
					case END_LOADED: case CUSTOMEND_LOADED:
					{
						float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

						GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
						GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

						float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
						GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
						break;
					}
					case BACK_LOADED: case CUSTOMBACK_LOADED:
					{
						float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
						// GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
						GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

						float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
						GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
					}

					default:
						break;
				}
			}

			if (rtype == ItemRenderType.EQUIPPED_FIRST_PERSON && model.hasArms && FlansMod.armsEnable)
			{
				Minecraft mc = Minecraft.getMinecraft();
				renderAnimArm(mc.thePlayer, model, type, animations);
			}
			renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));

			if (shouldRender)
			{
				if (gripAttachment != null && type.getSecondaryFire(item))
					renderAttachmentAmmo(f, gripAttachment, model, gripAttachment.getPaintjob(gripItemStack.getItemDamage()), type.getPaintjob(item.getItemDamage()));
				else
					model.renderAmmo(f);
			}
			// Renders fullammo model for 2nd half of reload animation
			float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
			if (effectiveReloadAnimationProgress > 0.5)
				model.renderfullAmmo(f);
		}
		GL11.glPopMatrix();
	}

	private void renderMuzzleFlash(GunAnimations animations, ModelGun model, GunType type, AttachmentType barrelAttachment, ItemStack item, float f) {
		boolean isFlashEnabled = barrelAttachment == null || !barrelAttachment.disableMuzzleFlash;

		if (isFlashEnabled && animations.muzzleFlashTime > 0 && type.flashModel != null && !type.getSecondaryFire(item))
		{
			GL11.glPushMatrix();
			ModelFlash flash = type.flashModel;
			GL11.glScalef(model.flashScale, model.flashScale, model.flashScale);
			{
				Vector3f base = model.muzzleFlashPoint == null ? Vector3f.Zero : model.muzzleFlashPoint;
				if (barrelAttachment != null) {
					Vector3f barrelOffset = (barrelAttachment.model != null && barrelAttachment.model.attachmentFlashOffset != null) ? barrelAttachment.model.attachmentFlashOffset : Vector3f.Zero;
					GL11.glTranslatef(base.x + barrelOffset.x,
							base.y + barrelOffset.y,
							base.z + barrelOffset.z);
				} else {
					Vector3f defaultOffset = model.defaultBarrelFlashPoint == null ? Vector3f.Zero : model.defaultBarrelFlashPoint;

					GL11.glTranslatef(base.x + defaultOffset.x,
							base.y + defaultOffset.y,
							base.z + defaultOffset.z);
				}

				renderEngine.bindTexture(FlansModResourceHandler.getAuxiliaryTexture(type.flashTexture));
				ModelGun.glowOn();
				flash.renderFlash(f, animations.flashInt);
				ModelGun.glowOff();
				renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));
			}
			GL11.glPopMatrix();
		}
	}

	private void renderSlideModels(AttachmentType scopeAttachment, GunType type, GunAnimations animations, ModelGun model, float f, ItemStack item) {

		//Render regular slide model
		GL11.glPushMatrix();
		if (!type.getSecondaryFire(item)) {
			GL11.glTranslatef(
					-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing)
							* model.gunSlideDistance,
					0F, 0F);
			GL11.glTranslatef(-(1 - Math.abs(
					animations.lastCharged + (animations.charged - animations.lastCharged) * smoothing))
					* model.chargeHandleDistance, 0F, 0F);

		}
		model.renderSlide(f);
		if (scopeAttachment == null && model.scopeIsOnSlide)
			model.renderDefaultScope(f);
		GL11.glPopMatrix();

		// Render the alternate slide
		GL11.glPushMatrix();
		if (!type.getSecondaryFire(item)) {
			GL11.glTranslatef(
					-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing)
							* model.altgunSlideDistance,
					0F, 0F);
			model.renderaltSlide(f);
		}
		GL11.glPopMatrix();
	}

	private void handleGunRecoil(GunAnimations animations, ModelGun model, float recoilDistance, float recoilAngle) {
		float min = -1.5f;
		float max = 1.5f;
		float randomNum = new Random().nextFloat();
		float result = min + (randomNum * (max - min));

		GL11.glTranslatef(0F, 0, 0);
		GL11.glTranslatef(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * recoilDistance, 0F, 0F);
		GL11.glRotatef(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * recoilAngle, 0F, 0F, 1F);
		GL11.glRotatef(((-animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * result * smoothing * model.ShakeDistance), 0.0f, 1.0f, 0.0f);
		GL11.glRotatef(((-animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * result * smoothing * model.ShakeDistance), 1.0f, 0.0f, 0.0f);

		// Do not move gun when there's a pump in the reload
		if (model.animationType == EnumAnimationType.SHOTGUN && !animations.reloading) {
			GL11.glRotatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * -5F, 0F, 1F, 0F);
			GL11.glRotatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * 5F, 1F, 0F, 0F);
		}

		if (model.isSingleAction) {
			GL11.glRotatef(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * -5F, 0F, 0F, 1F);
			GL11.glRotatef(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * 2.5F, 1F, 0F, 0F);
		}
	}


	/** Clean up some redundant code */
	private void preRenderAttachment(AttachmentType attachment, ItemStack stack, Vector3f model, GunType type) {
		Paintjob paintjob = attachment.getPaintjob(stack.getItemDamage());
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(paintjob));
		GL11.glTranslatef(model.x * type.modelScale, model.y * type.modelScale, model.z * type.modelScale);
		GL11.glScalef(attachment.modelScale, attachment.modelScale, attachment.modelScale);
	}

	private void postRenderAttachment(AttachmentType attachment, ItemStack stack, float f) {
		Paintjob paintjob = attachment.getPaintjob(stack.getItemDamage());
		ModelAttachment model = attachment.model;
		if (model != null)
			model.renderAttachment(f);
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(paintjob));
	}

	/** Load the attachment ammo model plus its texture */
	private void renderAttachmentAmmo(float f, AttachmentType grip, ModelGun model, Paintjob ammo, Paintjob otherAmmo) {
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(ammo));
		GL11.glTranslatef(model.gripAttachPoint.x, model.gripAttachPoint.y, model.gripAttachPoint.z);
		grip.model.renderAttachmentAmmo(f);
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(otherAmmo));
	}

	/** Load the corresponding casing model and texture */
//	private void renderCasingModel(float f, AttachmentType grip, GunType gun, ItemStack gunStack)
//	{

//	}

	/** Load the corresponding flash model and texture */
//	private void renderFlashModel()
//	{
//
//	}

	// Part of arms cleanup to rewrite into one method
	private void renderArms(EntityPlayer player, ModelGun model, GunType type, GunAnimations anim)
	{
		
	}

	private void renderFirstPersonArm(EntityPlayer player, ModelGun model, GunAnimations anim) {
		Minecraft mc = Minecraft.getMinecraft();
		ModelBiped modelBipedMain = new ModelBiped(0.0F);
		mc.renderEngine.bindTexture(mc.thePlayer.getLocationSkin());

		float f = 1.0F;
		GL11.glColor3f(f, f, f);
		modelBipedMain.onGround = 0.0F;
		
		GL11.glPushMatrix();
		{
			if (!anim.reloading && model.righthandPump) {
				RenderArms.renderArmPump(model, anim, smoothing, model.rightArmRot, model.rightArmPos);
			} 
			else if (anim.charged < 0.9 && model.leftHandAmmo && model.rightHandCharge && anim.charged != -1.0F) {
				RenderArms.renderArmCharge(model, anim, smoothing, model.rightArmChargeRot, model.rightArmChargePos);
			}
			else if (anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo) {
				RenderArms.renderArmBolt(model, anim, smoothing, model.rightArmChargeRot, model.rightArmChargePos);
			}
			else if (!anim.reloading && !model.righthandPump) {
				RenderArms.renderArmDefault(model, anim, smoothing, model.rightArmRot, model.rightArmPos);
			}
			else {
				RenderArms.renderArmReload(model, anim, smoothing, model.rightArmReloadRot, model.rightArmReloadPos);
			}
			
			GL11.glScalef(model.rightArmScale.x, model.rightArmScale.y, model.rightArmScale.z);
			modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
			modelBipedMain.bipedRightArm.offsetY = 0F;
			if (!model.rightHandAmmo) {
				modelBipedMain.bipedRightArm.render(0.0625F);
			}
		}
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		if (!anim.reloading && model.lefthandPump) {
			RenderArms.renderArmPump(model, anim, smoothing, model.leftArmRot, model.leftArmPos);
		}
		else if (anim.charged < 0.9 && model.rightHandCharge && model.leftHandAmmo && anim.charged != -1.0F) {
			RenderArms.renderArmCharge(model, anim, smoothing, model.leftArmChargeRot, model.leftArmChargePos);
		}
		else if (anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo) {
			RenderArms.renderArmBolt(model, anim, smoothing, model.leftArmChargeRot, model.leftArmChargePos);
		} else if (!anim.reloading && !model.lefthandPump) {
			RenderArms.renderArmDefault(model, anim, smoothing, model.leftArmRot, model.leftArmPos);
		} else {
			RenderArms.renderArmReload(model, anim, smoothing, model.leftArmReloadRot, model.leftArmReloadPos);
		}

		GL11.glScalef(model.leftArmScale.x, model.leftArmScale.y, model.leftArmScale.z);
		modelBipedMain.bipedLeftArm.offsetY = 0F;
		if (!model.leftHandAmmo) {
			modelBipedMain.bipedLeftArm.render(0.0625F);
		}
		GL11.glPopMatrix();
	}

	private void renderAnimArm(EntityPlayer player, ModelGun model, GunType type, GunAnimations anim) {
		Minecraft mc = Minecraft.getMinecraft();
		ModelBiped modelBipedMain = new ModelBiped(0.0F);
		mc.renderEngine.bindTexture(mc.thePlayer.getLocationSkin());
		GL11.glPushMatrix();
		GL11.glScalef(1 / type.modelScale, 1 / type.modelScale, 1 / type.modelScale);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		GL11.glPushMatrix();
		float effectiveReloadAnimationProgress = anim.lastReloadAnimationProgress
				+ (anim.reloadAnimationProgress - anim.lastReloadAnimationProgress) * smoothing;

		if (anim.charged < 0.9 && model.rightHandCharge && model.rightHandAmmo && anim.charged != -1.0F) {
			RenderArms.renderArmPump(model, anim, smoothing, model.rightArmRot, model.rightArmPos);
		}
		else if (anim.pumped < 0.9 && model.rightHandBolt && model.rightHandAmmo) {
			RenderArms.renderArmBolt(model, anim, smoothing, model.rightArmChargeRot, model.rightArmChargePos);
		}
		else if (!anim.reloading) {
			RenderArms.renderArmDefault(model, anim, smoothing, model.rightArmRot, model.rightArmPos);
		}
		else {
			RenderArms.renderArmReload(model, anim, smoothing, model.rightArmReloadRot, model.rightArmReloadPos);
		}

		GL11.glScalef(model.rightArmScale.x, model.rightArmScale.y, model.rightArmScale.z);
		modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
		modelBipedMain.bipedRightArm.offsetY = 0F;
		if (model.rightHandAmmo) {
			modelBipedMain.bipedRightArm.render(0.0625F);
		}
		GL11.glPopMatrix();

		GL11.glPushMatrix();
		if (anim.charged < 0.9 && model.leftHandCharge && model.leftHandAmmo && anim.charged != -1.0F) {
			RenderArms.renderArmCharge(model, anim, smoothing, model.leftArmChargeRot, model.leftArmChargePos);
		}

		else if (!anim.reloading && model.lefthandPump) {
			RenderArms.renderArmPump(model, anim, smoothing, model.leftArmRot, model.leftArmPos);
		}

		else if (!anim.reloading) {
			RenderArms.renderArmDefault(model, anim, smoothing, model.leftArmRot, model.leftArmPos);
		}
		else if (effectiveReloadAnimationProgress < 0.5 && model.stagedleftArmReloadPos.x != 0) {
			RenderArms.renderArmReload(model, anim, smoothing, model.leftArmReloadRot, model.leftArmReloadPos);
		} else if (effectiveReloadAnimationProgress > 0.5 && model.stagedleftArmReloadPos.x != 0) {
			RenderArms.renderArmReload(model, anim, smoothing, model.stagedleftArmReloadRot, model.stagedleftArmReloadPos);
		} else {
			RenderArms.renderArmReload(model, anim, smoothing, model.leftArmReloadRot, model.leftArmReloadPos);
		}

		GL11.glScalef(model.leftArmScale.x, model.leftArmScale.y, model.leftArmScale.z);
		modelBipedMain.bipedLeftArm.offsetY = 0F;
		if (model.leftHandAmmo) {
			modelBipedMain.bipedLeftArm.render(0.0625F);
		}
		GL11.glPopMatrix();

		GL11.glPopMatrix();
	}

	/** Get the end loaded distance, based on ammo type to reload */
	private float getEndLoadedDistance(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if (grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.endLoadedAmmoDistance;
		else
			return gun.model.endLoadedAmmoDistance;
	}

	/**
	 * Get the number of bullets to reload in animation, based on ammo type to
	 * reload
	 */
	private float getNumBulletsInReload(GunAnimations animations, AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		// If this is a singles reload, we want to know the number of bullets already in the gun
		if (animations.singlesReload) {
			return animations.reloadAmmoCount;
		} else {
			return gun.model.numBulletsInReloadAnimation;

		}
		/*if (grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.numBulletsInReloadAnimation;
		else
			return gun.model.numBulletsInReloadAnimation;*/
	}

	/** Get the recoil distance, based on ammo type to reload */
	private float getRecoilDistance(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if (grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.recoilDistance;
		else
			return gun.model.RecoilSlideDistance;
	}

	/** Get the recoil angle, based on ammo type to reload */
	private float getRecoilAngle(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if (grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.recoilAngle;
		else
			return gun.model.RotateSlideDistance;
	}

	/** Renders a cross at the rotation point */
	private void injectRotationTool(float f, ModelGun model) {
		ModelRotateTool rotateToolModel = new ModelRotateTool();
		GL11.glPushMatrix();
		ModelRotateTool tool = rotateToolModel;
		GL11.glTranslatef(model.rotationToolOffset.x, model.rotationToolOffset.y, model.rotationToolOffset.z);
		renderEngine.bindTexture(new ResourceLocation(null, "err"));
		tool.renderRotateTool(f);
		GL11.glTranslatef(-model.rotationToolOffset.x, -model.rotationToolOffset.y, -model.rotationToolOffset.z);
		GL11.glPopMatrix();
	}
}
