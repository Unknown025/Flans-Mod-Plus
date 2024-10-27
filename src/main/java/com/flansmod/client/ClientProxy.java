package com.flansmod.client;

import com.flansmod.client.debug.*;
import com.flansmod.client.gui.*;
import com.flansmod.client.model.*;
import com.flansmod.client.particle.*;
import com.flansmod.common.CommonProxy;
import com.flansmod.common.ContentManager;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.*;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.*;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.network.*;
import com.flansmod.common.paintjob.TileEntityPaintjobTable;
import com.flansmod.common.teams.*;
import com.flansmod.common.tools.EntityParachute;
import com.flansmod.common.types.InfoType;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLModContainer;
import cpw.mods.fml.common.MetadataCollection;
import cpw.mods.fml.common.discovery.ContainerType;
import cpw.mods.fml.common.discovery.ModCandidate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class ClientProxy extends CommonProxy {
    public static String modelDir = "com.flansmod.client.model.";

    /* These renderers handle rendering in hand items */
    public static RenderGun gunRenderer;
    public static RenderGrenade grenadeRenderer;
    public static RenderPlane planeRenderer;
    public static RenderVehicle vehicleRenderer;
    public static RenderMecha mechaRenderer;

    @Override
    public void load() {
        new FlansModClient().load();
        gunRenderer = new RenderGun();
        grenadeRenderer = new RenderGrenade();
        planeRenderer = new RenderPlane();
        vehicleRenderer = new RenderVehicle();
        mechaRenderer = new RenderMecha();

        //Register custom item renderers
        for (GunType gunType : GunType.guns.values())
            MinecraftForgeClient.registerItemRenderer(gunType.item, gunRenderer);

        for (GrenadeType grenadeType : GrenadeType.grenades)
            MinecraftForgeClient.registerItemRenderer(grenadeType.item, grenadeRenderer);

        for (PlaneType planeType : PlaneType.types)
            MinecraftForgeClient.registerItemRenderer(planeType.item, planeRenderer);

        for (VehicleType vehicleType : VehicleType.types)
            MinecraftForgeClient.registerItemRenderer(vehicleType.item, vehicleRenderer);

        for (MechaType mechaType : MechaType.types)
            MinecraftForgeClient.registerItemRenderer(mechaType.item, mechaRenderer);

        FMLCommonHandler.instance().bus().register(new KeyInputHandler());
        new TickHandlerClient();
    }

    /**
     * This method reloads all textures from all mods and resource packs. It forces Minecraft to read images from the content packs added after mod init
     */
    @Override
    public void forceReload() {
        Minecraft.getMinecraft().refreshResources();
    }

    @Override
    public void loadFlanAssets() {
        ClassLoader classloader = (net.minecraft.server.MinecraftServer.class).getClassLoader();
        Method method;
        try {
            method = (java.net.URLClassLoader.class).getDeclaredMethod("addURL", java.net.URL.class);
            method.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            FlansMod.logger.error("Failed to get class loader. All content loading will now fail.", e);
            return;
        }

        for (File file : Objects.requireNonNull(FlansMod.flanDir.listFiles())) {
            if (file.isDirectory() || ContentManager.ZIP_JAR.matcher(file.getName()).matches()) {
                try {
                    method.invoke(classloader, file.toURI().toURL());

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("modid", FlansMod.MODID);
                    map.put("name", "Flan's Mod : " + file.getName());
                    map.put("version", "1");
                    FMLModContainer container = new FMLModContainer(FlansMod.class.getCanonicalName(), new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), map);
                    container.bindMetadata(MetadataCollection.from(null, ""));
                    FMLClientHandler.instance().addModAsResource(container);

                } catch (Exception e) {
                    FlansMod.logger.error("Failed to load images for content pack : {}", file.getName(), e);
                }
                // Add the directory to the content pack list
                FlansMod.logger.info("Loaded content pack : {}", file.getName());
            }
        }
    }

    /**
     * Register entity renderers
     */
    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, new RenderBullet());
        RenderingRegistry.registerEntityRenderingHandler(EntityGrenade.class, new RenderGrenade());
        RenderingRegistry.registerEntityRenderingHandler(EntityPlane.class, new RenderPlane());
        RenderingRegistry.registerEntityRenderingHandler(EntityVehicle.class, new RenderVehicle());
        RenderingRegistry.registerEntityRenderingHandler(EntityAAGun.class, new RenderAAGun());
        RenderingRegistry.registerEntityRenderingHandler(EntityFlagpole.class, new RenderFlagpole());
        RenderingRegistry.registerEntityRenderingHandler(EntityFlag.class, new RenderFlag());
        RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, new RenderNull());
        RenderingRegistry.registerEntityRenderingHandler(EntityWheel.class, new RenderNull());
        RenderingRegistry.registerEntityRenderingHandler(EntityMG.class, new RenderMG());
        RenderingRegistry.registerEntityRenderingHandler(EntityParachute.class, new RenderParachute());
        RenderingRegistry.registerEntityRenderingHandler(EntityDebugDot.class, new RenderDebugDot());
        RenderingRegistry.registerEntityRenderingHandler(EntityDebugVector.class, new RenderDebugVector());
        RenderingRegistry.registerEntityRenderingHandler(EntityDebugAABB.class, new RenderDebugAABB());
        RenderingRegistry.registerEntityRenderingHandler(EntityMecha.class, new RenderMecha());

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySpawner.class, new TileEntitySpawnerRenderer());
    }

    /**
     * Old one time tutorial code that displays messages the first time you enter a plane / vehicle. Needs reworking
     */
    @Override
    public void doTutorialStuff(EntityPlayer player, EntityDriveable entityType) {
        if (!FlansModClient.doneTutorial) {
            FlansModClient.doneTutorial = true;

            player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.inventoryKey.getKeyCode()) + " to open the menu"));
            if (Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode() >= 0 && Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode() < Keyboard.KEYBOARD_SIZE) {
                player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode()) + " to get out"));
            } else {
                player.addChatComponentMessage(new ChatComponentText("Press <undefined> to get out"));
            }
            player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.controlSwitchKey.getKeyCode()) + " to switch controls"));

            if (entityType instanceof EntityPlane) {
                PlaneType type = PlaneType.getPlane(entityType.driveableType);
                Objects.requireNonNull(type, "Could not retrieve plane type!");

                if (type.hasGear)
                    player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.gearKey.getKeyCode()) + " to switch the gear"));
                if (type.hasDoor)
                    player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.doorKey.getKeyCode()) + " to switch the doors"));
                if (type.mode == EnumPlaneMode.VTOL)
                    player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.modeKey.getKeyCode()) + " to switch VTOL mode"));
                if (type.hasWing)
                    player.addChatComponentMessage(new ChatComponentText("Press " + Keyboard.getKeyName(KeyInputHandler.modeKey.getKeyCode()) + " to switch the wings"));
            }
        }
    }

    /**
     * Adds the client side text message regarding mouse control mode switching
     */
    @Override
    public void changeControlMode(EntityPlayer player) {
        if (FlansModClient.flipControlMode())
            player.addChatComponentMessage(new ChatComponentText("Mouse Control mode is now set to " + FlansModClient.controlModeMouse));
    }

    /**
     * Whether the player is in mouse control mode for planes. Now the default setting for planes, but it can be deactivated to look around while flying
     */
    @Override
    public boolean mouseControlEnabled() {
        return FlansModClient.controlModeMouse;
    }

    /**
     * Client GUI object getter
     */
    @Override
    public Object getClientGui(int ID, EntityPlayer player, World world, int x, int y, int z) {
        //Null riding entity, don't open GUI in this case
        if (((ID >= 6 && ID <= 10) || ID == 12) && player.ridingEntity == null) return null;

        switch (ID) {
            case 0:
                return new GuiDriveableCrafting(player.inventory, world, x, y, z);
            case 1:
                return new GuiDriveableRepair(player);
            case 2:
                return new GuiGunModTable(player.inventory, world);
            case 5:
                return new GuiGunBox(player.inventory, ((BlockGunBox) world.getBlock(x, y, z)).type, world);
            case 6:
                return new GuiDriveableInventory(player.inventory, world, ((EntitySeat) player.ridingEntity).driveable, 0);
            case 7:
                return new GuiDriveableInventory(player.inventory, world, ((EntitySeat) player.ridingEntity).driveable, 1);
            case 8:
                return new GuiDriveableFuel(player.inventory, world, ((EntitySeat) player.ridingEntity).driveable);
            case 9:
                return new GuiDriveableInventory(player.inventory, world, ((EntitySeat) player.ridingEntity).driveable, 2);
            case 10:
                return new GuiMechaInventory(player.inventory, world, (EntityMecha) ((EntitySeat) player.ridingEntity).driveable);
            case 11:
                return new GuiArmourBox(player.inventory, ((BlockArmourBox) world.getBlock(x, y, z)).type);
            case 12:
                return new GuiDriveableInventory(player.inventory, world, ((EntitySeat) player.ridingEntity).driveable, 3);
            case 13:
                return new GuiPaintjobTable(player.inventory, world, (TileEntityPaintjobTable) world.getTileEntity(x, y, z));
        }
        return null;
    }

    /**
     * Called when the player presses the plane inventory key. Opens menu client side
     */
    @Override
    public void openDriveableMenu(EntityPlayer player, World world, EntityDriveable driveable) {
        FMLClientHandler.instance().getClient().displayGuiScreen(new GuiDriveableMenu(player.inventory, world, driveable));
    }

    /**
     * Helper method that sorts out packages with model name input
     * For example, the model class "com.flansmod.client.model.mw.ModelMP5"
     * is referenced in the type file by the string "mw.MP5"
     */
    private String getModelName(String in) {
        //Split about dots
        String[] split = in.split("\\.");
        //If there is no dot, our model class is in the default model package
        if (split.length == 1)
            return "com.flansmod.client.model." + "Model" + in;
            //Otherwise, we need to slightly rearrange the wording of the string for it to make sense
        else if (split.length > 1) {
            if (FlansMod.getModelLocation(split[0]) != null) {
                StringBuilder location = new StringBuilder(FlansMod.getModelLocation(split[0]));
                for (int i = 1; i < split.length - 1; i++) {
                    location.append(split[i]);
                }
                return location + ".Model" + split[1];
            }

            StringBuilder out = new StringBuilder("Model" + split[split.length - 1]);
            for (int i = split.length - 2; i >= 0; i--) {
                out.insert(0, split[i] + ".");
            }
            return modelDir + out; //Assume the model is in the default model directory.
        }
        return in;
    }

    /**
     * Generic model loader method for getting model classes and casting them to the required class type
     */
    @Override
    public <T> T loadModel(String s, String shortName, Class<T> typeClass) {
        if (s == null || s.equalsIgnoreCase("None") || shortName == null)
            return null;
        try {
            return typeClass.cast(Class.forName(getModelName(s)).getConstructor().newInstance());
        } catch (InvocationTargetException ex) {
            FlansMod.logPackError("?", "?", shortName, "Model is incompatible, or you are in a dev environment", null, ex.getCause());
        } catch (ClassNotFoundException ex) {
            FlansMod.logPackError("?", "?", shortName, "Model (probably) not found", null, ex);
        } catch (Exception ex) {
            FlansMod.logPackError("?", "?", shortName, "Failed to load model (unknown cause)", null, ex);
        }
        return null;
    }

    /**
     * Sound loading method. Defers to FlansModResourceHandler
     */
    @Override
    public void loadSound(String contentPack, String type, String sound) {
        FlansModResourceHandler.getSound(sound);
        //FMLClientHandler.instance().getClient().installResource("sound3/" + type + "/" + sound + ".ogg", new File(FMLClientHandler.instance().getClient().mcDataDir, "/Flan/" + contentPack + "/sounds/" + sound + ".ogg"));
    }

    /**
     * Checks whether "player" is the current player. Always false on server, since there is no current player
     */
    @Override
    public boolean isThePlayer(EntityPlayer player) {
        return player == FMLClientHandler.instance().getClient().thePlayer;
    }

    @Override
    public EntityPlayer getThePlayer() {
        return FMLClientHandler.instance().getClient().thePlayer;
    }

    @Override
    public boolean isOnSameTeamClientPlayer(EntityLivingBase entity) {
        return FMLClientHandler.instance().getClient().thePlayer.isOnSameTeam(entity);
    }

    /* Gun and armour box crafting methods */
    @Override
    public void buyGun(GunBoxType type, InfoType gun) {
        FlansMod.getPacketHandler().sendToServer(new PacketBuyWeapon(type, gun));
        FlansModClient.shootTimeLeft += 10;
        FlansModClient.shootTimeRight += 10;
    }

    @Override
    public void buyArmour(String shortName, int piece, ArmourBoxType box) {
        FlansMod.getPacketHandler().sendToServer(new PacketBuyArmour(box.shortName, shortName, piece));
        FlansModClient.shootTimeLeft += 10;
        FlansModClient.shootTimeRight += 10;
    }

    @Override
    public void addItem(EntityPlayer player, int id) {
        super.addItem(player, id);
        if (player.worldObj.isRemote)
            FlansMod.getPacketHandler().sendToServer(new PacketGiveItem(57));
    }

    @Override
    public void craftDriveable(EntityPlayer player, DriveableType type) {
        //Craft it this side (so the inventory updates immediately) and then send a packet to the server so that it is crafted that side too
        super.craftDriveable(player, type);
        if (player.worldObj.isRemote)
            FlansMod.getPacketHandler().sendToServer(new PacketCraftDriveable(type.shortName));
    }

    @Override
    public void repairDriveable(EntityPlayer driver, EntityDriveable driving, DriveablePart part) {
        //Repair it this side (so the inventory updates immediately) and then send a packet to the server so that it is repaired that side too
        super.repairDriveable(driver, driving, part);
        if (driver.worldObj.isRemote)
            FlansMod.getPacketHandler().sendToServer(new PacketRepairDriveable(part.type));
    }

    /**
     * Helper method that returns whether there is a GUI open
     */
    @Override
    public boolean isScreenOpen() {
        return Minecraft.getMinecraft().currentScreen != null;
    }

    /**
     * Mecha input getters
     */
    @Override
    public boolean isKeyDown(int key) {
        switch (key) {
            case 0: //Press Forwards
                return keyDown(Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode());

            case 1: //Press Backwards
                return keyDown(Minecraft.getMinecraft().gameSettings.keyBindBack.getKeyCode());

            case 2: //Press Left
                return keyDown(Minecraft.getMinecraft().gameSettings.keyBindLeft.getKeyCode());

            case 3: //Press Right
                return keyDown(Minecraft.getMinecraft().gameSettings.keyBindRight.getKeyCode());

            case 4: //Press Jump
                return keyDown(Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode());
        }
        return false;
    }

    /**
     * Helper method that deals with the way Minecraft handles binding keys to the mouse
     */
    @Override
    public boolean keyDown(int keyCode) {
        return (keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode));
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double mx, double my, double mz) {
        spawnParticle(s, x, y, z, mx, my, mz, 1.0F);
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double mx, double my, double mz, float scale) {
        try {
            doSpawnParticle(s, x, y, z, mx, my, mz, scale);
        } catch (Throwable throwable) {
            FlansMod.logger.error(throwable);
        }
    }

    private static EntityFX doSpawnParticle(String particleType, double x, double y, double z, double vx, double vy, double vz, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        World theWorld = mc.theWorld;
		//RETURN SOONER
        if (mc.renderViewEntity == null || mc.effectRenderer == null) return null;
        double d6 = mc.renderViewEntity.posX - x;
        double d7 = mc.renderViewEntity.posY - y;
        double d8 = mc.renderViewEntity.posZ - z;
        EntityFX entityfx = null;

        // VANILLA PARTICLES
        if (particleType.equals("hugeexplosion")) mc.effectRenderer.addEffect(entityfx = new EntityHugeExplodeFX(theWorld, x, y, z, vx, vy, vz));
        else if (particleType.equals("largeexplode")) mc.effectRenderer.addEffect(entityfx = new EntityLargeExplodeFX(mc.renderEngine, theWorld, x, y, z, vx, vy, vz));
        else if (particleType.equals("fireworksSpark")) mc.effectRenderer.addEffect(entityfx = new EntityFireworkSparkFX(theWorld, x, y, z, vx, vy, vz, mc.effectRenderer));
		
		int i = mc.gameSettings.particleSetting;
		if (i == 1 && theWorld.rand.nextInt(3) == 0) i = 2;
        if (entityfx != null) return entityfx;
        else{
			if (d6 * d6 + d7 * d7 + d8 * d8 > 160.0D * 160.0D || i > 1) return null;
			else{
                // FLANS PARTICLES
                if (particleType.equals("flansmod.muzzleflash")) {
                    entityfx = new EntityFMMuzzleFlash(theWorld, x, y, z, vx, vy, vz);
                    entityfx.multipleParticleScaleBy(scale);
                }
                else if (particleType.equals("flansmod.flare")) 		entityfx = new EntityFlare(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.smoker")) 		entityfx = new EntitySmokeGrenade(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.fmflame")) 		entityfx = new EntityFMFlame(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.flash")) 		entityfx = new EntityFlash(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.smokeburst")) 	entityfx = new EntitySmokeBurst(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.bigsmoke")) 		entityfx = new EntityBigSmoke(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.debris1")) 		entityfx = new EntityDebris1(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.fmtracer")) 		entityfx = new EntityFMTracer(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.fmtracergreen")) entityfx = new EntityFMTracerGreen(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.fmtracerred")) 	entityfx = new EntityFMTracerRed(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("flansmod.afterburn")) 	entityfx = new EntityAfterburn(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.fmsmoke")) 		entityfx = new EntityFMSmoke(theWorld, x, y, z, vx, vy, vz);
                else if (particleType.equals("flansmod.rocketexhaust")) entityfx = new EntityRocketexhaust(theWorld, x, y, z, vx, vy, vz);

                // VANILLA PARTICLES
				else if (particleType.equals("explode")) 				entityfx = new EntityExplodeFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("flame")) 					entityfx = new EntityFlameFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("smoke")) 					entityfx = new EntitySmokeFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("bubble"))					entityfx = new EntityBubbleFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("suspended")) 				entityfx = new EntitySuspendFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("depthsuspend")) 			entityfx = new EntityAuraFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("townaura")) 				entityfx = new EntityAuraFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("crit")) 					entityfx = new EntityCritFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("note")) 					entityfx = new EntityNoteFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("portal")) 				entityfx = new EntityPortalFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("enchantmenttable")) 		entityfx = new EntityEnchantmentTableParticleFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("lava")) 					entityfx = new EntityLavaFX(theWorld, x, y, z);
				else if (particleType.equals("footstep")) 				entityfx = new EntityFootStepFX(mc.renderEngine, theWorld, x, y, z);
				else if (particleType.equals("splash")) 				entityfx = new EntitySplashFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("wake")) 					entityfx = new EntityFishWakeFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("largesmoke")) 			entityfx = new EntitySmokeFX(theWorld, x, y, z, vx, vy, vz, 2.5F);
				else if (particleType.equals("cloud")) 					entityfx = new EntityCloudFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("reddust")) 				entityfx = new EntityReddustFX(theWorld, x, y, z, (float) vx, (float) vy, (float) vz);
				else if (particleType.equals("snowballpoof")) 			entityfx = new EntityBreakingFX(theWorld, x, y, z, Items.snowball);
				else if (particleType.equals("dripWater")) 				entityfx = new EntityDropParticleFX(theWorld, x, y, z, Material.water);
				else if (particleType.equals("dripLava")) 				entityfx = new EntityDropParticleFX(theWorld, x, y, z, Material.lava);
				else if (particleType.equals("snowshovel")) 			entityfx = new EntitySnowShovelFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("slime")) 					entityfx = new EntityBreakingFX(theWorld, x, y, z, Items.slime_ball);
				else if (particleType.equals("heart")) 					entityfx = new EntityHeartFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("magicCrit")) {
					entityfx = new EntityCritFX(theWorld, x, y, z, vx, vy, vz);
					entityfx.setRBGColorF(entityfx.getRedColorF() * 0.3F, entityfx.getGreenColorF() * 0.8F, entityfx.getBlueColorF());
					entityfx.nextTextureIndexX();
                }
				else if (particleType.equals("mobSpell")) {
					entityfx = new EntitySpellParticleFX(theWorld, x, y, z, 0.0D, 0.0D, 0.0D);
					entityfx.setRBGColorF((float) vx, (float) vy, (float) vz);
				}
				else if (particleType.equals("mobSpellAmbient")) {
					entityfx = new EntitySpellParticleFX(theWorld, x, y, z, 0.0D, 0.0D, 0.0D);
					entityfx.setAlphaF(0.15F);
					entityfx.setRBGColorF((float) vx, (float) vy, (float) vz);
				}
				else if (particleType.equals("spell")) entityfx = new EntitySpellParticleFX(theWorld, x, y, z, vx, vy, vz);
				else if (particleType.equals("instantSpell")) {
					entityfx = new EntitySpellParticleFX(theWorld, x, y, z, vx, vy, vz);
					((EntitySpellParticleFX) entityfx).setBaseSpellTextureIndex(144);
				}
				else if (particleType.equals("witchMagic")) {
					entityfx = new EntitySpellParticleFX(theWorld, x, y, z, vx, vy, vz);
					((EntitySpellParticleFX) entityfx).setBaseSpellTextureIndex(144);
					float f = theWorld.rand.nextFloat() * 0.5F + 0.35F;
					entityfx.setRBGColorF(f, 0.0F, f);
				}
				else if (particleType.equals("angryVillager")) {
					entityfx = new EntityHeartFX(theWorld, x, y + 0.5D, z, vx, vy, vz);
					entityfx.setParticleTextureIndex(81);
					entityfx.setRBGColorF(1.0F, 1.0F, 1.0F);
				}
				else if (particleType.equals("happyVillager")) {
					entityfx = new EntityAuraFX(theWorld, x, y, z, vx, vy, vz);
					entityfx.setParticleTextureIndex(82);
					entityfx.setRBGColorF(1.0F, 1.0F, 1.0F);
				}
				else if (particleType.startsWith("iconcrack_")) {
					String[] astring = particleType.split("_", 3);
					int j = Integer.parseInt(astring[1]);
					entityfx = new EntityBreakingFX(theWorld, x, y, z, vx, vy, vz, Item.getItemById(j), (astring.length > 2) ? Integer.parseInt(astring[2]) : 0);
				}
				else if (particleType.startsWith("blockcrack_")) {
					String[] astring = particleType.split("_", 3);
					Block block = Block.getBlockById(Integer.parseInt(astring[1]));
					int temp = Integer.parseInt(astring[2]);
					entityfx = (new EntityDiggingFX(theWorld, x, y, z, vx, vy, vz, block, temp)).applyRenderColor(temp);
				}
				else if (particleType.startsWith("blockdust_")) {
					String[] astring = particleType.split("_", 3);
					Block block = Block.getBlockById(Integer.parseInt(astring[1]));
					int temp = Integer.parseInt(astring[2]);
					entityfx = (new EntityBlockDustFX(theWorld, x, y, z, vx, vy, vz, block, temp)).applyRenderColor(temp);
				}
					
				if (entityfx != null) {
					entityfx.multipleParticleScaleBy(scale);
					mc.effectRenderer.addEffect(entityfx);
				}

				return entityfx;
			}
		}
    }

    public float getMouseSensitivity() {
        return Minecraft.getMinecraft().gameSettings.mouseSensitivity;
    }
}
