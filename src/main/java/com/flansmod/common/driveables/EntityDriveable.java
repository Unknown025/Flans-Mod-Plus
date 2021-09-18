package com.flansmod.common.driveables;

import cofh.api.energy.IEnergyContainerItem;
import com.flansmod.api.IControllable;
import com.flansmod.api.IExplodeable;
import com.flansmod.client.EntityCamera;
import com.flansmod.client.FlansModClient;
import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.common.FlansMod;
import com.flansmod.common.RotatedAxes;
import com.flansmod.common.driveables.DriveableType.ParticleEmitter;
import com.flansmod.common.driveables.DriveableType.ShootParticle;
import com.flansmod.common.driveables.collisions.CollisionPlane;
import com.flansmod.common.driveables.collisions.CollisionShapeBox;
import com.flansmod.common.driveables.collisions.CollisionTest;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.*;
import com.flansmod.common.guns.raytracing.BulletHit;
import com.flansmod.common.guns.raytracing.DriveableHit;
import com.flansmod.common.network.PacketDriveableDamage;
import com.flansmod.common.network.PacketDriveableKeyHeld;
import com.flansmod.common.network.PacketParticle;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.vector.Vector3f;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

import java.util.ArrayList;

public abstract class EntityDriveable extends Entity implements IControllable, IExplodeable, IEntityAdditionalSpawnData {
    public boolean syncFromServer = true;
    /**
     * Ticks since last server update. Use to smoothly transition to new position
     */
    public int serverPositionTransitionTicker;
    /**
     * Server side position, as synced by PacketVehicleControl packets
     */
    public double serverPosX, serverPosY, serverPosZ;
    /**
     * Server side rotation, as synced by PacketVehicleControl packets
     */
    public double serverYaw, serverPitch, serverRoll;

    /**
     * The driveable data which contains the inventory, the engine and the fuel
     */
    public DriveableData driveableData;
    /**
     * The shortName of the driveable type, used to obtain said type
     */
    public String driveableType;

    /**
     * The throttle, in the range -1, 1 is multiplied by the maxThrottle (or maxNegativeThrottle) from the plane type to obtain the thrust
     */
    public float throttle;
    /**
     * The wheels on this plane
     */
    public EntityWheel[] wheels;

    public boolean fuelling;
    /**
     * Extra prevRoation field for smoothness in all 3 rotational axes
     */
    public float prevRotationRoll;
    /**
     * Angular velocity
     */
    public Vector3f angularVelocity = new Vector3f(0F, 0F, 0F);

    /**
     * Whether each mouse button is held
     */
    public boolean leftMouseHeld = false, rightMouseHeld = false;

    /**
     * Shoot delay variables
     */
    public float shootDelayPrimary, shootDelaySecondary;
    /**
     * Minigun speed variables
     */
    public float minigunSpeedPrimary, minigunSpeedSecondary;
    /**
     * Current gun variables for alternating weapons.
     */
    public int currentGunPrimary, currentGunSecondary;

    /**
     * Angle of harvester aesthetic piece
     */
    public float harvesterAngle;

    public RotatedAxes prevAxes;
    public RotatedAxes axes;

    public EntitySeat[] seats;

    public int lockOnSoundDelay;

    private int[] emitterTimers;

    public int animCountLeft = 0;
    public int animFrameLeft = 0;
    public int animCountRight = 0;
    public int animFrameRight = 0;

    public boolean leftTurnHeld = false;
    public boolean rightTurnHeld = false;


    public boolean isShowedPosition = false;

    public int tickCount = 0;

    //Gun recoil
    public boolean isRecoil = false;
    public float recoilPos = 0;
    public float lastRecoilPos = 0;
    public int recoilTimer = 0;

    public Vector3f lastPos = new Vector3f(0, 0, 0);
    public boolean hugeBoat = false;
    public boolean onDeck = false;
    public double deckHeight = 0;
    public int deckCheck = 0;
    public int prevDeckCheck = 0;

    public boolean isMecha = false;
    public boolean disabled = false;

    /**
     * The angle of the propeller for the renderer
     */
    public float propAngle = 0;
    public float prevPropAngle = 0;

    public float rotorAngle = 0;
    public float prevRotorAngle = 0;

    //Flares
    public int flareDelay = 0;
    public int ticksFlareUsing = 0;
    public boolean varFlare;

    //IT1 stuff
    public float drakonDoorAngle = 0;
    public float drakonArmAngle = 0;
    public float drakonRailAngle = 0;

    public float prevDrakonDoorAngle = 0;
    public float prevDrakonArmAngle = 0;
    public float prevDrakonRailAngle = 0;

    public boolean reloadingDrakon = false;
    public boolean canFireIT1 = true;

    public int stage = 1;
    public int reloadAnimTime = 0;

    public boolean toDeactivate = false;
    public int timeTillDeactivate = 0;

    //
    public boolean canFire = true;


    @SideOnly(Side.CLIENT)
    public EntityLivingBase camera;

    protected int invulnerableUnmountCount;

    private ItemStack[][] prevInventoryItems = new ItemStack[][]{null, null};

    public Entity lastAtkEntity = null;

    public Float collisionHardness = 0F;

    public int engineStartDelay;

    //public ArrayList<EntityPlayer> playerIDs = new ArrayList<EntityPlayer>();

    public EntityDriveable(World world) {
        super(world);
        axes = new RotatedAxes();
        prevAxes = new RotatedAxes();
        preventEntitySpawning = true;
        if (FlansMod.driveableHitboxes) {
            setSize(1F, 1F);
        } else {
            setSize(0.0F, 0.0F);
        }
        yOffset = 6F / 16F;
        ignoreFrustumCheck = true;
        renderDistanceWeight = 200D;
    }


    public EntityDriveable(World world, DriveableType t, DriveableData d) {
        this(world);
        driveableType = t.shortName;
        driveableData = d;
    }

    protected void initType(DriveableType type, boolean clientSide) {
        if (type == null) return;
        seats = new EntitySeat[type.numPassengers + 1];
        for (int i = 0; i < type.numPassengers + 1; i++) {
            if (!clientSide) {
                seats[i] = new EntitySeat(worldObj, this, i);
                worldObj.spawnEntityInWorld(seats[i]);
            }
        }
        wheels = new EntityWheel[type.wheelPositions.length];
        for (int i = 0; i < wheels.length; i++) {
            if (!clientSide) {
                wheels[i] = new EntityWheel(worldObj, this, i);
                worldObj.spawnEntityInWorld(wheels[i]);
            }
        }
        stepHeight = type.wheelStepHeight;
        yOffset = type.yOffset;

        emitterTimers = new int[type.emitters.size()];
        for (int i = 0; i < type.emitters.size(); i++) {
            emitterTimers[i] = rand.nextInt(type.emitters.get(i).emitRate);
        }

        getEntityData().setBoolean("CanMountEntity", type.canMountEntity);

        //Register Plane to Radar on Spawning
        //if(type.onRadar == true)
        //	RadarRegistry.register(this);

        for (int ps = 0; ps < 2; ps++) {
            EnumWeaponType weaponType = ps == 0 ? type.primary : type.secondary;
            if (weaponType == EnumWeaponType.GUN) {
                weaponType = EnumWeaponType.NONE;
            }
            int istart = getInventoryStart(weaponType);
            if (istart == driveableData.getAmmoInventoryStart()) {
                istart += type.numPassengerGunners;
            }
            final int isize = getInventorySize(weaponType);
            if (istart >= 0 || isize > 0) {
                prevInventoryItems[ps] = new ItemStack[isize];
                for (int i = 0; i < isize; i++) {
                    prevInventoryItems[ps][i] = driveableData.getStackInSlot(istart + i);
                }
            }
        }

        engineStartDelay = type.engineStartTime;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        driveableData.writeToNBT(tag);
        tag.setString("Type", driveableType);
        tag.setFloat("RotationYaw", axes.getYaw());
        tag.setFloat("RotationPitch", axes.getPitch());
        tag.setFloat("RotationRoll", axes.getRoll());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        driveableType = tag.getString("Type");
        driveableData = new DriveableData(tag);
        initType(DriveableType.getDriveable(driveableType), false);

        prevRotationYaw = tag.getFloat("RotationYaw");
        prevRotationPitch = tag.getFloat("RotationPitch");
        prevRotationRoll = tag.getFloat("RotationRoll");
        axes = new RotatedAxes(prevRotationYaw, prevRotationPitch, prevRotationRoll);
    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        ByteBufUtils.writeUTF8String(data, driveableType);

        NBTTagCompound tag = new NBTTagCompound();
        driveableData.writeToNBT(tag);
        ByteBufUtils.writeTag(data, tag);

        data.writeFloat(axes.getYaw());
        data.writeFloat(axes.getPitch());
        data.writeFloat(axes.getRoll());

        //Write damage
        for (EnumDriveablePart ep : EnumDriveablePart.values()) {
            DriveablePart part = getDriveableData().parts.get(ep);
            data.writeFloat(part.health);
            data.writeBoolean(part.onFire);
        }
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        try {
            driveableType = ByteBufUtils.readUTF8String(data);
            driveableData = new DriveableData(ByteBufUtils.readTag(data));
            initType(getDriveableType(), true);

            axes.setAngles(data.readFloat(), data.readFloat(), data.readFloat());
            prevRotationYaw = axes.getYaw();
            prevRotationPitch = axes.getPitch();
            prevRotationRoll = axes.getRoll();

            //Read damage
            for (EnumDriveablePart ep : EnumDriveablePart.values()) {
                DriveablePart part = getDriveableData().parts.get(ep);
                part.health = data.readFloat();
                part.onFire = data.readBoolean();
            }

        } catch (Exception e) {
            FlansMod.log("Failed to retrieve plane type from server.");
            super.setDead();
            e.printStackTrace();
        }

        camera = new EntityCamera(worldObj, this);
        worldObj.spawnEntityInWorld(camera);
    }

    /**
     * Called with the movement of the mouse. Used in controlling vehicles if need be.
     *
     * @param deltaY
     * @param deltaX
     * @return if mouse movement was handled.
     */
    @Override
    public abstract void onMouseMoved(int deltaX, int deltaY);

    @Override
    @SideOnly(Side.CLIENT)
    public EntityLivingBase getCamera() {
        return camera;
    }

    protected boolean canSit(int seat) {
        return getDriveableType().numPassengers >= seat && seats[seat].riddenByEntity == null;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
    }


    @Override
    public AxisAlignedBB getCollisionBox(Entity entity) {
        if (getDriveableType().collisionDamageEnable) {
            if (throttle > getDriveableType().collisionDamageThrottle) {
                if (entity instanceof EntityLiving) {
                    entity.attackEntityFrom(DamageSource.generic, throttle * getDriveableType().collisionDamageTimes);
                } else if (entity instanceof EntityPlayer) {
                    entity.attackEntityFrom(DamageSource.generic, throttle * getDriveableType().collisionDamageTimes);
                }
            }
        }
        return boundingBox;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return boundingBox;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public double getMountedYOffset() {
        return -0.3D;
    }

    /**
     * Pass generic damage to the core
     */
    @Override
    public boolean attackEntityFrom(DamageSource damagesource, float i) {
        if (worldObj.isRemote || isDead) return true;
//		if(damagesource.getDamageType().indexOf("explosion") < 0)
        {
            if (isMountedEntity(damagesource.getEntity())) {
                return false;
            }
        }

//		FlansMod.log(String.format("EntityDriveable.attackEntityFrom %.1f: %s : %s : %s", i,
//				damagesource.getDamageType(), damagesource.getEntity(), damagesource.getSourceOfDamage()));

        boolean broken = attackPart(EnumDriveablePart.core, damagesource, i);
        if (i > 0) {
            //checkParts();
            checkPartsWhenAttacked();
            //If it hit, send a damage update packet
            FlansMod.getPacketHandler().sendToAllAround(new PacketDriveableDamage(this), posX, posY, posZ, 100, dimension);
        }
        return true;
    }

    public boolean isMountedEntity(Entity entity) {
        if (entity != null) {
            Entity entity2 = this.worldObj.getEntityByID(entity.getEntityId());
            for (Entity seat : seats) {
                if (seat.riddenByEntity != null) {
                    if (seat.riddenByEntity == entity || seat.riddenByEntity == entity2) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void setDead() {
        super.setDead();

        //Unregister to Radar
        //RadarRegistry.unregister(this);
        if (worldObj.isRemote)
            camera.setDead();

        for (EntitySeat seat : seats)
            if (seat != null)
                seat.setDead();
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void applyEntityCollision(Entity entity) {
        //if(!isPartOfThis(entity))
        //super.applyEntityCollision(entity);
    }

    @Override
    public void setPositionAndRotation2(double d, double d1, double d2, float f, float f1, int i) {
        if (ticksExisted > 1)
            return;
        if (!(riddenByEntity instanceof EntityPlayer) || !FlansMod.proxy.isThePlayer((EntityPlayer) riddenByEntity)) {
            if (syncFromServer) {
                serverPositionTransitionTicker = i + 5;
            } else {
                double var10 = d - posX;
                double var12 = d1 - posY;
                double var14 = d2 - posZ;
                double var16 = var10 * var10 + var12 * var12 + var14 * var14;

                if (var16 <= 1.0D) {
                    return;
                }

                serverPositionTransitionTicker = 3;
            }
            serverPosX = d;
            serverPosY = d1;
            serverPosZ = d2;
            serverYaw = f;
            serverPitch = f1;
        }
    }

    public void setIT1(boolean canFire, boolean reloading, int stag, int stageTime) {
        if (worldObj.isRemote && ticksExisted % 5 == 0) {
            canFireIT1 = canFire;
            reloadingDrakon = reloading;
            stage = stag;
            reloadAnimTime = stageTime;
        }
    }

    public void setPositionRotationAndMotion(double x, double y, double z, float yaw, float pitch, float roll, double motX, double motY, double motZ, float velYaw, float velPitch, float velRoll, float throt, float steeringYaw) {
        if (worldObj.isRemote) {
            serverPosX = x;
            serverPosY = y;
            serverPosZ = z;
            serverYaw = yaw;
            serverPitch = pitch;
            serverRoll = roll;
            serverPositionTransitionTicker = 5;
        } else {
            setPosition(x, y, z);
            prevRotationYaw = yaw;
            prevRotationPitch = pitch;
            prevRotationRoll = roll;
            setRotation(yaw, pitch, roll);
        }
        //Set the motions regardless of side.
        motionX = motX;
        motionY = motY;
        motionZ = motZ;
        angularVelocity = new Vector3f(velYaw, velPitch, velRoll);
        throttle = throt;
    }


    @Override
    public void setVelocity(double d, double d1, double d2) {
        motionX = d;
        motionY = d1;
        motionZ = d2;
    }

    @Override
    public boolean pressKey(int key, EntityPlayer player) {
        if (!worldObj.isRemote && key == 9 && getDriveableType().modePrimary == EnumFireMode.SEMIAUTO) //Primary
        {
            shoot(false);
            return true;
        } else if (!worldObj.isRemote && key == 8 && getDriveableType().modeSecondary == EnumFireMode.SEMIAUTO) //Secondary
        {
            shoot(true);
            return true;
        }
        return false;
    }

    @Override
    public void updateKeyHeldState(int key, boolean held) {
        if (worldObj.isRemote) {
            FlansMod.getPacketHandler().sendToServer(new PacketDriveableKeyHeld(key, held));

            if (key == 2) {
                leftTurnHeld = true;
                rightTurnHeld = false;
            } else if (key == 3) {
                rightTurnHeld = true;
                leftTurnHeld = false;
            } else {
                leftTurnHeld = false;
                rightTurnHeld = false;
            }
        }
        switch (key) {
            case 9:
                leftMouseHeld = held;
                break;
            case 8:
                rightMouseHeld = held;
                break;
        }
    }

    /**
     * Shoot method called by pressing / holding shoot buttons
     */
    public void shoot(boolean secondary) {
        DriveableType type = getDriveableType();
        if (seats[0] == null)
            return;

        if (type.IT1 && !canFireIT1 && type.weaponType(secondary) == EnumWeaponType.MISSILE) return;

        if (!canFire || (isUnderWater() && !type.worksUnderWater)) return;

        //Check shoot delay
        if (getShootDelay(secondary) <= 0) {
            //We can shoot, so grab the available shoot points and the weaponType
            ArrayList<ShootPoint> shootPoints = type.shootPoints(secondary);
            EnumWeaponType weaponType = type.weaponType(secondary);
            //If there are no shoot points, return
            if (shootPoints.size() == 0)
                return;
            //For alternating guns, move on to the next one
            int currentGun = getCurrentGun(secondary);
            if (type.alternate(secondary)) {
                currentGun = (currentGun + 1) % shootPoints.size();
                setCurrentGun(currentGun, secondary);
                shootEach(type, shootPoints.get(currentGun), currentGun, secondary, weaponType);
            } else for (int i = 0; i < shootPoints.size(); i++)
                shootEach(type, shootPoints.get(i), i, secondary, weaponType);
        }
    }

    public boolean driverIsCreative() {
        return seats != null && seats[0] != null && seats[0].riddenByEntity instanceof EntityPlayer && ((EntityPlayer) seats[0].riddenByEntity).capabilities.isCreativeMode;
    }

    public void spawnParticle(ArrayList<ShootParticle> list, ShootPoint shootPoint, Vector3f v) {
        for (ShootParticle s : list) {
            float bkx = shootPoint.rootPos.position.x;
            float bky = shootPoint.rootPos.position.y;
            float bkz = shootPoint.rootPos.position.z;

            Vector3f velocity = new Vector3f(s.x, s.y, s.z);

            //if(shootPoint.rootPos.part == EnumDriveablePart.turret){
            velocity = getDirection(shootPoint, velocity);
            //}

            //Vector3f v = getFiringPosition(shootPoint);

            if (shootPoint.rootPos.part == EnumDriveablePart.core) {
                Vector3f v2 = axes.findLocalVectorGlobally(shootPoint.rootPos.position);
                Vector3f v3 = rotate(seats[0].looking.findLocalVectorGlobally(shootPoint.offPos));
                Vector3f.add(v2, v3, v);
            }

            FlansMod.getPacketHandler().sendToAllAround(
                    new PacketParticle(s.name, posX + v.x, posY + v.y, posZ + v.z, velocity.x, velocity.y, velocity.z),
                    posX + v.x, posY + v.y, posZ + v.z, 150, dimension);

            shootPoint.rootPos.position.x = bkx;
            shootPoint.rootPos.position.y = bky;
            shootPoint.rootPos.position.z = bkz;
        }
    }


    private void shootEach(DriveableType type, ShootPoint shootPoint, int currentGun, boolean secondary, EnumWeaponType weaponType) {
        //Rotate the gun vector to global axes
        Vector3f gunVec = getFiringPosition(shootPoint);
        Vector3f lookVector = getLookVector(shootPoint);

        if (!secondary && type.fixedPrimaryFire) {
            lookVector = axes.findLocalVectorGlobally(type.primaryFireAngle);
            if (shootPoint.rootPos.part == EnumDriveablePart.turret) {
                lookVector = getPositionOnTurret(type.primaryFireAngle, false);
            }
            if (shootPoint.rootPos.part == EnumDriveablePart.barrel) {
                lookVector = getPositionOnTurret(type.primaryFireAngle, true);
            }
        }

        if (weaponType == EnumWeaponType.SHELL)
            isRecoil = true;
        if (shootPoint.rootPos.part == null) return;
        if (!isPartIntact(shootPoint.rootPos.part)) return;

        if (disabled) return;
        float damageMultiplier = secondary ? type.damageMultiplierSecondary : type.damageMultiplierPrimary;
        //If its a pilot gun, then it is using a gun type, so do the following
        if (shootPoint.rootPos instanceof PilotGun) {
            PilotGun pilotGun = (PilotGun) shootPoint.rootPos;
            //Get the gun from the plane type and the ammo from the data
            GunType gunType = pilotGun.type;
            float shellSpeed = gunType.bulletSpeed;
            if (type.rangingGun)
                shellSpeed = type.bulletSpeed;
            ItemStack bulletItemStack = driveableData.ammo[getDriveableType().numPassengerGunners + currentGun];
            //Check that neither is null and that the bullet item is actually a bullet
            if (bulletItemStack != null && bulletItemStack.getItem() instanceof ItemShootable && TeamsManager.bulletsEnabled) {
                ShootableType bullet = ((ItemShootable) bulletItemStack.getItem()).type;
                if (gunType.isAmmo(bullet)) {
                    //spawnParticle(gunType, Vector3f.add(gunVec, new Vector3f((float)posX, (float)posY,V (float)posZ), null));

                    spawnParticle(type.shootParticle(secondary), shootPoint, gunVec);
                    //Spawn a new bullet item
                    worldObj.spawnEntityInWorld(((ItemShootable) bulletItemStack.getItem()).getEntity(worldObj, Vector3f.add(new Vector3f(posX, posY, posZ), gunVec, null), lookVector, (EntityLivingBase) seats[0].riddenByEntity, gunType.bulletSpread / 2, gunType.damage * damageMultiplier, shellSpeed, bulletItemStack.getItemDamage(), type));
                    //Play the shoot sound
                    PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.shootSound(secondary), false);
                    //Get the bullet item damage and increment it
                    int damage = bulletItemStack.getItemDamage();
                    bulletItemStack.setItemDamage(damage + 1);
                    //If the bullet item is completely damaged (empty)
                    if (damage + 1 == bulletItemStack.getMaxDamage()) {
                        //Set the damage to 0 and consume one ammo item (unless in creative)
                        bulletItemStack.setItemDamage(0);
                        if (!driverIsCreative()) {
                            bulletItemStack.stackSize--;
                            if (bulletItemStack.stackSize <= 0) {
                                onWeaponInventoryChanged(secondary);
                                bulletItemStack = null;
                            }
                            driveableData.setInventorySlotContents(getDriveableType().numPassengerGunners + currentGun, bulletItemStack);
                        }
                    }
                    if (type.shootDelay(secondary) == -1) {
                        setShootDelay(gunType.getShootDelay(), secondary);
                    } else {
                        setShootDelay(type.shootDelay(secondary), secondary);
                    }
                    //Reset the shoot delay
                }
            }
        } else //One of the other modes
        {
            switch (weaponType) {
                case BOMB: {
                    if (TeamsManager.bombsEnabled) {
                        int slot = -1;
                        for (int i = driveableData.getBombInventoryStart(); i < driveableData.getBombInventoryStart() + type.numBombSlots; i++) {
                            ItemStack bomb = driveableData.getStackInSlot(i);
                            if (bomb != null && bomb.getItem() instanceof ItemBullet && type.isValidAmmo(((ItemBullet) bomb.getItem()).type, weaponType)) {
                                slot = i;
                            }
                        }

                        if (slot != -1) {
                            int spread = 0;
                            float shellSpeed = 0F;

                            ItemStack bulletStack = driveableData.getStackInSlot(slot);
                            ItemBullet bulletItem = (ItemBullet) bulletStack.getItem();
                            if (shootPoint.rootPos instanceof PilotGun) {
                                PilotGun pilotGun = (PilotGun) shootPoint.rootPos;
                                //Get the gun from the plane type and the ammo from the data
                                GunType gunType = pilotGun.type;
                            }

                            EntityShootable bulletEntity = bulletItem.getEntity(worldObj, Vec3.createVectorHelper(gunVec.x + posX, gunVec.y + posY, gunVec.z + posZ), axes.getYaw(), axes.getPitch(), motionX, motionY, motionZ, (EntityLivingBase) seats[0].riddenByEntity, damageMultiplier, driveableData.getStackInSlot(slot).getItemDamage(), type);
                            worldObj.spawnEntityInWorld(bulletEntity);

                            spawnParticle(type.shootParticle(secondary), shootPoint, gunVec);


                            if (type.shootSound(secondary) != null)
                                PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.shootSound(secondary), false);
                            if (!driverIsCreative()) {
                                bulletStack.setItemDamage(bulletStack.getItemDamage() + 1);
                                if (bulletStack.getItemDamage() == bulletStack.getMaxDamage()) {
                                    bulletStack.setItemDamage(0);
                                    bulletStack.stackSize--;
                                    if (bulletStack.stackSize == 0) {
                                        onWeaponInventoryChanged(secondary);
                                        bulletStack = null;
                                    }
                                }
                                driveableData.setInventorySlotContents(slot, bulletStack);
                            }
                            if (type.shootDelay(secondary) == -1) {
                                setShootDelay(1, secondary);
                            } else {
                                setShootDelay(type.shootDelay(secondary), secondary);
                            }
                        }
                    }
                    break;
                }
                case MISSILE: //These two are actually almost identical
                case SHELL: {
                    //int numnum = driveableData.getMissileInventoryStart();
				/*ItemStack AmmoPlaced = driveableData.getStackInSlot(1);
				if(AmmoPlaced == null && type.enableReloadTime){
					isAmmoPlaced = false;
					break;
				}
				isAmmoPlaced = true;
				setShootDelay(type.shootDelay(secondary), secondary);*/
                    tryRecoil();

                    if (TeamsManager.shellsEnabled) {
                        int slot = -1;
                        for (int i = driveableData.getMissileInventoryStart(); i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++) {
                            ItemStack shell = driveableData.getStackInSlot(i);
                            if (shell != null && shell.getItem() instanceof ItemBullet && type.isValidAmmo(((ItemBullet) shell.getItem()).type, weaponType)) {
                                slot = i;
                            }
                        }

                        if (slot != -1) {
                            //int spread = 0;
                            //float shellSpeed = 3F;
                            float spread = type.bulletSpread;
                            float shellSpeed = type.bulletSpeed;
                            ItemStack bulletStack = driveableData.getStackInSlot(slot);
                            ItemBullet bulletItem = (ItemBullet) bulletStack.getItem();
                            EntityShootable bulletEntity = bulletItem.getEntity(worldObj, Vector3f.add(gunVec, new Vector3f(posX, posY, posZ), null), lookVector, (EntityLivingBase) seats[0].riddenByEntity, spread, damageMultiplier, shellSpeed, driveableData.getStackInSlot(slot).getItemDamage(), type);
                            //EntityShootable bulletEntity = bulletItem.getEntity(worldObj, Vector3f.add(gunVec, new Vector3f((float)posX, (float)posY, (float)posZ), null), lookVector, (EntityLivingBase)seats[0].riddenByEntity, spread, damageMultiplier, shellSpeed, bulletStack.getItemDamage(), type);
                            //EntityShootable bulletEntity = bulletItem.getEntity(worldObj, Vector3f.add(new Vector3f(posX, posY, posZ), gunVec, null), lookVector, (EntityLivingBase)seats[0].riddenByEntity, spread, damageMultiplier, shellSpeed, driveableData.getStackInSlot(slot).getItemDamage(), type);
                            worldObj.spawnEntityInWorld(bulletEntity); //SHELL
                            spawnParticle(type.shootParticle(secondary), shootPoint, gunVec);
                            isRecoil = true;

                            if (type.shootSound(secondary) != null)
                                PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.shootSound(secondary), false);
                            if (!driverIsCreative()) {
                                bulletStack.setItemDamage(bulletStack.getItemDamage() + 1);
                                if (bulletStack.getItemDamage() == bulletStack.getMaxDamage()) {
                                    bulletStack.setItemDamage(0);
                                    bulletStack.stackSize--;
                                    if (bulletStack.stackSize == 0) {
                                        onWeaponInventoryChanged(secondary);
                                        bulletStack = null;
                                    }
                                }
                                driveableData.setInventorySlotContents(slot, bulletStack);
                            }
                            if (type.shootDelay(secondary) == -1) {
                                setShootDelay(1, secondary);
                            } else {
                                setShootDelay(type.shootDelay(secondary), secondary);
                            }
                            canFireIT1 = false;
                        }
                    }
                    break;
                }
                case GUN: //Handled above
                    break;
                case MINE:
                case NONE:
                    break;
            }
        }
    }

    public Vector3f getOrigin(ShootPoint dp) {
        //Rotate the gun vector to global axes
        Vector3f localGunVec = new Vector3f(dp.rootPos.position);

        if (dp.rootPos.part == EnumDriveablePart.turret) {
            //Untranslate by the turret origin, to get the rotation about the right point
            Vector3f.sub(localGunVec, getDriveableType().turretOrigin, localGunVec);
            //Rotate by the turret angles
            localGunVec = seats[0].looking.findLocalVectorGlobally(localGunVec);
            //Translate by the turret origin
            Vector3f.add(localGunVec, getDriveableType().turretOrigin, localGunVec);
        }

        return rotate(localGunVec);
    }

    public Vector3f getPositionOnTurret(Vector3f vecIn, boolean barrel) {
        Vector3f transform = vecIn;
        RotatedAxes yawOnlyLooking = new RotatedAxes(seats[0].looking.getYaw(), 0, 0);
        if (barrel) yawOnlyLooking = seats[0].looking;

        //Calculate the root of the gun
        //Untranslate by the turret origin, to get the rotation about the right point
        Vector3f.sub(transform, getDriveableType().turretOrigin, transform);
        //Rotate by the turret angles
        transform = yawOnlyLooking.findLocalVectorGlobally(transform);
        //Translate by the turret origin
        Vector3f.add(transform, getDriveableType().turretOrigin, transform);
        Vector3f turretOriginOffset = new Vector3f(getDriveableType().turretOriginOffset);
        turretOriginOffset = yawOnlyLooking.findLocalVectorGloballyYaw(turretOriginOffset);
        Vector3f.add(transform, turretOriginOffset, transform);

        return rotate(transform);
    }

    public Vector3f getDirection(ShootPoint dp, Vector3f vIn) {
        //Rotate the gun vector to global axes
        Vector3f localGunVec = new Vector3f(vIn);

        //if(dp.rootPos.part == EnumDriveablePart.turret)
        //{
        //localGunVec = seats[0].looking.findLocalVectorGlobally(localGunVec);
        //}

        localGunVec = seats[0].looking.findLocalVectorGlobally(localGunVec);

        return rotate(localGunVec);
    }

    public Vector3f getLookVector(ShootPoint dp) {
        return axes.getXAxis();
    }

    public Vector3f getFiringPosition(ShootPoint dp) {
        Vector3f rootVector = new Vector3f(dp.rootPos.position);
        Vector3f offsetVector = new Vector3f(dp.offPos);
        Vector3f localGunVec = new Vector3f(dp.rootPos.position);

        if (dp.rootPos.part == EnumDriveablePart.turret) {
            if (offsetVector.x == 0 && offsetVector.y == 0 && offsetVector.z == 0) {
                //Untranslate by the turret origin, to get the rotation about the right point
                Vector3f.sub(localGunVec, getDriveableType().turretOrigin, localGunVec);
                //Rotate by the turret angles
                localGunVec = seats[0].looking.findLocalVectorGlobally(localGunVec);
                //Translate by the turret origin
                Vector3f.add(localGunVec, getDriveableType().turretOrigin, localGunVec);
            } else {
                RotatedAxes yawOnlyLooking = new RotatedAxes(seats[0].looking.getYaw(), 0, 0);

                //Calculate the root of the gun
                //Untranslate by the turret origin, to get the rotation about the right point
                Vector3f.sub(rootVector, getDriveableType().turretOrigin, rootVector);
                //Rotate by the turret angles
                rootVector = yawOnlyLooking.findLocalVectorGlobally(rootVector);
                //Translate by the turret origin
                Vector3f.add(rootVector, getDriveableType().turretOrigin, rootVector);

                //Calculate the tip of the gun
                //Untranslate by the turret origin, to get the rotation about the right point
                Vector3f.sub(offsetVector, getDriveableType().turretOrigin, offsetVector);
                //Rotate by the turret angles
                offsetVector = seats[0].looking.findLocalVectorGlobally(offsetVector);
                //Translate by the turret origin

                Vector3f.add(rootVector, offsetVector, localGunVec);
            }
        }

        return rotate(localGunVec);
    }

    public boolean isEngineActive() {
	if(getDriveableType().needsFuel == false) {
    		driveableData.fuelInTank = 1;
    	}
    	if(getDriveableType().fuelTankSize <= 0) {
    		getDriveableType().fuelTankSize = 1;
    	}
        return (driverIsCreative() || driveableData.fuelInTank > 0) && engineStartDelay == 0;
    }

    public void correctWheelPos() {
        if (this.ticksExisted % (10 * 20) == 0) {
            for (EntityWheel wheel : wheels) {
                if (wheel == null) continue;

                Vector3f target = axes.findLocalVectorGlobally(getDriveableType().wheelPositions[wheel.ID].position);
                target.x += posX;
                target.y += posY;
                target.z += posZ;

                int tf = 1;
                int cf = 1;
                int range = 5;

                if (MathHelper.abs(target.x - (float) wheel.posX) > range) {
                    wheel.posX = (target.x * tf + (float) wheel.posX * cf) / (tf + cf);
                }
                if (MathHelper.abs(target.y - (float) wheel.posY) > range) {
                    wheel.posY = (target.y * tf + (float) wheel.posY * cf) / (tf + cf);
                }
                if (MathHelper.abs(target.z - (float) wheel.posZ) > range) {
                    wheel.posZ = (target.z * tf + (float) wheel.posZ * cf) / (tf + cf);
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        //playerIDs.clear();
        DriveableType type = getDriveableType();
        DriveableData data = getDriveableData();
        //if(type.fancyCollision)
        //checkCollsionBox();
        hugeBoat = (getDriveableType().floatOnWater && getDriveableType().wheelStepHeight == 0);
        //hugeBoat = true;

        if (hugeBoat) {
            for (int i = 0; i < worldObj.loadedEntityList.size(); i++) {
                Object obj = worldObj.loadedEntityList.get(i);
                if (obj instanceof EntityPlayer && !isPartOfThis((Entity) obj)) {
                    moveRiders((Entity) obj);
                }

                if (obj instanceof EntityWheel && !isPartOfThis((Entity) obj) && getDistanceToEntity((Entity) obj) <= getDriveableType().bulletDetectionRadius) {
                    //moveRiders((Entity)obj);
                }

                if (obj instanceof EntityDriveable && !isPartOfThis((Entity) obj) && getDistanceToEntity((Entity) obj) <= getDriveableType().bulletDetectionRadius) {
                    //moveRiders((Entity)obj);
                }
            }
        }


        if (deckCheck != prevDeckCheck)
            onDeck = true;
        else onDeck = false;


        //Aesthetics
        if (type.IT1 && !disabled) {
            boolean fireButtonHeld = false;
            if (type.weaponType(false) == EnumWeaponType.MISSILE) fireButtonHeld = leftMouseHeld;
            if (type.weaponType(true) == EnumWeaponType.MISSILE) fireButtonHeld = rightMouseHeld;

            prevDrakonDoorAngle = drakonDoorAngle;
            prevDrakonArmAngle = drakonArmAngle;
            prevDrakonRailAngle = drakonRailAngle;
            if (canFireIT1) reloadingDrakon = false;
            if (stage == 0) stage = 1;

            if (stage == 8 && fireButtonHeld) {
                stage = 1;
                timeTillDeactivate = 5;
                toDeactivate = true;
            }
            if (timeTillDeactivate <= 0 && toDeactivate) {
                canFireIT1 = false;
                toDeactivate = false;
            }

            if (reloadAnimTime <= 0)
                IT1Reload();

            reloadAnimTime--;
            timeTillDeactivate--;
        }

        //Aesthetics
        prevPropAngle = propAngle;
        prevRotorAngle = rotorAngle;
        if (throttle != 0) {
            propAngle += (Math.pow(Math.abs(throttle), 0.4)) * 1.5;
            rotorAngle += throttle / 7F;
        }


        //Gun recoil
        if (leftMouseHeld && !disabled) {
            tryRecoil();
            setRecoilTimer();
        }
        lastRecoilPos = recoilPos;

        if (recoilPos > 180 - (180 / type.recoilTime)) {
            recoilPos = 0;
            isRecoil = false;
        }

        if (isRecoil)
            recoilPos = recoilPos + (180 / type.recoilTime);

        if (recoilTimer >= 0)
            recoilTimer--;

        checkInventoryChanged();

        if (isUnderWater() && !type.worksUnderWater && !hugeBoat) {
            throttle = 0;
            //this.driveableData.parts.get(EnumDriveablePart.core).health -= 1;
            disabled = true;
        } else disabled = false;


        if (type.lockOnToLivings || type.lockOnToMechas || type.lockOnToPlanes || type.lockOnToPlayers || type.lockOnToVehicles) {
            if (!worldObj.isRemote && this.seats.length > 0 && lockOnSoundDelay <= 0) {
                if (this.seats[0] != null && this.seats[0].riddenByEntity instanceof EntityPlayer) {
                    // int currentGun = getCurrentGun(false);
                    // Vector3f playerVec = getOrigin(type.shootPoints(false).get(currentGun));
                    Vector3f playerVecRelToVehicle = seats[0].playerLooking.findGlobalVectorLocally(new Vector3f(-1, 0, 0));
                    Vector3f playerVec = axes.findGlobalVectorLocally(playerVecRelToVehicle);

                    for (Object obj : worldObj.loadedEntityList) {
                        Entity entity = (Entity) obj;
                        if ((type.lockOnToMechas && entity instanceof EntityMecha) ||
                                (type.lockOnToVehicles && entity instanceof EntityVehicle) ||
                                (type.lockOnToPlanes && entity instanceof EntityPlane) ||
                                (type.lockOnToPlayers && entity instanceof EntityPlayer) ||
                                (type.lockOnToLivings && entity instanceof EntityLivingBase)) {
                            if (getDistanceSqToEntity(entity) < type.maxRangeLockOn * type.maxRangeLockOn) {
                                // Some heckery with vectors rotating about themselves or something
                                Vector3f relPosVec = new Vector3f(-entity.posX + seats[0].posX, -entity.posY + seats[0].posY, entity.posZ - seats[0].posZ);
                                float angle = Math.abs(Vector3f.angle(playerVec, relPosVec));
                                if (angle < Math.toRadians(type.canLockOnAngle)) {
                                    PacketPlaySound.sendSoundPacket(seats[0].posX, seats[0].posY, seats[0].posZ, 10, dimension, type.lockOnSound, false);
                                    if (entity instanceof EntityDriveable)
                                        PacketPlaySound.sendSoundPacket(entity.posX, entity.posY, entity.posZ, ((EntityDriveable) entity).getDriveableType().lockedOnSoundRange, entity.dimension, ((EntityDriveable) entity).getDriveableType().lockingOnSound, false);
                                    lockOnSoundDelay = type.lockOnSoundTime;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (lockOnSoundDelay > 0)
            lockOnSoundDelay--;


        if (this.ridingEntity != null) {
            invulnerableUnmountCount = 20 * 4;
        } else if (invulnerableUnmountCount > 0) {
            invulnerableUnmountCount--;
        }

        if (!worldObj.isRemote) {
            for (int i = 0; i < getDriveableType().numPassengers + 1; i++) {
                if (seats[i] == null || !seats[i].addedToChunk) {
                    seats[i] = new EntitySeat(worldObj, this, i);
                    worldObj.spawnEntityInWorld(seats[i]);
                }
            }
            for (int i = 0; i < type.wheelPositions.length; i++) {
                if (wheels[i] == null || !wheels[i].addedToChunk) {
                    wheels[i] = new EntityWheel(worldObj, this, i);
                    worldObj.spawnEntityInWorld(wheels[i]);
                }
            }
        }

        //Harvest stuff
        //Aesthetics
        if (hasEnoughFuel() && isEngineActive()) {
            harvesterAngle += throttle / 5F;
        }
        //Actual harvesting
        if (type.harvestBlocks && type.harvestBoxSize != null && type.harvestBoxPos != null && TeamsManager.driveablesBreakBlocks) {
            Vector3f size = new Vector3f(type.harvestBoxSize.x / 16F, type.harvestBoxSize.y / 16F, type.harvestBoxSize.z / 16F);
            Vector3f pos = new Vector3f(type.harvestBoxPos.x / 16F, type.harvestBoxPos.y / 16F, type.harvestBoxPos.z / 16F);
            for (float x = pos.x; x <= pos.x + size.x; x++) {
                for (float y = pos.y; y <= pos.y + size.y; y++) {
                    for (float z = pos.z; z <= pos.z + size.z; z++) {
                        Vector3f v = axes.findLocalVectorGlobally(new Vector3f(x, y, z));

                        int blockX = (int) Math.round(posX + v.x);
                        int blockY = (int) Math.round(posY + v.y);
                        int blockZ = (int) Math.round(posZ + v.z);
                        Block block = worldObj.getBlock(blockX, blockY, blockZ);

                        if (type.materialsHarvested.contains(block.getMaterial()) && block.getBlockHardness(worldObj, blockX, blockY, blockZ) >= 0F) {
                            if (type.collectHarvest) {
                                //Add the itemstack to mecha inventory
                                ArrayList<ItemStack> stacks = block.getDrops(worldObj, blockX, blockY, blockZ, worldObj.getBlockMetadata(blockX, blockY, blockZ), 0);
                                for (ItemStack stack : stacks) {
                                    if (!InventoryHelper.addItemStackToInventory(driveableData, stack, driverIsCreative()) && !worldObj.isRemote && worldObj.getGameRules().getGameRuleBooleanValue("doTileDrops")) {
                                        worldObj.spawnEntityInWorld(new EntityItem(worldObj, blockX + 0.5F, blockY + 0.5F, blockZ + 0.5F, stack));
                                    }
                                }
                            } else if (type.dropHarvest) {
                                ArrayList<ItemStack> stacks = block.getDrops(worldObj, blockX, blockY, blockZ, worldObj.getBlockMetadata(blockX, blockY, blockZ), 0);
                                for (ItemStack stack : stacks) {
                                    worldObj.spawnEntityInWorld(new EntityItem(worldObj, blockX + 0.5F, blockY + 0.5F, blockZ + 0.5F, stack));
                                }
                            }
                            //Destroy block
                            worldObj.func_147480_a(blockX, blockY, blockZ, false);
                        }
                    }
                }
            }
        }

		/*if(this.isLockedOn && soundTime <= 0 && !this.worldObj.isRemote)
		{
			PacketPlaySound.sendSoundPacket(posX,posY,posZ, 5, dimension, type.lockedOnSound, false);
			soundTime = type.soundTime;
		}*/

        for (DriveablePart part : getDriveableData().parts.values()) {
            if (part.box != null) {

                part.update(this);
                //Client side particles
                if (worldObj.isRemote) {
                    if (part.onFire) {
                        //Pick a random position within the bounding box and spawn a flame there
                        Vector3f pos = axes.findLocalVectorGlobally(new Vector3f(part.box.x + rand.nextFloat() * part.box.w, part.box.y + rand.nextFloat() * part.box.h, part.box.z + rand.nextFloat() * part.box.d));
                        worldObj.spawnParticle("flame", posX + pos.x, posY + pos.y, posZ + pos.z, 0, 0, 0);
                    }
                    if (part.health > 0 && part.health < part.maxHealth / 2) {
                        //Pick a random position within the bounding box and spawn a flame there
                        Vector3f pos = axes.findLocalVectorGlobally(new Vector3f(part.box.x + rand.nextFloat() * part.box.w, part.box.y + rand.nextFloat() * part.box.h, part.box.z + rand.nextFloat() * part.box.d));
                        worldObj.spawnParticle(part.health < part.maxHealth / 4 ? "largesmoke" : "smoke", posX + pos.x, posY + pos.y, posZ + pos.z, 0, 0, 0);
                    }
                }
                //Server side fire handling
                if (part.onFire) {
                    //Rain can put out fire
                    if (worldObj.isRaining() && rand.nextInt(40) == 0)
                        part.onFire = false;
                    //Also water blocks
                    //Get the centre point of the part
                    Vector3f pos = axes.findLocalVectorGlobally(new Vector3f(part.box.x + part.box.w / 2F, part.box.y + part.box.h / 2F, part.box.z + part.box.d / 2F));
                    if (worldObj.getBlock(MathHelper.floor_double(posX + pos.x), MathHelper.floor_double(posY + pos.y), MathHelper.floor_double(posZ + pos.z)).getMaterial() == Material.water) {
                        part.onFire = false;
                    }
                } else {
                    Vector3f pos = axes.findLocalVectorGlobally(new Vector3f(part.box.x / 16F + part.box.w / 32F, part.box.y / 16F + part.box.h / 32F, part.box.z / 16F + part.box.d / 32F));
                    if (worldObj.getBlock(MathHelper.floor_double(posX + pos.x), MathHelper.floor_double(posY + pos.y), MathHelper.floor_double(posZ + pos.z)).getMaterial() == Material.lava) {
                        part.onFire = true;
                    }
                }
            }
        }

        for (int i = 0; i < type.emitters.size(); i++) {
            ParticleEmitter emitter = type.emitters.get(i);
            emitterTimers[i]--;
            boolean canEmit;
            boolean inThrottle = false;
            DriveablePart part = getDriveableData().parts.get(EnumDriveablePart.getPart(emitter.part));
            float healthPercentage = (float) part.health / (float) part.maxHealth;
            canEmit = isPartIntact(EnumDriveablePart.getPart(emitter.part)) && healthPercentage >= emitter.minHealth && healthPercentage <= emitter.maxHealth;

            if ((throttle >= emitter.minThrottle && throttle <= emitter.maxThrottle))
                inThrottle = true;
            if (isMecha)
                inThrottle = true;

            if (emitterTimers[i] <= 0) {
                if (inThrottle && canEmit) {
                    //Emit!
                    Vector3f velocity = new Vector3f(0, 0, 0);
                    ;
                    Vector3f pos = new Vector3f(0, 0, 0);
                    if (seats != null && seats[0] != null) {
                        if (EnumDriveablePart.getPart(emitter.part) != EnumDriveablePart.turret && EnumDriveablePart.getPart(emitter.part) != EnumDriveablePart.barrel) {
                            Vector3f localPosition = new Vector3f(emitter.origin.x + rand.nextFloat() * emitter.extents.x - emitter.extents.x * 0.5f,
                                    emitter.origin.y + rand.nextFloat() * emitter.extents.y - emitter.extents.y * 0.5f,
                                    emitter.origin.z + rand.nextFloat() * emitter.extents.z - emitter.extents.z * 0.5f);

                            pos = axes.findLocalVectorGlobally(localPosition);
                            velocity = axes.findLocalVectorGlobally(emitter.velocity);
                        } else if (EnumDriveablePart.getPart(emitter.part) == EnumDriveablePart.turret || EnumDriveablePart.getPart(emitter.part) == EnumDriveablePart.head && emitter.part != "barrel") {

                            Vector3f localPosition2 = new Vector3f(emitter.origin.x + rand.nextFloat() * emitter.extents.x - emitter.extents.x * 0.5f,
                                    emitter.origin.y + rand.nextFloat() * emitter.extents.y - emitter.extents.y * 0.5f,
                                    emitter.origin.z + rand.nextFloat() * emitter.extents.z - emitter.extents.z * 0.5f);
                            pos = getPositionOnTurret(localPosition2, false);
                            velocity = getPositionOnTurret(emitter.velocity, false);
                        } else if (EnumDriveablePart.getPart(emitter.part) == EnumDriveablePart.barrel) {
                            Vector3f localPosition2 = new Vector3f(emitter.origin.x + rand.nextFloat() * emitter.extents.x - emitter.extents.x * 0.5f,
                                    emitter.origin.y + rand.nextFloat() * emitter.extents.y - emitter.extents.y * 0.5f,
                                    emitter.origin.z + rand.nextFloat() * emitter.extents.z - emitter.extents.z * 0.5f);


                            pos = getPositionOnTurret(localPosition2, true);
                            velocity = getPositionOnTurret(emitter.velocity, true);
                        }
                        //FlansMod.proxy.spawnParticle(emitter.effectType, posX + pos.x, posY + pos.y, posZ + pos.z, velocity.x, velocity.y, velocity.z);

                        FlansMod.getPacketHandler().sendToAllAround(
                                new PacketParticle(emitter.effectType,
                                        posX + pos.x, posY + pos.y, posZ + pos.z, velocity.x, velocity.y, velocity.z),
                                posX + pos.x, posY + pos.y, posZ + pos.z, 150, dimension);

                    }
                }
                emitterTimers[i] = emitter.emitRate;
            }
        }

        checkParts();

        prevRotationYaw = axes.getYaw();
        prevRotationPitch = axes.getPitch();
        prevRotationRoll = axes.getRoll();
        prevAxes = axes.clone();

        if (riddenByEntity != null && riddenByEntity.isDead) {
            riddenByEntity = null;
        }
        if (riddenByEntity != null && isDead) {
            riddenByEntity.mountEntity(null);
        }
        if (riddenByEntity != null)
            riddenByEntity.fallDistance = 0F;

        //If there's no player in the driveable or it cannot thrust, slow the plane and turn off mouse held actions
        if ((seats != null && seats[0] != null && seats[0].riddenByEntity == null) || !isEngineActive() && getDriveableType().maxThrottle != 0 && getDriveableType().maxNegativeThrottle != 0) {
            throttle *= 0.99F;
        }
        if (seats != null && seats[0] != null && seats[0].riddenByEntity == null) {
            rightMouseHeld = leftMouseHeld = false;
        }

        //Check if shooting
        if (shootDelayPrimary > 0)
            shootDelayPrimary--;
        if (shootDelaySecondary > 0)
            shootDelaySecondary--;
        if (getDriveableType().reloadSoundTick != 15214541 && shootDelayPrimary == getDriveableType().reloadSoundTick) {
            PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension, type.shootReloadSound, false);
        }
// on first update
        if (this.ticksExisted == 1) {
            setShootDelay(getDriveableType().placeTimePrimary, false);
            setShootDelay(getDriveableType().placeTimeSecondary, true);
            if (!this.worldObj.isRemote) {
                if (!getDriveableType().placeSoundPrimary.isEmpty()) {
                    PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension,
                            getDriveableType().placeSoundPrimary, false);
                }
                if (!getDriveableType().placeSoundSecondary.isEmpty()) {
                    PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension,
                            getDriveableType().placeSoundSecondary, false);
                }
            }
        }
        if (seats[0] != null && seats[0].riddenByEntity != null && seats[0].riddenByEntity instanceof EntityPlayer && worldObj.isRemote) {
            EntityPlayer p = (EntityPlayer) seats[0].riddenByEntity;
            if (this.ticksExisted < getDriveableType().placeTimePrimary && (getShootDelay(false) % 100) == 0 ) {
                p.addChatComponentMessage(new ChatComponentText("Primary gun ready to use in " + getShootDelay(false)/20 + " seconds."));
            } else if (this.ticksExisted == getDriveableType().placeTimePrimary) {
                p.addChatComponentMessage(new ChatComponentText("Primary gun ready to use!"));
            }

            if (this.ticksExisted < getDriveableType().placeTimeSecondary && (getShootDelay(true) % 100) == 0) {
                p.addChatComponentMessage(
                        new ChatComponentText("Secondary gun ready to use in " + getShootDelay(true)/20 + " seconds."));
            } else if (this.ticksExisted == getDriveableType().placeTimeSecondary) {
                p.addChatComponentMessage(new ChatComponentText("Secondary gun ready to use!"));
            }

            if (engineStartDelay > 0 && engineStartDelay % (2.5*20) == 0) {
                p.addChatComponentMessage(new ChatComponentText("Engine starting.. " + (float)engineStartDelay/20 + " seconds remaining."));
            } else if (engineStartDelay == 1) {
                p.addChatComponentMessage(new ChatComponentText("Engine started!"));
            }
        }

        if (!worldObj.isRemote) {
            if (leftMouseHeld && getDriveableType().modePrimary == EnumFireMode.FULLAUTO)
                shoot(false);
            if (rightMouseHeld && getDriveableType().modeSecondary == EnumFireMode.FULLAUTO)
                shoot(true);
            minigunSpeedPrimary *= 0.9F;
            minigunSpeedSecondary *= 0.9F;
            if (leftMouseHeld && getDriveableType().modePrimary == EnumFireMode.MINIGUN) {
                minigunSpeedPrimary += 0.1F;
                if (minigunSpeedPrimary > 1F)
                    shoot(false);
            }
            if (rightMouseHeld && getDriveableType().modeSecondary == EnumFireMode.MINIGUN) {
                minigunSpeedSecondary += 0.1F;
                if (minigunSpeedSecondary > 1F)
                    shoot(true);
            }
        }

        prevDeckCheck = deckCheck;

        if (engineStartDelay > 0) { engineStartDelay--; }
        //Handle fuel

        int fuelMultiplier = 2;

        //The tank is currently full, so do nothing
        if (data.fuelInTank >= type.fuelTankSize)
            return;

        //Look through the entire inventory for fuel cans, buildcraft fuel buckets and RedstoneFlux power sources
        for (int i = 0; i < data.getSizeInventory(); i++) {
            ItemStack stack = data.getStackInSlot(i);
            if (stack == null || stack.stackSize <= 0)
                continue;
            Item item = stack.getItem();
            //If we have an electric engine, look for RedstoneFlux power source items, such as power cubes
            if (data.engine.useRFPower) {
                if (item instanceof IEnergyContainerItem) {
                    IEnergyContainerItem energy = (IEnergyContainerItem) item;
                    data.fuelInTank += (fuelMultiplier * energy.extractEnergy(stack, data.engine.RFDrawRate, false)) / data.engine.RFDrawRate;
                }
            } else {
                //Check for Flan's Mod fuel items
                if (item instanceof ItemPart) {
                    PartType part = ((ItemPart) item).type;
                    //Check it is a fuel item
                    if (part.category == 9) {
                        //Put 2 points of fuel
                        data.fuelInTank += fuelMultiplier;

                        //Damage the fuel item to indicate being used up
                        int damage = stack.getItemDamage();
                        stack.setItemDamage(damage + 1);

                        //If we have finished this fuel item
                        if (damage >= stack.getMaxDamage()) {
                            //Reset the damage to 0
                            stack.setItemDamage(0);
                            //Consume one item
                            stack.stackSize--;
                            //If we consumed the last one, destroy the stack
                            if (stack.stackSize <= 0)
                                data.setInventorySlotContents(i, null);
                        }

                        //We found a fuel item and consumed some, so we are done
                        break;
                    }
                }
                //Check for Buildcraft oil and fuel buckets
                else if (FlansMod.hooks.BuildCraftLoaded && stack.isItemEqual(FlansMod.hooks.BuildCraftOilBucket) && data.fuelInTank + 1000 * fuelMultiplier <= type.fuelTankSize) {
                    data.fuelInTank += 1000 * fuelMultiplier;
                    data.setInventorySlotContents(i, new ItemStack(Items.bucket));
                } else if (FlansMod.hooks.BuildCraftLoaded && stack.isItemEqual(FlansMod.hooks.BuildCraftFuelBucket) && data.fuelInTank + 2000 * fuelMultiplier <= type.fuelTankSize) {
                    data.fuelInTank += 2000 * fuelMultiplier;
                    data.setInventorySlotContents(i, new ItemStack(Items.bucket));
                }

                prevPosX = posX;
                prevPosY = posY;
                prevPosZ = posZ;
            }
        }
    }

    public void checkInventoryChanged() {
        DriveableType type = getDriveableType();
        if (type == null) return;

        if (worldObj.isRemote) return;

        if (!driveableData.inventoryChanged) return;

        driveableData.inventoryChanged = false;

        try {
            for (int ps = 0; ps < 2; ps++) {
                EnumWeaponType weaponType = ps == 0 ? type.primary : type.secondary;
                if (weaponType == EnumWeaponType.GUN) {
                    weaponType = EnumWeaponType.NONE;
                }
                int istart = getInventoryStart(weaponType);
                if (istart == driveableData.getAmmoInventoryStart()) {
                    istart += type.numPassengerGunners;
                }
                final int isize = getInventorySize(weaponType);
                if (istart >= 0 || isize > 0) {
                    if (prevInventoryItems[ps] == null) {
                        prevInventoryItems[ps] = new ItemStack[isize];
                    }

                    for (int i = 0; i < isize; i++) {
                        ItemStack itemStack = driveableData.getStackInSlot(istart + i);
                        if (itemStack != null && itemStack.getItem() instanceof ItemBullet) {
                            if (prevInventoryItems[ps][i] == null || !ItemStack.areItemStacksEqual(itemStack, prevInventoryItems[ps][i])) {
                                if (type.isValidAmmo(((ItemBullet) itemStack.getItem()).type, weaponType)) {
                                    onWeaponInventoryChanged(ps == 1);
                                    break;
                                }
                            }
                        }
                    }

                    for (int i = 0; i < isize; i++) {
                        prevInventoryItems[ps][i] = driveableData.getStackInSlot(istart + i);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onWeaponInventoryChanged(boolean secondary) {
        DriveableType type = getDriveableType();
        if (!secondary) {
            if (type.reloadTimePrimary > 0 && getShootDelay(secondary) <= 0) {
                setShootDelay(type.reloadTimePrimary, secondary);
                PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension,
                        getDriveableType().reloadSoundPrimary, false);
            }
        } else {
            if (type.reloadTimeSecondary > 0 && getShootDelay(secondary) <= 0) {
                setShootDelay(type.reloadTimeSecondary, secondary);
                PacketPlaySound.sendSoundPacket(posX, posY, posZ, FlansMod.soundRange, dimension,
                        getDriveableType().reloadSoundSecondary, false);
            }
        }
    }

    public int getInventoryStart(EnumWeaponType wt) {
        switch (wt) {
            case NONE:
            case GUN:
                return driveableData.getAmmoInventoryStart();

            case MISSILE:
            case SHELL:
                return driveableData.getMissileInventoryStart();

            case BOMB:
            case MINE:
                return driveableData.getBombInventoryStart();

            default:
                break;
        }
        return -1;
    }

    public int getInventorySize(EnumWeaponType wt) {
        switch (wt) {
            case NONE:
            case GUN:
                return driveableData.ammo.length;

            case MISSILE:
            case SHELL:
                return driveableData.missiles.length;

            case BOMB:
            case MINE:
                return driveableData.bombs.length;

            default:
                break;
        }
        return -1;
    }

    public void checkForCollisions() {
        boolean damagePart = false;
        boolean crashInWater = false;
        double speed = getSpeedXYZ();
        for (DriveablePosition p : getDriveableType().collisionPoints) {
            if (driveableData.parts.get(p.part).dead)
                continue;
            Vector3f lastRelPos = prevAxes.findLocalVectorGlobally(p.position);
            Vec3 lastPos = Vec3.createVectorHelper(prevPosX + lastRelPos.x, prevPosY + lastRelPos.y, prevPosZ + lastRelPos.z);

            Vector3f currentRelPos = axes.findLocalVectorGlobally(p.position);
            Vec3 currentPos = Vec3.createVectorHelper(posX + currentRelPos.x, posY + currentRelPos.y, posZ + currentRelPos.z);

            if (FlansMod.DEBUG && worldObj.isRemote) {
                worldObj.spawnEntityInWorld(new EntityDebugVector(worldObj, new Vector3f(lastPos), Vector3f.sub(currentRelPos, lastRelPos, null), 10, 1F, 0F, 0F));
            }

            MovingObjectPosition hit = worldObj.rayTraceBlocks(lastPos, currentPos, crashInWater);
            if (hit != null && hit.typeOfHit == MovingObjectType.BLOCK) {
                int x = hit.blockX;
                int y = hit.blockY;
                int z = hit.blockZ;
                Block blockHit = worldObj.getBlock(x, y, z);
                int meta = worldObj.getBlockMetadata(x, y, z);

                float blockHardness = blockHit.getBlockHardness(worldObj, x, y, z);

                float damage = 0;
                if (blockHardness > 0.2) {
                    damage = blockHardness * blockHardness * (float) speed;
                }

                if (null == blockHit.getCollisionBoundingBoxFromPool(this.worldObj, x, y, z)) {
                    damage = 0;
                }

                if (damage > 0) {
                    damagePart = true;
                    if (!attackPart(p.part, DamageSource.inWall, damage) && TeamsManager.driveablesBreakBlocks) {
                        // And if it didn't die from the attack, break the block
                        worldObj.playAuxSFXAtEntity(null, 2001, x, y, z, Block.getIdFromBlock(blockHit) + (meta << 12));

                        if (!worldObj.isRemote) {
                            blockHit.dropBlockAsItem(worldObj, x, y, z, meta, 1);
                            worldObj.setBlockToAir(x, y, z);
                        }
                    } else {
                        // The part died!
                        worldObj.createExplosion(this, currentPos.xCoord, currentPos.yCoord, currentPos.zCoord, 1F,
                                false);
                    }
                }
            }

        }

        if (FlansMod.seatCollisions) {
            // This is for preventing vehicle glitching. It makes seats collideable, and stops their motion if 
            for (EntitySeat seat : seats) {
                if (seat == null || wheels == null ||wheels[0] == null || wheels[1] == null || seat.riddenByEntity == null) 
                    continue;
                DriveablePosition p = seat.getAsDriveablePosition();
                if (driveableData.parts.get(p.part).dead)
                    continue;
                Vector3f fwd = axes.getXAxis();
                float a = 0F;
                if (getSpeedXZ() > 1) {
                    if (getSpeedXZ() > 2) {
                        a = 6F;
                    } else {
                        a = 3F;
                    }
                }

                double checkY = Math.max((wheels[0].posY + wheels[1].posY)/2F + (this instanceof EntityVehicle ?  1.5 : 0), seat.posY);
                Vec3 seatPos = Vec3.createVectorHelper(seat.posX + fwd.x * a, checkY + fwd.y * a, seat.posZ + fwd.z * a);
                Vec3 wheelMidPos = Vec3.createVectorHelper((wheels[0].posX + wheels[1].posX)/2F, checkY, (wheels[0].posZ + wheels[1].posZ)/2F);

                MovingObjectPosition hit = worldObj.rayTraceBlocks(seatPos, wheelMidPos, crashInWater);
                if (hit != null && hit.typeOfHit == MovingObjectType.BLOCK) {
                    int x = hit.blockX;
                    int y = hit.blockY;
                    int z = hit.blockZ;
                    Block blockHit = worldObj.getBlock(x, y, z);

                    float blockHardness = blockHit.getBlockHardness(worldObj, x, y, z);

                    collisionHardness = blockHardness;
                }
            }
        }

        if (damagePart) {
            //This is server side bsns
            if (!worldObj.isRemote) {
//				checkParts();
                //If it hit, send a damage update packet
                FlansMod.getPacketHandler().sendToAllAround(new PacketDriveableDamage(this), posX, posY, posZ, 100, dimension);
            }
        }
    }

    @Override
    protected void fall(float k) {
        double fallDist = ((this.posY - this.prevPosY) + this.motionY) / 2;
        float damage = (float) (fallDist < -0.3 ? -fallDist * 50 : 0);

        boolean noDamage = true;
        if (damage > 0 && invulnerableUnmountCount == 0 && this.ticksExisted > 20) {
            DriveableType type = getDriveableType();
            damage = (int) (damage * type.fallDamageFactor);
            attackPart(EnumDriveablePart.core, DamageSource.fall, damage);
            if (type.wheelPositions.length > 0) {
                attackPart(type.wheelPositions[0].part, DamageSource.fall, damage / 5);
            }

            noDamage = false;
        }
        //	FlansMod.log("fall%s : tick=%d damage=%.1f, posY-prevPosY=%.3f, mY=%.3f, fallDist=%.2f",
        //		noDamage? " no damage":"", this.ticksExisted, damage, this.posY - this.prevPosY,
        //		this.motionY, fallDist);
    }

    /**
     * Attack a certain part of a driveable and return whether it broke or not
     */
    public boolean attackPart(EnumDriveablePart ep, DamageSource source, float damage) {
        if (ep == EnumDriveablePart.core) {
            if (source.getSourceOfDamage() instanceof EntityLivingBase) {
                this.lastAtkEntity = source.getSourceOfDamage();
            } else if (source.getEntity() instanceof EntityLivingBase) {
                this.lastAtkEntity = source.getEntity();
            } else {
                this.lastAtkEntity = null;
            }
        }
        DriveablePart part = driveableData.parts.get(ep);
        //	FlansMod.log("EntityDriveable.attackPart %s : %s : damage=%.1f : %s : health=%d", ep.name(), ep.getName(), damage,
        //			part.type.name(), part.health);
        return part.attack(damage, source.isFireDamage());
    }

    /**
     * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global coordinates
     */
    public Vector3f rotate(Vector3f inVec) {
        return axes.findLocalVectorGlobally(inVec);
    }

    /**
     * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global coordinates
     */
    public Vector3f rotate(Vec3 inVec) {
        return rotate(inVec.xCoord, inVec.yCoord, inVec.zCoord);
    }

    /**
     * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global coordinates
     */
    public Vector3f rotate(double x, double y, double z) {
        return rotate(new Vector3f((float) x, (float) y, (float) z));
    }

    //Rotate the plane locally by some angle about the yaw axis
    public void rotateYaw(float rotateBy) {
        if (Math.abs(rotateBy) < 0.01F)
            return;
        axes.rotateLocalYaw(rotateBy);
        updatePrevAngles();
    }

    //Rotate the plane locally by some angle about the pitch axis
    public void rotatePitch(float rotateBy) {
        if (Math.abs(rotateBy) < 0.01F)
            return;
        axes.rotateLocalPitch(rotateBy);
        updatePrevAngles();
    }

    //Rotate the plane locally by some angle about the roll axis
    public void rotateRoll(float rotateBy) {
        if (Math.abs(rotateBy) < 0.01F)
            return;
        axes.rotateLocalRoll(rotateBy);
        updatePrevAngles();
    }

    public void updatePrevAngles() {
        //Correct angles that crossed the +/- 180 line, so that rendering doesnt make them swing 360 degrees in one tick.
        double dYaw = axes.getYaw() - prevRotationYaw;
        if (dYaw > 180)
            prevRotationYaw += 360F;
        if (dYaw < -180)
            prevRotationYaw -= 360F;

        double dPitch = axes.getPitch() - prevRotationPitch;
        if (dPitch > 180)
            prevRotationPitch += 360F;
        if (dPitch < -180)
            prevRotationPitch -= 360F;

        double dRoll = axes.getRoll() - prevRotationRoll;
        if (dRoll > 180)
            prevRotationRoll += 360F;
        if (dRoll < -180)
            prevRotationRoll -= 360F;
    }

    public void setRotation(float rotYaw, float rotPitch, float rotRoll) {
        axes.setAngles(rotYaw, rotPitch, rotRoll);
    }

    //Used to stop self collision
    public boolean isPartOfThis(Entity ent) {
        for (EntitySeat seat : seats) {
            if (seat == null)
                continue;
            if (ent == seat)
                return true;
            if (seat.riddenByEntity == ent)
                return true;
        }
        return ent == this;
    }

    @Override
    public float getShadowSize() {
        return 0.0F;
    }

    public DriveableType getDriveableType() {
        return DriveableType.getDriveable(driveableType);
    }

    public DriveableData getDriveableData() {
        return driveableData;
    }

    @Override
    public boolean isDead() {
        return isDead;
    }

    @Override
    public Entity getControllingEntity() {
        return seats[0].getControllingEntity();
    }

    @Override
    public ItemStack getPickedResult(MovingObjectPosition target) {
        ItemStack stack = new ItemStack(getDriveableType().item, 1, 0);
        stack.stackTagCompound = new NBTTagCompound();
        driveableData.writeToNBT(stack.stackTagCompound);
        return stack;
    }

    public boolean hasEnoughFuel() {
        //if (seats == null || seats[0] == null || seats[0].riddenByEntity == null)
        //return false;
        return driverIsCreative() || driveableData.fuelInTank > Math.abs(driveableData.engine.fuelConsumption * throttle);
    }

    //Physics time! Oooh yeah

    public double getSpeedXYZ() {
        return Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
    }

    public double getSpeedXZ() {
        return Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    public double getHackySpeedXYZ() {
        double dx = (posX - lastTickPosX);
        double dy = (posY - lastTickPosY);
        double dz = (posZ - lastTickPosZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
        // Blocks per tick.
    }

    /**
     * To be overriden by vehicles to get alternate collision system
     */
    public boolean landVehicle() {
        return false;
    }

    /**
     * Overriden by planes for wheel parts
     */
    public boolean gearDown() {
        return true;
    }

    /**
     * Whether or not the plane is on the ground
     * TODO : Replace with proper check based on wheels
     */
    public boolean onGround() {
        return onGround;
    }


    //Collision mechanism mkII
    @SuppressWarnings("unused")
    public void moveRiders(Entity rider) {
        if (isPartOfThis(rider)) return;
        boolean isHuman = false;
        boolean isDriveable = false;
        if (!(rider instanceof EntityPlayer)) return;


        Vector3f riderPos = new Vector3f(rider.posX, rider.posY, rider.posZ);
        Vector3f riderMotion = new Vector3f(rider.motionX, rider.motionY, rider.motionY);
        Vector3f vehicleMotion = new Vector3f(posX - lastPos.x, posY - lastPos.y, posZ - lastPos.z);
        if (rider instanceof EntityVehicle) vehicleMotion = ((EntityVehicle) rider).lastPos;
        //riderMotion = Vector3f.sub(riderMotion, vehicleMotion, riderMotion);
        Vector3f vehiclePos = new Vector3f(this.posX, this.posY, this.posZ);
        Vector3f relativePos = Vector3f.sub(riderPos, vehiclePos, null);
        if (rider instanceof EntityPlayer) isHuman = true;
        if (rider instanceof EntityDriveable) isDriveable = true;
        relativePos = new Vector3f(relativePos.x, relativePos.y - ((isHuman) ? 0.55F : 0), relativePos.z);

        Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePos);
        Vector3f rotatedMotionVector = axes.findGlobalVectorLocally(riderMotion);

        Vector3f ellipsoid = new Vector3f(rider.width / 2, rider.height, rider.width / 2);

        CollisionTest test = new CollisionTest(ellipsoid, new Vector3f(relativePos.x, relativePos.y, relativePos.z), riderMotion);
        test.collisionRecursiveDepth = 0;

        Vector3f eSpacePosition = test.ConvertR3ToESpace(test.R3Position);
        Vector3f eSpaceVelocity = test.velocity;

        //Vector3f finalPos = collideWithDriveable(test, eSpacePosition, eSpaceVelocity);
        DriveableType type = getDriveableType();
        //Check parts for collision
        if (type.fancyCollision) {
            //checkCollision(test, getDriveableType().colbox);
            for (CollisionShapeBox sbox : type.collisionBox) {
                checkCollision(test, sbox);
            }
        } else {
            for (DriveablePart ppart : getDriveableData().parts.values()) {
                ppart.rayTraceRider(this, test);
            }
        }

        if (test.didCollide) {
            Vector3f finalPos = collideWithDriveable(test, eSpacePosition, eSpaceVelocity);

            if (finalPos == null) {
                finalPos = new Vector3f(0, 0, 0);
                if (FlansMod.debugMode) FlansMod.log("EntityDriveable.java moveRiders> finalPos is null [1]");
            }

            Vector3f velocity = Vector3f.sub(finalPos, test.basePoint, null);
            test.ConvertESpaceToR3(velocity);
            finalPos = new Vector3f(finalPos.x * test.eRad.x, finalPos.y * test.eRad.y, finalPos.z * test.eRad.z);
            // TODO: Better way to check this
            if (finalPos == null) {
                finalPos = new Vector3f(0, 0, 0);
                if (FlansMod.debugMode) FlansMod.log("EntityDriveable.java moveRiders> finalPos is null [2]");
            }
            Vector3f diff = Vector3f.sub(finalPos, vehiclePos, null);


            if (rider.onGround && (posY + finalPos.y + 10 / 16F) < riderPos.y) {
                //finalPos = new Vector3f(finalPos.x, 0, finalPos.z);
            }
            //boolean onTop = (rider.posY + 0.65 > test.intersectionPoint.y);

            boolean stationary = (throttle == 0);

            //If finalPos returns null, do something about it. Probably not the best way to handle this.
            //if(finalPos == null) finalPos = new Vector3f(0,0,0);

            test.ConvertESpaceToR3(finalPos);
            boolean onTop = (test.collisionPlaneNormal.y >= 0.5F);
            if (posY + finalPos.y + 10 / 16F < riderPos.y) finalPos.y = (riderPos.y - (float) posY - 10F / 16F);
            if (!hugeBoat)
                rider.setPosition((!onTop) ? riderPos.x + finalPos.x / (48 * Math.abs(relativePos.x)) : riderPos.x, (onTop) ? posY + finalPos.y + 10 / 16F : riderPos.y, (!onTop) ? riderPos.z + finalPos.z / (48 * Math.abs(relativePos.z)) : riderPos.z);
            //test.ConvertESpaceToR3(test.intersectionPoint);
            //FlansMod.proxy.spawnParticle("flame", test.intersectionPoint.x + posX, test.intersectionPoint.y + posY - 1, test.intersectionPoint.z + posZ, 0, 0, 0);

            if (hugeBoat && !stationary) {
                rider.setPosition(riderPos.x, posY + finalPos.y + 9.5 / 16F, riderPos.z);
            } else if (hugeBoat && stationary) {
                rider.setPosition(riderPos.x, posY + finalPos.y + 10 / 16F, riderPos.z);
            }
            finalPos = Vector3f.sub(finalPos, riderPos, null);
            finalPos.normalise();

            //rider.motionX = rider.motionX * finalPos.x;
            rider.motionY = 0;
            //rider.motionZ = rider.motionZ * finalPos.z;


            //Vector3f intersect = test.intersectionPoint;
            //worldObj.spawnParticle((test.isOnTop)?"fireworksSpark":"explode", posX + intersect.x, posY + intersect.y, posZ + intersect.z, 0,1,0);
            //worldObj.spawnParticle((test.isOnTop)?"fireworksSpark":"explode", posX, posY, posZ, 0,1,0);
            //worldObj.spawnParticle((test.isOnTop)?"fireworksSpark":"explode", riderPos.x, riderPos.y, riderPos.z, 0,1,0);

            //worldObj.spawnParticle("crit", posX + finalPos.x, posY + finalPos.y, posZ + finalPos.z, 0,0,0);
            //worldObj.spawnParticle("reddust", riderPos.x, riderPos.y - 0.65, riderPos.z, 0,0,0);


            updateRiderPos(rider, test, finalPos, riderMotion);

            if (getDriveableType().collisionDamageEnable && !test.isOnTop) {
                if (throttle > getDriveableType().collisionDamageThrottle) {
                    boolean canDamage = true;
                    if (TeamsManager.getInstance() != null && TeamsManager.getInstance().currentRound != null && rider instanceof EntityPlayerMP && seats[0].riddenByEntity instanceof EntityPlayer) {
                        EntityPlayerMP attacker = (EntityPlayerMP) seats[0].riddenByEntity;
                        EntityPlayerMP player = (EntityPlayerMP) rider;
                        if (TeamsManager.getInstance().currentRound.gametype.getPlayerData(attacker) != null && TeamsManager.getInstance().currentRound.gametype.getPlayerData(attacker).team != null) {
                            if (TeamsManager.getInstance().currentRound.gametype.getPlayerData(player) != null && TeamsManager.getInstance().currentRound.gametype.getPlayerData(player).team != null) {
                                if (TeamsManager.getInstance().currentRound.gametype.getPlayerData(player).team == TeamsManager.getInstance().currentRound.gametype.getPlayerData(attacker).team) {
                                    canDamage = false;
                                }
                            }
                        }
                    }
                    for (EntitySeat seat : seats) {
                        if (rider == seat.lastRiddenByEntity) {
                            canDamage = false;
                            break;
                        }
                    }

                    if (canDamage) {
                        if (rider instanceof EntityLiving) {
                            rider.attackEntityFrom(DamageSource.generic, throttle * getDriveableType().collisionDamageTimes);
                        } else if (rider instanceof EntityPlayer) {
                            rider.attackEntityFrom(DamageSource.generic, throttle * getDriveableType().collisionDamageTimes);
                        }
                    }
                }
            }
            if (rider instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) rider;
                //playerIDs.add(player);
                player.onGround = true;
                player.isAirBorne = false;
                player.fallDistance = 0;
            }

        } else {
            if (rider instanceof EntityDriveable) {
                //((EntityDriveable)rider).onDeck = false;
                ((EntityDriveable) rider).deckHeight = 0;
            }
        }


    }

    /**
     * @SubscribeEvent public void updateRiders(LivingUpdateEvent event){
     * <p>
     * for(EntityPlayer player: playerIDs){
     * Entity p = (Entity)player;
     * if(p == event.entity){
     * player.onGround = true;
     * player.isAirBorne = false;
     * player.fallDistance = 0;
     * playerIDs.remove(player);
     * }
     * }
     * }
     */

    public DamageSource getBulletDamage(boolean headshot) {
        DriveableType type = getDriveableType();
        EntityLivingBase owner = (EntityLivingBase) seats[0].riddenByEntity;
        if (owner instanceof EntityPlayer)
            return (new EntityDamageSourceFlans(getDriveableType().shortName, this, (EntityPlayer) owner, type, headshot, false)).setProjectile();
        else return (new EntityDamageSourceIndirect(type.shortName, this, owner)).setProjectile();
    }

    public void checkCollision(CollisionTest tester, CollisionShapeBox box) {
        {
            double distance = tester.nearestDistance;
            Vector3f collisionPoint = new Vector3f(0, 0, 0);
            int surface = 0;

            Vector3f pos = new Vector3f(this.posX, this.posY, this.posZ);

            RotatedAxes shift = axes;

            float f4 = box.pos.x + box.size.x;
            float f5 = -box.pos.y + box.size.y;
            float f6 = box.pos.z + box.size.z;

            box.pos = new Vector3f(box.pos.x, box.pos.y, box.pos.z);
            //if(EnumDriveablePart.getPart(box.part) == EnumDriveablePart.turret) return;
            //Define box verticies, where z > 0 is right. See shapeboxes in the toolbox for a visual reference
            Vector3f p1 = new Vector3f(box.pos.x - box.p1.x, box.pos.y + box.size.y + box.p1.y - box.size.y + 0.625F, box.pos.z - box.p1.z);
            Vector3f p2 = new Vector3f(box.pos.x + box.size.x + box.p2.x, box.pos.y + box.size.y + box.p2.y - box.size.y + 0.625F, box.pos.z - box.p2.z);
            Vector3f p3 = new Vector3f(box.pos.x + box.size.x + box.p3.x, box.pos.y + box.size.y + box.p3.y - box.size.y + 0.625F, box.pos.z + box.size.z + box.p3.z);
            Vector3f p4 = new Vector3f(box.pos.x - box.p4.x, box.pos.y + box.size.y + box.p4.y - box.size.y + 0.625F, box.pos.z + box.size.z + box.p4.z);
            Vector3f p5 = new Vector3f(box.pos.x - box.p5.x, box.pos.y - box.p5.y - box.size.y + 0.625F, box.pos.z - box.p5.z);
            Vector3f p6 = new Vector3f(box.pos.x + box.size.x + box.p6.x, box.pos.y - box.p6.y - box.size.y + 0.625F, box.pos.z - box.p6.z);
            Vector3f p7 = new Vector3f(box.pos.x + box.size.x + box.p7.x, box.pos.y - box.p7.y - box.size.y + 0.625F, box.pos.z + box.size.z + box.p7.z);
            Vector3f p8 = new Vector3f(box.pos.x - box.p8.x, box.pos.y - box.p8.y - box.size.y + 0.625F, box.pos.z + box.size.z + box.p8.z);

            if (EnumDriveablePart.getPart(box.part) == EnumDriveablePart.turret && seats[0] != null) {
                p1 = getPositionOnTurret(p1, false); //Front upper left
                p2 = getPositionOnTurret(p2, false); //Front upper right
                p3 = getPositionOnTurret(p3, false); //Rear upper right
                p4 = getPositionOnTurret(p4, false); //Rear upper left
                p5 = getPositionOnTurret(p5, false); //Front lower left
                p6 = getPositionOnTurret(p6, false); //Front lower right
                p7 = getPositionOnTurret(p7, false); //Rear lower right
                p8 = getPositionOnTurret(p8, false); //Rear lower left
            } else {
                p1 = shift.findLocalVectorGlobally(p1);
                p2 = shift.findLocalVectorGlobally(p2);
                p3 = shift.findLocalVectorGlobally(p3);
                p4 = shift.findLocalVectorGlobally(p4);
                p5 = shift.findLocalVectorGlobally(p5);
                p6 = shift.findLocalVectorGlobally(p6);
                p7 = shift.findLocalVectorGlobally(p7);
                p8 = shift.findLocalVectorGlobally(p8);
            }


            //Check top face
            double topFaceDist = 100;

            tester.checkTriangle(tester, p3, p2, p1);
            if (tester.didCollide && tester.nearestDistance < distance) {
                collisionPoint = tester.intersectionPoint;
                surface = 1;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            tester.checkTriangle(tester, p4, p3, p1);
            if (tester.didCollide && tester.nearestDistance < distance) {
                collisionPoint = tester.intersectionPoint;
                surface = 1;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            if (tester.didCollide) {
                tester.isOnTop = true;
                topFaceDist = tester.nearestDistance;
            }

            //Check front face
            tester.checkTriangle(tester, p6, p7, p3);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 2;
                tester.part = EnumDriveablePart.getPart(box.part);
            }
            tester.checkTriangle(tester, p3, p2, p6);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 2;
                tester.part = EnumDriveablePart.getPart(box.part);
            }


            //Check rear face
            tester.checkTriangle(tester, p4, p1, p5);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 3;
                tester.part = EnumDriveablePart.getPart(box.part);
            }
            tester.checkTriangle(tester, p5, p8, p4);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 3;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            //Check Left Face
            tester.checkTriangle(tester, p6, p5, p1);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 4;
                tester.part = EnumDriveablePart.getPart(box.part);
            }
            tester.checkTriangle(tester, p1, p2, p6);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 4;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            //Check right face
            tester.checkTriangle(tester, p8, p7, p3);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 5;
                tester.part = EnumDriveablePart.getPart(box.part);
            }
            tester.checkTriangle(tester, p3, p4, p8);
            if (tester.didCollide && tester.nearestDistance < distance) {
                distance = tester.nearestDistance;
                collisionPoint = tester.intersectionPoint;
                surface = 5;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            //Check bottom face
            tester.checkTriangle(tester, p5, p6, p7);
            if (tester.didCollide && tester.nearestDistance < distance) {
                collisionPoint = tester.intersectionPoint;
                surface = 1;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            tester.checkTriangle(tester, p8, p7, p5);
            if (tester.didCollide && tester.nearestDistance < distance) {
                collisionPoint = tester.intersectionPoint;
                surface = 1;
                tester.part = EnumDriveablePart.getPart(box.part);
            }

            if (tester.didCollide) {
                tester.isOnTop = true;
                topFaceDist = tester.nearestDistance;
            }

            Vector3f.add(p1, pos, p1);
            Vector3f.add(p2, pos, p2);
            Vector3f.add(p3, pos, p3);
            Vector3f.add(p4, pos, p4);
            Vector3f.add(p5, pos, p5);
            Vector3f.add(p6, pos, p6);
            Vector3f.add(p7, pos, p7);
            Vector3f.add(p8, pos, p8);

            String particleType = "crit";


            if (FlansMod.DEBUG) {
                FlansMod.proxy.spawnParticle(particleType, p1.x, p1.y, p1.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p2.x, p2.y, p2.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p3.x, p3.y, p3.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p4.x, p4.y, p4.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p5.x, p5.y, p5.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p6.x, p6.y, p6.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p7.x, p7.y, p7.z, 0, 0, 0);
                FlansMod.proxy.spawnParticle(particleType, p8.x, p8.y, p8.z, 0, 0, 0);

                renderTri(p1, p2, p3);
                renderTri(p3, p4, p1);
            }
            
            if (tester.nearestDistance < topFaceDist) tester.isOnTop = false;


            if (surface == 1) tester.isOnTop = true;
        }

    }

    public void renderTri(Vector3f p1, Vector3f p2, Vector3f p3) {
        Vector3f pos = new Vector3f(posX, posY, posZ);
        Vector3f p1a = Vector3f.add(p1, pos, null);
        Vector3f p2a = Vector3f.add(p2, pos, null);
        Vector3f p3a = Vector3f.add(p3, pos, null);

        renderLine(p1a, p2a);
        renderLine(p2a, p3a);
        renderLine(p3a, p1a);
    }

    public void renderLine(Vector3f in, Vector3f out) {
        float dx = out.x - in.x;
        float dy = out.y - in.y;
        float dz = out.z - in.z;
        Vector3f diff = Vector3f.sub(out, in, null);
        diff.normalise();
        float distance = (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
        for (int i = 0; i < 10; i++) {
            float dist2 = (distance / 10) * i;
            Vector3f newVec = new Vector3f(in.x + (dist2 * diff.x), in.y + (dist2 * diff.y), in.z + (dist2 * diff.z));
            FlansMod.proxy.spawnParticle("reddust", newVec.x, newVec.y, newVec.z, 0, 0, 0);
        }
    }


    public Vector3f collideWithDriveable(CollisionTest tester, Vector3f Pos, Vector3f vel) {
        float unitScale = 1 / 16F;
        float veryCloseDistance = 0.005F * unitScale;

        if (tester.collisionRecursiveDepth > 2) return Pos;

        tester.basePoint = Pos;
        tester.didCollide = false;

        if (getDriveableType().fancyCollision) {
            //checkCollision(tester, getDriveableType().colbox);
            for (CollisionShapeBox sbox : getDriveableType().collisionBox) {
                checkCollision(tester, sbox);
            }
        } else {
            for (DriveablePart ppart : getDriveableData().parts.values()) {
                ppart.rayTraceRider(this, tester);
            }
        }

        //If no collision, we just move along the velocity
        if (tester.didCollide = false) return Vector3f.add(Pos, vel, null);


        //Collision occurred, time to sort this out
        Vector3f destinationPoint = Vector3f.add(Pos, vel, null);
        Vector3f newBasePoint = Pos;

        if (tester.nearestDistance >= veryCloseDistance) {
            vel.normalise();
            vel.scale((float) (tester.nearestDistance - veryCloseDistance));
            newBasePoint = Vector3f.add(tester.basePoint, vel, null);

            if (vel.normalise().equals(new Vector3f(0, 0, 0))) return Vector3f.add(Pos, vel, null);

            vel.normalise();

            //Change polygon intersection point so that the sliding plane is unaffected by the fact we move slightly less than collision tells us
            Vector3f.sub(tester.intersectionPoint, new Vector3f(vel.x * veryCloseDistance, vel.y * veryCloseDistance, vel.z * veryCloseDistance), tester.intersectionPoint);
        }

        //Determine the sliding plane
        Vector3f slidePlaneOrigin = tester.intersectionPoint;
        if (tester.intersectionPoint == null) return Vector3f.add(Pos, vel, null);
        Vector3f slidePlaneNormal = Vector3f.sub(newBasePoint, tester.intersectionPoint, null);
        slidePlaneNormal.normalise();

        tester.collisionPlaneNormal = slidePlaneNormal;
        CollisionPlane plane = new CollisionPlane(slidePlaneOrigin, slidePlaneNormal);

        double sDV = plane.signedDistanceTo(destinationPoint);
        Vector3f scaledNormal = new Vector3f(slidePlaneNormal.x * sDV, slidePlaneNormal.y * sDV, slidePlaneNormal.z * sDV);
        Vector3f newDestPoint = Vector3f.sub(destinationPoint, scaledNormal, null);

        //Generate slide vector
        Vector3f newVelocityVector = Vector3f.sub(newDestPoint, tester.intersectionPoint, null);

        if (newVelocityVector.length() < veryCloseDistance) {
            return newBasePoint;
        }

        tester.collisionRecursiveDepth++;

        return collideWithDriveable(tester, newBasePoint, newVelocityVector);
    }

    @SuppressWarnings("unused")
    public void updateRiderPos(Entity rider, CollisionTest test, Vector3f pos, Vector3f motion) {
        boolean isDriveable = false;
        if (rider instanceof EntityDriveable) isDriveable = true;
        Vector3f vehicleMotion = lastPos;

        Vector3f riderMountPoint = new Vector3f(rider.posX - posX, rider.posY - posY, rider.posZ - posZ);

        float yawDiff = axes.getYaw() - prevAxes.getYaw();
        float pitchDiff = axes.getPitch() - prevAxes.getPitch();
        float rollDiff = axes.getRoll() - prevAxes.getRoll();

        RotatedAxes velAxes = new RotatedAxes(axes.getYaw() + yawDiff, axes.getPitch() + pitchDiff, axes.getRoll() + rollDiff);

        Vector3f currentLocalPos = axes.findGlobalVectorLocally(riderMountPoint);
        Vector3f nextGlobalPos = velAxes.findLocalVectorGlobally(currentLocalPos);

        Vector3f diff = new Vector3f(0, 0, 0);

        //Some rubbish null checks
        if (nextGlobalPos == null) nextGlobalPos = new Vector3f(0, 0, 0);

        Vector3f.add(vehicleMotion, diff, diff);
        rider.setPosition(nextGlobalPos.x + posX + ((hugeBoat) ? diff.x / (1.5) : 0), (!isDriveable) ? rider.posY : ((EntityDriveable) rider).deckHeight, nextGlobalPos.z + posZ + ((hugeBoat) ? diff.z / (1.5) : 0));


        if (hugeBoat) {
            if (lastPos.x == 0 && lastPos.y == 0 && lastPos.z == 0) {
                if (rider.motionY < 0) rider.motionY = 0;
            }
        } else {
            if (lastPos.x != 0 || lastPos.y != 0 || lastPos.z != 0) {
                rider.motionX = diff.x;
                rider.motionY = diff.y;
                rider.motionZ = diff.z;
            }
        }
    }

    /**
     * Attack method called by bullets hitting the plane. Does advanced raytracing to detect which part of the plane is hit
     */
    public ArrayList<BulletHit> attackFromBullet(Vector3f origin, Vector3f motion) {
        //Make an array to contain the hits
        ArrayList<BulletHit> hits = new ArrayList<BulletHit>();
        //Get the position of the bullet origin, relative to the centre of the plane, and then rotate the vectors onto local co-ordinates
        Vector3f relativePosVector = Vector3f.sub(origin, new Vector3f((float) posX, (float) posY, (float) posZ), null);
        Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePosVector);
        Vector3f rotatedMotVector = axes.findGlobalVectorLocally(motion);
        //Check each part
        for (DriveablePart part : getDriveableData().parts.values()) {
            //Ray trace the bullet
            DriveableHit hit = part.rayTrace(this, rotatedPosVector, rotatedMotVector);
            if (hit != null)
                hits.add(hit);
        }
        return hits;
    }

    /**
     * Attack method called by bullets hitting the plane. Does advanced raytracing to detect which part of the plane is hit
     */
    public ArrayList<BulletHit> attackFromBulletButBetter(Vector3f origin, Vector3f motion, float size) {
        ArrayList<BulletHit> hits = new ArrayList<BulletHit>();


        Vector3f vehicleMotion = new Vector3f(posX - lastPos.x, posY - lastPos.y, posZ - lastPos.z);
        //riderMotion = Vector3f.sub(riderMotion, vehicleMotion, riderMotion);
        Vector3f vehiclePos = new Vector3f(this.posX, this.posY, this.posZ);
        Vector3f relativePos = Vector3f.sub(origin, vehiclePos, null);

        Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePos);
        Vector3f rotatedMotionVector = axes.findGlobalVectorLocally(motion);

        Vector3f ellipsoid = new Vector3f(size, size, size);

        CollisionTest test = new CollisionTest(ellipsoid, new Vector3f(relativePos.x, relativePos.y, relativePos.z), motion);
        test.collisionRecursiveDepth = 0;

        Vector3f eSpacePosition = test.ConvertR3ToESpace(test.R3Position);
        Vector3f eSpaceVelocity = test.velocity;

        for (DriveablePart ppart : getDriveableData().parts.values()) {
            ppart.rayTraceRider(this, test);
        }

        if (test.didCollide) {
            Vector3f hitPos = new Vector3f(0, 0, 0);
            Vector3f intersect2 = new Vector3f(test.ConvertESpaceToR3(test.intersectionPoint));
            Vector3f.sub(origin, intersect2, hitPos);
            float f = (hitPos.length() / motion.length());
            DriveableHit hit = new DriveableHit(this, test.part, f);
            hits.add(hit);
        }

        return hits;

    }

    /**
     * Called if the bullet actually hit the part returned by the raytrace
     *
     * @param penetratingPower
     */
    public float bulletHit(EntityBullet bullet, DriveableHit hit, float penetratingPower) {
        DriveablePart part = getDriveableData().parts.get(hit.part);
        if (bullet != null)
            penetratingPower = part.hitByBullet(bullet, hit, penetratingPower);
        else
            penetratingPower -= 5F;

        //This is server side bsns
        if (!worldObj.isRemote) {
            checkParts();
            //If it hit, send a damage update packet
            FlansMod.getPacketHandler().sendToAllAround(new PacketDriveableDamage(this), posX, posY, posZ, 100, dimension);
        }

        return penetratingPower;
    }

    /**
     * A simple raytracer for the driveable. Called by tools
     */
    public DriveablePart raytraceParts(Vector3f origin, Vector3f motion) {
        //Get the position of the bullet origin, relative to the centre of the plane, and then rotate the vectors onto local co-ordinates
        Vector3f relativePosVector = Vector3f.sub(origin, new Vector3f((float) posX, (float) posY, (float) posZ), null);
        Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePosVector);
        Vector3f rotatedMotVector = axes.findGlobalVectorLocally(motion);
        //Check each part
        for (DriveablePart part : getDriveableData().parts.values()) {
            //Ray trace the bullet
            if (part.rayTrace(this, rotatedPosVector, rotatedMotVector) != null) {
                return part;
            }
        }
        return null;
    }

    /**
     * For overriding for toggles such as gear up / down on planes
     */
    public boolean canHitPart(EnumDriveablePart part) {
        return true;
    }

    /**
     * Internal method for checking that all parts are ok, destroying broken ones, dropping items and making sure that child parts are destroyed when their parents are
     */
    public void checkParts() {
        for (DriveablePart part : getDriveableData().parts.values()) {
            if (part != null && !part.dead && part.health <= 0 && part.maxHealth > 0) {
                killPart(part);
            }
        }

        //If the core was destroyed, kill the driveable
        if (getDriveableData().parts.get(EnumDriveablePart.core).dead) {
            int seatNum = seats.length;

            DriveableType type = getDriveableType();

            if (!worldObj.isRemote) {
                for (EntitySeat seat : seats) {
                    if (seat.riddenByEntity instanceof EntityPlayer) {
//						((EntityPlayer)seats[i].riddenByEntity).addPotionEffect(new PotionEffect(Potion.harm.id, 10, 5));
                        Entity entity = seat.riddenByEntity;
                        seat.riddenByEntity.mountEntity(null);
                        if (this.lastAtkEntity instanceof EntityPlayer) {
                            entity.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) this.lastAtkEntity), 10000000);
                        } else if (this.lastAtkEntity instanceof EntityLivingBase) {
                            entity.attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase) this.lastAtkEntity), 10000000);
                        }
                    }
                }

                if (type.isExplosionWhenDestroyed)
//Create a flans mod explosion rather than a standard MC one. allows control over death boom   	
    	        {
        	        new FlansModExplosion(worldObj, this, null, type, posX, posY, posZ,
        		        	type.deathExplosionRadius, type.deathExplosionPower,TeamsManager.explosions && type.deathExplosionBreaksBlocks,
        		        	type.deathExplosionDamageVsLiving, type.deathExplosionDamageVsPlayer, type.deathExplosionDamageVsPlane, type.deathExplosionDamageVsVehicle, seatNum, seatNum);
        	
        	        }
        		if(!worldObj.isRemote && type.deathFireRadius > 0.1F)
        		{
        			for(float i = -type.deathFireRadius; i < type.deathFireRadius; i++)
        			{
        				for(float j = -type.deathFireRadius; j < type.deathFireRadius; j++)
        				{
        					for(float k = -type.deathFireRadius; k < type.deathFireRadius; k++)
        					{
        						int x = MathHelper.floor_double(i + posX);
        						int y = MathHelper.floor_double(j + posY);
        						int z = MathHelper.floor_double(k + posZ);
        						if(i * i + j * j + k * k <= type.deathFireRadius * type.deathFireRadius && worldObj.getBlock(x, y, z) == Blocks.air && rand.nextBoolean())
        						{
        							worldObj.setBlock(x, y, z, Blocks.fire, 0, 3);
        						}
        					}
        				}
        			}
        		}

                for (DriveablePart part : driveableData.parts.values()) {
                    if (part.health > 0 && !part.dead)
                        killPart(part);
                }
            }
            setDead();

        }

    }

    public void checkPartsWhenAttacked() {
        for (DriveablePart part : getDriveableData().parts.values()) {
            if (part != null && !part.dead && part.health <= 0 && part.maxHealth > 0) {
                killPart(part);
            }
        }
    }

    /**
     * Internal method for killing driveable parts
     */
    private void killPart(DriveablePart part) {
        if (part.dead)
            return;
        part.health = 0;
        part.dead = true;

        //Drop items
        DriveableType type = getDriveableType();
        if (!worldObj.isRemote) {
            Vector3f pos = new Vector3f(0, 0, 0);

            //Get the midpoint of the part
            if (part.box != null)
                pos = axes.findLocalVectorGlobally(new Vector3f(part.box.x / 16F + part.box.w / 32F, part.box.y / 16F + part.box.h / 32F, part.box.z / 16F + part.box.d / 32F));

            ArrayList<ItemStack> drops = type.getItemsRequired(part, getDriveableData().engine);
            if (drops != null) {
                //Drop each ItemStack
                for (ItemStack stack : drops) {
                    worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX + pos.x, posY + pos.y, posZ + pos.z, stack.copy()));
                }
            }
            dropItemsOnPartDeath(pos, part);

            //Inventory is in the core, so drop it if the core is broken
            if (part.type == EnumDriveablePart.core) {
                for (int i = 0; i < getDriveableData().getSizeInventory(); i++) {
                    ItemStack stack = getDriveableData().getStackInSlot(i);
                    if (stack != null) {
                        worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX + rand.nextGaussian(), posY + rand.nextGaussian(), posZ + rand.nextGaussian(), stack));
                    }
                }
            }
        }

        //Kill all child parts to stop things floating unconnected
        for (EnumDriveablePart child : part.type.getChildren()) {
            killPart(getDriveableData().parts.get(child));
        }
    }

    /**
     * Method for planes, vehicles and whatnot to drop their own specific items if they wish
     */
    protected abstract void dropItemsOnPartDeath(Vector3f midpoint, DriveablePart part);

    @Override
    public float getPlayerRoll() {
        return axes.getRoll();
    }

    @Override
    public void explode() {

    }

    @Override
    public float getCameraDistance() {
        return getDriveableType().cameraDistance;
    }

    public boolean isPartIntact(EnumDriveablePart part) {
        DriveablePart thisPart = getDriveableData().parts.get(part);
        return thisPart.maxHealth == 0 || thisPart.health > 0;
    }

    public abstract boolean hasMouseControlMode();

    public abstract String getBombInventoryName();

    public abstract String getMissileInventoryName();

    public boolean rotateWithTurret(Seat seat) {
        return seat.part == EnumDriveablePart.turret;
    }

    @Override
    public String getCommandSenderName() {
        return getDriveableType().name;
    }

    @SideOnly(Side.CLIENT)
    public boolean showInventory(int seat) {
        return seat != 0 || !FlansModClient.controlModeMouse;
    }

    //-------------------------------------
    // Getters and setters for dual fields
    //-------------------------------------

    public float getShootDelay(boolean secondary) {
        return secondary ? shootDelaySecondary : shootDelayPrimary;
    }

    public boolean canLaunchIT1() {
        return canFireIT1;
    }

    public float getMinigunSpeed(boolean secondary) {
        return secondary ? minigunSpeedSecondary : minigunSpeedPrimary;
    }

    public int getCurrentGun(boolean secondary) {
        return secondary ? currentGunSecondary : currentGunPrimary;
    }

    public void setShootDelay(float i, boolean secondary) {
        setRecoilTimer();
        // If current delay is greater than i, use that. If current delay is less than 0, add that to new shoot delay
        if (secondary)
            shootDelaySecondary = i > shootDelaySecondary ? (shootDelaySecondary < 0 ? i + shootDelaySecondary : i) : shootDelaySecondary;
        else
            shootDelayPrimary = i > shootDelayPrimary ? (shootDelayPrimary < 0 ? i + shootDelayPrimary : i) : shootDelayPrimary;
    }

    public void setMinigunSpeed(float f, boolean secondary) {
        if (secondary)
            minigunSpeedSecondary = f;
        else minigunSpeedPrimary = f;
    }

    public void setCurrentGun(int i, boolean secondary) {
        if (secondary)
            currentGunSecondary = i;
        else currentGunPrimary = i;
    }

    public void setEntityMarker(int tick) {
        this.isShowedPosition = true;
        this.tickCount = tick;
    }

    public void IT1Reload() {
        DriveableType type = getDriveableType();


        if (stage == 1) {
            //canFireIT1 = false;
            drakonDoorAngle = moveToTarget(drakonDoorAngle, 0, 5);
            drakonArmAngle = moveToTarget(drakonArmAngle, 0, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 5);

            if (drakonRailAngle == -10) stage++;
        }


        if (stage == 2) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, -90, 5);
            drakonArmAngle = moveToTarget(drakonArmAngle, 0, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 1);

            if (drakonDoorAngle == -90) stage++;
        }

        if (stage == 3) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, -90, 5);
            drakonArmAngle = moveToTarget(drakonArmAngle, 179, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 1);

            if (drakonArmAngle == 179) stage++;
        }

        if (stage == 4) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, 0, 10);
            drakonArmAngle = moveToTarget(drakonArmAngle, 180, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 1);

            if (drakonDoorAngle == 0) {
                if (IT1Loaded()) {
                    stage++;
                    reloadAnimTime = 60;
                }
            }
        }

        if (stage == 5) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, -90, 10);
            drakonArmAngle = moveToTarget(drakonArmAngle, 180, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 1);
            reloadingDrakon = true;

            if (drakonDoorAngle == -90) stage++;
        }

        if (stage == 6) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, -90, 5);
            drakonArmAngle = moveToTarget(drakonArmAngle, 0, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, -10, 1);

            if (drakonArmAngle == 0) stage++;
        }

        if (stage == 7) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, 0, 10);
            drakonArmAngle = moveToTarget(drakonArmAngle, 0, 3);
            drakonRailAngle = moveToTarget(drakonRailAngle, 0, 1);

            if (drakonRailAngle == 0 && drakonDoorAngle == 0) {
                stage++;
                canFireIT1 = true;
                reloadingDrakon = false;
            }
        }

        if (stage == 8) {
            drakonDoorAngle = moveToTarget(drakonDoorAngle, 0, 10);
            drakonArmAngle = moveToTarget(drakonArmAngle, 0, 3);
            if (worldObj.isRemote && this.ticksExisted > 2)
                drakonRailAngle = moveToTarget(drakonRailAngle, -seats[0].looking.getPitch(), seats[0].seatInfo.aimingSpeed.y);
            //reloadAnimTime = 60;

            if (!IT1Loaded()) {
                stage = 1;
                canFireIT1 = false;
            }
        }
    }

    public float moveToTarget(float current, float target, float speed) {

        float pitchToMove = (float) ((Math.sqrt(target * target)) - Math.sqrt((current * current)));
        for (; pitchToMove > 180F; pitchToMove -= 360F) {
        }
        for (; pitchToMove <= -180F; pitchToMove += 360F) {
        }

        float signDeltaY = 0;
        if (pitchToMove > speed) {
            signDeltaY = 1;
        } else if (pitchToMove < -speed) {
            signDeltaY = -1;
        } else {
            signDeltaY = 0;
            return target;
        }


        if (current > target) {
            current = current - speed;
        } else if (current < target) {
            current = current + speed;
        }

        return current;
    }

    public boolean IT1Loaded() {
        DriveableType type = getDriveableType();
        boolean loaded = false;
        for (int i = driveableData.getMissileInventoryStart(); i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++) {
            ItemStack shell = driveableData.getStackInSlot(i);
            if (shell != null && shell.getItem() instanceof ItemBullet && type.isValidAmmo(((ItemBullet) shell.getItem()).type, EnumWeaponType.MISSILE)) {
                loaded = true;
            }
        }

        return loaded;
    }

    public void tryRecoil() {
        int slot = -1;
        DriveableType type = getDriveableType();
        for (int i = driveableData.getMissileInventoryStart(); i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++) {
            ItemStack shell = driveableData.getStackInSlot(i);
            if (shell != null && shell.getItem() instanceof ItemBullet && type.isValidAmmo(((ItemBullet) shell.getItem()).type, EnumWeaponType.SHELL)) {
                slot = i;
            }
        }

        if (recoilTimer <= 0 && slot != -1)
            isRecoil = true;
    }

    public void setRecoilTimer() {
        int slot = -1;
        DriveableType type = getDriveableType();
        for (int i = driveableData.getMissileInventoryStart(); i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++) {
            ItemStack shell = driveableData.getStackInSlot(i);
            if (shell != null && shell.getItem() instanceof ItemBullet && type.isValidAmmo(((ItemBullet) shell.getItem()).type, EnumWeaponType.SHELL)) {
                slot = i;
            }
        }

        if (recoilTimer <= 0 && slot != -1)
            recoilTimer = (int) getDriveableType().shootDelayPrimary;
    }

    // Returns if the bounding box is under the 
    public boolean isUnderWater() {
        return worldObj.isAnyLiquid(this.boundingBox.copy().offset(0, getDriveableType().maxDepth, 0));
    }
}
