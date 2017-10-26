/*
 * D:/Development/TIP/tip/image-search/src/main/resources/module-resources/scripts/tip/PacsSeriesFinder.js
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

/*jslint white: true, browser: true, vars: true */

function PacsSeriesFinder(study, targetDomElement, rowExpansionImage, rowExpansionHandler, pacsId) {
    "use strict";

    this.constants = {
        "CLIENT_ERROR_NOT_FOUND": 404
    };

    this.study = study;

    this.targetDomElement = targetDomElement;

    this.rowExpansionImage = rowExpansionImage;

    this.rowExpansionHandler = rowExpansionHandler;

    this.findSeries = function () {
        jq.ajax({
            type: "GET",
            url: serverRoot + "/data/services/pacs/" + pacsId + "/search/studies/" + study.studyInstanceUid + "/series?XNAT_CSRF=" + csrfToken,
            dataType: "json",
            context: this,
            success: this.showSeriesSearchResults,
            error: this.handleSeriesSearchFailure
        });
    };

    this.showSeriesSearchResults = function (data) {
        this.sortResults(data.ResultSet.Result);

        var seriesDataTableIdSuffix = this.study.studyInstanceUid.replace(/\./g, "_");
        var seriesDataTable = jq('<table id="pacsSeriesFinderSearchResults_' + seriesDataTableIdSuffix + '" cellpadding="0" cellspacing="0" border="0" class="pacsSeriesSearchResults"/>');
        var that = this;
        var dataTableOptions = {
            "aaData": data.ResultSet.Result,
            "aoColumns": [
                {
                    "mData": "seriesNumber",
                    "sTitle": "Series",
                    "sWidth": "150px"
                },
                {
                    "mData": "modality",
                    "sTitle": "Modality",
                    "sWidth": "150px"
                },
                {
                    "mData": "seriesDescription",
                    "sTitle": "Description",
                    "sWidth": "100%"
                }
            ],
            "bFilter": false,
            "bPaginate": false,
            "bSort": false,
            "bLengthChange": false,
            "bInfo": false,
            "bAutoWidth": false
        };

        jq(seriesDataTable).dataTable(dataTableOptions);
        this.writeResultsToTarget(seriesDataTable);

        this.reattachRowExpansionHandler();
    };

    /*
     * Since we're disabling sorting in the DataTable,
     * sort it before presenting
     */
    this.sortResults = function (resultsJsonArray) {
        resultsJsonArray.sort(function (a, b) {
            return a.seriesNumber - b.seriesNumber;
        });
    };

    this.handleSeriesSearchFailure = function (jqXHR) {
        if (this.constants.CLIENT_ERROR_NOT_FOUND === jqXHR.status) {
            this.writeResultsToTarget("There were no series found.");
        } else {
            this.writeResultsToTarget("Error " + jqXHR.status + ": " + jqXHR.responseText);
        }

        this.reattachRowExpansionHandler();
    };

    this.writeResultsToTarget = function (results) {
        jq(this.targetDomElement).html(results);
        jq(this.targetDomElement).removeClass("rowDetailsExpanding").addClass("rowDetailsExpanded");
    };

    this.reattachRowExpansionHandler = function () {
        jq(this.rowExpansionImage).addClass("rowDetailsExpander");
    };
}
