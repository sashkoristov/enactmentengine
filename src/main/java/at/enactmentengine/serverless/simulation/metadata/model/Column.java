package at.enactmentengine.serverless.simulation.metadata.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String name();

    Class clazz();

    Class converter() default Void.class;
}
