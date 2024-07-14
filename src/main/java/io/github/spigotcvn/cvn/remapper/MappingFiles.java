package io.github.spigotcvn.cvn.remapper;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.utils.URLUtil;
import io.github.spigotcvn.merger.MappingMerger;
import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import io.github.spigotcvn.smdownloader.SpigotMappingsDownloader;
import io.github.spigotcvn.smdownloader.io.IOUtils;
import io.github.spigotcvn.smdownloader.json.BuildDataInfo;
import io.github.spigotcvn.smdownloader.mappings.MappingFile;
import org.stianloader.picoresolve.version.MavenVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.List;

public class MappingFiles {
    private static final String MAPPINGS_URL = "https://raw.githubusercontent.com/SpigotCVN/CVN-mappings/main/mappings/%s.tiny";
    private static final MavenVersion RELOCATED_BEFORE = MavenVersion.parse("1.17");

    private final CVN plugin;
    private final Mappings mappings;

    private final File originalMappingFile;
    private final File finalMappingFile;

    public MappingFiles(CVN plugin, Mappings mappings) {
        this.plugin = plugin;
        this.mappings = mappings;
        this.finalMappingFile = new File(plugin.getCacheFolder(), "intermediary-class-replaced-" + plugin.getActualVersion().getOriginText() + ".tiny");
        this.originalMappingFile = new File(plugin.getCacheFolder(), "intermediary-mappings-"  + plugin.getActualVersion().getOriginText() +  ".tiny");
    }

    public File getOriginalMappingFile() {
        return originalMappingFile.exists() ? originalMappingFile : null;
    }

    public File getFinalMappingFile() {
        return finalMappingFile.exists() ? finalMappingFile : null;
    }

    public boolean isFinalMappingFilePresent() {
        return finalMappingFile.exists();
    }

    public void generateFinalMappingFile() {
        try {
            URL url = new URL(String.format(MAPPINGS_URL, plugin.getActualVersion().getOriginText()));

            File buildDataDir = new File(plugin.getCacheFolder(), "spigot");
            File craftBukkitDir = new File(plugin.getCacheFolder(), "craftbukkit");

            try (SpigotMappingsDownloader smd = new SpigotMappingsDownloader(buildDataDir, plugin.getActualVersion().getOriginText())) {
                File infoFile = new File(buildDataDir, "info.json");
                boolean shouldUpdateRepo = shouldUpdateRepo(infoFile);
                List<MappingFile> mappings = smd.downloadMappings(shouldUpdateRepo);

                if (shouldUpdateRepo) {
                    updateCBRepo(buildDataDir, craftBukkitDir, smd);
                }

                File cbPom = new File(craftBukkitDir, "pom.xml");
                String cbNotation = getCbNotation(cbPom);
                String nmsPackage = "net/minecraft/server/v" + cbNotation + "/";

                File intermediaryMappings = new File(buildDataDir, "intermediary-mappings-" + plugin.getActualVersion().getOriginText() + ".tiny");
                if (!intermediaryMappings.exists()) {
                    URLUtil.download(url, intermediaryMappings);
                }

                processIntermediaryMappings(buildDataDir, mappings, nmsPackage, intermediaryMappings);

                if (!finalMappingFile.exists()) {
                    TinyMappingFile intermediaryClass = new TinyMappingFile();
                    intermediaryClass.loadFromFile(new File(buildDataDir, "intermediary-class-" + plugin.getActualVersion().getOriginText() + ".tiny"));
                    MappingMerger.replaceOriginalNamespace(intermediaryClass, "spigot");
                    intermediaryClass.saveToFile(finalMappingFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldUpdateRepo(File infoFile) throws Exception {
        if (infoFile.exists()) {
            BuildDataInfo info = CVN.GSON.fromJson(new FileReader(infoFile), BuildDataInfo.class);
            return !info.getMinecraftVersion().equals(plugin.getActualVersion().getOriginText());
        }
        return true;
    }

    private void updateCBRepo(File buildDataDir, File craftBukkitDir, SpigotMappingsDownloader smd) throws Exception {
        String cbRev = smd.getVersionData().getRefs().getCraftBukkit();
        if (buildDataDir.exists() && buildDataDir.isDirectory()) {
            IOUtils.deleteDirectory(buildDataDir);
        }
        try (SpigotMappingsDownloader cbSmd = new SpigotMappingsDownloader(
                craftBukkitDir, plugin.getActualVersion().getOriginText(), "https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git")) {
            cbSmd.pullBuildDataGit(cbRev);
        }
    }

    private String getCbNotation(File cbPom) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cbPom);
        doc.getDocumentElement().normalize();
        Element properties = (Element) doc.getElementsByTagName("properties").item(0);
        return properties.getElementsByTagName("minecraft_version").item(0).getTextContent();
    }

    private void processIntermediaryMappings(File buildDataDir, List<MappingFile> mappings, String nmsPackage, File intermediaryMappings) throws Exception {
        File intermediarySpigotClass = new File(buildDataDir, "intermediary-class-" + plugin.getActualVersion().getOriginText() + ".tiny");
        if (!intermediarySpigotClass.exists()) {
            TinyMappingFile intermediary = new TinyMappingFile();
            CSRGMappingFile spigotClass = new CSRGMappingFile();

            intermediary.loadFromFile(intermediaryMappings);
            spigotClass.loadFromFile(mappings.stream()
                    .filter(m -> m.getType() == MappingFile.MappingType.CLASS)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Class mappings not found"))
                    .getFile());

            MappingMerger.mergeTinyWithCSRG(intermediary, spigotClass, "spigot");
            intermediary.saveToFile(intermediarySpigotClass);

            if (!plugin.getActualVersion().isNewerThan(RELOCATED_BEFORE)) {
                intermediary.loadFromFile(intermediarySpigotClass);
                applyPackageMappings(intermediary, nmsPackage);
                intermediary.saveToFile(intermediarySpigotClass);
            }
        }
    }

    private void applyPackageMappings(TinyMappingFile intermediary, String nmsPackage) throws Exception {
        CSRGMappingFile packageMappings = new CSRGMappingFile();
        String packageMappingsString = "./ " + nmsPackage + "\nnet/minecraft/server/ " + nmsPackage;
        packageMappings.loadFromStream(new ByteArrayInputStream(packageMappingsString.getBytes()));
        MappingMerger.applyPackageMapping(intermediary, packageMappings, true);
    }
}
