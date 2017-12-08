/*
 * PacsAdministration
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

import java.util.*;

public class PacsAdministration extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
//        Map<String, OrmStrategy> strategyMap = XDAT.getContextService().getBeansOfType(OrmStrategy.class);
          Map<OrmStrategy, String> strategies = new TreeMap<OrmStrategy, String>();
//        for (Map.Entry<String, OrmStrategy> strategy : strategyMap.entrySet()) {
//            strategies.put(strategy.getValue(), strategy.getKey());
//        }
        Object strategy = XDAT.getContextService().getBean("dicomOrmStrategy");
        if(strategy!=null && strategy instanceof OrmStrategy){
            strategies.put((OrmStrategy)strategy, "dicomOrmStrategy");
        }
        context.put("strategies", strategies);
    }

}
