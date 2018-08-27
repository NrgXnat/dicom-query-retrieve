/*
 * D:/Development/DQR/dqr/src/main/resources/module-resources/scripts/dqr/PacsSessionFinder.js
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

/*jslint white: true, browser: true, vars: true */

console.log('PacsSessionFinder.js');

function PacsSessionFinder(sessionSearchFormId, sessionSelectionFormId, sessionSelectionStudyInstanceUidInputId, sessionSelectionPacsIdInputId, sessionSearchResultsDivId, imagePath) {
    "use strict";

    var that = this;

    this.constants = {
        "MODAL_WINDOW_NAME": "loadData",
        "CLIENT_ERROR_BAD_REQUEST": 400,
        "CLIENT_ERROR_NOT_FOUND": 404,
        "STUDY_DATE_COLUMN": 4,
        "OPEN_ROW_IMAGE": "row_details_open.png",
        "CLOSED_ROW_IMAGE": "row_details_close.png",
        "OPEN_ROW_ALT": "Show the series for this study",
        "CLOSED_ROW_ALT": "Hide the series for this study",
        "PROCESS_BUTTON_CLASS": "processButton"
    };

    this.sessionSearchFormId = sessionSearchFormId;

    this.sessionSelectionFormId = sessionSelectionFormId;

    this.sessionSelectionStudyInstanceUidInputId = sessionSelectionStudyInstanceUidInputId;

    this.sessionSelectionPacsIdInputId = sessionSelectionPacsIdInputId;

    this.sessionSearchResultsDivId = sessionSearchResultsDivId;

    this.imagePath = imagePath;

    this.activeElementBeforeSearch = null;

    this.currentPacsId = null;

    this.findSessions = function (pacsId) {
        this.currentPacsId = pacsId;


        XNAT.xhr.ajax({
            type: "POST",
            url: XNAT.url.csrfUrl("/data/services/pacs/" + pacsId + "/search/studies"),
            data: jq("#" + this.sessionSearchFormId).serialize(),
            dataType: "json",
            context: this,
            success: this.showSessionSearchResults,
            error: this.handleSessionSearchFailure
        });

        this.saveFocusedField();
        openModalPanel(this.constants.MODAL_WINDOW_NAME, "Loading data...");
    };

    this.showSessionSearchResults = function (data) {
        var sessionSearchResultsTableId = sessionSearchResultsDivId + "Table";

        jq("#" + sessionSearchResultsDivId).empty().html('<div class="friendlyForm"><h4>PACS Query Results</h4></div><table cellpadding="0" cellspacing="0" border="0" class="pacsSessionSearchResults xnat-table data-table compact" id="' + sessionSearchResultsTableId + '"/>');

        var stringStartsWithFilter = new StringStartsWithFilter();

        var dataTableOptions = {
            "aaData": data.ResultSet.Result,
            "aoColumns": [
                {
                    "bSearchable": false,
                    "bSortable": false,
                    "mData": null,
                    "sDefaultContent": '<i title="' + that.constants.OPEN_ROW_ALT + '" class="fa fa-plus-square rowDetailsExpander" style="color: green; font-size: 1.25em;" />'
                },
                {
                    "mData": "patient.id",
                    "sTitle": "Patient ID",
                    "dqrCustomFilter": stringStartsWithFilter
                },
                {
                    "mData": function (source) {
                        return source.patient.name.lastNameCommaFirstName;
                    },
                    "sTitle": "Patient Name"
                },
                {
                    "mData": "accessionNumber",
                    "sTitle": "Accession #",
                    "dqrCustomFilter": stringStartsWithFilter
                },
                {
                    "mData": function (source) {
                        return that.dateFormatter(source.studyDate);
                    },
                    "sTitle": "Study Date",
                    "dqrCustomFilter": stringStartsWithFilter
                },
                {
                    "mData": "studyDescription",
                    "sTitle": "Study Description"
                },
                {
                    "mData": "patient.sex",
                    "sTitle": "Gender"
                },
                {
                    "mData": function (source) {
                        return that.ageFormatter(source.patient.birthDate);
                    },
                    "sTitle": "Age",
                    "dqrCustomFilter": stringStartsWithFilter
                },
                {
                    "mData": "studyId",
                    "sTitle": "Study ID",
                    "dqrCustomFilter": stringStartsWithFilter
                },
                /* {
                    "mData": function (source) {
                        return source.referringPhysicianName.lastNameCommaFirstName;
                    },
                    "sTitle": "Referring Physician"
                }, */
                {
                    "bSearchable": false,
                    "bSortable": false,
                    "mData": null,
                    "sDefaultContent": '<button class="' + that.constants.PROCESS_BUTTON_CLASS + '">Choose</button>'
                }
            ],
            "oLanguage": {
                "sInfoPostFix": ""
            },
            "iDisplayLength": 10,
            "aaSorting": [
                [that.constants.STUDY_DATE_COLUMN, "desc"]
            ]
        };

        if (data.ResultSet.limitedResultSetSize) {
            dataTableOptions.oLanguage.sInfoPostFix = "<br/><span style='color:red;'>These search results were limited to " + data.ResultSet.resultSetSize + " records to avoid overtaxing the PACS.  You may need to narrow your search to find what you're looking for.</span>";
        } else if (data.ResultSet.studyDateRangeLimitResults.limited) {
            dataTableOptions.oLanguage.sInfoPostFix = "<br/><span style='color:red;'>" + data.ResultSet.studyDateRangeLimitResults.limitExplanation + "</span>";
        }

        jq("#" + sessionSearchResultsTableId).dataTable(dataTableOptions);

        this.addColumnFilters(sessionSearchResultsTableId, dataTableOptions.aoColumns);

        this.bindRowExpansionHandler(sessionSearchResultsTableId);

        this.bindProcessButtonHandler(sessionSearchResultsTableId);

        $('table.dataTable').removeClass('dataTable');

        closeModalPanel(this.constants.MODAL_WINDOW_NAME);
        this.restoreFocusedField();
    };

    this.addColumnFilters = function (sessionSearchResultsTableId, dataTableColumns) {
        var filterHeaderRowId = "filterHeaderRow";
        jq("#" + sessionSearchResultsTableId).find('thead').append('<tr id="' + filterHeaderRowId + '" class="filter">');

        dataTableColumns.forEach(function(column){
            if (column.mData) {
                var inputId = filterHeaderRowId + "Input" + i;
                jq("#" + filterHeaderRowId).append('<th class="noPointer"><input type="text" id="' + inputId + '" name="' + inputId + '" placeholder="Filter..." class="filter_init" /></th>');
            } else {
                jq("#" + filterHeaderRowId).append('<th class="noPointer"/>');
            }
        });

        var asInitVals = [];

        jq("#" + sessionSearchResultsTableId + " thead input").each(function (i) {
            asInitVals[i] = this.value;
        });

        jq("#" + sessionSearchResultsTableId + " thead input").focus(function () {
            if (this.className === "filter_init") {
                this.className = "";
                this.value = "";
            }
        });

        jq("#" + sessionSearchResultsTableId + " thead input").blur(function () {
            if (this.value === "") {
                this.className = "filter_init";
                this.value = asInitVals[jq("#" + sessionSearchResultsTableId + " thead input").index(this)];
            }
        });

        jq("#" + sessionSearchResultsTableId + " thead input").keyup(function () {
            /* Filter on the column (the index) of this element, +1 to account for the row expander column */
            var columnIndexOfThisFilter = jq("#" + sessionSearchResultsTableId + " thead input").index(this) + 1;
            jq("#" + sessionSearchResultsTableId).dataTable().fnFilter(that.getFilterRegex(this.value, dataTableColumns[columnIndexOfThisFilter]), columnIndexOfThisFilter, true);
        });

        // we can't turn off filtering entirely on the table cause then our individual column filters won't work
        // so just hide the global (all-column) filter
        jq("#" + sessionSearchResultsTableId + "_filter").css("display", "none");
    };

    this.getFilterRegex = function (filterText, dataTableColumn) {
        if (dataTableColumn.dqrCustomFilter) {
            return dataTableColumn.dqrCustomFilter.getFilterRegex(filterText);
        }
        // no custom filter specified, use the default
        return new StringIndexOfFilter().getFilterRegex(filterText);
    };

    this.bindRowExpansionHandler = function (sessionSearchResultsTableId) {
        var rowExpansionHandler = function () {
            var nTr = jq(this).parents('tr')[0];
            var oTable = jq("#" + sessionSearchResultsTableId).dataTable();
            if (oTable.fnIsOpen(nTr)) {
                // This row is already open - close it

                $(this).removeClass('fa-minus-square').addClass('fa-plus-square').css('color','green');
                this.title = that.constants.OPEN_ROW_ALT;
                oTable.fnClose(nTr);
            } else {
                //Open this row

                // prevent overcaffeinated clicking of the image
                jq(this).removeClass("rowDetailsExpander");

                $(this).removeClass('fa-plus-square').addClass('fa-minus-square').css('color','#888');
                this.title = that.constants.CLOSED_ROW_ALT;
                var seriesRow = oTable.fnOpen(nTr, "Loading series...<img src=\"" + serverRoot + "/scripts/yui/build/assets/skins/images/wait.gif\"/>", 'rowDetailsExpanding');
                var pacsSeriesFinder = new PacsSeriesFinder(oTable.fnGetData(nTr), jq(seriesRow).children().first(), this, rowExpansionHandler, that.currentPacsId);
                pacsSeriesFinder.findSeries();
            }
        };
        jq("#" + sessionSearchResultsTableId).on("click", ".rowDetailsExpander", rowExpansionHandler);
    };

    this.bindProcessButtonHandler = function (sessionSearchResultsTableId) {
        var processButtonHandler = function () {
            var nTr = jq(this).parents('tr')[0];
            var oTable = jq("#" + sessionSearchResultsTableId).dataTable();
            var study = oTable.fnGetData(nTr);
            jq("#" + that.sessionSelectionStudyInstanceUidInputId).val(study.studyInstanceUid);
            jq("#" + that.sessionSelectionPacsIdInputId).val(that.currentPacsId);
            concealContent();
            jq("#" + that.sessionSelectionFormId).submit();
        };
        jq("#" + sessionSearchResultsTableId).on("click", "button.processButton", processButtonHandler);
    };

    this.handleSessionSearchFailure = function (jqXHR) {
        closeModalPanel(this.constants.MODAL_WINDOW_NAME);
        this.restoreFocusedField();
        var errorMsg, errorTitle = 'Could not complete search';

        if (this.constants.CLIENT_ERROR_BAD_REQUEST === jqXHR.status) {
            errorMsg = "Please specify at least one of the search criteria.";
        } else if (this.constants.CLIENT_ERROR_NOT_FOUND === jqXHR.status) {
            errorMsg = "There were no results found that match this search criteria.";
            errorTitle = "Nothing to display";
        } else {
            errorMsg = "Error " + jqXHR.status + ": " + jqXHR.statusText;
        }
        XNAT.dialog.message({ title: errorTitle, content: spawn('p',errorMsg) });
    };

    this.saveFocusedField = function () {
        this.activeElementBeforeSearch = document.activeElement;
    };

    this.restoreFocusedField = function () {
        if (this.activeElementBeforeSearch) {
            this.activeElementBeforeSearch.focus();
        }
    };

    this.dateFormatter = function (oData) {
        if (!oData) {
            return "";
        }
        // we're expecting the date as milliseconds since epoch
        var oDate = new Date(oData);

        return YAHOO.util.Date.format(oDate, {
            format: "%m/%d/%Y"
        });
    };

    this.ageFormatter = function (dob) {
        if (!dob) {
            return "";
        }
        // we're expecting the dob as milliseconds since epoch
        var dCurrent = new Date();
        var dDob = new Date(dob);
        var ageInMilliSeconds = Math.abs(dCurrent - dDob);
        var milliSecondsInAYear = 31556900000;
        return Math.floor(ageInMilliSeconds / milliSecondsInAYear);
    };

    this.getImageURI = function (imageName) {
        return this.imagePath + imageName;
    };
}
