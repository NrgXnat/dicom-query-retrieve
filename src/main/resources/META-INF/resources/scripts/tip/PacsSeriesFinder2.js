/*
 * D:/Development/TIP/tip/image-search/src/main/resources/module-resources/scripts/tip/PacsSeriesFinder2.js
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

if (!TIP) {
    var TIP = {};
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
        jq.ajax({
            type: "GET",
            url: serverRoot + "/data/services/pacs/" + pacsId + "/search/studies/" + studyInstanceUid + "/series?XNAT_CSRF=" + csrfToken,
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
                        return '<input type="checkbox" id="pacsSeriesFinderCheckbox' + checkboxId + '" name="selectedSeries" value="' + checkboxId + '" onclick="TIP.selectAllHandler(this)" />';
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
        var selectAll = '<label><input type="checkbox" id="selectAll" onclick="TIP.selectAllHandler()" /> <span>Select All</span></label>';
        jq("#pacsSeriesFinderDivTable").find("thead").find("th").first().append(selectAll);

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
        jq.ajax({
            type: "PUT",
            url: serverRoot + "/data/services/pacs/" + pacsId + "/import/series?XNAT_CSRF=" + csrfToken,
            data: data,
            context: this,
            success: this.showSeriesRequestResults,
            error: this.handleSeriesRequestFailure
        });
    };

    this.showSeriesRequestResults = function () {
        closeModalPanel("requestSeries");

        try {
            xmodal.open({
                title: 'Selected series requested',
                content: 'The selected series have been requested from the PACS system and should be available in the system prearchive shortly. Click <b>OK</b> to go directly to the prearchive or <b>Cancel</b> to return to your search results page.',
                okAction: function (obj) {
                    xmodal.close(obj.$modal);
                    xmodal.loading.open({title:'Please wait...'});
                    window.location = serverRoot + "/app/template/XDATScreen_prearchives.vm";
                },
                cancelAction: function (obj) {
                    xmodal.close(obj.$modal);
                }
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

TIP.selectAllHandler = function(baseElement) {
    var selectAll = $('input#selectAll');
    if(!baseElement) {
        baseElement = 'input[name=selectedSeries]';
        if ($(selectAll).prop('indeterminate') || $(selectAll).is(':checked')) {
            // if none or some checkboxes are selected, select all
            $(baseElement).prop('checked', 'checked');
            $(selectAll)
                .prop('indeterminate', false)
                .next().html('Deselect All');
        } else {
            // otherwise, deselect all
            $(baseElement).prop('checked', false);
            $(selectAll)
                .prop('indeterminate', false)
                .next().html('Select All');
        }
    } else {
        baseElement = 'input[name='+ $(baseElement).prop('name') + ']';

        // place Select All button in a default state.
        $(selectAll)
            .prop('checked', false)
            .prop('indeterminate', true)
            .next().html('Select All');

        // compare the number of checked checkboxes to N number of checkboxes. '0' = an unchecked, determinate state for Select All. 'N' = a fully checked, determinate state for Select All.
        if (document.querySelectorAll(baseElement+':checked').length === 0) {
            $(selectAll)
                .prop('indeterminate', false);
        } else if (document.querySelectorAll(baseElement+':checked').length === document.querySelectorAll(baseElement).length) {
            $(selectAll)
                .prop('indeterminate', false)
                .prop('checked', 'checked')
                .next().html('Deselect All');
        }
    }
};

TIP.presetScanSelector = function(event,keywords,baseElement,source) {
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
    TIP.selectAllHandler(baseElement);
}