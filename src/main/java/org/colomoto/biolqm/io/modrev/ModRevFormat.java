package org.colomoto.biolqm.io.modrev;

import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.io.AbstractFormat;
import org.colomoto.biolqm.io.LogicalModelFormat;
import org.colomoto.biolqm.service.MultivaluedSupport;
import org.kohsuke.MetaInfServices;

/**
 * Format description for ModRev (.lp) files.
 * 
 * @author Pedro T. Monteiro
 */
@MetaInfServices(LogicalModelFormat.class)
public class ModRevFormat extends AbstractFormat {

	public ModRevFormat() {
		super("modrev", "Model Revision format", MultivaluedSupport.BOOLEAN_STRICT);
	}

//	@Override
//	public ModRevImport getLoader() {
//		return new ModRevImport();
//	}

	@Override
	public ModRevExport getExporter(LogicalModel model) {
		return new ModRevExport(model);
	}
}
