package com.flansmod.client.gui;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.network.PacketSetPreferredAmmo;
import com.flansmod.common.types.InfoType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;

@SideOnly(Side.CLIENT)
public class GuiSelectAmmo extends GuiScreen {

    private static final ResourceLocation texture = new ResourceLocation("FlansMod:" + "gui/baseEdit.png");


    protected int xSize = 256;
    protected int ySize = 256;
    protected int guiLeft;
    protected int guiTop;
    protected int guiTop2;
    ArrayList<ShootableType> ammoForThisGun = new ArrayList<>();

    @Override
    public void initGui() {
        guiLeft = 2;
        guiTop = (height - ySize) / 2;
        guiTop2 = 10;
        buttonList.clear();
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemGun) {
            ItemGun itemGun = (ItemGun) mc.thePlayer.getHeldItem().getItem();
            for (ShootableType shootableType : itemGun.type.ammo) {
                ammoForThisGun.add(shootableType);
            }
            for (int i = 0; i < ammoForThisGun.size(); i++) {
                int x = i > 5 ? 70 : 20;
                int y = i > 5 ? i - 6 : i;
                buttonList.add(new GuiButton(i + 1, (guiLeft + x), guiTop2 + 10 + (y + 1) * 30, 20, 20, String.valueOf(i + 1)));
            }
        }


        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemGun) {
            ItemGun itemGun = (ItemGun) mc.thePlayer.getHeldItem().getItem();
            for (ShootableType shootableType : ammoForThisGun) {
                if (shootableType.shortName.equals(itemGun.getPreferredAmmoStack(mc.thePlayer.getHeldItem()))) {
                    GuiButton button = (GuiButton) buttonList.get(ammoForThisGun.indexOf(shootableType));
                    button.enabled = false;
                }
            }
        }
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void actionPerformed(GuiButton btn) {
        if (btn.enabled) {
            FlansMod.getPacketHandler().sendToServer(new PacketSetPreferredAmmo(ammoForThisGun.get(btn.id - 1).shortName));
            System.out.println(ammoForThisGun.get(btn.id - 1).shortName);
            System.out.println(btn.id);
            System.out.println(ammoForThisGun);
            for (Object obj : buttonList) {
                GuiButton button = (GuiButton) obj;
                button.enabled = true;
            }
            btn.enabled = false;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float parTick) {
        ArrayList<ammoForRender> ammoList = new ArrayList<>();
        int bullets_counter = 0;
        for (ShootableType shootableType : ammoForThisGun) {
            for (ItemStack itemStack : mc.thePlayer.inventory.mainInventory) {
                if (itemStack != null && itemStack.getItem() instanceof ItemShootable) {
                    ItemShootable itemShootable = (ItemShootable) itemStack.getItem();
                    bullets_counter += itemStack.getItemDamage();
                }
            }
            ammoList.add(new ammoForRender(shootableType, bullets_counter));
            bullets_counter = 0;
        }


//        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
//        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
//        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        super.drawScreen(mouseX, mouseY, parTick);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        for (int i = 0; i < ammoForThisGun.size(); i++) {
            int x = i > 5 ? 95 : 45;
            int y = i > 5 ? i - 6 : i;
            drawSlotInventory(Minecraft.getMinecraft().fontRenderer, new ItemStack(ammoForThisGun.get(i).item), (guiLeft + x), guiTop2 + 10 + (y + 1) * 30);
            //mc.fontRenderer.drawString(String.valueOf(0), (guiLeft + x)+10, (guiTop2 + 10 + (y + 1) * 30)+5,0xFFFFFF);
        }
        GL11.glDisable(3042);
        RenderHelper.disableStandardItemLighting();
    }

    private static final RenderItem itemRenderer = new RenderItem();

    private void drawSlotInventory(FontRenderer fontRenderer, ItemStack itemstack, int i, int j) {
        if (itemstack == null || itemstack.getItem() == null)
            return;
        itemRenderer.renderItemIntoGUI(fontRenderer, FlansModClient.minecraft.renderEngine, itemstack, i, j);
        itemRenderer.renderItemOverlayIntoGUI(fontRenderer, FlansModClient.minecraft.renderEngine, itemstack, i, j);
    }

    private static class ammoForRender {
        public ammoForRender(ShootableType st, int bc) {
            shootableType = st;
            numOfBullets = bc;
        }

        public ShootableType shootableType;
        public int numOfBullets;
    }

}
