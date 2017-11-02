package org.nrg.xnat.generated.plugin;

import org.nrg.framework.annotations.XnatDataModel;
import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@XnatPlugin(value = "image-search", name = "TIP Image Search Plugin", description = "This is the XNAT 1.7 TIP Image Search Plugin.",
    entityPackages = "org.nrg.tip.domain.entities")
@ComponentScan({"org.nrg.tip.services",
        "org.nrg.tip.daos"})
public class TipImageSearchPlugin {
}