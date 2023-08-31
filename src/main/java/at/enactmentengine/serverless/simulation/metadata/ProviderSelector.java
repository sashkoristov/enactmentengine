package at.enactmentengine.serverless.simulation.metadata;

import at.enactmentengine.serverless.simulation.metadata.cache.JsonProvider;
import at.enactmentengine.serverless.simulation.metadata.database.DatabaseProvider;

public class ProviderSelector {
    public static DataProvider getProvider(boolean forceDatabaseProvider) {
        if (!forceDatabaseProvider && jsonFilesExist()) {
            MetadataStore.USE_JSON_METADATA = true;
            return JsonProvider.get();
        } else {
            return DatabaseProvider.get();
        }
    }

    private static boolean jsonFilesExist() {
        // TODO check if the metadata/*.json files exist
        return false;
    }
}
