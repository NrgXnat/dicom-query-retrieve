/*
 * dicom-query-retrieve: xnat-patch.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*
 Function overrides.
 */

(function(window, XNAT){


    XNAT.tab  = getObject(XNAT.tab || {});
    XNAT.tabs = getObject(XNAT.tabs || {});

    // ==================================================
    // Override `XNAT.tab.activate()` until it can be fixed
    XNAT.tab.activate = XNAT.tab.select = function(name, container){
        // console.log('tab.select / tab.activate');
        XNAT.tab.active = XNAT.tabs.active =
            name || XNAT.tab.active || XNAT.tabs.active;
        var $container  = $$(container || XNAT.tabs.container || 'body');
        var tabSelector =
                XNAT.tab.active ?
                    '[data-tab="' + XNAT.tab.active + '"]' :
                    '[data-tab]';
        if (!$(tabSelector).first().length) return;
        $container
            .find('li.tab')
            .removeClass('active')
            .filter(tabSelector)
            .addClass('active')
            .hidden(false);
        $container
            .find('div.tab-pane')
            .removeClass('active')
            .filter(tabSelector)
            .addClass('active')
            .hidden(false);
        // if a tab is being activated, make sure
        // the container is NOT hidden
        $container.hidden(false, 200);
        window.location.hash = XNAT.url.updateHashQuery(window.location.hash, 'tab', XNAT.tab.active, /\*#\/*|#+/);
    };

})(this, getObject(XNAT || {}));