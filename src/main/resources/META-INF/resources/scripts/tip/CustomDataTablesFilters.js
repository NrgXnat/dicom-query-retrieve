/*
 * D:/Development/TIP/tip/image-search/src/main/resources/module-resources/scripts/tip/CustomDataTablesFilters.js
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

/*jslint white: true, vars: true */
function StringIndexOfFilter() {
    "use strict";

    var that = this;

    this.getFilterRegex = function (filterText) {
        return filterText;
    };

}

function StringStartsWithFilter() {
    "use strict";

    var that = this;

    this.getFilterRegex = function (filterText) {
        return "^" + filterText;
    };

}
