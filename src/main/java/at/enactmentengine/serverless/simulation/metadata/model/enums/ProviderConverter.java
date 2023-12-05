package at.enactmentengine.serverless.simulation.metadata.model.enums;

import at.uibk.dps.util.Provider;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public class ProviderConverter implements DbConverter {

    @Override
    public Object from(String dbValue) {
        return dbValue != null ? Provider.valueOf(dbValue) : null;
    }

    @Override
    public String to(Object object) {
        return object != null ? object.toString() : null;
    }
}
