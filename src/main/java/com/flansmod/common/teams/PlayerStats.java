package com.flansmod.common.teams;

import com.flansmod.common.FlansMod;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PlayerStats {

    private World world;
    private EntityPlayerMP playerMP;
    public String nickname = "NaN";
    public int kills = 0; //done
    public int deaths = 0; //done
    public int exp = 0; //done
    public int totalExp = 0; //done
    public int rank = 1; //done
    public double avg = 0; //done
    public double longestKill = 0; //done
    public int playedRounds = 0; //done
    public double playTime = 0;
    public int MVPCount = 0; //done
    public int capturedFlags = 0; //done
    public int savedFlags = 0; //done
    public int vehiclesDestroyed = 0; //done 75%

    public PlayerStats(World world, EntityPlayerMP player) {
        playerMP = player;
        this.world = world;
        nickname = player.getCommandSenderName();
        savePlayerStats();
    }

    private PlayerStats() {

    }

    public EntityPlayerMP getPlayer(String username) {
        return MinecraftServer.getServer().getConfigurationManager().func_152612_a(username);
    }

    public void savePlayerStats() {
        File dir = new File(MinecraftServer.getServer().getEntityWorld().getSaveHandler().getWorldDirectory() + "\\FlansMod players statistics\\");
        dir.mkdirs();
        dir.setReadable(true);
        dir.setWritable(true);
        File file = new File(dir, getPlayer(nickname).getDisplayName() + " " + getPlayer(nickname).getUniqueID().toString() + ".dat");
        dir.mkdirs();
        dir.setReadable(true);
        dir.setWritable(true);
        checkFileExists(file);
        try {
            NBTTagCompound tags = new NBTTagCompound();

            tags.setString("Nickname", nickname);
            tags.setInteger("Kills", kills);
            tags.setInteger("Deaths", deaths);
            tags.setInteger("Exp", exp);
            tags.setInteger("Total Exp", totalExp);
            tags.setInteger("Rank", rank);
            tags.setDouble("AVG", avg);
            tags.setDouble("Longest Kill", longestKill);
            tags.setInteger("Rounds Played", playedRounds);
            tags.setDouble("Play Time", playTime);
            tags.setInteger("MVP Count", MVPCount);
            tags.setInteger("Flags Captured", capturedFlags);
            tags.setInteger("Flags Saved", savedFlags);
            tags.setInteger("Vehicles Destroyed", vehiclesDestroyed);


            CompressedStreamTools.write(tags, new DataOutputStream(new FileOutputStream(file)));
        } catch (Exception e) {
            FlansMod.log("Failed to save to teams.dat");
            e.printStackTrace();
        }
    }


    private static boolean checkFileExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                FlansMod.log("Created new file");
            } catch (Exception e) {
                FlansMod.log("Failed to create file");
                FlansMod.log(file.getAbsolutePath());
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public void addExp(float a) {
        exp += a;
        totalExp += a;
        if (exp >= 1000) {
            rank++;
            exp = exp - 1000;
        }
    }

    public void updateLongestKill(float distance) {
        addExp(distance / 10);
        if (distance > longestKill) {
            longestKill = distance;
        }
    }

    public void updateAVG() {
        avg = (double) kills / playedRounds;
    }


    public static PlayerStats getPlayerStatsFromFile(String name) {
        PlayerStats toSend = new PlayerStats();
        File dir = new File(MinecraftServer.getServer().getEntityWorld().getSaveHandler().getWorldDirectory() + "\\FlansMod players statistics\\");
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith(name)) {
                checkFileExists(file);
                try {
                    NBTTagCompound tags = CompressedStreamTools.read(new DataInputStream(new FileInputStream(file)));
                    toSend.nickname = tags.getString("Nickname");
                    toSend.kills = tags.getInteger("Kills");
                    toSend.deaths = tags.getInteger("Deaths");
                    toSend.exp = tags.getInteger("Exp");
                    toSend.totalExp = tags.getInteger("Total Exp");
                    toSend.rank = tags.getInteger("Rank");
                    toSend.avg = tags.getDouble("AVG");
                    toSend.longestKill = tags.getDouble("Longest Kill");
                    toSend.playedRounds = tags.getInteger("Rounds Played");
                    toSend.playTime = tags.getDouble("Play Time");
                    toSend.MVPCount = tags.getInteger("MVP Count");
                    toSend.capturedFlags = tags.getInteger("Flags Captured");
                    toSend.savedFlags = tags.getInteger("Flags Saved");
                    toSend.vehiclesDestroyed = tags.getInteger("Vehicles Destroyed");
                    return toSend;
                } catch (Exception e) {
                    FlansMod.log("Failed to save to teams.dat");
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public static void printLeaderboardExp(ICommandSender sender) {
        List<String> nameList = new ArrayList<>();
        File dir = new File(MinecraftServer.getServer().getEntityWorld().getSaveHandler().getWorldDirectory() + "\\FlansMod players statistics\\");
        for (File file : dir.listFiles()) {
            checkFileExists(file);
            try {
                NBTTagCompound tags = CompressedStreamTools.read(new DataInputStream(new FileInputStream(file)));
                String nickname = tags.getString("Nickname");
                nameList.add(nickname);
            } catch (Exception e) {
                FlansMod.log("Failed to print leaderboard");
                e.printStackTrace();
            }
        }
        Collections.sort(nameList, new ComparatorExp());
        int counter1 = 0;
        for (String name : nameList) {
            switch (counter1) {
                case 0:
                    sender.addChatMessage(new ChatComponentText("\u00a76\u00a7l1. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 1:
                    sender.addChatMessage(new ChatComponentText("\u00a74\u00a7l2. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 2:
                    sender.addChatMessage(new ChatComponentText("\u00a7a\u00a7l3. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 3:
                    sender.addChatMessage(new ChatComponentText("\u00a7l4. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 4:
                    sender.addChatMessage(new ChatComponentText("\u00a7l5. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 5:
                    sender.addChatMessage(new ChatComponentText("\u00a7l6. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 6:
                    sender.addChatMessage(new ChatComponentText("\u00a7l7. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 7:
                    sender.addChatMessage(new ChatComponentText("\u00a7l8. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 8:
                    sender.addChatMessage(new ChatComponentText("\u00a7l9. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 9:
                    sender.addChatMessage(new ChatComponentText("\u00a7l10. " + name + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
                case 10:
                    sender.addChatMessage(new ChatComponentText("\u00a7b\u00a7l"+nameList.indexOf(sender.getCommandSenderName())+1+". " + sender.getCommandSenderName() + " - Rank " + getPlayerStatsFromFile(name).rank + "(" + getPlayerStatsFromFile(name).totalExp + " Exp)"));
                    break;
            }
            counter1++;
            if (counter1 >= 11) break;
        }
        counter1 = 0;
    }

    public static List<PlayerStats> getAllPlayersStats() {
        List<PlayerStats> listToSend = new ArrayList<>();
        File dir = new File(MinecraftServer.getServer().getEntityWorld().getSaveHandler().getWorldDirectory() + "\\FlansMod players statistics\\");
        if(dir.listFiles()==null) return null;
        for (File file : dir.listFiles()) {
            checkFileExists(file);
            try {
                PlayerStats toSend = new PlayerStats();
                NBTTagCompound tags = CompressedStreamTools.read(new DataInputStream(new FileInputStream(file)));
                toSend.nickname = tags.getString("Nickname");
                toSend.kills = tags.getInteger("Kills");
                toSend.deaths = tags.getInteger("Deaths");
                toSend.exp = tags.getInteger("Exp");
                toSend.totalExp = tags.getInteger("Total Exp");
                toSend.rank = tags.getInteger("Rank");
                toSend.avg = tags.getDouble("AVG");
                toSend.longestKill = tags.getDouble("Longest Kill");
                toSend.playedRounds = tags.getInteger("Rounds Played");
                toSend.playTime = tags.getDouble("Play Time");
                toSend.MVPCount = tags.getInteger("MVP Count");
                toSend.capturedFlags = tags.getInteger("Flags Captured");
                toSend.savedFlags = tags.getInteger("Flags Saved");
                toSend.vehiclesDestroyed = tags.getInteger("Vehicles Destroyed");
                listToSend.add(toSend);
            } catch (Exception e) {
                FlansMod.log("Failed to save to teams.dat");
                e.printStackTrace();
                return null;
            }
        }
        return listToSend;
    }
    public static int getPlayerLvl(EntityPlayerMP player){
        PlayerStats stats = getPlayerStatsFromFile(player.getDisplayName());
        if(stats!=null){
            return stats.totalExp/1000;
        } else {
            new PlayerStats(player.worldObj,player);
            stats = getPlayerStatsFromFile(player.getDisplayName());
        }
        return stats.totalExp/1000;
    }
}

class ComparatorExp implements Comparator<String> {
    @Override
    public int compare(String a, String b) {
        PlayerStats info1 = PlayerStats.getPlayerStatsFromFile(a);
        PlayerStats info2 = PlayerStats.getPlayerStatsFromFile(b);
        if (info1 == null || info2 == null)
            return 0;
        return info2.totalExp - info1.totalExp;
    }
}
