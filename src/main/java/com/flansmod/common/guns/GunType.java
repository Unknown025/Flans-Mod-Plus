package com.flansmod.common.guns;

import com.flansmod.client.model.*;
import com.flansmod.common.FlansMod;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.paintjob.Paintjob;
import com.flansmod.common.types.TypeFile;
import com.flansmod.common.vector.Vector3f;
import com.flansmod.utils.ConfigMap;
import com.flansmod.utils.ConfigUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GunType extends PaintableType implements IScope {
    public static final Random rand = new Random();
    private static final int DEFAULT_SHOOT_DELAY = 2;

    /**Extended Recoil System
     * ported by SecretAgent12
     */
    public GunRecoil recoil = new GunRecoil();
    public boolean useFancyRecoil = false;

    //Gun Behaviour Variables

    //Recoil Variables
    /**
     * Base value for Upwards cursor/view recoil
     */
    public float recoilPitch = 0.0F;
    /**
     * Base value for Left/Right cursor/view recoil
     */
    public float recoilYaw = 0.0F;
    /**
     * Modifier for setting the maximum pitch divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     */
    public float rndRecoilPitchRange = 0.5F;
    /**
     * Modifier for setting the maximum yaw divergence when randomizing recoil (Recoil 2 + rndRecoil 0.5 == 1.5-2.5 Recoil range)
     */
    public float rndRecoilYawRange = 0.3F;

    /**
     * DEPRECATED DO NOT USE. Subtracts from pitch recoil when crouching.
     */
    public float decreaseRecoilPitch = 0F;
    /**
     * DEPRECATED DO NOT USE. Divisor for yaw recoil when crouching.
     */
    public float decreaseRecoilYaw = 0F;

    /**
     * The alternatives to the above. Simple multipliers for sneaking, sprinting on yaw and pitch respectively. 1F = no change.
     * -1F is to be used to enable backwards compatibility for sneaking (-2F rather than multiplying)
     */
    public float recoilSneakingMultiplier = -1F;
    public float recoilSprintingMultiplier = 1F;
    public float recoilSneakingMultiplierYaw = 0.8F;
    public float recoilSprintingMultiplierYaw = 1.2F;

    /* Countering gun recoil can be modelled with angle=n^tick where n is the coefficient here. */
    /**
     * HIGHER means less force to center, meaning it takes longer to return.
     */
    public float recoilCounterCoefficient = 0.8F;
    /**
     * The above variable but for sprinting.
     */
    public float recoilCounterCoefficientSprinting = 0.9F;
    /**
     * The above variable but for sneaking.
     */
    public float recoilCounterCoefficientSneaking = 0.7F;

    //Ammo & Reload Variables
    /**
     * The list of bullet types that can be used in this gun
     */
    public List<ShootableType> ammo = new ArrayList<>();
    /**
     * Whether the player can press the reload key (default R) to reload this gun
     */
    public boolean canForceReload = true;
    /**
     * Whether the player can receive ammo for this gun from an ammo mag
     */
    public boolean allowRearm = true;
    /**
     * The time (in ticks) it takes to reload this gun
     */
    public int reloadTime;
    /**
     * Number of ammo items that the gun may hold. Most guns will hold one magazine.
     * Some may hold more, such as Nerf pistols, revolvers or shotguns
     */
    public int numPrimaryAmmoItems = 1;

    //Projectile Mechanic Variables
    /**
     * The amount that bullets spread out when fired from this gun
     */
    public float bulletSpread;
    public float sneakSpreadModifier = 0.63F;
    public float sprintSpreadModifier = 1.75F;
    /**
     * If true, spread determined by loaded ammo type
     */
    public boolean allowSpreadByBullet = false;
    /**
     * Damage inflicted by this gun. Multiplied by the bullet damage.
     */
    public float damage = 0;
    /**
     * The damage inflicted upon punching someone with this gun
     */
    public float meleeDamage = 1;
    // Modifier for melee damage against specifically driveable entities.
    public float meleeDamageDriveableModifier = 1;
    /**
     * The speed of bullets upon leaving this gun
     */
    public float bulletSpeed = 5.0F;
    /**
     * The number of bullet entities created by each shot
     */
    public int numBullets = 1;
    /**
     * Allows you to set how many bullet entities are fired per shot via the ammo used
     */
    public boolean allowNumBulletsByBulletType = false;
    /**
     * The delay between shots in ticks (1/20ths of seconds) OUTDATED, USE RPM
     */
    public float shootDelay = 0;
    /**
     * The fire rate of the gun in RPM, 1200 = MAX
     */
    public float roundsPerMin = 0;
    /**
     * The firing mode of the gun. One of semi-auto, full-auto, minigun or burst
     */
    public EnumFireMode mode = EnumFireMode.FULLAUTO;
    public EnumFireMode[] submode = new EnumFireMode[]{EnumFireMode.FULLAUTO};
    public EnumFireMode defaultmode = mode;
    /**
     * The number of bullets to fire per burst in burst mode
     */
    public int numBurstRounds = 3;
    /**
     * The required speed for minigun mode guns to start firing
     */
    public float minigunStartSpeed = 15F;
    /**
     * how fast does the barrel spin 
     */
    public float minigunAnimateSpeed = 2F;
    /**
     * spin direction of spin part gun type
     */
    public float spinX = 1F, spinY = 0F, spinZ = 0F;
    /**
     * Whether this gun can be used underwater
     */
    public boolean canShootUnderwater = true;
    /**
     * The secondary function of this gun. By default, the left mouse button triggers this
     */
    public EnumSecondaryFunction secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;
    public EnumSecondaryFunction secondaryFunctionWhenShoot = null;
    /**
     * If true, then this gun can be dual wielded
     */
    private boolean oneHanded = false;
    /**
     * For one shot items like a panzerfaust
     */
    public boolean consumeGunUponUse = false;
    /**
     * Show the crosshair when holding this weapon
     */
    public boolean showCrosshair = true;
    /**
     * Item to drop on shooting
     */
    public String dropItemOnShoot = null;
    /**
     * Set these to make guns only usable by a certain type of entity
     */
    public boolean usableByPlayers = true, usableByMechas = true;
    /**
     * Whether Gun makes players to be EnumAction.bow
     */
    public EnumAction itemUseAction = EnumAction.bow;
    /* Whether the gun can be hipfired while sprinting */
    /**
     * 0=use flansmod.cfg default, 1=force allow, 2=force deny
     **/
    public int hipFireWhileSprinting = 0;

    //Launcher variables
    public int canLockOnAngle = 5;
    public int lockOnSoundTime = 0;
    public String lockOnSound = "";
    public int maxRangeLockOn = 80;
    public boolean canSetPosition = false;
    /**
     * Determines what the launcher can lock on to
     */
    public boolean lockOnToPlanes = false, lockOnToVehicles = false, lockOnToMechas = false, lockOnToPlayers = false, lockOnToLivings = false;


    //Shields
	/*A shield is actually a gun without any shoot functionality (similar to knives or binoculars)
	and a load of shield code on top. This means that guns can have in built shields (think Nerf Stampede) */
    /**
     * Whether or not this gun has a shield piece
     */
    public boolean shield = false;
    /**
     * Shield collision box definition. In model co-ordinates
     */
    public Vector3f shieldOrigin, shieldDimensions;
    /**
     * Percentage of damage blocked between 0.00-1.00 (0.50F = 50%)
     */
    public float shieldDamageAbsorption = 0F;

    //Sounds
    /**
     * The sound played upon shooting
     */
    public String shootSound;
    /**
     * Bullet insert reload sound
     */
    public String bulletInsert = "defaultshellinsert";
    /**
     * Pump Sound
     */
    public String actionSound;
    /**
     * The sound to play upon shooting on last round
     */
    public String lastShootSound;

    /**
     * The sound played upon shooting with a suppressor
     */
    public String suppressedShootSound;

    public String lastShootSoundSuppressed;

    /**
     * The length of the sound for looping sounds
     */
    public int shootSoundLength;
    /**
     * Whether to distort the sound or not. Generally only set to false for looping sounds
     */
    public String reloadSound;
    /**
     * The sound to play upon reloading when empty
     */
    public String reloadSoundOnEmpty;
    /**
     * The sound to play open firing when empty(once)
     */
    public String clickSoundOnEmpty;
    /**
     * Sound to play on firing when empty(multiple times)
     */
    public String clickSoundOnEmptyRepeated;
    /**
     * The sound to play while holding the weapon in the hand
     */
    public String idleSound;

    //Sound Modifiers
    /**
     * Whether to distort the sound or not. Generally only set to false for looping sounds
     */
    public boolean distortSound = true;
    /**
     * The length of the idle sound for looping sounds (miniguns)
     */
    public int idleSoundLength;
    /**
     * The block range for idle sounds (for miniguns etc)
     */
    public int idleSoundRange = 50;
    /**
     * The block range for melee sounds
     */
    public int meleeSoundRange = 50;
    /**
     * The block range for reload sounds
     */
    public int reloadSoundRange = 50;
    /**
     * The block range for gunshots sounds
     */
    public int gunSoundRange = 50;

    /**
     * Sound to be played outside of normal range
     */
    public String distantShootSound = "";
    /**
     * Max range for the sound to be played
     */
    public int distantSoundRange = 100;

    //Looping sounds
    /**
     * Whether the looping sounds should be used. Automatically set if the player sets any one of the following sounds
     */
    public boolean useLoopingSounds = false;
    /**
     * Played when the player starts to hold shoot
     */
    public String warmupSound;
    public int warmupSoundLength = 20;
    /**
     * Played in a loop until player stops holding shoot
     */
    public String loopedSound;
    public int loopedSoundLength = 20;
    /**
     * Played when the player stops holding shoot
     */
    public String cooldownSound;

    //Custom Melee Stuff
    /**
     * The sound to play upon weapon swing
     */
    public String meleeSound;
    /**
     * The time delay between custom melee attacks
     */
    public int meleeTime = 1;
    /**
     * The path the melee weapon takes
     */
    public ArrayList<Vector3f> meleePath = new ArrayList<>(), meleePathAngles = new ArrayList<>();
    /**
     * The points on the melee weapon that damage is actually done from.
     */
    public ArrayList<Vector3f> meleeDamagePoints = new ArrayList<>();


    //Deployable Settings
    /**
     * If true, then the bullet does not shoot when right-clicked, but must instead be placed on the ground
     */
    public boolean deployable = false;
    /**
     * The deployable model
     */
    public ModelMG deployableModel;
    public String deployableModelString;
    /**
     * The deployable model's texture
     */
    public String deployableTexture;
    /**
     * Various deployable settings controlling the player view limits and standing position
     */
    public float standBackDist = 1.5F, topViewLimit = -60F, bottomViewLimit = 30F, sideViewLimit = 45F, pivotHeight = 0.375F;

    //Default Scope Settings. Overriden by scope attachments
    //In many cases, this will simply be iron sights
    /**
     * Default scope overlay texture
     */
    public String defaultScopeTexture;
    /**
     * Whether the default scope has an overlay
     */
    public boolean hasScopeOverlay = false;
    /**
     * The zoom level of the default scope
     */
    public float zoomLevel = 1.0F;
    /**
     * The FOV zoom level of the default scope
     */
    public float FOVFactor = 1.5F;
    /**
     * Gives night vision while scoped if true
     */
    public boolean allowNightVision = false;

    //Model variables
    /**
     * For guns with 3D models
     */
    public ModelGun model;

    /**
     * For adding a bullet casing model to render
     */
    public ModelCasing casingModel;
    public String casingModelString;
    /**
     * For adding a muzzle flash model to render
     */
    public ModelFlash flashModel;
    public String flashModelString;
    /**
     * Set a bullet casing texture
     */
    public String casingTexture;
    /**
     * Set a muzzle flash texture
     */
    public String flashTexture;
    /**
     * Set a hit marker texture
     */
    public String hitTexture;

    public String muzzleFlashParticle = "flansmod.muzzleflash";
    public float muzzleFlashParticleSize = 1F;
    private Boolean useMuzzleFlashDefaults = true;
    private Boolean showMuzzleFlashParticles = true;
    public Boolean showMuzzleFlashParticlesFirstPerson = false;
    public Vector3f muzzleFlashParticlesHandOffset = new Vector3f();
    public Vector3f muzzleFlashParticlesShoulderOffset = new Vector3f();

    //Attachment settings
    /**
     * If this is true, then all attachments are allowed. Otherwise, the list is checked.
     */
    public boolean allowAllAttachments = false;
    /**
     * The list of allowed attachments for this gun
     */
    public ArrayList<AttachmentType> allowedAttachments = new ArrayList<>();
    /**
     * Whether each attachment slot is available
     */
    public boolean allowBarrelAttachments = false, allowScopeAttachments = false,
            allowStockAttachments = false, allowGripAttachments = false, allowGadgetAttachments = false,
            allowSlideAttachments = false, allowPumpAttachments = false, allowAccessoryAttachments = false;
    /**
     * The number of generic attachment slots there are on this gun
     */
    public int numGenericAttachmentSlots = 0;

    /**
     * The static hashmap of all guns by shortName
     */
    public static HashMap<String, GunType> guns = new HashMap<>();
    /**
     * The static list of all guns
     */
    public static ArrayList<GunType> gunList = new ArrayList<>();

    //Modifiers
    /**
     * Speeds up or slows down player movement when this item is held
     */
    public float moveSpeedModifier = 1F;
    /**
     * Gives knockback resistance to the player
     */
    public float knockbackModifier = 0F;
    /**
     * Default spread of the gun. Do not modify.
     */
    private float defaultSpread = 0F;
    // Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg
    public float adsSpreadModifier = -1F;
    // Modifier for (usually decreasing) spread when gun is ADS. -1 uses default values from flansmod.cfg. For shotguns.
    public float adsSpreadModifierShotgun = -1F;

    public float switchDelay = 0;

    private boolean hasVariableZoom = false;
    private float minZoom = 1;
    private float maxZoom = 4;
    private float zoomAugment = 1;


    public GunType(TypeFile file) {
        super(file);
    }

    @Override
    public void preRead(TypeFile file) {
        super.preRead(file);
    }

    @Override
    public void postRead(TypeFile file) {
        super.postRead(file);

        if (this.shortName != null & isValid) {
            gunList.add(this);
            guns.put(shortName, this);
        }
    }

    @Override
    protected void read(ConfigMap config, TypeFile file) {
        super.read(config, file);

        try {
            damage = ConfigUtils.configFloat(config, "Damage", damage);


            meleeDamageDriveableModifier = ConfigUtils.configFloat(config, "MeleeDamageDriveableModifier", meleeDamageDriveableModifier);
            recoilCounterCoefficient = ConfigUtils.configFloat(config, "CounterRecoilForce", recoilCounterCoefficient);
            recoilCounterCoefficientSneaking = ConfigUtils.configFloat(config, "CounterRecoilForceSneaking", recoilCounterCoefficientSneaking);
            recoilCounterCoefficientSprinting = ConfigUtils.configFloat(config, "CounterRecoilForceSprinting", recoilCounterCoefficientSprinting);
            sneakSpreadModifier = ConfigUtils.configFloat(config, new String[]{"SneakSpreadModifier", "SneakSpreadMultiplier"}, sneakSpreadModifier);
            sprintSpreadModifier = ConfigUtils.configFloat(config, new String[]{"SprintSpreadModifier", "SprintSpreadMultiplier"}, sprintSpreadModifier);
            canForceReload = ConfigUtils.configBool(config, "CanForceReload", canForceReload);
            allowRearm = ConfigUtils.configBool(config, "AllowRearm", allowRearm);
            reloadTime = ConfigUtils.configInt(config, "ReloadTime", reloadTime);

            //Recoil
            recoilPitch = ConfigUtils.configFloat(config, "Recoil", recoilPitch);

            String[] aSplit = ConfigUtils.getSplitFromKey(config, "FancyRecoil");
            try {
                if (aSplit != null && aSplit.length > 1) {
                    recoil.read(aSplit);
                    useFancyRecoil = true;
                }
            } catch (Exception ex) {
                useFancyRecoil = false;
                FlansMod.logPackError(file.name, packName, shortName, "Failed to read fancy recoil", aSplit, ex);
            }

            recoilYaw = ConfigUtils.configFloat(config, "RecoilYaw", recoilYaw) / 10;
            rndRecoilPitchRange = ConfigUtils.configFloat(config, "RandomRecoilRange", rndRecoilPitchRange);
            rndRecoilYawRange = ConfigUtils.configFloat(config, "RandomRecoilYawRange", rndRecoilYawRange);
            decreaseRecoilPitch = ConfigUtils.configFloat(config, "DecreaseRecoil", decreaseRecoilPitch);
            decreaseRecoilYaw = ConfigUtils.configFloat(config, "DecreaseRecoilYaw", decreaseRecoilYaw);
            decreaseRecoilYaw = decreaseRecoilYaw > 0 ? decreaseRecoilYaw : 0.5F;
            recoilSneakingMultiplier = ConfigUtils.configFloat(config, "RecoilSneakingMultiplier", recoilSneakingMultiplier);
            recoilSprintingMultiplier = ConfigUtils.configFloat(config, "RecoilSprintingMultiplier", recoilSprintingMultiplier);
            recoilSneakingMultiplierYaw = ConfigUtils.configFloat(config, "RecoilSneakingMultiplierYaw", recoilSneakingMultiplierYaw);
            recoilSprintingMultiplierYaw = ConfigUtils.configFloat(config, "RecoilSprintingMultiplierYaw", recoilSprintingMultiplierYaw);
            defaultSpread = bulletSpread = ConfigUtils.configFloat(config, new String[]{"Accuracy", "Spread"}, defaultSpread);
            adsSpreadModifier = ConfigUtils.configFloat(config, "ADSSpreadModifier", adsSpreadModifier);
            adsSpreadModifierShotgun = ConfigUtils.configFloat(config, "ADSSpreadModifierShotgun", adsSpreadModifierShotgun);
            numBullets = ConfigUtils.configInt(config, "NumBullets", numBullets);
            allowNumBulletsByBulletType = ConfigUtils.configBool(config, "AllowNumBulletsByBulletType", allowNumBulletsByBulletType);
            allowSpreadByBullet = ConfigUtils.configBool(config, "AllowSpreadByBullet", allowSpreadByBullet);
            canLockOnAngle = ConfigUtils.configInt(config, "CanLockAngle", canLockOnAngle);

            //Lock on settings
            lockOnSoundTime = ConfigUtils.configInt(config, "LockOnSoundTime", lockOnSoundTime);
            if (config.containsKey("LockOnToDriveables"))
                lockOnToPlanes = lockOnToVehicles = lockOnToMechas = ConfigUtils.configBool(config, "LockOnToDriveables", false);
            lockOnToVehicles = ConfigUtils.configBool(config, "LockOnToVehicles", lockOnToVehicles);
            lockOnToPlanes = ConfigUtils.configBool(config, "LockOnToPlanes", lockOnToPlanes);
            lockOnToMechas = ConfigUtils.configBool(config, "LockOnToMechas", lockOnToMechas);
            lockOnToPlayers = ConfigUtils.configBool(config, "LockOnToPlayers", lockOnToPlayers);
            lockOnToLivings = ConfigUtils.configBool(config, "LockOnToLivings", lockOnToLivings);
            maxRangeLockOn = ConfigUtils.configInt(config, "MaxRangeLockOn", maxRangeLockOn);

            consumeGunUponUse = ConfigUtils.configBool(config, "ConsumeGunOnUse", consumeGunUponUse);
            showCrosshair = ConfigUtils.configBool(config, "ShowCrosshair", showCrosshair);
            dropItemOnShoot = ConfigUtils.configString(config, "DropItemOnShoot", null);
            numBurstRounds = ConfigUtils.configInt(config, "NumBurstRounds", numBurstRounds);
            minigunStartSpeed = ConfigUtils.configFloat(config, "MinigunStartSpeed", minigunStartSpeed);
	    minigunAnimateSpeed = ConfigUtils.configFloat(config, "MinigunAnimateSpeed", minigunAnimateSpeed);
	    spinX = ConfigUtils.configFloat(config, "SpinX", spinX);
	    spinY = ConfigUtils.configFloat(config, "SpinY", spinY);
	    spinZ = ConfigUtils.configFloat(config, "SpinZ", spinZ);

            String line = ConfigUtils.configString(config, "ItemUseAction", null);
            try {
                if (line != null) {
                    itemUseAction = EnumAction.valueOf(line.toLowerCase());
                }
            } catch (Exception ex) {
                FlansMod.logPackError(file.name, packName, shortName, "ItemUseAction not recognised in gun", new String[]{"ItemUseAction", line}, ex);
            }

            // This is needed, because the presence of the value overrides the default value of zero.
            if (config.containsKey("HipFireWhileSprinting"))
                hipFireWhileSprinting = ConfigUtils.configBool(config, "HipFireWhileSprinting", false) ? 1 : 2;

            //Sounds
            shootDelay = ConfigUtils.configFloat(config, "ShootDelay", shootDelay);
            roundsPerMin = ConfigUtils.configFloat(config, "RoundsPerMin", roundsPerMin);
            shootSoundLength = ConfigUtils.configInt(config, "SoundLength", shootSoundLength);
            distortSound = ConfigUtils.configBool(config, "DistortSound", distortSound);
            idleSoundRange = ConfigUtils.configInt(config, "IdleSoundRange", idleSoundRange);
            meleeSoundRange = ConfigUtils.configInt(config, "MeleeSoundRange", meleeSoundRange);
            reloadSoundRange = ConfigUtils.configInt(config, "ReloadSoundRange", reloadSoundRange);
            gunSoundRange = ConfigUtils.configInt(config, "GunSoundRange", gunSoundRange);
            shootSound = ConfigUtils.configGunSound(packName, config, "ShootSound", shootSound);
            bulletInsert = ConfigUtils.configGunSound(packName, config, "BulletInsertSound", bulletInsert);
            actionSound = ConfigUtils.configGunSound(packName, config, "ActionSound", actionSound);
            lastShootSound = ConfigUtils.configGunSound(packName, config, "LastShootSound", lastShootSound);
            suppressedShootSound = ConfigUtils.configGunSound(packName, config, "SuppressedShootSound", suppressedShootSound);
            lastShootSoundSuppressed = ConfigUtils.configGunSound(packName, config, "LastSuppressedShootSound", lastShootSoundSuppressed);
            reloadSound = ConfigUtils.configGunSound(packName, config, "ReloadSound", reloadSound);
            reloadSoundOnEmpty = ConfigUtils.configGunSound(packName, config, "EmptyReloadSound", reloadSoundOnEmpty);
            clickSoundOnEmpty = ConfigUtils.configGunSound(packName, config, "EmptyClickSound", clickSoundOnEmpty);
            clickSoundOnEmptyRepeated = ConfigUtils.configGunSound(packName, config, "EmptyClickSoundRepeated", clickSoundOnEmptyRepeated);
            idleSound = ConfigUtils.configGunSound(packName, config, "IdleSound", idleSound);
            idleSoundLength = ConfigUtils.configInt(config, "IdleSoundLength", idleSoundLength);
            meleeSound = ConfigUtils.configGunSound(packName, config, "MeleeSound", meleeSound);

            //Looping sounds
            warmupSound = ConfigUtils.configGunSound(packName, config, "WarmupSound", warmupSound);
            warmupSoundLength = ConfigUtils.configInt(config, "WarmupSoundLength", warmupSoundLength);

            loopedSound = ConfigUtils.configGunSound(packName, config, new String[]{"LoopedSound", "SpinSound"}, loopedSound);
            if (loopedSound != null && !loopedSound.isEmpty())
                useLoopingSounds = true;

            loopedSoundLength = ConfigUtils.configInt(config, new String[]{"LoopedSoundLength", "SpinSoundLength"}, loopedSoundLength);
            cooldownSound = ConfigUtils.configGunSound(packName, config, "CooldownSound", cooldownSound);
            lockOnSound = ConfigUtils.configGunSound(packName, config, "LockOnSound", lockOnSound);
            distantShootSound = ConfigUtils.configGunSound(packName, config, new String[]{"DistantSound", "DistantShootSound"}, distantShootSound);
            distantSoundRange = ConfigUtils.configInt(config, "DistantSoundRange", distantSoundRange);


            aSplit = ConfigUtils.getSplitFromKey(config, "Mode");

            if (aSplit != null) {
                try {
                    mode = EnumFireMode.getFireMode(aSplit[1]);
                    defaultmode = mode;
                    submode = new EnumFireMode[aSplit.length - 1];
                    for (int i = 0; i < submode.length; i++) {
                        submode[i] = EnumFireMode.getFireMode(aSplit[1 + i]);
                    }
                } catch (Exception ex) {
                    FlansMod.logPackError(file.name, packName, shortName, "Error thrown while setting gun mode", aSplit, ex);
                }
            }

            String scopeString = ConfigUtils.configString(config, "Scope", null);
            if (scopeString == null || scopeString.equalsIgnoreCase("None")) {
                hasScopeOverlay = false;
            } else {
                hasScopeOverlay = true;
                defaultScopeTexture = scopeString;
            }

            allowNightVision = ConfigUtils.configBool(config, "AllowNightVision", allowNightVision);


            deployable = ConfigUtils.configBool(config, "Deployable", deployable);

            deployableModelString = ConfigUtils.configString(configMap, "DeployedModel", null);

            casingModelString = ConfigUtils.configString(config, "CasingModel", null);

            flashModelString = ConfigUtils.configString(config, "FlashModel", null);

            if (FMLCommonHandler.instance().getSide().isClient()) {
                deployableModel = FlansMod.proxy.loadModel(deployableModelString, shortName, ModelMG.class);
                casingModel = FlansMod.proxy.loadModel(casingModelString, shortName, ModelCasing.class);
                flashModel = FlansMod.proxy.loadModel(flashModelString, shortName, ModelFlash.class);
                model = FlansMod.proxy.loadModel(modelString, shortName, ModelGun.class);
            }


            casingTexture = ConfigUtils.configString(config, "CasingTexture", null);
            flashTexture = ConfigUtils.configString(config, "FlashTexture", null);
            muzzleFlashParticle = ConfigUtils.configString(config, "MuzzleFlashParticle", muzzleFlashParticle);
            muzzleFlashParticleSize = ConfigUtils.configFloat(config, "MuzzleFlashParticleSize", muzzleFlashParticleSize);

            useMuzzleFlashDefaults = !config.containsKey("ShowMuzzleFlashParticle");
            showMuzzleFlashParticles = ConfigUtils.configBool(config, "ShowMuzzleFlashParticle", showMuzzleFlashParticles);

            showMuzzleFlashParticlesFirstPerson = ConfigUtils.configBool(config, "ShowMuzzleFlashParticleFirstPerson", showMuzzleFlashParticlesFirstPerson);
            muzzleFlashParticlesShoulderOffset = ConfigUtils.configVector(config, "MuzzleFlashParticleShoulderOffset", muzzleFlashParticlesShoulderOffset);
            muzzleFlashParticlesHandOffset = ConfigUtils.configVector(config, "MuzzleFlashParticleHandOffset", muzzleFlashParticlesHandOffset);
            modelScale = ConfigUtils.configFloat(config, "ModelScale", modelScale);
            texture = ConfigUtils.configString(config, "Texture", texture);
            hitTexture = ConfigUtils.configString(config, "HitTexture", hitTexture);
            deployableTexture = ConfigUtils.configString(config, "DeployedTexture", deployableTexture);
            topViewLimit = ConfigUtils.configFloat(config, "TopViewLimit", modelScale);
            bottomViewLimit = ConfigUtils.configFloat(config, "BottomViewLimit", modelScale);
            sideViewLimit = ConfigUtils.configFloat(config, "SideViewLimit", modelScale);
            pivotHeight = ConfigUtils.configFloat(config, "PivotHeight", modelScale);

            ArrayList<String[]> splits = ConfigUtils.getSplitsFromKey(config, new String[]{"Ammo"});
            for (String[] split : splits) {
                try {
                    ShootableType type = ShootableType.getShootableType(split[1]);
                    if (type == null) {
                        FlansMod.logPackError(file.name, packName, shortName, "Couldn't find shootable type for adding Ammo to gun", split, null);
                    } else {
                        ammo.add(type);
                    }
                } catch (Exception ex) {
                    FlansMod.logPackError(file.name, packName, shortName, "Error thrown while adding Ammo for gun", split, ex);
                }
            }

            numPrimaryAmmoItems = ConfigUtils.configInt(config, new String[]{"NumAmmoSlots", "NumAmmoItemsInGun", "LoadIntoGun"}, numPrimaryAmmoItems);
            bulletSpeed = ConfigUtils.configFloat(config, "BulletSpeed", bulletSpeed);
            canShootUnderwater = ConfigUtils.configBool(config, "CanShootUnderwater", canShootUnderwater);
            canSetPosition = ConfigUtils.configBool(config, "CanSetPosition", canSetPosition);
            oneHanded = ConfigUtils.configBool(config, "OneHanded", oneHanded);


            usableByPlayers = ConfigUtils.configBool(config, "UsableByPlayers", usableByPlayers);
            usableByMechas = ConfigUtils.configBool(config, "UsableByMechas", usableByMechas);


            if (ConfigUtils.configBool(config, "UseCustomMeleeWhenShoot", false)) {
                secondaryFunctionWhenShoot = EnumSecondaryFunction.CUSTOM_MELEE;
            }

            meleeTime = ConfigUtils.configInt(config, "MeleeTime", meleeTime);

            splits = ConfigUtils.getSplitsFromKey(config, new String[]{"AddNode"});
            for (String[] split : splits) {
                try {
                    meleePath.add(new Vector3f(Float.parseFloat(split[1]) / 16F, Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F));
                    meleePathAngles.add(new Vector3f(Float.parseFloat(split[4]), Float.parseFloat(split[5]), Float.parseFloat(split[6])));
                } catch (Exception ex) {
                    FlansMod.logPackError(file.name, packName, shortName, "Error thrown during AddNode", split, ex);
                }
            }

            splits = ConfigUtils.getSplitsFromKey(config, new String[]{"MeleeDamagePoint", "MeleeDamageOffset"});
            for (String[] split : splits) {
                try {
                    meleeDamagePoints.add(new Vector3f(Float.parseFloat(split[1]) / 16F, Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F));
                } catch (Exception ex) {
                    FlansMod.logPackError(file.name, packName, shortName, "Error thrown during MeleeDamagePoint", split, ex);
                }
            }


            //Player modifiers
            moveSpeedModifier = ConfigUtils.configFloat(config, new String[]{"MoveSpeedModifier", "Slowness"}, moveSpeedModifier);
            knockbackModifier = ConfigUtils.configFloat(config, new String[]{"KnockbackReduction", "KnockbackModifier"}, knockbackModifier);
            switchDelay = ConfigUtils.configFloat(config, "SwitchDelay", switchDelay);

            //Attachment settings
            allowAllAttachments = ConfigUtils.configBool(config, "AllowAllAttachments", allowAllAttachments);

            splits = ConfigUtils.getSplitsFromKey(config, new String[]{"AllowAttachments"});
            try {
                for (String[] split : splits) {
                    for (int i = 1; i < split.length; i++) {
                        AttachmentType type = AttachmentType.getAttachment(split[i]);
                        if (type != null) {
                            allowedAttachments.add(type);
                        } else {
                            FlansMod.logPackError(file.name, packName, shortName, "Attachment type not found for AllowAttachments (" + split[i] + ")", split, null);
                        }
                    }
                }
            } catch (Exception ex) {
                FlansMod.logPackError(file.name, packName, shortName, "Failed to add allowed attachment with AllowAttachments", aSplit, ex);
            }

            allowBarrelAttachments = ConfigUtils.configBool(config, "AllowBarrelAttachments", allowBarrelAttachments);
            allowScopeAttachments = ConfigUtils.configBool(config, "AllowScopeAttachments", allowScopeAttachments);
            allowStockAttachments = ConfigUtils.configBool(config, "AllowStockAttachments", allowStockAttachments);
            allowGripAttachments = ConfigUtils.configBool(config, "AllowGripAttachments", allowGripAttachments);
            allowGadgetAttachments = ConfigUtils.configBool(config, "AllowGadgetAttachments", allowGadgetAttachments);
            allowSlideAttachments = ConfigUtils.configBool(config, "AllowSlideAttachments", allowSlideAttachments);
            allowPumpAttachments = ConfigUtils.configBool(config, "AllowPumpAttachments", allowPumpAttachments);
            allowAccessoryAttachments = ConfigUtils.configBool(config, "AllowAccessoryAttachments", allowAccessoryAttachments);
            numGenericAttachmentSlots = ConfigUtils.configInt(config, "NumGenericAttachmentSlots", numGenericAttachmentSlots);

            //Shield settings
            String[] split = ConfigUtils.getSplitFromKey(config, "Shield");
            try {
                if (split != null) {
                    shield = true;
                    shieldDamageAbsorption = Float.parseFloat(split[1]);
                    shieldOrigin = new Vector3f(Float.parseFloat(split[2]) / 16F, Float.parseFloat(split[3]) / 16F, Float.parseFloat(split[4]) / 16F);
                    shieldDimensions = new Vector3f(Float.parseFloat(split[5]) / 16F, Float.parseFloat(split[6]) / 16F, Float.parseFloat(split[7]) / 16F);
                }
            } catch (Exception ex) {
                FlansMod.logPackError(file.name, packName, shortName, "Failed to config Shield", split, ex);
            }

            // secondary functions are listed here highest priority last. (overwritten)

            // Zoom: lowest priority
            zoomLevel = ConfigUtils.configFloat(config, "ZoomLevel", zoomLevel);
            if (config.containsKey("ZoomLevel") && zoomLevel > 1F)
                secondaryFunction = EnumSecondaryFunction.ZOOM;

            FOVFactor = ConfigUtils.configFloat(config, "FOVZoomLevel", FOVFactor);
            if (config.containsKey("FOVZoomLevel") && FOVFactor > 1F)
                secondaryFunction = EnumSecondaryFunction.ADS_ZOOM;

            //Modes and zoom settings
            hasVariableZoom = ConfigUtils.configBool(config, "HasVariableZoom", hasVariableZoom);
            minZoom = ConfigUtils.configFloat(config, "MinZoom", minZoom);
            maxZoom = ConfigUtils.configFloat(config, "MaxZoom", maxZoom);
            if (maxZoom > 1F && hasVariableZoom)
                secondaryFunction = EnumSecondaryFunction.ZOOM;
            zoomAugment = ConfigUtils.configFloat(config, "ZoomAugment", zoomAugment);

            // Melee: higher priority
            meleeDamage = ConfigUtils.configFloat(config, "MeleeDamage", meleeDamage);
            secondaryFunction = meleeDamage > 0 && config.containsKey("MeleeDamage") && !hasZoomOverlay() ? EnumSecondaryFunction.MELEE : secondaryFunction;

            if (ConfigUtils.configBool(config, "UseCustomMelee", false)) {
                secondaryFunction = EnumSecondaryFunction.CUSTOM_MELEE;
            }

            String secondaryFunctionString = ConfigUtils.configString(config, "SecondaryFunction", secondaryFunction.toString());
            secondaryFunction = EnumSecondaryFunction.get(secondaryFunctionString);


            if (FMLCommonHandler.instance().getSide().isClient() && model != null) {
                processAnimationConfigs(config);
            }
        } catch (Exception ex) {
            FlansMod.logPackError(file.name, shortName, packName, "Fatal error while loading gun", null, ex);
            isValid = false;
        }
    }

    public void processAnimationConfigs(ConfigMap config) {
        // These provide a way to override model configs without recompiling the model.

        model.minigunBarrelOrigin = ConfigUtils.configVector(config, "animMinigunBarrelOrigin", model.minigunBarrelOrigin);
        model.barrelAttachPoint = ConfigUtils.configVector(config, "animBarrelAttachPoint", model.barrelAttachPoint);
        model.scopeAttachPoint = ConfigUtils.configVector(config, "animScopeAttachPoint", model.scopeAttachPoint);
        model.stockAttachPoint = ConfigUtils.configVector(config, "animStockAttachPoint", model.stockAttachPoint);
        model.gripAttachPoint = ConfigUtils.configVector(config, "animGripAttachPoint", model.gripAttachPoint);
        model.gadgetAttachPoint = ConfigUtils.configVector(config, "animGadgetAttachPoint", model.gadgetAttachPoint);
        model.slideAttachPoint = ConfigUtils.configVector(config, "animSlideAttachPoint", model.slideAttachPoint);
        model.pumpAttachPoint = ConfigUtils.configVector(config, "animPumpAttachPoint", model.pumpAttachPoint);
        model.accessoryAttachPoint = ConfigUtils.configVector(config, "animAccessoryAttachPoint", model.accessoryAttachPoint);

        model.defaultBarrelFlashPoint = ConfigUtils.configVector(config, "animDefaultBarrelFlashPoint", model.defaultBarrelFlashPoint);
        model.muzzleFlashPoint = ConfigUtils.configVector(config, "animMuzzleFlashPoint", model.muzzleFlashPoint);

        model.hasFlash = ConfigUtils.configBool(config, "animHasFlash", model.hasFlash);
        model.hasArms = ConfigUtils.configBool(config, "animHasArms", model.hasArms);
        model.easyArms = ConfigUtils.configBool(config, "easyArms", model.easyArms);
        model.armScale = ConfigUtils.configVector(config, "armScale", model.armScale);

        model.leftArmPos = ConfigUtils.configVector(config, "animLeftArmPos", model.leftArmPos);
        model.leftArmRot = ConfigUtils.configVector(config, "animLeftArmRot", model.leftArmRot);
        model.leftArmScale = ConfigUtils.configVector(config, "animLeftArmScale", model.leftArmScale);
        model.rightArmPos = ConfigUtils.configVector(config, "animRightArmPos", model.rightArmPos);
        model.rightArmRot = ConfigUtils.configVector(config, "animRightArmRot", model.rightArmRot);
        model.rightArmScale = ConfigUtils.configVector(config, "animRightArmScale", model.rightArmScale);

        model.rightArmReloadPos = ConfigUtils.configVector(config, "animRightArmReloadPos", model.rightArmReloadPos);
        model.rightArmReloadRot = ConfigUtils.configVector(config, "animRightArmReloadRot", model.rightArmReloadRot);
        model.leftArmReloadPos = ConfigUtils.configVector(config, "animLeftArmReloadPos", model.leftArmReloadPos);
        model.leftArmReloadRot = ConfigUtils.configVector(config, "animLeftArmReloadRot", model.leftArmReloadRot);

        model.rightArmChargePos = ConfigUtils.configVector(config, "animRightArmChargePos", model.rightArmChargePos);
        model.rightArmChargeRot = ConfigUtils.configVector(config, "animRightArmChargeRot", model.rightArmChargeRot);
        model.leftArmChargePos = ConfigUtils.configVector(config, "animLeftArmChargePos", model.leftArmChargePos);
        model.leftArmChargeRot = ConfigUtils.configVector(config, "animLeftArmChargeRot", model.leftArmChargeRot);

        model.stagedrightArmReloadPos = ConfigUtils.configVector(config, "animStagedRightArmReloadPos", model.stagedrightArmReloadPos);
        model.stagedrightArmReloadRot = ConfigUtils.configVector(config, "animStagedRightArmReloadRot", model.stagedrightArmReloadRot);
        model.stagedleftArmReloadPos = ConfigUtils.configVector(config, "animStagedLeftArmReloadPos", model.stagedleftArmReloadPos);
        model.stagedleftArmReloadRot = ConfigUtils.configVector(config, "animStagedLeftArmReloadRot", model.stagedleftArmReloadRot);

        model.rightHandAmmo = ConfigUtils.configBool(config, "animRightHandAmmo", model.rightHandAmmo);
        model.leftHandAmmo = ConfigUtils.configBool(config, "animLeftHandAmmo", model.leftHandAmmo);

        model.gunSlideDistance = ConfigUtils.configFloat(config, "animGunSlideDistance", model.gunSlideDistance);
        model.altgunSlideDistance = ConfigUtils.configFloat(config, "animAltGunSlideDistance", model.altgunSlideDistance);
        model.RecoilSlideDistance = ConfigUtils.configFloat(config, "animRecoilSlideDistance", model.RecoilSlideDistance);
        model.RotateSlideDistance = ConfigUtils.configFloat(config, "animRotatedSlideDistance", model.RotateSlideDistance);
        model.ShakeDistance = ConfigUtils.configFloat(config, "animShakeDistance", model.ShakeDistance);
        model.recoilAmount = ConfigUtils.configFloat(config, "animRecoilAmount", model.recoilAmount);

        model.casingAnimDistance = ConfigUtils.configVector(config, "animCasingAnimDistance", model.casingAnimDistance);
        model.casingAnimSpread = ConfigUtils.configVector(config, "animCasingAnimSpread", model.casingAnimSpread);
        model.casingAnimTime = ConfigUtils.configInt(config, "animCasingAnimTime", model.casingAnimTime);
        model.casingRotateVector = ConfigUtils.configVector(config, "animCasingRotateVector", model.casingRotateVector);
        model.casingAttachPoint = ConfigUtils.configVector(config, "animCasingAttachPoint", model.casingAttachPoint);
        model.casingDelay = ConfigUtils.configInt(config, "animCasingDelay", model.casingDelay);
        model.caseScale = ConfigUtils.configFloat(config, "animCasingScale", model.caseScale);
        model.flashScale = ConfigUtils.configFloat(config, "animFlashScale", model.flashScale);

        model.chargeHandleDistance = ConfigUtils.configFloat(config, "animChargeHandleDistance", model.chargeHandleDistance);
        model.chargeDelay = ConfigUtils.configInt(config, "animChargeDelay", model.chargeDelay);
        model.chargeDelayAfterReload = ConfigUtils.configInt(config, "animChargeDelayAfterReload", model.chargeDelayAfterReload);
        model.chargeTime = ConfigUtils.configInt(config, "animChargeTime", model.chargeTime);
        model.countOnRightHandSide = ConfigUtils.configBool(config, "animCountOnRightHandSide", model.countOnRightHandSide);
        model.isBulletCounterActive = ConfigUtils.configBool(config, "animIsBulletCounterActive", model.isBulletCounterActive);
        model.isAdvBulletCounterActive = ConfigUtils.configBool(config, "animIsAdvBulletCounterActive", model.isAdvBulletCounterActive);

        String[] split = ConfigUtils.getSplitFromKey(config, "animAnimationType");
        if (split != null) {
            if (split[1].equalsIgnoreCase("NONE"))
                model.animationType = EnumAnimationType.NONE;
            else if (split[1].equalsIgnoreCase("BOTTOM_CLIP"))
                model.animationType = EnumAnimationType.BOTTOM_CLIP;
            else if (split[1].equalsIgnoreCase("CUSTOMBOTTOM_CLIP"))
                model.animationType = EnumAnimationType.CUSTOMBOTTOM_CLIP;
            else if (split[1].equalsIgnoreCase("PISTOL_CLIP"))
                model.animationType = EnumAnimationType.PISTOL_CLIP;
            else if (split[1].equalsIgnoreCase("CUSTOMPISTOL_CLIP"))
                model.animationType = EnumAnimationType.CUSTOMPISTOL_CLIP;
            else if (split[1].equalsIgnoreCase("TOP_CLIP"))
                model.animationType = EnumAnimationType.TOP_CLIP;
            else if (split[1].equalsIgnoreCase("CUSTOMTOP_CLIP"))
                model.animationType = EnumAnimationType.CUSTOMTOP_CLIP;
            else if (split[1].equalsIgnoreCase("SIDE_CLIP"))
                model.animationType = EnumAnimationType.SIDE_CLIP;
            else if (split[1].equalsIgnoreCase("CUSTOMSIDE_CLIP"))
                model.animationType = EnumAnimationType.CUSTOMSIDE_CLIP;
            else if (split[1].equalsIgnoreCase("P90"))
                model.animationType = EnumAnimationType.P90;
            else if (split[1].equalsIgnoreCase("CUSTOMP90"))
                model.animationType = EnumAnimationType.CUSTOMP90;
            else if (split[1].equalsIgnoreCase("SHOTGUN"))
                model.animationType = EnumAnimationType.SHOTGUN;
            else if (split[1].equalsIgnoreCase("CUSTOMSHOTGUN"))
                model.animationType = EnumAnimationType.CUSTOMSHOTGUN;
            else if (split[1].equalsIgnoreCase("RIFLE"))
                model.animationType = EnumAnimationType.RIFLE;
            else if (split[1].equalsIgnoreCase("CUSTOMRIFLE"))
                model.animationType = EnumAnimationType.CUSTOMRIFLE;
            else if (split[1].equalsIgnoreCase("REVOLVER"))
                model.animationType = EnumAnimationType.REVOLVER;
            else if (split[1].equalsIgnoreCase("CUSTOMREVOLVER"))
                model.animationType = EnumAnimationType.CUSTOMREVOLVER;
            else if (split[1].equalsIgnoreCase("REVOLVER2"))
                model.animationType = EnumAnimationType.REVOLVER;
            else if (split[1].equalsIgnoreCase("CUSTOMREVOLVER2"))
                model.animationType = EnumAnimationType.CUSTOMREVOLVER;
            else if (split[1].equalsIgnoreCase("END_LOADED"))
                model.animationType = EnumAnimationType.END_LOADED;
            else if (split[1].equalsIgnoreCase("CUSTOMEND_LOADED"))
                model.animationType = EnumAnimationType.CUSTOMEND_LOADED;
            else if (split[1].equalsIgnoreCase("RIFLE_TOP"))
                model.animationType = EnumAnimationType.RIFLE_TOP;
            else if (split[1].equalsIgnoreCase("CUSTOMRIFLE_TOP"))
                model.animationType = EnumAnimationType.CUSTOMRIFLE_TOP;
            else if (split[1].equalsIgnoreCase("BULLPUP"))
                model.animationType = EnumAnimationType.BULLPUP;
            else if (split[1].equalsIgnoreCase("CUSTOMBULLPUP"))
                model.animationType = EnumAnimationType.CUSTOMBULLPUP;
            else if (split[1].equalsIgnoreCase("ALT_PISTOL_CLIP"))
                model.animationType = EnumAnimationType.ALT_PISTOL_CLIP;
            else if (split[1].equalsIgnoreCase("CUSTOMALT_PISTOL_CLIP"))
                model.animationType = EnumAnimationType.CUSTOMALT_PISTOL_CLIP;
            else if (split[1].equalsIgnoreCase("GENERIC"))
                model.animationType = EnumAnimationType.GENERIC;
            else if (split[1].equalsIgnoreCase("CUSTOMGENERIC"))
                model.animationType = EnumAnimationType.CUSTOMGENERIC;
            else if (split[1].equalsIgnoreCase("BACK_LOADED"))
                model.animationType = EnumAnimationType.BACK_LOADED;
            else if (split[1].equalsIgnoreCase("CUSTOMBACK_LOADED"))
                model.animationType = EnumAnimationType.CUSTOMBACK_LOADED;
            else if (split[1].equalsIgnoreCase("STRIKER"))
                model.animationType = EnumAnimationType.STRIKER;
            else if (split[1].equalsIgnoreCase("CUSTOMSTRIKER"))
                model.animationType = EnumAnimationType.CUSTOMSTRIKER;
            else if (split[1].equalsIgnoreCase("BREAK_ACTION"))
                model.animationType = EnumAnimationType.BREAK_ACTION;
            else if (split[1].equalsIgnoreCase("CUSTOMBREAK_ACTION"))
                model.animationType = EnumAnimationType.CUSTOMBREAK_ACTION;
            else if (split[1].equalsIgnoreCase("CUSTOM"))
                model.animationType = EnumAnimationType.CUSTOM;
        }

        split = ConfigUtils.getSplitFromKey(config, "animMeleeAnimation");
        if (split != null) {
            if (split[1].equalsIgnoreCase("DEFAULT"))
                model.meleeAnimation = EnumMeleeAnimation.DEFAULT;
            else if (split[1].equalsIgnoreCase("NONE"))
                model.meleeAnimation = EnumMeleeAnimation.NONE;
            else if (split[1].equalsIgnoreCase("BLUNT_SWING"))
                model.meleeAnimation = EnumMeleeAnimation.BLUNT_SWING;
            else if (split[1].equalsIgnoreCase("BLUNT_BASH"))
                model.meleeAnimation = EnumMeleeAnimation.BLUNT_BASH;
            else if (split[1].equalsIgnoreCase("STAB_UNDERARM"))
                model.meleeAnimation = EnumMeleeAnimation.STAB_UNDERARM;
            else if (split[1].equalsIgnoreCase("STAB_OVERARM"))
                model.meleeAnimation = EnumMeleeAnimation.STAB_OVERARM;
        }

        model.tiltGunTime = ConfigUtils.configFloat(config, "animTiltGunTime", model.tiltGunTime);
        model.unloadClipTime = ConfigUtils.configFloat(config, "animUnloadClipTime", model.unloadClipTime);
        model.loadClipTime = ConfigUtils.configFloat(config, "animLoadClipTime", model.loadClipTime);

        model.scopeIsOnSlide = ConfigUtils.configBool(config, "animScopeIsOnSlide", model.scopeIsOnSlide);
        model.scopeIsOnBreakAction = ConfigUtils.configBool(config, "animScopeIsOnBreakAction", model.scopeIsOnBreakAction);

        model.numBulletsInReloadAnimation = ConfigUtils.configFloat(config, "animNumBulletsInReloadAnimation", model.numBulletsInReloadAnimation);
        model.pumpDelay = ConfigUtils.configInt(config, "animPumpDelay", model.pumpDelay);
        model.pumpDelayAfterReload = ConfigUtils.configInt(config, "animPumpDelayAfterReload", model.pumpDelayAfterReload);
        model.pumpTime = ConfigUtils.configInt(config, "animPumpTime", model.pumpTime);
        model.hammerDelay = ConfigUtils.configInt(config, "animHammerDelay", model.hammerDelay);

        model.pumpHandleDistance = ConfigUtils.configFloat(config, "animPumpHandleDistance", model.pumpHandleDistance);
        model.endLoadedAmmoDistance = ConfigUtils.configFloat(config, "animEndLoadedAmmoDistance", model.endLoadedAmmoDistance);
        model.breakActionAmmoDistance = ConfigUtils.configFloat(config, "animBreakActionAmmoDistance", model.breakActionAmmoDistance);
        model.scopeIsOnBreakAction = ConfigUtils.configBool(config, "animScopeIsOnBreakAction", model.scopeIsOnBreakAction);

        model.gripIsOnPump = ConfigUtils.configBool(config, "animGripIsOnPump", model.gripIsOnPump);
        model.gripIsOnPump = ConfigUtils.configBool(config, "animGadgetsOnPump", model.gripIsOnPump);

        model.barrelBreakPoint = ConfigUtils.configVector(config, "animBarrelBreakPoint", model.barrelBreakPoint);
        model.altbarrelBreakPoint = ConfigUtils.configVector(config, "animAltBarrelBreakPoint", model.altbarrelBreakPoint);

        model.revolverFlipAngle = ConfigUtils.configFloat(config, "animRevolverFlipAngle", model.revolverFlipAngle);
        model.revolver2FlipAngle = ConfigUtils.configFloat(config, "animRevolver2FlipAngle", model.revolver2FlipAngle);

        model.revolverFlipPoint = ConfigUtils.configVector(config, "animRevolverFlipPoint", model.revolverFlipPoint);
        model.revolver2FlipPoint = ConfigUtils.configVector(config, "animRevolver2FlipPoint", model.revolver2FlipPoint);

        model.breakAngle = ConfigUtils.configFloat(config, "animBreakAngle", model.breakAngle);
        model.altbreakAngle = ConfigUtils.configFloat(config, "animAltBreakAngle", model.altbreakAngle);

        model.spinningCocking = ConfigUtils.configBool(config, "animSpinningCocking", model.spinningCocking);

        model.spinPoint = ConfigUtils.configVector(config, "animSpinPoint", model.spinPoint);
        model.hammerSpinPoint = ConfigUtils.configVector(config, "animHammerSpinPoint", model.hammerSpinPoint);
        model.althammerSpinPoint = ConfigUtils.configVector(config, "animAltHammerSpinPoint", model.althammerSpinPoint);
        model.hammerAngle = ConfigUtils.configFloat(config, "animHammerAngle", model.hammerAngle);
        model.althammerAngle = ConfigUtils.configFloat(config, "animAltHammerAngle", model.althammerAngle);

        model.isSingleAction = ConfigUtils.configBool(config, "animIsSingleAction", model.isSingleAction);
        model.slideLockOnEmpty = ConfigUtils.configBool(config, "animSlideLockOnEmpty", model.slideLockOnEmpty);
        model.lefthandPump = ConfigUtils.configBool(config, "animLeftHandPump", model.lefthandPump);
        model.righthandPump = ConfigUtils.configBool(config, "animRightHandPump", model.righthandPump);
        model.leftHandCharge = ConfigUtils.configBool(config, "animLeftHandCharge", model.leftHandCharge);
        model.rightHandCharge = ConfigUtils.configBool(config, "animRightHandCharge", model.rightHandCharge);
        model.leftHandBolt = ConfigUtils.configBool(config, "animLeftHandBolt", model.leftHandBolt);
        model.rightHandBolt = ConfigUtils.configBool(config, "animRightHandBolt", model.rightHandBolt);

        model.pumpModifier = ConfigUtils.configFloat(config, "animPumpModifier", model.pumpModifier);
        model.chargeModifier = ConfigUtils.configVector(config, "animChargeModifier", model.chargeModifier);
        model.gunOffset = ConfigUtils.configFloat(config, "animGunOffset", model.gunOffset);
        model.crouchZoom = ConfigUtils.configFloat(config, "animCrouchZoom", model.crouchZoom);
        model.fancyStance = ConfigUtils.configBool(config, "animFancyStance", model.fancyStance);
        model.stanceTranslate = ConfigUtils.configVector(config, "animTranslateClip", model.stanceTranslate);
        model.stanceRotate = ConfigUtils.configVector(config, "animStanceRotate", model.stanceRotate);

        model.rotateGunVertical = ConfigUtils.configFloat(config, "animRotateGunVertical", model.rotateGunVertical);
        model.rotateGunHorizontal = ConfigUtils.configFloat(config, "animRotateGunHorizontal", model.rotateGunHorizontal);
        model.tiltGun = ConfigUtils.configFloat(config, "animTiltGun", model.tiltGun);
        model.translateGun = ConfigUtils.configVector(config, "animTranslateGun", model.translateGun);
        model.rotateClipVertical = ConfigUtils.configFloat(config, "animRotateClipVertical", model.rotateClipVertical);
        model.stagedrotateClipVertical = ConfigUtils.configFloat(config, "animStagedRotateClipVertical", model.stagedrotateClipVertical);
        model.rotateClipVertical = ConfigUtils.configFloat(config, "animRotateClipHorizontal", model.rotateClipVertical);
        model.stagedrotateClipVertical = ConfigUtils.configFloat(config, "animStagedRotateClipHorizontal", model.stagedrotateClipVertical);
        model.tiltClip = ConfigUtils.configFloat(config, "animTiltClip", model.tiltClip);
        model.stagedtiltClip = ConfigUtils.configFloat(config, "animStagedTiltClip", model.stagedtiltClip);
        model.translateClip = ConfigUtils.configVector(config, "animTranslateClip", model.translateClip);
        model.stagedtranslateClip = ConfigUtils.configVector(config, "animStagedTranslateClip", model.stagedtranslateClip);
        model.stagedReload = ConfigUtils.configBool(config, "animStagedReload", model.stagedReload);

        model.thirdPersonOffset = ConfigUtils.configVector(config, "animThirdPersonOffset", model.thirdPersonOffset);
        model.itemFrameOffset = ConfigUtils.configVector(config, "animItemFrameOffset", model.itemFrameOffset);
        model.stillRenderGunWhenScopedOverlay = ConfigUtils.configBool(config, "animStillRenderGunWhenScopedOverlay", model.stillRenderGunWhenScopedOverlay);
        model.adsEffectMultiplier = ConfigUtils.configFloat(config, "animAdsEffectMultiplier", model.adsEffectMultiplier);
    }

    /**
     * Used only for driveables
     */
    public boolean isAmmo(ShootableType type) {
        return ammo.contains(type);
    }

    public boolean isAmmo(ShootableType type, ItemStack stack) {
        boolean result = ammo.contains(type);

        if (getGrip(stack) != null && getSecondaryFire(stack)) {
            List<ShootableType> t = new ArrayList<>();
            for (String s : getGrip(stack).secondaryAmmo) {
                ShootableType shoot = ShootableType.getShootableType(s);
                if (type != null)
                    t.add(shoot);
            }
            result = t.contains(type);
        }

        return result;
    }

    public ShootableType getDefaultAmmo() {
        if (!ammo.isEmpty()) {
            return ammo.get(0);
        }
        return null;
    }

    public boolean getOneHanded() {
        return !FlansMod.masterDualWieldDisable && oneHanded;
    }

    public boolean isAmmo(ItemStack stack) {
        if (stack == null)
            return false;
        else if (stack.getItem() instanceof ItemBullet) {
            return isAmmo(((ItemBullet) stack.getItem()).type, stack);
        } else if (stack.getItem() instanceof ItemGrenade) {
            return isAmmo(((ItemGrenade) stack.getItem()).type, stack);
        }
        return false;
    }

    /**
     * To be overriden by subtypes for model reloading
     */
    public void reloadModel() {
        model = FlansMod.proxy.loadModel(modelString, shortName, ModelGun.class);
        deployableModel = FlansMod.proxy.loadModel(deployableModelString, shortName, ModelMG.class);
        casingModel = FlansMod.proxy.loadModel(casingModelString, shortName, ModelCasing.class);
        flashModel = FlansMod.proxy.loadModel(flashModelString, shortName, ModelFlash.class);
    }

    @Override
    public float getZoomFactor() {
        return zoomLevel;
    }

    @Override
    public boolean hasZoomOverlay() {
        return hasScopeOverlay;
    }

    @Override
    public String getZoomOverlay() {
        return defaultScopeTexture;
    }

    @Override
    public float getFOVFactor() {
        return FOVFactor;
    }

    //ItemStack specific methods

    /**
     * Return the currently active scope on this gun. Search attachments, and by default, simply give the gun
     */
    public IScope getCurrentScope(ItemStack gunStack) {
        IScope attachedScope = getScope(gunStack);
        return attachedScope == null ? this : attachedScope;
    }

    /**
     * Returns all attachments currently attached to the specified gun
     */
    public ArrayList<AttachmentType> getCurrentAttachments(ItemStack gun) {
        checkForTags(gun);
        ArrayList<AttachmentType> attachments = new ArrayList<AttachmentType>();
        NBTTagCompound attachmentTags = gun.stackTagCompound.getCompoundTag("attachments");
        NBTTagList genericsList = attachmentTags.getTagList("generics", (byte) 10); //TODO : Check this 10 is correct
        for (int i = 0; i < numGenericAttachmentSlots; i++) {
            appendToList(gun, "generic_" + i, attachments);
        }
        appendToList(gun, "barrel", attachments);
        appendToList(gun, "scope", attachments);
        appendToList(gun, "stock", attachments);
        appendToList(gun, "grip", attachments);
        appendToList(gun, "gadget", attachments);
        appendToList(gun, "slide", attachments);
        appendToList(gun, "pump", attachments);
        appendToList(gun, "accessory", attachments);
        return attachments;
    }

    /**
     * Private method for attaching attachments to a list of attachments with a null check
     */
    private void appendToList(ItemStack gun, String name, ArrayList<AttachmentType> attachments) {
        AttachmentType type = getAttachment(gun, name);
        if (type != null) attachments.add(type);
    }

    //Attachment getter methods
    public AttachmentType getBarrel(ItemStack gun) { return getAttachment(gun, "barrel"); }
    public AttachmentType getScope(ItemStack gun) {
        return getAttachment(gun, "scope");
    }
    public AttachmentType getStock(ItemStack gun) {
        return getAttachment(gun, "stock");
    }
    public AttachmentType getGrip(ItemStack gun) {
        return getAttachment(gun, "grip");
    }
    public AttachmentType getGadget(ItemStack gun) {
        return getAttachment(gun, "gadget");
    }
    public AttachmentType getSlide(ItemStack gun) {
        return getAttachment(gun, "slide");
    }
    public AttachmentType getPump(ItemStack gun) {
        return getAttachment(gun, "pump");
    }
    public AttachmentType getAccessory(ItemStack gun) {
        return getAttachment(gun, "accessory");
    }
    public AttachmentType getGeneric(ItemStack gun, int i) {
        return getAttachment(gun, "generic_" + i);
    }

    //Attachment ItemStack getter methods
    public ItemStack getBarrelItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "barrel");
    }
    public ItemStack getScopeItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "scope");
    }
    public ItemStack getStockItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "stock");
    }
    public ItemStack getGripItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "grip");
    }
    public ItemStack getGadgetItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "gadget");
    }
    public ItemStack getSlideItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "slide");
    }
    public ItemStack getPumpItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "pump");
    }
    public ItemStack getAccessoryItemStack(ItemStack gun) {
        return getAttachmentItemStack(gun, "accessory");
    }
    public ItemStack getGenericItemStack(ItemStack gun, int i) {
        return getAttachmentItemStack(gun, "generic_" + i);
    }

    /**
     * Generalised attachment getter method
     */
    public AttachmentType getAttachment(ItemStack gun, String name) {
        if (gun == null || !(gun.getItem() instanceof ItemGun)) {
            return null;
        }
        checkForTags(gun);
        return AttachmentType.getFromNBT(gun.stackTagCompound.getCompoundTag("attachments").getCompoundTag(name));
    }

    /**
     * Generalised attachment ItemStack getter method
     */
    public ItemStack getAttachmentItemStack(ItemStack gun, String name) {
        checkForTags(gun);
        return ItemStack.loadItemStackFromNBT(gun.stackTagCompound.getCompoundTag("attachments").getCompoundTag(name));
    }

    /**
     * Method to check for null tags and assign default empty tags in that case
     */
    public void checkForTags(ItemStack gun) {
        //If the gun has no tags, give it some
        if (!gun.hasTagCompound()) {
            gun.stackTagCompound = new NBTTagCompound();
        }
        //If the gun has no attachment tags, give it some
        if (!gun.stackTagCompound.hasKey("attachments")) {
            NBTTagCompound attachmentTags = new NBTTagCompound();
            for (int i = 0; i < numGenericAttachmentSlots; i++)
                attachmentTags.setTag("generic_" + i, new NBTTagCompound());
            attachmentTags.setTag("barrel", new NBTTagCompound());
            attachmentTags.setTag("scope", new NBTTagCompound());
            attachmentTags.setTag("stock", new NBTTagCompound());
            attachmentTags.setTag("grip", new NBTTagCompound());
            attachmentTags.setTag("gadget", new NBTTagCompound());
            attachmentTags.setTag("slide", new NBTTagCompound());
            attachmentTags.setTag("pump", new NBTTagCompound());
            attachmentTags.setTag("accessory", new NBTTagCompound());

            gun.stackTagCompound.setTag("attachments", attachmentTags);
        }
    }

    /**
     * Get the melee damage of a specific gun, taking into account attachments
     */
    public float getMeleeDamage(ItemStack stack, boolean driveable) {
        float stackMeleeDamage = meleeDamage;
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackMeleeDamage *= attachment.meleeDamageMultiplier;
        }
        return stackMeleeDamage * (driveable ? meleeDamageDriveableModifier : 1);
    }

    /**
     * Get the damage of a specific gun, taking into account attachments
     */
    public float getDamage(ItemStack stack) {
        float stackDamage = damage;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackDamage = getGrip(stack).secondaryDamage;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackDamage *= attachment.damageMultiplier;
        }

        return stackDamage * FlansMod.masterDamageModifier;
    }

    /**
     * Get the bullet spread of a specific gun, taking into account attachments
     */
    public float getSpread(ItemStack stack, boolean sneaking, boolean sprinting) {
        float stackSpread = bulletSpread;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackSpread = getGrip(stack).secondarySpread;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackSpread *= attachment.spreadMultiplier;
        }
        if (sprinting) {
            stackSpread *= sprintSpreadModifier;
        } else if (sneaking) {
            stackSpread *= sneakSpreadModifier;
        }
        return stackSpread;
    }

    /**
     * Get the default spread of a specific gun, taking into account attachments
     */
    public float getDefaultSpread(ItemStack stack) {
        float stackSpread = defaultSpread;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackSpread = getGrip(stack).secondaryDefaultSpread;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackSpread *= attachment.spreadMultiplier;
        }
        return stackSpread;
    }

    /**
     * Get the pitch recoil mean, without any randomness but including attachments.
     */
    public float getRecoilDisplay(ItemStack stack) {
        float stackRecoil = this.recoilPitch;
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackRecoil *= attachment.recoilMultiplier;
        }
        return stackRecoil;
    }

    /**
     * Get the pitch recoil of a specific gun, taking into account attachments, randomess and sneak/sprint
     */
    public float getRecoilPitch(ItemStack stack, boolean sneaking, boolean sprinting) {
        float stackRecoil = this.recoilPitch + (rand.nextFloat() * this.rndRecoilPitchRange);
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackRecoil *= attachment.recoilMultiplier;
        }

        if (sneaking) {
            if (decreaseRecoilPitch != 0) {
                // backwards compatibility
                stackRecoil -= decreaseRecoilPitch;
            } else if (recoilSneakingMultiplier == -1) {
                // backwards compatibility 2: simulate decreaseRecoilPitch 2
                stackRecoil = stackRecoil < 0.5F ? 0 : stackRecoil - 0.5F;
            } else {
                stackRecoil *= recoilSneakingMultiplier;
            }
        } else if (sprinting) {
            stackRecoil *= recoilSprintingMultiplier;
        }

        return stackRecoil * FlansMod.masterRecoilModifier;
    }


    /**
     * Get the yaw recoil of a specific gun, taking into account attachments, randomess and sneak/sprint
     */
    public float getRecoilYaw(ItemStack stack, boolean sneaking, boolean sprinting) {
        float stackRecoilYaw = this.recoilYaw + ((rand.nextFloat() - 0.5F) * this.rndRecoilYawRange);
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackRecoilYaw *= attachment.recoilMultiplier;
        }

        if (sneaking) {
            if (decreaseRecoilYaw < 0) {
                stackRecoilYaw /= decreaseRecoilYaw;
            } else {
                stackRecoilYaw *= recoilSneakingMultiplierYaw;
            }
        } else if (sprinting) {
            stackRecoilYaw *= recoilSprintingMultiplierYaw;
        }

        return stackRecoilYaw * FlansMod.masterRecoilModifier;
    }


    /**
     * Get the bullet speed of a specific gun, taking into account attachments
     */
    public float getBulletSpeed(ItemStack stack, ItemStack bulletStack) {
        float stackBulletSpeed;
        if (bulletStack != null && bulletStack.getItem() != null && bulletStack.getItem() instanceof ItemBullet) {
            stackBulletSpeed = bulletSpeed * ((ItemBullet) bulletStack.getItem()).type.speedMultiplier;
        } else {
            stackBulletSpeed = bulletSpeed;
        }

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackBulletSpeed = getGrip(stack).secondarySpeed;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackBulletSpeed *= attachment.bulletSpeedMultiplier;
        }
        return stackBulletSpeed;
    }

    /**
     * Get the bullet speed of a specific gun, taking into account attachments
     */
    public float getBulletSpeed(ItemStack stack) {

        float stackBulletSpeed = bulletSpeed;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackBulletSpeed = getGrip(stack).secondarySpeed;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackBulletSpeed *= attachment.bulletSpeedMultiplier;
        }
        return stackBulletSpeed;
    }

    /**
     * Get the reload time of a specific gun, taking into account attachments
     */
    public float getReloadTime(ItemStack stack) {
        float stackReloadTime = reloadTime;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            stackReloadTime = getGrip(stack).secondaryReloadTime;

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackReloadTime *= attachment.reloadTimeMultiplier;
        }
        return stackReloadTime;
    }

    /**
     * Get the fire rate of a specific gun
     */
    public float getShootDelay(ItemStack stack) {
        float fireRate;
        if (roundsPerMin != 0) {
            fireRate = roundsPerMin;
            if (getGrip(stack) != null && getSecondaryFire(stack)) {
                fireRate = getGrip(stack).secondaryShootDelay;
            }

            return 1200 / fireRate;
        } else if (shootDelay != 0) {
            fireRate = shootDelay;
            if (getGrip(stack) != null && getSecondaryFire(stack)) {
                fireRate = getGrip(stack).secondaryShootDelay;
            }

            return fireRate;
        } else {
            return DEFAULT_SHOOT_DELAY;
        }


    }

    public float getShootDelay() {
        if (roundsPerMin != 0) {
            return 1200 / roundsPerMin;
        } else if (shootDelay != 0) {
            return shootDelay;
        } else {
            return DEFAULT_SHOOT_DELAY;
        }
    }

    /**
     * Get the number of bullets fired per shot of a specific gun
     */
    public int getNumBullets(ItemStack stack) {
        int amount = numBullets;

        if (getGrip(stack) != null && getSecondaryFire(stack))
            amount = getGrip(stack).secondaryNumBullets;

        return amount;
    }

    /**
     * Get the movement speed of a specific gun, taking into account attachments
     */
    public float getMovementSpeed(ItemStack stack) {
        float stackMovement = moveSpeedModifier;
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackMovement *= attachment.moveSpeedMultiplier;
        }
        return stackMovement;
    }

    /**
     * Get the recoil counter coefficient of the gun, taking into account attachments
     */
    public float getRecoilControl(ItemStack stack, boolean isSprinting, boolean isSneaking) {
        float control;
        if (isSprinting) {
            control = recoilCounterCoefficientSprinting;
        } else if (isSneaking) {
            control = recoilCounterCoefficientSneaking;
        } else {
            control = recoilCounterCoefficient;
        }

        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            if (isSprinting) {
                control *= attachment.recoilControlMultiplierSprinting;
            } else if (isSneaking) {
                control *= attachment.recoilControlMultiplierSneaking;
            } else {
                control *= attachment.recoilControlMultiplier;
            }
        }

        // Clamp to [0, 1]
        return Math.max(0, Math.min(1, control));
    }

    public void setFireMode(ItemStack stack, int fireMode) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        if (fireMode < EnumFireMode.values().length) {
            stack.getTagCompound().setByte("GunMode", (byte) fireMode);
        } else {
            stack.getTagCompound().setByte("GunMode", (byte) mode.ordinal());
        }
    }

    /**
     * Get the firing mode of a specific gun, taking into account attachments and secondary fire mode
     */
    public EnumFireMode getFireMode(ItemStack stack) {
        //Check for secondary fire mode
        if (getGrip(stack) != null && getSecondaryFire(stack))
            return getGrip(stack).secondaryFireMode;

        //Else check for any mode overrides from attachments
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            if (attachment.modeOverride != null)
                return attachment.modeOverride;
        }

        //Else set the fire mode from the gun
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("GunMode")) {
            int gm = stack.getTagCompound().getByte("GunMode");
            if (gm < EnumFireMode.values().length) {
                for (EnumFireMode enumFireMode : submode) {
                    if (gm == enumFireMode.ordinal()) {
                        return EnumFireMode.values()[gm];
                    }
                }
            }
        }

        setFireMode(stack, mode.ordinal());
        return mode;
    }

    /**
     * Set the secondary or primary fire mode
     */
    public void setSecondaryFire(ItemStack stack, boolean mode) {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());

        stack.stackTagCompound.setBoolean("secondaryFire", mode);
    }

    /**
     * Get whether the gun is in secondary or primary fire mode
     */
    public boolean getSecondaryFire(ItemStack stack) {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());

        if (!stack.getTagCompound().hasKey("secondaryFire")) {
            stack.stackTagCompound.setBoolean("secondaryFire", false);
            return stack.getTagCompound().getBoolean("secondaryFire");
        }

        return stack.getTagCompound().getBoolean("secondaryFire");
    }

    /**
     * Get the max size of ammo items depending on what mode the gun is in
     */
    public int getNumAmmoItemsInGun(ItemStack stack) {
        if (getGrip(stack) != null && getSecondaryFire(stack))
            return getGrip(stack).numSecAmmoItems;
        else
            return numPrimaryAmmoItems;
    }

    /**
     * Static String to GunType method
     */
    public static GunType getGun(String s) {
        return guns.get(s);
    }

    public Paintjob getPaintjob(String s) {
        for (Paintjob paintjob : paintjobs) {
            if (paintjob.iconName.equals(s))
                return paintjob;
        }
        return defaultPaintjob;
    }

    public boolean getShouldShowMuzzleFlash() {
        return useMuzzleFlashDefaults ? FlansMod.showMuzzleFlashParticlesDefault : showMuzzleFlashParticles;
    }

    @Override
    public float GetRecommendedScale() {
        return 60.0f;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBase GetModel() {
        return model;
    }

    public static class GunRecoil {
        public static final Random rand = new Random();
        public float vertical;
        public float horizontal;

        //how fast the upwards motion stops after shooting. lower is better. 0-2, default 1
        public float recovery;
        //modifies how much scoping modifies recovery (usually 20%). default 1%, lower is better, 0-2
        public float recoveryScope;
        //how much the aim goes back down after shooting. lower is better. 0-2, default 1f
        public float fall;
        //how much spraying increases recoil over time. lower is better
        public float increase;
        //modifies how much sneaking modifies recovery (usually 10%). default 1%, lower is better, 0-2
        public float sneak;
        //how much walking speed affects recoil. lower is bettr
        public float speed;

        public float sprayLength;
        public float antiRecoil;

        public GunRecoil(float vertical, float horizontal, float recovery, float recoveryScope,
                         float fall, float increase, float sneak, float speed) {
            this.vertical = vertical;
            this.horizontal = horizontal;
            this.recovery = recovery;
            this.recoveryScope = recoveryScope;
            this.fall = fall;
            this.increase = increase;
            this.sneak = sneak;
            this.speed = speed;
        }

        //create empty recoil
        public GunRecoil() {
            this(0, 0, 0, 0, 0, 0, 0, 0);
        }

        //copy
        public GunRecoil(GunRecoil gunRecoil) {
            this(gunRecoil.vertical, gunRecoil.horizontal, gunRecoil.recovery, gunRecoil.recoveryScope,
                    gunRecoil.fall, gunRecoil.increase, gunRecoil.sneak, gunRecoil.speed);
        }

        public GunRecoil read(String[] split) {
            vertical = read(split, 1, vertical);
            horizontal = read(split, 2, horizontal);
            recovery = read(split, 3, recovery);
            recoveryScope = read(split, 4, recoveryScope);
            fall = read(split, 5, fall);
            increase = read(split, 6, increase);
            sneak = read(split, 7, sneak);
            speed = read(split, 8, speed);

            if (split.length < 2) {
                horizontal = vertical * 0.3f;
            }
            return this;
        }

        private float read(String[] split, int i, float alt) {
            return split.length > i ? Float.parseFloat(
                    split[i].indexOf('=') == -1 ? split[i] : split[i].substring(split[i].indexOf('=') + 1))
                    : alt;
        }

        public GunRecoil copy() {
            //
            //  if (horizontal == 2 && recovery == 1 && recoveryScope == 1 && sneak == 1 && speed == 1) {

            String rc = "Recoil 1.9 horizontal=1.9 recovery=1 recoveryScope=1 fall=0.9 increase=2 sneak=1 speed=1.2";
            //   if(true)
            //     return new GunRecoil().read(rc.split(" "));
            //     {}

            //     }

            return new GunRecoil(this);
        }

        public void applyModifier(float recoilMultiplier) {
            vertical *= recoilMultiplier;
            horizontal *= recoilMultiplier;
        }

        public void addRecoil(GunRecoil recoil) {
            this.vertical += recoil.vertical;
            this.horizontal += 0.2f * (rand.nextBoolean() ? -1 : 1)
                    * recoil.horizontal;
            this.recovery = recoil.recovery;
            this.recoveryScope = recoil.recoveryScope;
            this.increase = recoil.increase;
            this.sneak = recoil.sneak;
            this.fall = recoil.fall;
            this.speed = recoil.speed;

            this.sprayLength += 0.05;
            vertical *= (1 + sprayLength * 2 * recoil.increase);
            horizontal *= (1 + sprayLength * 2 * recoil.increase);

            antiRecoil *= rand.nextFloat() * 0.1f;
        }

        public float update(boolean sneaking, boolean scoping, float playerSpeed) {

            float recov = 0.5F * recovery;
            if (sneaking) {
                recov *= 0.9f * sneak;
            }
            if (scoping) {
                recov *= 0.8f * recoveryScope;
            }

            if (vertical > 0) {
                vertical *= recov;
            }
            if (horizontal != 0) {
                horizontal *= recov;
            }

            sprayLength *= 0.95f;

            if (playerSpeed > 0.00f) {
                float speedMod = (1 + playerSpeed * speed);
                vertical *= speedMod;
                horizontal *= speedMod;
            }

            float anti = antiRecoil * 0.2f;

            antiRecoil *= 0.8F;
            antiRecoil += vertical * Math.max(0, Math.min(1, 1 - rand.nextFloat() * 0.2f - (fall - 1)));

            float add = -vertical + anti;
            return add;
        }
    }
    public GunRecoil getRecoil(ItemStack stack) {
        GunRecoil stackRecoil = recoil.copy();
        for (AttachmentType attachment : getCurrentAttachments(stack)) {
            stackRecoil.applyModifier(attachment.recoilMultiplier);
        }
        return stackRecoil;
    }
    /**Variable Zoom*/
    @Override
    public float getMinZoom(){
        return hasVariableZoom?minZoom:-1F;
    }
    @Override
    public float getMaxZoom(){
        return hasVariableZoom?maxZoom:-1F;
    }
    @Override
    public float getZoomAugment(){
        return hasVariableZoom?zoomAugment:-1F;
    }
    @Override
    public boolean hasVariableZoom() {
        return hasVariableZoom;
    }
}
