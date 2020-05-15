/*
 * dicom-query-retrieve: CustomDataTablesFilters.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
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
