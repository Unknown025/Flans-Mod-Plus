package com.flansmod.client;

import com.flansmod.common.FlansMod;
import cpw.mods.fml.common.ICrashCallable;

import java.io.File;
import java.util.List;

public class FlansCrash implements ICrashCallable {
    @Override
    public String getLabel() {
        return "Flan's Mod Content Packs";
    }

    @Override
    public String call() throws Exception {
        List<File> contentPacks = FlansMod.CONTENT_MANAGER.getContentList();
        StringBuilder builder = new StringBuilder();
        for (File file : contentPacks) {
            builder.append("\n").append(file.getName()).append(" (filepath: ").append(file.getAbsolutePath()).append(")");
        }
        return builder.toString();
    }
}
