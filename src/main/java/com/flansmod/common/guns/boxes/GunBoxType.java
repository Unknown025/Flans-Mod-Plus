package com.flansmod.common.guns.boxes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.guns.GunType;
import com.flansmod.utils.ConfigMap;
import com.flansmod.utils.ConfigUtils;
import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public class GunBoxType extends InfoType
{
	public BlockGunBox block;
	public String topTexturePath;
	public String sideTexturePath;
	public String bottomTexturePath;
	public IIcon top;
	public IIcon side;
	public IIcon bottom;
	public int nextGun = -1;

	public GunBoxEntry[] gunEntries;
	public List<GunPage> gunPages = new ArrayList<GunPage>();
	public GunPage currentPage;

	/** Custom GUI variables. Use an unsigned hex code for colors.*/
	public String guiTexturePath;
	public String gunBoxTextColor = "404040";
	public String itemListTextColor = "404040";
	public String itemTextColor= "404040";
	public String pageTextColor = "FFFFFF";
	public String buttonTextColor = "FFFFFF";
	public String buttonTextHoverColor = "FFFFA0";

	private static int lastIconIndex = 2;
	public static HashMap<String, GunBoxType> gunBoxMap = new HashMap<String, GunBoxType>();
	
	public GunBoxType(TypeFile file)
	{
		super(file);
	}
	
	@Override
	public void preRead(TypeFile file) {
		gunEntries = new GunBoxEntry[8];
		currentPage = new GunPage("Default");
	}
	
	@Override
	public void postRead(TypeFile file) {

		//Add any remaining gun entries to the pagelist if EOF.
		currentPage.addGunList(Arrays.copyOf(gunEntries, nextGun + 1));
		gunPages.add(currentPage);

		if (this.shortName != null && isValid) {
			gunBoxMap.put(this.shortName, this);
		}
	}

	@Override
	protected void readLine(String[] split, TypeFile file) {
		if (split.length < 1) {
			return;
		}

		String kword = split[0];

		if (kword.equalsIgnoreCase("Page") || kword.equalsIgnoreCase("SetPage")) {
			try {
				//If empty, rename the page. If not, add the current page to list and start next one.
				StringBuilder pageName = new StringBuilder();
				for(int i = 1; i < split.length; i++)
				{
					pageName.append(split[i]);
					if((i + 1) < split.length)
					{
						pageName.append(" ");
					}
				}

				if(gunEntries[0] != null)
				{
					currentPage.addGunList(Arrays.copyOf(gunEntries, nextGun + 1));
					iteratePage(pageName.toString());
				}
				else
				{
					currentPage.setPageName(pageName.toString());
				}
			} catch (Exception ex) {
				FlansMod.logPackError(file.name, packName, shortName, "Setting Page name failed", split, ex);
			}
		}


		if (kword.equalsIgnoreCase("AddGun"))
		{
			try {
				InfoType type = InfoType.getType(split[1]);

				if (type == null) {
					FlansMod.logPackError(file.name, packName, shortName, "Unable to find item for gunbox, skipping entry", split, null);
					return;
				}

				List<ItemStack> parts = getRecipe(split);
				nextGun++;
				if(nextGun > gunEntries.length - 1)
				{
					currentPage.addGunList(Arrays.copyOf(gunEntries, nextGun));
					iteratePage("Default " + (gunPages.size() + 2));
					nextGun++;
				}
				gunEntries[nextGun] = new GunBoxEntry(type, parts);
			} catch(Exception ex) {
				FlansMod.logPackError(file.name, packName, shortName, "Adding gun to GunBox failed", split, ex);
			}
		}


		if (kword.equalsIgnoreCase("AddAmmo") || kword.equalsIgnoreCase("AddAltAmmo") || kword.equalsIgnoreCase("AddAlternateAmmo")) {
			try {
				InfoType ammoType = InfoType.getType(split[1]);
				if (ammoType == null || ammoType.item == null) {
					FlansMod.logPackError(file.name, packName, shortName, "Ammo item not found for gunbox, skipping", split, null);
					return;
				}

				gunEntries[nextGun].addAmmoEntry(new GunBoxEntry(ammoType, getRecipe(split)));

			} catch(Exception ex) {
				FlansMod.logPackError(file.name, packName, shortName, "Adding ammo to GunBox failed", split, ex);
			}
		}
	}

	@Override
	protected void read(ConfigMap config, TypeFile file) {
		super.read(config, file);
		try {
			//Block Textures
			topTexturePath = ConfigUtils.configString(config, "TopTexture", topTexturePath);
			bottomTexturePath = ConfigUtils.configString(config, "BottomTexture", bottomTexturePath);
			sideTexturePath = ConfigUtils.configString(config, "SideTexture", sideTexturePath);

			//GunBox gui customisation
			guiTexturePath = ConfigUtils.configString(config, "GuiTexture", guiTexturePath);
			gunBoxTextColor = ConfigUtils.configString(config, "GunBoxNameColor", gunBoxTextColor);
			pageTextColor = ConfigUtils.configString(config, "PageTextColor", pageTextColor);
			itemListTextColor = ConfigUtils.configString(config, "ListTextColor", itemListTextColor);
			itemTextColor = ConfigUtils.configString(config, "ItemTextColor", itemTextColor);
			buttonTextColor = ConfigUtils.configString(config, "ButtonTextColor", buttonTextColor);
			buttonTextHoverColor = ConfigUtils.configString(config, "ButtonTextHighlight", buttonTextHoverColor);

			// Entry loading MUST happen in order, so cannot be done using the config map.
		}
		catch (Exception ex) {
			FlansMod.logPackError(file.name, packName, shortName, "Fatal error thrown while loading GunBox", null, ex);
			isValid = false;
		}
	}

	public void iteratePage(String s) {
		//Add current to the pages and setup a new current
		gunPages.add(currentPage);
		gunEntries = new GunBoxEntry[8];
		nextGun = -1;
		currentPage = new GunPage(s);
	}

	public static GunBoxType getBox(String s) {
		return gunBoxMap.get(s);
	}
	
	public static GunBoxType getBox(Block block) {
		for(GunBoxType type : gunBoxMap.values()) {
			if(type.block == block)
				return type;
		}
		return null;
	}

	//Gunbox Entry method
	public List<ItemStack> getRecipe(String[] split) {
		List<ItemStack> recipe = new ArrayList<ItemStack>();

		for (int i = 0; i < (split.length - 2) / 2; i++) {
			ItemStack recipeElement = null;
			if (split[i * 2 + 3].contains(".")) {
				recipeElement = getRecipeElement(split[i * 2 + 3].split("\\.")[0], Integer.parseInt(split[i * 2 + 2]), Integer.valueOf(split[i * 2 + 3].split("\\.")[1]), shortName);
			} else {
				 recipeElement = getRecipeElement(split[i * 2 + 3], Integer.parseInt(split[i * 2 + 2]), 0, shortName);
			}

			if (recipeElement != null) {
				recipe.add(recipeElement);
			} else {
				FlansMod.logPackError(null, packName, shortName, "Could not find item for recipe", split, null);
			}
		}

		return recipe;
	}
	
	/** Reimported from old code */
	@Override
	public void addRecipe(Item par1Item) {
		if (smeltableFrom != null) {
			ItemStack recipeElement = getRecipeElement(smeltableFrom, 0);

			if (recipeElement != null) {
				GameRegistry.addSmelting(recipeElement, new ItemStack(item), 0.0F);
			} else {
				FlansMod.logPackError(null, packName, shortName, "Could not find item for SmeltableFrom", null, null);
			}
		}
		if (recipeLine == null)
			return;
		try {
			if (!shapeless)
			{
				// Fix oversized recipes
				int rows = 3;
				// First column
				if (((String) recipe[0]).charAt(0) == ' ' && ((String) recipe[1]).charAt(0) == ' ' && ((String) recipe[2]).charAt(0) == ' ') {
					for (int i = 0; i < 3; i++) {
						recipe[i] = ((String) recipe[i]).substring(1);
					}

					// New first column
					if (((String) recipe[0]).charAt(0) == ' ' && ((String) recipe[1]).charAt(0) == ' ' && ((String) recipe[2]).charAt(0) == ' ') {
						for (int i = 0; i < 3; i++)
							recipe[i] = ((String) recipe[i]).substring(1);
					}
				}
				// Last column
				int last = ((String) recipe[0]).length() - 1;
				if (((String) recipe[0]).charAt(last) == ' ' && ((String) recipe[1]).charAt(last) == ' ' && ((String) recipe[2]).charAt(last) == ' ') {
					for (int i = 0; i < 3; i++)
						recipe[i] = ((String) recipe[i]).substring(0, last);
					// New last column
					last--;
					if (((String) recipe[0]).charAt(last) == ' ' && ((String) recipe[1]).charAt(last) == ' ' && ((String) recipe[2]).charAt(last) == ' ') {
						for (int i = 0; i < 3; i++)
							recipe[i] = ((String) recipe[i]).substring(0, 0);
					}
				}
				// Top row
				if (recipe[0].equals(" ") || recipe[0].equals("  ") || recipe[0].equals("   ")) {
					Object[] newRecipe = new Object[recipe.length - 1];
					newRecipe[0] = recipe[1];
					newRecipe[1] = recipe[2];
					recipe = newRecipe;
					rows--;
					// Next top row
					if (recipe[0].equals(" ") || recipe[0].equals("  ") || recipe[0].equals("   "))
					{
						Object[] newRecipe1 = new Object[recipe.length - 1];
						newRecipe1[0] = recipe[1];
						recipe = newRecipe1;
						rows--;
					}
				}
				// Bottom row
				if (recipe[rows - 1].equals(" ") || recipe[rows - 1].equals("  ") || recipe[rows - 1].equals("   ")) {
					Object[] newRecipe = new Object[recipe.length - 1];
					newRecipe[0] = recipe[0];
					newRecipe[1] = recipe[1];
					recipe = newRecipe;
					rows--;
					// Next bottom row
					if (recipe[rows - 1].equals(" ") || recipe[rows - 1].equals("  ") || recipe[rows - 1].equals("   ")) {
						Object[] newRecipe1 = new Object[recipe.length - 1];
						newRecipe1[0] = recipe[0];
						recipe = newRecipe1;
						rows--;
					}
				}
				for (int i = 0; i < (recipeLine.length - 1) / 2; i++) {
					recipe[i * 2 + rows] = recipeLine[i * 2 + 1].charAt(0);
					// Split ID with . and if it contains a second part, use it
					// as damage value.
					ItemStack recipeElement = null;
					if (recipeLine[i * 2 + 2].contains("."))
						recipeElement = getRecipeElement(recipeLine[i * 2 + 2].split("\\.")[0], Integer.valueOf(recipeLine[i * 2 + 2].split("\\.")[1]));
					else
						recipeElement = getRecipeElement(recipeLine[i * 2 + 2], 0);

					if (recipeElement == null) {
						FlansMod.logPackError(null, packName, shortName, "Could not find item for recipe", new String[] { recipeLine[i * 2 + 2] }, null);
					}
					recipe[i * 2 + rows + 1] = recipeElement;
				}
				GameRegistry.addRecipe(new ItemStack(block, recipeOutput, 0), recipe);
			}
			else {
				recipe = new Object[recipeLine.length - 1];
				for (int i = 0; i < (recipeLine.length - 1); i++) {
					ItemStack recipeElement = null;
					if (recipeLine[i + 1].contains("."))
						recipeElement = getRecipeElement(recipeLine[i + 1].split("\\.")[0], Integer.valueOf(recipeLine[i + 1].split("\\.")[1]));
					else
						recipeElement = getRecipeElement(recipeLine[i + 1], 0);

					if (recipeElement == null) {
						FlansModClient.logPackError(null, packName, shortName, "Could not find item for recipe", new String[] { recipeLine[i+1] }, null);
					}

					recipe[i] = recipeElement;
				}
				GameRegistry.addShapelessRecipe(new ItemStack(block, recipeOutput, 0), recipe);
			}
		}
		catch (Exception e) {
			if(recipe!=null) {
				StringBuilder msg = new StringBuilder(" : ");
				for(Object o : recipe) msg.append(" ").append(o);
				FlansMod.log("Failed to add recipe for : " + shortName + msg);
			} else {
				FlansMod.log("Failed to add recipe for : " + shortName);
			}
			if(FlansMod.printStackTrace) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public float GetRecommendedScale() { return 50.0f; }

	@Override
	@SideOnly(Side.CLIENT)
	public ModelBase GetModel() { return null; }

	public static GunBoxEntry getEntryFromType(GunType gunType){
		for(GunBoxType gbt : gunBoxMap.values()){
			for(GunBoxEntry gbe : gbt.gunEntries){
				if(gbe.type.shortName.equals(gunType.shortName))
					return gbe;
			}
		}
		return null;
	}
}
