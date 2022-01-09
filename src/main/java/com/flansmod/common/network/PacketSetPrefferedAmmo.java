package com.flansmod.common.network;

import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShootableType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class PacketSetPreferredAmmo extends PacketBase {
    public String ammoName;

    @SuppressWarnings("unused")
    public PacketSetPreferredAmmo() {
    }

    public PacketSetPreferredAmmo(String ammoName) {
        this.ammoName = ammoName;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUTF(data,ammoName);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        ammoName = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        ItemStack currentItem = playerEntity.inventory.getCurrentItem();
        if (currentItem != null && currentItem.getItem() instanceof ItemGun) {
            ItemGun itemGun = (ItemGun) currentItem.getItem();
                ShootableType newPreferredAmmo = ShootableType.getShootableType(ammoName);
                playerEntity.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY+newPreferredAmmo.name+" is new preferred ammo for "+itemGun.type.name));
                itemGun.setPreferredAmmoStack(currentItem,ammoName);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        System.out.println("2");
    }
}

