package com.flansmod.common.network;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.ItemGun;
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
    public int indexOfAmmo;

    @SuppressWarnings("unused")
    public PacketSetPreferredAmmo() {
    }

    public PacketSetPreferredAmmo(int indexOfAmmo) {
        this.indexOfAmmo = indexOfAmmo;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(indexOfAmmo);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        indexOfAmmo = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        ItemStack currentItem = playerEntity.inventory.getCurrentItem();
        if (currentItem != null && currentItem.getItem() instanceof ItemGun) {
            ItemGun itemGun = (ItemGun) currentItem.getItem();
            if(playerEntity.inventory.mainInventory.length!=0){
                ItemStack selectedAmmoStack = playerEntity.inventory.mainInventory[indexOfAmmo];
                playerEntity.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY+selectedAmmoStack.getDisplayName()+" is new preferred ammo for "+itemGun.type.name));
                itemGun.setPreferedAmmoStack(currentItem,selectedAmmoStack);
                System.out.println(itemGun.getPreferedAmmoStack(currentItem));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}

