package com.flansmod.client.gui;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.network.PacketSetPreferredAmmo;
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
    ArrayList<ItemStack> ammoList = new ArrayList<>();
    ArrayList<ItemStack> ammoListForRender = new ArrayList<>();
    ArrayList<ItemStack> inv = new ArrayList<>();
    @Override
    public void initGui() {
        ammoList.clear();
        ammoListForRender.clear();
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        guiTop2 = guiTop - 30;
        buttonList.clear();

        inv.clear();
        for (ItemStack itemStack : mc.thePlayer.inventory.mainInventory) {
            inv.add(itemStack);
            if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemGun) {
                ItemGun itemGun = (ItemGun) mc.thePlayer.getHeldItem().getItem();
                if (itemStack != null) {
                    Item item = itemStack.getItem();
                    for (ShootableType ammoForCurrentGun : itemGun.type.ammo) {
                        if (ammoForCurrentGun.getItem() != null) {
                            if (ammoForCurrentGun.getItem() == item) {
                                ammoList.add(itemStack);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < ammoList.size(); i++) {
            int x = i>5?70:20;
            int y = i>5?i-6:i;
            System.out.println(x);
            buttonList.add(new GuiButton(i + 1, (guiLeft + x), guiTop2 + 10 + (y + 1) * 30, 20, 20, String.valueOf(i + 1)));
        }
        ammoListForRender.clear();
        ammoListForRender = (ArrayList<ItemStack>) ammoList.clone();
        ammoList.clear();
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
            ItemStack itemStack = ammoListForRender.get(btn.id-1);
            FlansMod.getPacketHandler().sendToServer(new PacketSetPreferredAmmo(inv.indexOf(itemStack)));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float parTick) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        super.drawScreen(mouseX, mouseY, parTick);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        for (int i = 0; i < ammoListForRender.size(); i++) {
            int x = i>5?95:45;
            int y = i>5?i-6:i;
            drawSlotInventory(Minecraft.getMinecraft().fontRenderer, ammoListForRender.get(i), (guiLeft + x), guiTop2 + 10 + (y + 1) * 30);
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
}

