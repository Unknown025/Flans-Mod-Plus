package com.flansmod.common.network;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.vector.Vector3f;
import com.flansmod.utils.PlayerItemPositionUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.flansmod.common.FlansMod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMuzzleFlash extends PacketBase
{
    public int playerID;
    /** Particle type */
    public String type;
    public float scale;
    public boolean showMuzzleFlashParticlesToShooter;
    public String gunType;

    public PacketMuzzleFlash() {}

    public PacketMuzzleFlash(EntityPlayer p, String particleType, float size, boolean showToShooter, String shortName)
    {
        playerID = p.getEntityId();
        type = particleType;
        scale = size;
        showMuzzleFlashParticlesToShooter = showToShooter;
        gunType = shortName;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data)
    {
        data.writeInt(playerID);
        writeUTF(data, type);
        data.writeFloat(scale);
        data.writeBoolean(showMuzzleFlashParticlesToShooter);
        writeUTF(data, gunType);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data)
    {
        playerID = data.readInt();
        type = readUTF(data);
        scale = data.readFloat();
        showMuzzleFlashParticlesToShooter = data.readBoolean();
        gunType = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity)
    {
        FlansMod.log("Received muzzle flash packet on server. Disregarding.");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer)
    {
        boolean isThisPlayer = playerID == clientPlayer.getEntityId();
        GunType g = GunType.getGun(gunType);
        if (g == null || g.model == null) {
            return;
        }

        if (!isThisPlayer || (showMuzzleFlashParticlesToShooter && (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0 || g.showMuzzleFlashParticlesFirstPerson))) {
            Entity p = clientPlayer.worldObj.getEntityByID(playerID);
            if (p instanceof EntityPlayer) {
                EntityPlayer entityPlayer = (EntityPlayer) p;

                AttachmentType barrelAttachment = g.getBarrel(entityPlayer.getHeldItem());

                RotatedAxes axes = new RotatedAxes(entityPlayer.rotationYawHead+90, entityPlayer.rotationPitch, 0);

                Vector3f shoulderOffset = isThisPlayer ? new Vector3f(0, -22F/16F, 0) : new Vector3f(0, 0, 0);
                Vector3f handOffset = /*isThisPlayer ? new Vector3f(0.5, 0, 0.5) :*/ new Vector3f(0, 0, 0);

                Vector3f.add(shoulderOffset, g.muzzleFlashParticlesShoulderOffset, shoulderOffset);

                if (g.model.muzzleFlashPoint != null) {
                    Vector3f.add(handOffset, (Vector3f) g.model.muzzleFlashPoint, handOffset);
                } else {
                    Vector3f.add(handOffset, new Vector3f(0.5F, 0.22F, 0), handOffset);
                }

                if (barrelAttachment != null && barrelAttachment.model != null && barrelAttachment.model.attachmentFlashOffset != null) {
                    Vector3f.add(handOffset, (Vector3f) barrelAttachment.model.attachmentFlashOffset, handOffset);
                } else if (g.model.defaultBarrelFlashPoint != null) {
                    Vector3f.add(handOffset, (Vector3f) g.model.defaultBarrelFlashPoint, handOffset);
                }

                if (isThisPlayer/* && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0*/) {
                    Vector3f.add(handOffset, new Vector3f(-0.7, -0.35, 0.1), handOffset);
                }

                Vector3f.add(handOffset, g.muzzleFlashParticlesHandOffset, handOffset);

                Vector3f pos = PlayerItemPositionUtils.GetPlayerHandPosition(entityPlayer, shoulderOffset, handOffset, !isThisPlayer);

                Vector3f v = (Vector3f) axes.getXAxis().translate((float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1, (float)Math.random() * 2 - 1).scale(0.05F);

                FlansMod.proxy.spawnParticle(type, pos.x, pos.y, pos.z, v.x, v.y, v.z, scale);
            }
        }
    }
}
