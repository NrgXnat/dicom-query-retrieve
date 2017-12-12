/*
 * D:/Development/DQR/dqr/src/main/resources/module-resources/scripts/dqr/PacsSeriesFinder2.js
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

if (!DQR) {
    var DQR = {};
}

function PacsSeriesFinder2(studyInstanceUid, seriesSearchResultsDivId, seriesSearchResultsFormId, seriesSearchResultsSubmitButtonId, seriesSearchResultsCheckAllButtonId, seriesSearchResultsValidationErrorMessageHolderId, pacsId) {
    "use strict";

    var that = this;

    this.constants = {
        "MODAL_WINDOW_NAME": "loadData",
        "CLIENT_ERROR_NOT_FOUND": 404,
        "SERIES_NUMBER_COLUMN": 2
    };

    this.findSeries = function () {
        XNAT.xhr.ajax({
            type: "GET",
            url: XNAT.url.csrfUrl("/data/services/pacs/" + pacsId + "/search/studies/" + studyInstanceUid + "/series"),
            dataType: "json",
            context: this,
            success: this.showSeriesSearchResults,
            error: this.handleSeriesSearchFailure
        });

        openModalPanel(this.constants.MODAL_WINDOW_NAME, "Loading data...");
    };

    this.showSeriesSearchResults = function (data) {
        var seriesSearchResultsTableId = seriesSearchResultsDivId + "Table";

        jq("#" + seriesSearchResultsDivId).empty().html('<table cellpadding="0" cellspacing="0" border="0" id="' + seriesSearchResultsTableId + '"/>');

        var dataTableOptions = {
            "aaData": data.ResultSet.Result,
            "aoColumns": [
                {
                    "bSearchable": false,
                    "bSortable": false,
                    "mData": function (source) {
                        var checkboxId = source.seriesInstanceUid.replace(/\./g, "_");
                        return '<input type="checkbox" id="pacsSeriesFinderCheckbox' + checkboxId + '" name="selectedSeries" value="' + checkboxId + '" onclick="DQR.selectAllHandler(this)" />';
                    }
                },
                {
                    "mData": "seriesDescription",
                    "sTitle": "Description"
                },
                {
                    "mData": "seriesNumber",
                    "sTitle": "Series"
                },
                {
                    "mData": "modality",
                    "sTitle": "Modality"
                }

            ],
            "oLanguage": {
                "sInfoPostFix": ""
            },
            "aaSorting": [
                [that.constants.SERIES_NUMBER_COLUMN, "asc"]
            ],
            "bFilter": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bInfo": false
        };

        jq("#" + seriesSearchResultsTableId).dataTable(dataTableOptions);

        jq("#" + seriesSearchResultsFormId).validate({
            rules: {
                "selectedSeries": {
                    required: true,
                    minlength: 1
                }
            },
            messages: {
                "selectedSeries": "Please select at least one series."
            },
            submitHandler: function (form) {
                concealContent();
                form.submit();
            },
            errorPlacement: function (error, element) {
                jq("#" + seriesSearchResultsValidationErrorMessageHolderId).append(error);
            }
        });

        jq("#" + seriesSearchResultsCheckAllButtonId).click(function () {
            var allCheckboxesAlreadyChecked = (jq("#" + seriesSearchResultsFormId + " input[type=checkbox]:checked").length === jq("#" + seriesSearchResultsFormId + " input[type=checkbox]").length);
            jq("#" + seriesSearchResultsFormId + " input[type=checkbox]").prop("checked", !allCheckboxesAlreadyChecked);
            jq("#" + seriesSearchResultsFormId).valid();
        });

        jq("#" + seriesSearchResultsSubmitButtonId).removeAttr("disabled");
        jq("#" + seriesSearchResultsCheckAllButtonId).removeAttr("disabled");

        // after table has rendered add "Select All" checkbox
        var selectAll = '<input type="checkbox" id="selectAll" onclick="DQR.selectAllHandler()" />';
        jq("#pacsSeriesFinderDivTable").find("thead").find("th").first().addClass("left").append(selectAll);

        closeModalPanel(this.constants.MODAL_WINDOW_NAME);
    };

    this.handleSeriesSearchFailure = function (jqXHR) {
        if (this.constants.CLIENT_ERROR_NOT_FOUND === jqXHR.status) {
            jq("#" + seriesSearchResultsDivId).text("There were no series found for this " + XNAT.app.displayNames.singular.imageSession.toLowerCase() + ".");
        } else {
            jq("#" + seriesSearchResultsDivId).text("Error " + jqXHR.status + ": " + jqXHR.responseText);
        }

        closeModalPanel(this.constants.MODAL_WINDOW_NAME);
    };

    this.requestSeries = function (project, selectedSeries) {
        openModalPanel("requestSeries", "Requesting " + selectedSeries.length + " selected series");
        var data = "SERIES_IDS=";
        for (var index = 0; index < selectedSeries.length; index++) {
            if (index > 0) {
                data += ",";
            }
            data += selectedSeries[index];
        }
        data += "&STUDY_ID=" + studyInstanceUid;
        data += "&PROJECT=" + project;
        XNAT.xhr.ajax({
            type: "PUT",
            url: XNAT.url.csrfUrl("/data/services/pacs/" + pacsId + "/import/series"),
            data: data,
            context: this,
            success: this.showSeriesRequestResults,
            error: this.handleSeriesRequestFailure
        });
    };

    this.showSeriesRequestResults = function () {
        closeModalPanel("requestSeries");

        try {
            XNAT.ui.dialog.open({
                title: 'Selected series requested',
                width: 540,
                content: 'The selected series have been requested from the PACS system and should be available in the system prearchive shortly. Contact your PACS administrator if your requested series are not imported in a timely manner.',
                buttons: [
                    {
                        label: 'Go to the Prearchive',
                        isDefault: true,
                        close: true,
                        action: function (obj) {
                            xmodal.loading.open({title: 'Please wait...'});
                            window.location = serverRoot + "/app/template/XDATScreen_prearchives.vm";
                        }
                    },
                    {
                        label: 'Return to DICOM Import Screen',
                        isDefault: false,
                        close: true,
                        action: function(obj){
                            xmodal.loading.open({title:'Please wait...'});
                            window.location = serverRoot + "/app/template/PacsSessionFinder.vm";
                        }
                    }
                ]
            });
        } catch (e) {
            alert(e.toString());
        }
    };

    this.handleSeriesRequestFailure = function (results) {
        closeModalPanel("requestSeries");
        var message = "";
        if (results.message) {
            message = results.message;
        } else if (results.responseText) {
            var html = jq.parseHTML(results.responseText);
            message = "<p>The following response was received from the server:</p><span style='white-space: nowrap; font-size: smaller;'>" + jq(html).next('h3').html().trim().replace(/(?:\r\n|\r|\n)/g, '<br/>\n') + "</span>";        }
        if (!message) {
            message = "Returned HTTP status code: [" + results.status + "] " + results.statusText;
        }
        xmodal.message("Error occurred requesting selected series", message);
    };
}

DQR.selectAllHandler = function(baseElement) {
    var selectAll = $('input#selectAll');
    if(!baseElement) {
        baseElement = 'input[name=selectedSeries]';
        if ($(selectAll).prop('indeterminate') || $(selectAll).is(':checked')) {
            // if none or some checkboxes are selected, select all
            $(baseElement).prop('checked', 'checked');
            $(selectAll)
                .prop('indeterminate', false);
        } else {
            // otherwise, deselect all
            $(baseElement).prop('checked', false);
            $(selectAll)
                .prop('indeterminate', false);
        }
    } else {
        baseElement = 'input[name='+ $(baseElement).prop('name') + ']';

        // place Select All button in a default state.
        $(selectAll)
            .prop('checked', false)
            .prop('indeterminate', true);

        // compare the number of checked checkboxes to N number of checkboxes. '0' = an unchecked, determinate state for Select All. 'N' = a fully checked, determinate state for Select All.
        if (document.querySelectorAll(baseElement+':checked').length === 0) {
            $(selectAll)
                .prop('indeterminate', false);
        } else if (document.querySelectorAll(baseElement+':checked').length === document.querySelectorAll(baseElement).length) {
            $(selectAll)
                .prop('indeterminate', false)
                .prop('checked', 'checked');
        }
    }
};

DQR.presetScanSelector = function(event,keywords,baseElement,source) {
    event.preventDefault();
    var clicked = event.target;
    if (!keywords && !$(clicked).data('keywords')) {
        xmodal.alert("Error: no keywords defined for this filter.");
    } else {
        keywords = keywords || $(clicked).data('keywords').split(',');
    }

    baseElement = baseElement || 'input[name=selectedSeries]';

    source = source || $('#layout_content').find('form').first();

    // deselect all checkboxes
    $(baseElement).prop("checked",false);

    // find matching series descriptions in a datatable and check corresponding checkboxes.
    var matchesFound = 0;
    for (i=0, j=keywords.length; i<j; i++) {
        var keyword = keywords[i].toLowerCase();
        $(source).find('tr:containsNC('+keyword+')').each(function(){
            $(this).find('input[type=checkbox]').prop('checked','checked');
            matchesFound++;
        });
    }

    if (matchesFound === 0) xmodal.alert("No scans were found matching keywords ["+keywords.join(', ')+"]");

    // reset status of "Select All" button
    DQR.selectAllHandler(baseElement);
}