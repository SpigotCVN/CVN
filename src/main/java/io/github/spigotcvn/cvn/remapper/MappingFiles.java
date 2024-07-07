package io.github.spigotcvn.cvn.remapper;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.utils.URLUtil;
import io.github.spigotcvn.merger.MappingMerger;
import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import io.github.spigotcvn.smdownloader.SpigotMappingsDownloader;
import io.github.spigotcvn.smdownloader.json.BuildDataInfo;
import io.github.spigotcvn.smdownloader.mappings.MappingFile;

import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MappingFiles {
    private static final String MAPPINGS_URL = "https://raw.githubusercontent.com/SpigotCVN/CVN-mappings/main/mappings/%s.tiny";

    private final CVN plugin;
    private final Mappings mappings;

    File originalMappingFile;
    File finalMappingFile;
    public MappingFiles(CVN plugin, Mappings mappings) {
        this.plugin = plugin;
        this.mappings = mappings;
        this.finalMappingFile = new File(plugin.getCacheFolder(), "intermediary-class-replaced-" + plugin.getActualVersion().getOriginText() + ".tiny");
        this.originalMappingFile = new File(plugin.getCacheFolder(), "intermediary-mappings-"  + plugin.getActualVersion().getOriginText() +  ".tiny");
    }

    public File getOriginalMappingFile() {
        if(originalMappingFile.exists()) return originalMappingFile;
        else return null;
    }

    public File getFinalMappingFile() {
        if(finalMappingFile.exists()) return finalMappingFile;
        else return null;
    }

    public boolean isFinalMappingFilePresent() {
        return finalMappingFile.exists();
    }

    public void generateFinalMappingFile() {
        URL url;
        try {
            url = new URL(String.format(MAPPINGS_URL, plugin.getActualVersion().getOriginText()));
        } catch (MalformedURLException e) {
            // this should never happen but just in case lets print the stacktrace
            e.printStackTrace();
            return;
        }

        File buildDataDir = new File(plugin.getCacheFolder(), "spigot");
        try(SpigotMappingsDownloader smd =
                    new SpigotMappingsDownloader(buildDataDir, plugin.getActualVersion().getOriginText())) {
            File infoFile = new File(buildDataDir, "info.json");
            boolean shouldUpdateRepo = true;
            if(infoFile.exists()) {
                BuildDataInfo info = CVN.GSON.fromJson(new FileReader(infoFile), BuildDataInfo.class);
                shouldUpdateRepo = !info.getMinecraftVersion().equals(plugin.getActualVersion().getOriginText());
            }
            List<MappingFile> mappings = smd.downloadMappings(shouldUpdateRepo);
            MappingFile classMappings = mappings.stream()
                    .filter(m -> m.getType() == MappingFile.MappingType.CLASS)
                    .findFirst()
                    .orElse(null);

            File intermediaryMappings = new File(buildDataDir, "intermediary-mappings-" + plugin.getActualVersion().getOriginText() + ".tiny");
            if(!intermediaryMappings.exists()) {
                URLUtil.download(url, intermediaryMappings);
            }

            File intermediarySpigotClass = new File(buildDataDir, "intermediary-class-" + plugin.getActualVersion().getOriginText() + ".tiny");
            if(!intermediarySpigotClass.exists()) {
                TinyMappingFile intermediary = new TinyMappingFile();
                CSRGMappingFile spigotClass = new CSRGMappingFile();

                intermediary.loadFromFile(intermediaryMappings);
                spigotClass.loadFromFile(classMappings.getFile());

                MappingMerger.mergeTinyWithCSRG(intermediary, spigotClass, "spigot");

                intermediary.saveToFile(intermediarySpigotClass);
            }

            if(!finalMappingFile.exists()) {
                TinyMappingFile intermediaryClass = new TinyMappingFile();
                intermediaryClass.loadFromFile(intermediarySpigotClass);

                MappingMerger.replaceOriginalNamespace(intermediaryClass, "spigot");

                intermediaryClass.saveToFile(finalMappingFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
