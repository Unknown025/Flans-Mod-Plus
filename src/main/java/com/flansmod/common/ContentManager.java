package com.flansmod.common;

import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.*;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.sync.Sync;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.tools.ToolType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ContentManager {
    public static final Pattern ZIP_JAR = Pattern.compile("(.+).(zip|jar)$");

    /**
     * Enables various development environment features.
     */
    public static boolean DEV_ENV = (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    private boolean loggedDeobf;
    private boolean reloadResources;
    private final HashMap<String, IFlansContentProvider> contentPacks = new HashMap<>();
    private final List<File> contentList = new LinkedList<>();

    /**
     * Finds and loads all Flans content.
     */
    public void loadContent() {
        //Mods folder
        for (ModContainer container : Loader.instance().getActiveModList()) {
            for (ArtifactVersion requirements : container.getRequirements()) {
                if (requirements.getLabel().equals(FlansMod.MODID)) {
                    if (container.getMod() instanceof IFlansContentProvider) {
                        IFlansContentProvider provider = (IFlansContentProvider) container.getMod();
                        String directory = provider.getContentDirectory();

                        File source = container.getSource();
                        if (DEV_ENV) {
                            if (!loggedDeobf) {
                                FlansMod.logger.warn("Detected deobfuscated Forge environment, content packs will be loaded from directories");
                                loggedDeobf = true;
                            }
                            contentPacks.put(directory, new ContentPackMod(container, provider));
                        } else if (ZIP_JAR.matcher(source.getName()).matches()) {
                            contentPacks.put(directory, new ContentPackMod(container, provider));
                        }
                        contentList.add(source);
                    }
                }
            }
        }

        //Flans folder
        for (File file : Objects.requireNonNull(FlansMod.flanDir.listFiles())) {
            //Load folders and valid zip files
            if (file.isDirectory() || ZIP_JAR.matcher(file.getName()).matches()) {
                if (contentPacks.containsKey(file.getName())) {
                    FlansMod.logger.warn("Skipping duplicated content pack {}", file.getName());
                } else {
                    contentPacks.put(file.getName(), new ContentPackFlans(file.getName(), file));
                    FlansMod.logger.info("Loaded content pack : {}", file.getName());

                    contentList.add(file);
                }
                reloadResources = true; //Reload Minecraft resources to load Flans content.
            }
        }

        //Register model locations
        for (IFlansContentProvider provider : contentPacks.values())
            provider.registerModelLocations();

        //Load types
        for (Map.Entry<String, IFlansContentProvider> entry : contentPacks.entrySet()) {
            String name = entry.getKey();
            IFlansContentProvider provider = entry.getValue();

            if (provider instanceof ContentPackFlans) {
                ContentPackFlans contentPack = (ContentPackFlans) provider;
                if (contentPack.source.isDirectory()) {
                    loadTypesDirectory(name, contentPack.source);
                } else {
                    loadTypesArchive(name, contentPack.source);
                }
            } else if (provider instanceof ContentPackMod) {
                ContentPackMod mod = (ContentPackMod) provider;

                if (DEV_ENV) {
                    //Load deobfuscated version.
                    loadTypesDirectory(name, new File(FlansMod.flanDir, provider.getContentDirectory()));
                } else if (ZIP_JAR.matcher(mod.container.getSource().getName()).matches()) {
                    loadTypesArchive(name, mod.container.getSource());
                }
            }
        }
    }

    /**
     * Creates all the mod items.
     * <i>Execute only after registering all models!</i>
     */
    public void createItems() {
        //Create items
        for (EnumType type : EnumType.values()) {
            Class<? extends InfoType> typeClass = type.getTypeClass();
            for (TypeFile typeFile : TypeFile.files.get(type)) {
                try {
                    if (!(typeFile.lines.isEmpty())) {
                        InfoType infoType = (typeClass.getConstructor(TypeFile.class).newInstance(typeFile));
                        infoType.read(typeFile);

                        if (infoType.shortName != null && infoType.isValid) {
                            switch (type) {
                                case bullet:
                                    FlansMod.bulletItems.add((ItemBullet) new ItemBullet((BulletType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case attachment:
                                    FlansMod.attachmentItems.add((ItemAttachment) new ItemAttachment((AttachmentType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case gun:
                                    FlansMod.gunItems.add((ItemGun) new ItemGun((GunType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case grenade:
                                    FlansMod.grenadeItems.add((ItemGrenade) new ItemGrenade((GrenadeType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case part:
                                    FlansMod.partItems.add((ItemPart) new ItemPart((PartType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case plane:
                                    FlansMod.planeItems.add((ItemPlane) new ItemPlane((PlaneType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case vehicle:
                                    FlansMod.vehicleItems.add((ItemVehicle) new ItemVehicle((VehicleType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case aa:
                                    FlansMod.aaGunItems.add((ItemAAGun) new ItemAAGun((AAGunType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case mechaItem:
                                    FlansMod.mechaToolItems.add((ItemMechaAddon) new ItemMechaAddon((MechaItemType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case mecha:
                                    FlansMod.mechaItems.add((ItemMecha) new ItemMecha((MechaType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case tool:
                                    FlansMod.toolItems.add((ItemTool) new ItemTool((ToolType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case box:
                                    FlansMod.gunBoxBlocks.add((BlockGunBox) new BlockGunBox((GunBoxType) infoType).setBlockName(infoType.shortName));
                                    break;
                                case armour:
                                    FlansMod.armourItems.add((ItemTeamArmour) new ItemTeamArmour((ArmourType) infoType).setUnlocalizedName(infoType.shortName));
                                    break;
                                case armourBox:
                                    FlansMod.armourBoxBlocks.add((BlockArmourBox) new BlockArmourBox((ArmourBoxType) infoType).setBlockName(infoType.shortName));
                                    break;
                                case playerClass:
                                case team:
                                    break;
                                default:
                                    FlansMod.logger.error("Unrecognized type for {}", infoType.shortName);
                                    break;
                            }
                            Sync.addHash(typeFile.lines.toString(), infoType.shortName);
                        }

                    }
                } catch (Exception e) {
                    FlansMod.logger.error("Failed to add {} : {}", type.name(), typeFile.name, e);
                }
            }
            FlansMod.logger.info("Loaded {}.", type.folderNames[0]);
        }
        Sync.getUnifiedHash();
        FlansMod.logger.info("Client Hash: {}", Sync.getUnifiedHash());
    }

    @SuppressWarnings("ConstantConditions")
    private void loadTypesDirectory(String packName, File contentPack) {
        for (EnumType typeToCheckFor : EnumType.values()) {
            for (String folderName : typeToCheckFor.folderNames) {
                File typesDir = new File(contentPack, "/" + folderName + "/");
                if (!typesDir.exists())
                    continue;
                for (File file : FileUtils.listFiles(typesDir, null, true)) {
                    if (!file.isDirectory()) {
                        if (!file.getName().endsWith(".txt")) {
                            FlansMod.logger.warn("Type file {} does not have a recognized file extension", file.getName());
                        }
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            String[] splitName = file.getName().split("/");
                            TypeFile typeFile = new TypeFile(typeToCheckFor, splitName[splitName.length - 1].split("\\.")[0], packName);
                            for (; ; ) {
                                String line;
                                try {
                                    line = reader.readLine();
                                } catch (Exception e) {
                                    break;
                                }
                                if (line == null)
                                    break;
                                typeFile.lines.add(line);
                            }
                            reader.close();
                        } catch (IOException e) {
                            FlansMod.logger.error("Could not read {}", file.getName(), e);
                        }
                    }
                }
            }
        }
    }

    private void loadTypesArchive(String packName, File contentPack) {
        try {
            ZipFile zip = new ZipFile(contentPack);
            ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(contentPack.toPath()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream));
            ZipEntry zipEntry;
            do {
                zipEntry = zipStream.getNextEntry();
                if (zipEntry == null)
                    continue;
                if (zipEntry.isDirectory())
                    continue;
                TypeFile typeFile = null;
                for (EnumType type : EnumType.values()) {
                    if (zipEntry.getName().startsWith(type.folderNames[0] + "/") && zipEntry.getName().split(type.folderNames[0] + "/").length > 1 && !zipEntry.getName().split(type.folderNames[0] + "/")[1].isEmpty()) {
                        String[] splitName = zipEntry.getName().split("/");
                        typeFile = new TypeFile(type, splitName[splitName.length - 1].split("\\.")[0], packName);
                    }
                }
                if (typeFile == null) {
                    continue;
                }
                for (; ; ) {
                    String line;
                    try {
                        line = reader.readLine();
                    } catch (Exception e) {
                        break;
                    }
                    if (line == null)
                        break;
                    typeFile.lines.add(line);
                }
            }
            while (zipEntry != null);
            reader.close();
            zip.close();
            zipStream.close();
        } catch (IOException e) {
            FlansMod.logger.error("Could not read zip archive {}", contentPack.getName(), e);
        }
    }

    /**
     * Whether resources need to be reloaded.
     *
     * @return Reload resources.
     */
    public boolean reloadResources() {
        return reloadResources;
    }

    /**
     * Gets a list of all the loaded content packs.
     *
     * @return List of files.
     */
    public List<File> getContentList() {
        return Collections.unmodifiableList(contentList);
    }

    /**
     * Wrapper for content packs loaded from the mods directory.
     */
    private static class ContentPackMod implements IFlansContentProvider {
        public final IFlansContentProvider provider;
        public final ModContainer container;

        public ContentPackMod(ModContainer container, IFlansContentProvider provider) {
            this.container = container;
            this.provider = provider;
        }

        @Override
        public String getContentDirectory() {
            return provider.getContentDirectory();
        }

        @Override
        public void registerModelLocations() {
            provider.registerModelLocations();
        }
    }

    /**
     * Wrapper for content packs loaded from the Flan directory.
     */
    private static class ContentPackFlans implements IFlansContentProvider {
        public final String directory;
        public final File source;

        public ContentPackFlans(String directory, File source) {
            this.directory = directory;
            this.source = source;
        }

        @Override
        public String getContentDirectory() {
            return directory;
        }

        @Override
        public void registerModelLocations() {
            try {
                if (source.isDirectory()) {
                    File redirectFile = new File(source, "redirect.txt");
                    if (redirectFile.exists()) {
                        BufferedReader reader = new BufferedReader(new FileReader(redirectFile));
                        String src = reader.readLine();
                        String destination = reader.readLine();

                        if (src != null && destination != null)
                            FlansMod.registerModelLocation(src, destination);
                    }
                } else if (ZIP_JAR.matcher(source.getName()).matches()) {
                    ZipFile zip = new ZipFile(source);
                    ZipEntry entry = zip.getEntry("redirect.txt");

                    if (entry != null && !entry.isDirectory()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                        String src = reader.readLine();
                        String destination = reader.readLine();

                        if (src != null && destination != null)
                            FlansMod.registerModelLocation(src, destination);

                        reader.close();
                    }

                    zip.close();
                }
            } catch (IOException ex) {
                FlansMod.logger.error("Could not register model locations", ex);
            }
        }
    }
}
