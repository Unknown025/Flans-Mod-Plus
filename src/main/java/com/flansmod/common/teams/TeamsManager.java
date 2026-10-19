package com.flansmod.common.teams;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.guns.*;
import com.flansmod.common.network.*;
import com.flansmod.common.types.InfoType;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("unchecked")
public class TeamsManager {
    /**
     * Overall switch for teams mod
     */
    public static boolean enabled = true;
    /**
     * The instance
     */
    public static TeamsManager instance;

    //Configuration variables
    // Player changeable stuff
    public static boolean voting = false, explosions = true, roundsGenerator = false, driveablesBreakBlocks = true,
            bombsEnabled = true, shellsEnabled = true, bulletsEnabled = true, forceAdventureMode = true, canBreakGuns = true, canBreakGlass = true,
            armourDrops = true, vehiclesNeedFuel = true, overrideHunger = true, survivalCanBreakVehicles = true, survivalCanPlaceVehicles = true;

    public static int weaponDrops = 1; //0 = no drops, 1 = drops, 2 = smart drops
    //Life of certain entity types. 0 is eternal.
    public static int mgLife = 0, planeLife = 0, vehicleLife = 0, mechaLove = 0, aaLife = 0;

    /**
     * The number of ticks for which to display the round summary page
     */
    public static int scoreDisplayTime = 200;
    /**
     * The number of ticks for which to display the voting box, if enabled
     */
    public static int votingTime = 200;

    /**
     * The current round in play. This class replaces the old set of 3 fields "currentGametype", "currentMap" and "teams"
     */
    public TeamsRound currentRound;
    /**
     * This contains a list of all the valid rounds, similar to the old RotationEntry and map rotation
     */
    public ArrayList<TeamsRound> rounds;
    /**
     * The list of all available maps
     */
    public HashMap<String, TeamsMap> maps;

    /**
     * For assigning base IDs to bases. Used primarily in client-server syncing and saving
     */
    private int nextBaseID = 1;
    public ArrayList<ITeamBase> bases;
    public ArrayList<ITeamObject> objects;

    private long time;

    /**
     * A downwards counter that times the round (in ticks)
     */
    public int roundTimeLeft;
    /**
     * A downwards counter that times inter-round phases (in ticks)
     */
    public int interRoundTimeLeft;
    /**
     * The list of rounds currently being voted upon
     */
    public TeamsRound[] voteOptions;
    /**
     * For forcing the next round. Not normally used
     */
    public TeamsRound nextRound;

    /**
     * Whether to use autobalance
     */
    public static boolean autoBalance = true;
    /**
     * Time between autobalance attempts
     */
    public static int autoBalanceInterval = 20 * 20;
    public static boolean allowVehicleZoom;
    public static int bulletSnapshotMin = 0;
    public static int bulletSnapshotDivisor = 50;

    //Disused. Delete when done
    public GameType currentGameType;
    public TeamsMap currentMap;
    public Team[] teams;
    public List<RotationEntry> rotation;
    public int currentRotationEntry;

    public TeamsManager() {
        instance = this;
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        //Init arrays
        bases = new ArrayList<>();
        objects = new ArrayList<>();
        maps = new HashMap<>();
        rounds = new ArrayList<>();

        rotation = new ArrayList<>();
        currentMap = null;


        //Testing stuff
        new GameTypeTDM();
        new GameTypeZombies();
//        new GameTypeConquest();
        new GameTypeDM();
        new GameTypeCTF();
        //new GametypeNerf();
        //-----
    }

    public void reset() {
        currentGameType = null;
        //currentMap = TeamsMap.def;
        teams = null;

        currentRound = null;

        bases = new ArrayList<>();
        objects = new ArrayList<>();
        maps = new HashMap<>();
        rounds = new ArrayList<>();

        rotation = new ArrayList<>();
    }

    public static TeamsManager getInstance() {
        return instance;
    }

    public void tick() {
        //Send a full team info update to players every 2 seconds.
        if (time % 40 == 0) {
            FlansMod.getPacketHandler().sendToAll(new PacketTeamInfo());
            showTeamsMenuToAll(true);
        }

        if (!enabled)
            return;

        if (currentRound != null)
            currentRound.gametype.tick();
        time++;


        //Tick bases and objects
        for (ITeamBase base : bases)
            base.tick();
        for (ITeamObject object : objects)
            object.tick();
        if (overrideHunger && currentRound != null)
            for (World world : MinecraftServer.getServer().worldServers)
                for (Object player : world.playerEntities)
                    ((EntityPlayer) player).getFoodStats().addStats(20, 10F);

        //Check round timer
        //If inbetween rounds
        if (interRoundTimeLeft > 0) {
            interRoundTimeLeft--;
            //If we're done showing scores, show the voting box
            if (voting) {
                //If the next round is forced, go to it
                if (nextRound != null) {
                    startNextRound();
                    interRoundTimeLeft = 0;
                    return;
                } else {
                    if (interRoundTimeLeft == votingTime)
                        pickVoteOptions();
                    if (interRoundTimeLeft <= votingTime) {
                        if (voteOptions == null)
                            pickVoteOptions();
                        displayVotingGUI();
                    }
                }
            }
            //If the timer is finished, start the next round
            if (interRoundTimeLeft == 0) {
                startNextRound();
            }
        }

        //If in a round
        if (currentRound != null && roundTimeLeft > 0) {
            roundTimeLeft--;
            boolean roundEnded = roundTimeLeft == 0;
            if (roundEnded)
                messageAll(randomTimeOutString());
            for (Team team : currentRound.teams) {
                if (currentRound.gametype.teamHasWon(team)) {
                    roundEnded = true;
                    messageAll(team.name + " won the round!");
                }
            }

            if (autoBalance() && time % autoBalanceInterval == autoBalanceInterval - 200 && needAutobalance()) {
                TeamsManager.messageAll("§fAutobalancing teams...");
            }
            if (autoBalance() && time % autoBalanceInterval == 0 && needAutobalance()) {
                autobalance();
            }

            if (roundEnded) {
                //The round has ended on a timer, so display the scoreboard summary
                roundTimeLeft = 0;
                interRoundTimeLeft = voting ? (votingTime + scoreDisplayTime) : scoreDisplayTime;
                displayScoreboardGUI();
                currentRound.gametype.roundEnd();
                PlayerHandler.roundEnded();
            }
        }
    }

    public boolean needAutobalance() {
        if (!autoBalance() || currentRound == null || currentRound.teams.length != 2)
            return false;
        int membersTeamA = currentRound.teams[0].members.size();
        int membersTeamB = currentRound.teams[1].members.size();
        return Math.abs(membersTeamA - membersTeamB) > 1;
    }

    public void autobalance() {
        if (!autoBalance() || currentRound == null || currentRound.teams.length != 2)
            return;
        int membersTeamA = currentRound.teams[0].members.size();
        int membersTeamB = currentRound.teams[1].members.size();
        if (membersTeamA - membersTeamB > 1) {
            for (int i = 0; i < (membersTeamA - membersTeamB) / 2; i++) {
                //My goodness this is convoluted...
                EntityPlayerMP playerToKick = getPlayer(currentRound.teams[1]
                        .addPlayer(currentRound.teams[0].removeWorstPlayer()));
                PlayerData data = PlayerHandler.getPlayerData(playerToKick);
                movePlayerProcedure(playerToKick, data);
            }
        }
        if (membersTeamB - membersTeamA > 1) {
            for (int i = 0; i < (membersTeamB - membersTeamA) / 2; i++) {
                EntityPlayerMP playerToKick = getPlayer(currentRound.teams[0]
                        .addPlayer(currentRound.teams[1].removeWorstPlayer()));
                PlayerData data = PlayerHandler.getPlayerData(playerToKick);
                movePlayerProcedure(playerToKick, data);
            }
        }
    }

    private void movePlayerProcedure(EntityPlayerMP playerMP, PlayerData data) {
        data.playerMovedByAutobalancer = true;
        messagePlayer(playerMP, "You were moved to the other team by the autobalancer.");
        sendClassMenuToPlayer(playerMP);
        setPlayersNextSpawnpoint(playerMP);
    }

    public void switchToNextGameType() {
        PlayerHandler.roundEnded();
        currentRotationEntry = (currentRotationEntry + 1) % rotation.size();
        RotationEntry entry = rotation.get(currentRotationEntry);
        if (currentGameType != null && currentGameType != entry.gametype) {
            currentGameType.roundEnd();
        }
        currentGameType = entry.gametype;
        currentMap = entry.map;
        teams = entry.teams;
        currentGameType.roundStart();
    }

    public String randomTimeOutString() {
        switch (GameType.rand.nextInt(4)) {
            case 0:
                return "That's time!";
            case 1:
                return "How dull; a tie...";
            case 2:
                return "Everybody's a loser but the clock.";
            default:
                return "Time's up.";
        }
    }

    public void displayScoreboardGUI() {
        for (EntityPlayerMP player : getPlayers()) {
            PlayerData data = PlayerHandler.getPlayerData(player);
            if (!data.builder)
                sendPacketToPlayer(new PacketRoundFinished(scoreDisplayTime), player);
        }
    }

    public void displayVotingGUI() {
        for (EntityPlayerMP player : getPlayers()) {
            PlayerData data = PlayerHandler.getPlayerData(player);
            if (!data.builder)
                sendPacketToPlayer(new PacketVoting(this), player);
        }
    }

    public void pickVoteOptions() {
        Collections.sort(rounds);
        voteOptions = new TeamsRound[Math.min(5, rounds.size())];
        for (int i = 0; i < voteOptions.length; i++) {
            voteOptions[i] = rounds.get(i);
        }

        //Wildcard option!
        voteOptions[GameType.rand.nextInt(voteOptions.length)] = rounds.get(GameType.rand.nextInt(rounds.size()));
    }

    public void start() {
        if (!enabled || rounds.isEmpty())
            return;

        //Can only start once
        //if(currentRound != null)
        //	return;

        if (currentRound != null) {
            currentRound.gametype.roundCleanup();
            resetScores();
        }

        currentRound = rounds.get(0);
        startRound();
    }

    public void startNextRound() {
        if (!enabled || rounds.isEmpty())
            return;

        //If the next round has not been forced
        if (nextRound == null) {
            if (voting) {
                //Gather votes and decide which map to play
                int winner = 0;
                int mostVotes = 0;

                //Collect the votes from player data
                int[] numVotes = new int[voteOptions.length];
                for (PlayerData data : PlayerHandler.serverSideData.values()) {
                    if (data.vote > 0)
                        numVotes[data.vote - 1]++;
                }

                //Find the highest one
                for (int i = 0; i < voteOptions.length; i++) {
                    if (numVotes[i] > mostVotes) {
                        mostVotes = numVotes[i];
                        winner = i;
                    }
                }
                nextRound = voteOptions[winner];


                //Update ratings
                for (TeamsRound round : rounds)
                    round.roundsSincePlayed++;

                for (int i = 0; i < voteOptions.length; i++) {
                    if (i == winner) {
                        voteOptions[i].popularity = 1F - (1F - voteOptions[i].popularity) * 0.8F;
                        voteOptions[i].roundsSincePlayed = 0;
                    } else {
                        voteOptions[i].popularity *= 0.9F;
                        voteOptions[i].popularity += 0.01F;
                    }
                }

                //Clear votes
                for (PlayerData data : PlayerHandler.serverSideData.values())
                    data.vote = 0;
            } else //Use standard rotation. Go to next map
            {
                int lastRoundID = rounds.indexOf(currentRound);
                int nextRoundID = ++lastRoundID % rounds.size();
                nextRound = rounds.get(nextRoundID);
            }
        }

        //End the last round
        if (currentRound != null) {
            for (ITeamBase base : currentRound.map.bases)
                base.roundCleanup();
            currentRound.gametype.roundCleanup();
        }
        resetScores();

        //Advance to next round
        if (nextRound != null)
            currentRound = nextRound;
        //Note that if nextRound is null, we stay on the round we just played

        //Begin the next round
        startRound();

        //Reset this. Used for round forcing only.
        nextRound = null;
    }

    private void startRound() {
        if (roundsGenerator && rounds.size() < 4) {
            int roundCountForGenerate = 4 - rounds.size();
            generateRounds(roundCountForGenerate);
        }


        currentRound.gametype.roundStart();
        roundTimeLeft = currentRound.timeLimit * 60 * 20;
        for (ITeamBase base : bases) {
            base.startRound();
        }

        for (EntityPlayerMP player : getPlayers())
            forceRespawn(player);

        showTeamsMenuToAll();

        messageAll("§fA new round has started!");
    }

    private void generateRounds(int roundCountForGenerate) {
        Random rand = new Random();
        List<Team> allowedTeams = new ArrayList<>();
        List<GameType> allowedGameTypes = new ArrayList<>();
        for (Team team : Team.teams) {
            if (team.allowedForRoundsGenerator) allowedTeams.add(team);
        }
        for (GameType gameType : GameType.gameTypeList) {
            if (gameType.allowedForRoundsGenerator) allowedGameTypes.add(gameType);
        }

        for (int i = 0; i <= roundCountForGenerate; i++) {
            GameType nextGameType = allowedGameTypes.get(rand.nextInt(allowedGameTypes.size()));
            Team[] teamsToAdd = new Team[nextGameType.numTeamsRequired];
            teamsToAdd[0] = allowedTeams.get(rand.nextInt(allowedTeams.size()));
            teamsToAdd[1] = allowedTeams.get(rand.nextInt(allowedTeams.size()));
            int timeLimit = 10;
            int scoreLimit = 10;
            if (nextGameType instanceof GameTypeCTF) {
                scoreLimit = 5;
            } else if (nextGameType instanceof GameTypeTDM) {
                scoreLimit = 30;
            } else if (nextGameType instanceof GameTypeDM) {
                scoreLimit = 20;
            }
            rounds.add(new TeamsRound(
                    TeamsMap.mapList.get(rand.nextInt(TeamsMap.mapList.size())),
                    allowedGameTypes.get(rand.nextInt(allowedGameTypes.size())),
                    teamsToAdd,
                    timeLimit + rand.nextInt(10), scoreLimit
            ));
        }

    }

    /**
     * Called at the start of a round. Shows all players the team selection menu. Exludes people on the building / op team
     */
    public void showTeamsMenuToAll() {
        showTeamsMenuToAll(false);
    }

    public void showTeamsMenuToAll(boolean info) {
        for (EntityPlayerMP player : getPlayers()) {
            PlayerData data = PlayerHandler.getPlayerData(player);
            //Catch for broken player data
            if (data == null)
                continue;
            //Catch for people not on a team, such as builders
            if (data.builder && playerIsOp(player))
                continue;

            sendTeamsMenuToPlayer(player, info);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(EntityInteractEvent event) {
        if (event.entityPlayer.inventory.getCurrentItem() != null && event.entityPlayer.inventory.getCurrentItem().getItem() instanceof ItemOpStick)
            ((ItemOpStick) event.entityPlayer.inventory.getCurrentItem().getItem()).clickedEntity(event.entityPlayer.worldObj, event.entityPlayer, event.target);
    }

    /**
     * Stop damage being taken when it shouldn't
     * N - NoTeam, S - Spectator, 1 - Team 1, 2 - Team 2, O - Other (mobs and world inflicted damage etc)
     * <p>
     * | N S O 1 2
     * ------------
     * N| y n y n n
     * S| n n n n n
     * O| y n y y y
     * 1| n n y G G
     * 2| n n y G G
     * <p>
     * y - yes, can hurt
     * n - no, can't hurt
     * G - decided by gametype
     */
    @SubscribeEvent
    public void onEntityHurt(LivingAttackEvent event) {
        if (!enabled || currentRound == null)
            return;
        if (event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entity;
            PlayerData data = PlayerHandler.getPlayerData(player);
            DamageSource source = event.source;

            if (data.team == Team.spectators && source != DamageSource.generic) {
                event.setCanceled(true);
                return;
            }

            if (source instanceof EntityDamageSource && source.getEntity() instanceof EntityPlayerMP) {
                EntityPlayerMP attacker = ((EntityPlayerMP) source.getEntity());
                PlayerData attackerData = PlayerHandler.getPlayerData(attacker);

                if (attackerData == null)
                    return;

                //Can hurt self
                if (attacker == player)
                    return;

                //Cannot be attacked by a spectator
                if (attackerData.team == Team.spectators) {
                    event.setCanceled(true);
                    return;
                }

                //Cannot be fights between people in the game and outside the game
                if ((attackerData.team == null && data.team != null) || (attackerData.team != null && data.team == null)) {
                    event.setCanceled(true);
                    return;
                }

                //Final case. Either the two players are not in the game (in which case, ignore) or they are both in the game.
                //At this point, we pass over to the gametype
                if (attackerData.team != null) {
                    //The roundTimeLeft check ensures that players do not fight during the cooldown period
                    if (roundTimeLeft > 0 && !currentRound.gametype.playerCanAttack(attacker, attackerData.team, player, data.team)) {
                        event.setCanceled(true);
                    }
                }
            }  //Not being attacked by a player, so this is fine
        }
    }

    /**
     * Handles entity deaths. Passes information to gametype for scoring
     */
    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        if (!enabled)
            return;
        if (currentRound != null) {
            currentRound.gametype.entityKilled(event.entity, event.source);
            if (event.entity instanceof EntityPlayerMP)
                currentRound.gametype.playerKilled((EntityPlayerMP) event.entity, event.source);
        }
    }

    /**
     * Base and object gathering hooks for entities, not tile entities
     */
    @SubscribeEvent
    public void entityJoinedWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof ITeamBase) {
            registerBase((ITeamBase) event.entity);
        }
        if (event.entity instanceof ITeamObject) {
            objects.add((ITeamObject) event.entity);
        }
    }

    @SubscribeEvent
    public void playerUseEntity(EntityInteractEvent event) {
        if (!enabled)
            return;
        if (event.entityPlayer.worldObj.isRemote)
            return;

        ItemStack currentItem = event.entityPlayer.getCurrentEquippedItem();
        if (currentItem != null && currentItem.getItem() != null && currentItem.getItem() instanceof ItemOpStick) {
            if (event.target instanceof ITeamObject)
                ((ItemOpStick) currentItem.getItem()).clickedObject(event.entityPlayer.worldObj, (EntityPlayerMP) event.entityPlayer, (ITeamObject) event.target);
            if (event.target instanceof ITeamBase)
                ((ItemOpStick) currentItem.getItem()).clickedBase(event.entityPlayer.worldObj, (EntityPlayerMP) event.entityPlayer, (ITeamBase) event.target);
        } else if (currentRound != null) {
            if (event.target instanceof ITeamObject)
                currentRound.gametype.objectClickedByPlayer((ITeamObject) event.target, (EntityPlayerMP) event.entityPlayer);
            if (event.target instanceof ITeamBase)
                currentRound.gametype.baseClickedByPlayer((ITeamBase) event.target, (EntityPlayerMP) event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void playerInteracted(PlayerInteractEvent event) {
        if (!enabled)
            return;
        if (event.action == Action.LEFT_CLICK_BLOCK && !event.entityPlayer.capabilities.allowEdit && !event.entityPlayer.capabilities.isCreativeMode) {
            event.setCanceled(true);
            return;
        }

        if (event.entityPlayer.worldObj.isRemote)
            return;
        TileEntity te = event.entityPlayer.worldObj.getTileEntity(event.x, event.y, event.z);
        if (te != null) {
            ItemStack currentItem = event.entityPlayer.getCurrentEquippedItem();
            if (currentItem != null && currentItem.getItem() != null && currentItem.getItem() instanceof ItemOpStick) {
                if (te instanceof ITeamObject)
                    ((ItemOpStick) currentItem.getItem()).clickedObject(event.entityPlayer.worldObj, (EntityPlayerMP) event.entityPlayer, (ITeamObject) te);
                if (te instanceof ITeamBase)
                    ((ItemOpStick) currentItem.getItem()).clickedBase(event.entityPlayer.worldObj, (EntityPlayerMP) event.entityPlayer, (ITeamBase) te);
            } else if (currentRound != null) {
                if (te instanceof ITeamObject)
                    currentRound.gametype.objectClickedByPlayer((ITeamObject) te, (EntityPlayerMP) event.entityPlayer);
                if (te instanceof ITeamBase)
                    currentRound.gametype.baseClickedByPlayer((ITeamBase) te, (EntityPlayerMP) event.entityPlayer);
            }
        }
    }

    @SubscribeEvent
    public void playerDrops(PlayerDropsEvent event) {
        ArrayList<EntityItem> dropsToThrow = new ArrayList<>();
        //First collect together guns and ammo if smart drops are enabled
        if (weaponDrops == 2) {
            for (EntityItem entity : event.drops) {
                ItemStack stack = entity.getEntityItem();
                if (stack != null && stack.getItem() != null) {
                    if (stack.getItem() instanceof ItemGun) {
                        EntityGunItem gunEntity = new EntityGunItem(entity);
                        stack.stackSize = 0;
                        boolean alreadyAdded = false;
                        for (EntityItem check : dropsToThrow) {
                            if (((ItemGun) stack.getItem()).type == ((ItemGun) check.getEntityItem().getItem()).type)
                                alreadyAdded = true;
                        }
                        if (!alreadyAdded) {
                            event.entityPlayer.worldObj.spawnEntityInWorld(gunEntity);
                            dropsToThrow.add(gunEntity);
                        }
                    }
                }
            }
        }
        //Now iterate again and look for ammo
        for (EntityItem entity : dropsToThrow) {
            EntityGunItem gunEntity = (EntityGunItem) entity;
            GunType gunType = ((ItemGun) gunEntity.getEntityItem().getItem()).type;
            for (EntityItem ammoEntity : event.drops) {
                ItemStack ammoItemstack = ammoEntity.getEntityItem();
                if (ammoItemstack != null && ammoItemstack.getItem() instanceof ItemShootable) {
                    ShootableType bulletType = ((ItemShootable) ammoItemstack.getItem()).type;
                    if (gunType.isAmmo(bulletType)) {
                        gunEntity.ammoStacks.add(ammoItemstack.copy());
                        ammoItemstack.stackSize = 0;
                    }
                }
            }
        }
        //Now check the remaining items to see if they should be dropped
        for (EntityItem entity : event.drops) {
            ItemStack stack = entity.getEntityItem();
            if (stack != null && stack.getItem() != null && stack.stackSize > 0) {
                if (stack.getItem() instanceof ItemGun || stack.getItem() instanceof ItemPlane || stack.getItem() instanceof ItemVehicle || stack.getItem() instanceof ItemAAGun || stack.getItem() instanceof ItemBullet) {
                    if (weaponDrops != 1)
                        dropsToThrow.add(entity);
                } else if (stack.getItem() instanceof ItemTeamArmour) {
                    if (!armourDrops)
                        dropsToThrow.add(entity);
                }
            }
        }
        event.drops.removeAll(dropsToThrow);

    }

    /**
     * Stop spectators looting items
     */
    @SubscribeEvent
    public void playerLoot(EntityItemPickupEvent event) {
        if (event.entity instanceof EntityPlayer) {
            ItemStack itemStack = event.item.getEntityItem();
            PlayerData data = PlayerHandler.getPlayerData(event.entityPlayer);
            if (enabled && currentRound != null && data != null) {
                if (data.team == Team.spectators || !currentRound.gametype.playerCanLoot(itemStack, InfoType.getType(itemStack), event.entityPlayer, data.team))
                    event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent event) {
        if (event instanceof PlayerEvent.PlayerRespawnEvent)
            respawnPlayer(event.player, false);
        if (event instanceof PlayerEvent.PlayerLoggedOutEvent)
            onPlayerLogout(event.player);
        if (event instanceof PlayerEvent.PlayerLoggedInEvent)
            onPlayerLogin(event.player);
    }

    public void onPlayerLogin(EntityPlayer player) {
        if (!enabled || currentRound == null)
            return;

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            sendTeamsMenuToPlayer(playerMP);
            currentRound.gametype.playerJoined(playerMP);
        }
    }

    public void onPlayerLogout(EntityPlayer player) {
        for (Team team : Team.teams)
            team.removePlayer(player);
    }

    public void respawnPlayer(EntityPlayer player, boolean firstSpawn) {
        if (player.worldObj.isRemote)
            return;

        if (!enabled || currentRound == null)
            return;

        EntityPlayerMP playerMP = ((EntityPlayerMP) player);
        PlayerData data = PlayerHandler.getPlayerData(playerMP);

        if (data == null || (data.builder && playerIsOp(playerMP)))
            return;

        //On the first spawn, we don't kill the player, we simply move them over, so do a /tp like command
        if (firstSpawn) {
            Vec3 spawnPoint = currentRound.gametype.getSpawnPoint(playerMP);
            if (spawnPoint != null) {
                player.setPositionAndUpdate(spawnPoint.xCoord, spawnPoint.yCoord, spawnPoint.zCoord);
            }
        }

        //To set their next spawn position, override their bed position
        setPlayersNextSpawnpoint(playerMP);

        if (forceAdventureMode)
            player.setGameType(net.minecraft.world.WorldSettings.GameType.ADVENTURE);
        resetInventory(player);
        currentRound.gametype.playerRespawned((EntityPlayerMP) player);
    }

    private void setPlayersNextSpawnpoint(EntityPlayerMP player, ChunkCoordinates coords) {
        player.setSpawnChunk(coords, true);
    }

    protected void setPlayersNextSpawnpoint(EntityPlayerMP player) {
        if (!enabled || currentRound == null)
            return;

        PlayerData data = PlayerHandler.getPlayerData(player);

        Vec3 spawnPoint = currentRound.gametype.getSpawnPoint(player);
        if (spawnPoint != null)
            setPlayersNextSpawnpoint(player, new ChunkCoordinates(MathHelper.floor_double(spawnPoint.xCoord), MathHelper.floor_double(spawnPoint.yCoord) + 1, MathHelper.floor_double(spawnPoint.zCoord)));
        else
            FlansMod.log("Could not find spawn point for " + player.getDisplayName() + " on team " + (data.newTeam == null ? "null" : data.newTeam.name));
    }

    /**
     * Force a respawn
     */
    public void forceRespawn(EntityPlayerMP player) {
        if (playerIsOp(player) && PlayerHandler.getPlayerData(player).builder)
            return;
        player.inventory.armorInventory = new ItemStack[4];
        player.inventory.mainInventory = new ItemStack[36];
        player.heal(9001);
        if (forceAdventureMode)
            player.setGameType(net.minecraft.world.WorldSettings.GameType.ADVENTURE);
        respawnPlayer(player, true);
    }

    public void sendTeamsMenuToPlayer(EntityPlayerMP player) {
        sendTeamsMenuToPlayer(player, false);
    }

    public void sendTeamsMenuToPlayer(EntityPlayerMP player, boolean info) {
        if (!enabled || currentRound == null || currentRound.teams == null)
            return;
        //Get the available teams from the gametype
        Team[] availableTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
        //Add in the spectators as an option and "none" if the player is an op
        boolean playerIsOp = MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
        Team[] allAvailableTeams = new Team[availableTeams.length + (playerIsOp ? 2 : 1)];
        System.arraycopy(availableTeams, 0, allAvailableTeams, 0, availableTeams.length);
        allAvailableTeams[availableTeams.length] = Team.spectators;

        sendPacketToPlayer(new PacketTeamSelect(allAvailableTeams, info), player);
    }

    public void sendClassMenuToPlayer(EntityPlayerMP player) {
        Team team = PlayerHandler.getPlayerData(player).newTeam;
        if (team == null) {
            sendTeamsMenuToPlayer(player);
        } else if (team != Team.spectators && !team.classes.isEmpty()) {
            sendPacketToPlayer(new PacketTeamSelect(team.classes.toArray(new PlayerClass[0]), PlayerStats.getPlayerLvl(player)), player);
        }
    }

    public boolean playerIsOp(EntityPlayer player) {
        return MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
    }

    public boolean autoBalance() {
        return !(currentRound != null && !currentRound.gametype.shouldAutobalance()) && autoBalance;
    }

    public void playerSelectedTeam(EntityPlayerMP player, String teamName) {
        if (!enabled || currentRound == null)
            return;

        PlayerData data = PlayerHandler.getPlayerData(player);

        data.builder = false;

        //The player picked the op / builder team
        if (teamName.equals("null")) {
            if (playerIsOp(player)) {
                data.team = null;
                data.builder = true;
                return;
            } else teamName = "spectators";
        }

        //The team the player selected
        Team selectedTeam = Team.getTeam(teamName);
        //They cannot pick no team
        if (selectedTeam == null)
            selectedTeam = Team.spectators;

        //Validate the selected team
        boolean isValid = selectedTeam == Team.spectators;
        Team[] validTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
        for (Team validTeam : validTeams) {
            if (selectedTeam == validTeam) {
                isValid = true;
                break;
            }
        }
        //Default to spectator
        if (!isValid) {
            player.addChatMessage(new ChatComponentText("You may not join " + selectedTeam.name + " for it is invalid. Please try again"));
            FlansMod.log(player.getCommandSenderName() + " tried to spawn on an invalid team : " + selectedTeam.name);
            selectedTeam = Team.spectators;
        }

        //Spawn spectators immediately
        if (selectedTeam == Team.spectators) {
            messageAll(player.getCommandSenderName() + " joined \u00a7" + selectedTeam.textColour + selectedTeam.name);
            if (data.team != null)
                data.team.removePlayer(player);
            data.newTeam = data.team = Team.spectators;
            data.team.addPlayer(player);
            player.heal(9001);
            respawnPlayer(player, true);
            player.inventory.armorInventory = new ItemStack[4];
            player.inventory.mainInventory = new ItemStack[36];
        }
        //Give other players the chance to select a class
        else {
            Team otherTeam = currentRound.getOtherTeam(selectedTeam);
            if (autoBalance() && selectedTeam.members.size() > otherTeam.members.size() + 1) {
                player.addChatMessage(new ChatComponentText("You may not join " + selectedTeam.name + " due to imbalance. Please try again"));
                sendTeamsMenuToPlayer(player);
                return;
            }
            data.newTeam = selectedTeam;
            sendClassMenuToPlayer(player);
        }

        currentRound.gametype.playerChoseTeam(player, data.team, selectedTeam);
    }

    public void playerSelectedClass(EntityPlayerMP player, String className) {
        if (!enabled || currentRound == null)
            return;

        PlayerData data = PlayerHandler.getPlayerData(player);

        //Get player class requested
        PlayerClass playerClass = PlayerClass.getClass(className);
        //Validate class
        if (!data.newTeam.classes.contains(playerClass)) {
            player.addChatMessage(new ChatComponentText("You may not select " + playerClass.name + ". Please try again"));
            FlansMod.log(player.getCommandSenderName() + " tried to pick an invalid class : " + playerClass.name);
            //sendClassMenuToPlayer(player);
            return;
        }

        //Check cases
        //1 : Player switched class only
        if (data.team == data.newTeam && data.playerClass != playerClass && !data.playerMovedByAutobalancer) {
            currentRound.gametype.playerChoseNewClass(player, playerClass);
            data.newPlayerClass = playerClass;
            player.addChatMessage(new ChatComponentText("You will respawn with the " + playerClass.name + " class"));
        }
        //2 : Player switched team
        else if (data.team != null && data.team != data.newTeam) {
            messageAll(player.getCommandSenderName() + " switched to \u00a7" + data.newTeam.textColour + data.newTeam.name);
            currentRound.gametype.playerDefected(player, data.team, data.newTeam);
            setPlayersNextSpawnpoint(player);
            player.attackEntityFrom(DamageSource.generic, 10000F);
            if (data.team != null)
                data.team.removePlayer(player);
            data.newTeam.addPlayer(player);
            data.team = data.newTeam;
            data.newPlayerClass = playerClass;
        }
        //3 : Player has only just joined
        else if (data.team == null) {
            messageAll(player.getCommandSenderName() + " joined \u00a7" + data.newTeam.textColour + data.newTeam.name);
            currentRound.gametype.playerEnteredTheGame(player, data.newTeam, playerClass);
            data.newTeam.addPlayer(player);
            data.team = data.newTeam;
            data.newPlayerClass = playerClass;
            currentRound.gametype.playerChoseNewClass(player, playerClass);
            respawnPlayer(player, true);
        }
        //4 : Player has moved my autobalancer
        else if (data.team == data.newTeam && data.playerClass != playerClass && data.playerMovedByAutobalancer) {
            currentRound.gametype.playerChoseNewClass(player, playerClass);
            data.newPlayerClass = playerClass;
            resetInventory(player);
            Vec3 spawnPoint = currentRound.gametype.getSpawnPoint(player);
            if (spawnPoint != null) {
                player.setPositionAndUpdate(spawnPoint.xCoord, spawnPoint.yCoord, spawnPoint.zCoord);
                data.playerMovedByAutobalancer = false;
            }
        }
    }

    public void resetInventory(EntityPlayer player) {
        Team team = PlayerHandler.getPlayerData(player).team;
        PlayerClass playerClass = PlayerHandler.getPlayerData(player).getPlayerClass();

        if (team == null)
            return;

        player.inventory.armorInventory = new ItemStack[4];
        player.inventory.mainInventory = new ItemStack[36];

        //Set team armour
        if (team.hat != null)
            player.inventory.armorInventory[3] = team.hat.copy();
        if (team.chest != null)
            player.inventory.armorInventory[2] = team.chest.copy();
        if (team.legs != null)
            player.inventory.armorInventory[1] = team.legs.copy();
        if (team.shoes != null)
            player.inventory.armorInventory[0] = team.shoes.copy();

        if (playerClass == null)
            return;

        //Override with class armour
        if (playerClass.hat != null)
            player.inventory.armorInventory[3] = playerClass.hat.copy();
        if (playerClass.chest != null)
            player.inventory.armorInventory[2] = playerClass.chest.copy();
        if (playerClass.legs != null)
            player.inventory.armorInventory[1] = playerClass.legs.copy();
        if (playerClass.shoes != null)
            player.inventory.armorInventory[0] = playerClass.shoes.copy();

        for (ItemStack stack : playerClass.startingItems) {
            player.inventory.addItemStackToInventory(stack.copy());
            //Load up as many guns as possible
        }
        PlayerData data = PlayerHandler.getPlayerData(player);
        data.reloadedAfterRespawn = false;
        FlansMod.getPacketHandler().sendTo(new PacketRespawnFinished(), getPlayer(player.getDisplayName()));
//        Preload each gun
//        for (ItemStack stack : player.inventory.mainInventory) {
//            if (stack != null && stack.getItem() instanceof ItemGun && !((ItemGun) stack.getItem()).type.ammo.isEmpty()) {
//                ItemStack ammo = new ItemStack(((ItemGun) stack.getItem()).type.ammo.get(0).item);
//                ((ItemGun) stack.getItem()).setBulletItemStack(stack,ammo,0);
//            }
//        }
    }

    //---------------------------------------------------------
    // Saving and Loading
    //---------------------------------------------------------

    @SubscribeEvent
    public void chunkLoaded(ChunkDataEvent event) {
        Chunk chunk = event.getChunk();
        for (List<Entity> list : chunk.entityLists) {
            for (Entity entity : list) {
                if (entity instanceof ITeamBase) {
                    bases.add((ITeamBase) entity);
                    if (((ITeamBase) entity).getBaseID() > nextBaseID) {
                        FlansMod.log("Loaded base with ID higher than the supposed highest ID. Adjusted highest ID");
                        nextBaseID = ((ITeamBase) entity).getBaseID();
                    }
                }
                if (entity instanceof ITeamObject)
                    objects.add((ITeamObject) entity);
            }
        }
    }

    @SubscribeEvent
    public void worldData(WorldEvent event) {
        if (event.world.isRemote) {
            if (event instanceof WorldEvent.Unload) {
                // Clear gun animations to prevent old references to world objects.
                FlansMod.gunAnimationsLeft.clear();
                FlansMod.gunAnimationsRight.clear();
                // Clear sound cache.
                FlansModClient.reloadSound.clear();
                // Clear teams cache.
                PlayerHandler.clientSideData.clear();
                PlayerHandler.clientSidePlayerStats.clear();
            }
            return;
        }
        if (event instanceof WorldEvent.Load) {
            loadPerWorldData(event, event.world);
            savePerWorldData(event, event.world);
        }
        if (event instanceof WorldEvent.Save) {
            savePerWorldData(event, event.world);
        }
    }

    private void loadPerWorldData(Event event, World world) {
        //Reset the teams manager before loading a new world
        reset();
        //Read the teams dat file
        File file = new File(world.getSaveHandler().getWorldDirectory(), "teams_" + world.provider.getDimensionName() + ".dat");
        if (!checkFileExists(file))
            return;
        try {
            NBTTagCompound tags = CompressedStreamTools.read(new DataInputStream(Files.newInputStream(file.toPath())));
            nextBaseID = tags.getInteger("NextBaseID");
            //Read maps
            for (int i = 0; i < tags.getInteger("NumberOfMaps"); i++) {
                TeamsMap map = new TeamsMap(world, tags.getCompoundTag("Map_" + i));
                TeamsMap.mapList.add(map);
                maps.put(map.shortName, map);
            }

            if (maps.isEmpty()) {
                maps.put("default" + world.getWorldInfo().getVanillaDimension(), new TeamsMap(world, "default" + world.getWorldInfo().getVanillaDimension(), "Default " + world.getWorldInfo().getWorldName()));
            }

            //Read the rounds list
            for (int i = 0; i < tags.getInteger("RoundsSize"); i++) {
                TeamsRound round = new TeamsRound(tags.getCompoundTag("Round_" + i));
                rounds.add(round);
            }

            //Read variables
            enabled = tags.getBoolean("Enabled");
            voting = tags.getBoolean("Voting");
            votingTime = tags.getInteger("VotingTime");
            scoreDisplayTime = tags.getInteger("ScoreTime");
            bombsEnabled = tags.getBoolean("Bombs");
            bulletsEnabled = tags.getBoolean("Bullets");
            explosions = tags.getBoolean("Explosions");
            forceAdventureMode = tags.getBoolean("ForceAdventure");
            canBreakGuns = tags.getBoolean("CanBreakGuns");
            canBreakGlass = tags.getBoolean("CanBreakGlass");
            if (tags.hasKey("SurvivalCanBreakVehicles")) {
                survivalCanBreakVehicles = tags.getBoolean("SurvivalCanBreakVehicles");
                // default is false if key ain't there
            } else {
                survivalCanBreakVehicles = true;
            }

            if (tags.hasKey("SurvivalCanPlaceVehicles")) {
                survivalCanPlaceVehicles = tags.getBoolean("SurvivalCanPlaceVehicles");
            } else {
                survivalCanPlaceVehicles = true;
            }

            armourDrops = tags.getBoolean("ArmourDrops");
            weaponDrops = tags.getInteger("WeaponDrops");
            vehiclesNeedFuel = tags.getBoolean("NeedFuel");
            mgLife = tags.getInteger("MGLife");
            aaLife = tags.getInteger("AALife");
            vehicleLife = tags.getInteger("VehicleLife");
            mechaLove = tags.getInteger("MechaLove");
            planeLife = tags.getInteger("PlaneLife");
            driveablesBreakBlocks = tags.getBoolean("BreakBlocks");

            //Start the rotation
            if (enabled && !rounds.isEmpty())
                start();
        } catch (Exception e) {
            FlansMod.logException("Failed to load from teams.dat", e);
        }

        //Reset all infotypes. Specifically, send this to player classes so that they may create itemstacks from strings regarding attachments for guns
        for (InfoType type : InfoType.infoTypes)
            type.onWorldLoad(world);
    }

    private void savePerWorldData(Event event, World world) {
        File file = new File(world.getSaveHandler().getWorldDirectory(), "teams_" + world.provider.getDimensionName() + ".dat");
        checkFileExists(file);
        try {
            NBTTagCompound tags = new NBTTagCompound();
            tags.setInteger("NextBaseID", nextBaseID);
            //Changed name so that it does not try to read old maps
            tags.setInteger("NumberOfMaps", maps.size());
            //Write the maps to memory
            if (maps != null) {
                int i = 0;
                for (TeamsMap map : maps.values()) {
                    NBTTagCompound mapTags = new NBTTagCompound();
                    map.writeToNBT(mapTags);
                    tags.setTag("Map_" + i, mapTags);
                    i++;
                }
            }
            //Write the rounds list to memory
            if (rounds != null) {
                tags.setInteger("RoundsSize", rounds.size());
                for (int i = 0; i < rounds.size(); i++) {
                    TeamsRound entry = rounds.get(i);
                    if (entry != null) {
                        NBTTagCompound roundTags = new NBTTagCompound();
                        entry.writeToNBT(roundTags);
                        tags.setTag("Round_" + i, roundTags);
                    }
                }
            } else tags.setInteger("RoundsSize", 0);
            //Write the current round to memory
            if (currentRound != null)
                tags.setInteger("CurrentRound", rounds.indexOf(currentRound));
            //Save gametype settings to memory
            for (GameType gametype : GameType.gameTypes.values()) {
                gametype.saveToNBT(tags);
            }

            //Save variables
            tags.setBoolean("Enabled", enabled);
            tags.setBoolean("Voting", voting);
            tags.setInteger("VotingTime", votingTime);
            tags.setInteger("ScoreTime", scoreDisplayTime);
            tags.setBoolean("Bombs", bombsEnabled);
            tags.setBoolean("Bullets", bulletsEnabled);
            tags.setBoolean("Explosions", explosions);
            tags.setBoolean("ForceAdventure", forceAdventureMode);
            tags.setBoolean("CanBreakGuns", canBreakGuns);
            tags.setBoolean("CanBreakGlass", canBreakGlass);
            tags.setBoolean("SurvivalCanBreakVehicles", survivalCanBreakVehicles);
            tags.setBoolean("SurvivalCanPlaceVehicles", survivalCanPlaceVehicles);
            tags.setBoolean("ArmourDrops", armourDrops);
            tags.setInteger("WeaponDrops", weaponDrops);
            tags.setBoolean("NeedFuel", vehiclesNeedFuel);
            tags.setInteger("MGLife", mgLife);
            tags.setInteger("AALife", aaLife);
            tags.setInteger("VehicleLife", vehicleLife);
            tags.setInteger("MechaLove", mechaLove);
            tags.setInteger("PlaneLife", planeLife);
            tags.setBoolean("BreakBlocks", driveablesBreakBlocks);

            CompressedStreamTools.write(tags, new DataOutputStream(Files.newOutputStream(file.toPath())));
        } catch (Exception e) {
            FlansMod.logger.error("Failed to save to teams.dat", e);
        }
    }

    public static boolean checkFileExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                FlansMod.logException("Failed to create file", e);
            }
            return false;
        }
        return true;
    }

    //------------------------------------------------------------------------------
    // Getters, setters, registers, loggers and the likes
    //------------------------------------------------------------------------------

    public void resetScores() {
        for (Team team : Team.teams) {
            team.score = 0;
            team.members.clear();
        }
        for (EntityPlayer player : getPlayers())
            if (PlayerHandler.getPlayerData(player) != null)
                PlayerHandler.getPlayerData(player).resetScore();
    }

    public ITeamBase getBase(int ID) {
        for (ITeamBase base : bases) {
            if (base.getBaseID() == ID)
                return base;
        }
        return null;
    }

    public void registerBase(ITeamBase base) {
        if (base.getBaseID() == 0)
            base.setBaseID(nextBaseID++);
        bases.add(base);
    }

    public void registerObject(ITeamObject obj) {
        objects.add(obj);
    }

    public EntityPlayerMP getPlayer(String username) {
        return MinecraftServer.getServer().getConfigurationManager().func_152612_a(username);
    }

    public static void log(String s) {
        FlansMod.log("Teams Info : " + s);
    }

    public static void messagePlayer(EntityPlayerMP player, String s) {
        player.addChatComponentMessage(new ChatComponentText(s));
    }

    public static void messageAll(String s) {
        FlansMod.log("Teams Announcement : " + s);
        for (EntityPlayerMP player : (List<EntityPlayerMP>) MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            player.addChatComponentMessage(new ChatComponentText(s));
        }
    }

    public static void sendPacketToPlayer(PacketBase packet, EntityPlayerMP player) {
        FlansMod.getPacketHandler().sendTo(packet, player);
    }

    public static List<EntityPlayerMP> getPlayers() {
        return MinecraftServer.getServer().getConfigurationManager().playerEntityList;
    }

    /**
     * Returns the team associated with the given ID
     */
    public Team getTeam(int spawnerTeamID) {
        if (!enabled || currentRound == null || spawnerTeamID == 0)
            return null;
        if (spawnerTeamID == 1)
            return Team.spectators;
        return currentRound.teams[spawnerTeamID - 2];
    }

    /**
     * The maps HashMap is indexed by shortName, not full name, so this method helps there
     */
    public TeamsMap getMapFromFullName(String string) {
        for (TeamsMap map : maps.values()) {
            if (map.name.equals(string))
                return map;
        }
        return null;
    }

    public static class RotationEntry {
        public TeamsMap map;
        public GameType gametype;
        public Team[] teams;

        public RotationEntry(TeamsMap m, GameType g, Team[] t) {
            map = m;
            gametype = g;
            teams = t;
        }
    }
}
