package at.enactmentengine.serverless.simulation.metadata;

import at.enactmentengine.serverless.simulation.metadata.cache.JsonProvider;
import at.enactmentengine.serverless.simulation.metadata.database.DatabaseProvider;

import java.io.File;

public class ProviderSelector {

    private static final String[] REQUIRED_FILES = {
            "metadata/functionimplementation.json",
            "metadata/functiondeployment.json",
            "metadata/provider.json",
            "metadata/region.json",
            "metadata/cpu.json",
            "metadata/datatransfer.json",
            "metadata/networking.json",
            "metadata/service.json",
            "metadata/servicedeployment.json"
    };

    public static DataProvider getProvider(boolean forceDatabaseProvider) {
        if (!forceDatabaseProvider && jsonFilesExist()) {
            MetadataStore.USE_JSON_METADATA = true;
            return JsonProvider.get();
        } else {
            return DatabaseProvider.get();
        }
    }

    private static boolean jsonFilesExist() {
        for (String filePath : REQUIRED_FILES) {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }
}
