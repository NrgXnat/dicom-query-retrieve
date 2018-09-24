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

        var scans = data.ResultSet.Result;
        scans = scans.sort(function(a,b){ return (parseInt(a.seriesNumber) > parseInt(b.seriesNumber)) ? 1 : -1 });

        var scanTable = XNAT.table({
            className: 'xnat-table selectable compact',
            id: seriesSearchResultsTableId,
            style: { border: '1px solid #ccc', width: '100%' }
        });

        scanTable.thead().tr()
            .th({
                addClass: 'toggle-all left',
                style: { width: '40px' },
                html: '<input type="checkbox" class="selectable-select-all" id="toggle-all-scans" title="Toggle All Scans" />'
            })
            .th({ addClass: 'left'}, '<b>Series</b>')
            .th({ addClass: 'left'}, '<b>Series Description</b>')
            .th({ addClass: 'left'}, '<b>Modality</b>');

        var tbody = scanTable.tbody();

        scans.forEach(function(scan){
            var checkboxId = scan.seriesInstanceUid.replace(/\./g, "_");
            tbody.tr()
                .td([ spawn('input.selectable-select-one', {type: 'checkbox', name: 'selectedSeries', id: 'pacsSeriesFinderCheckbox' + checkboxId, value: checkboxId })])
                .td({ addClass: 'max120' }, scan.seriesNumber.toString())
                .td(scan.seriesDescription)
                .td(scan.modality)
        });

        $('#'+seriesSearchResultsDivId).empty().append(scanTable.table);

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

    this.requestSeries = function (project, selectedSeries, ae) {
        that.project = project;
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
        if(ae) {
            data += "&AE=" + ae;
        }
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
                content: 'The selected series have been requested from the PACS and should be available in the system prearchive shortly. Contact your PACS administrator if your requested series are not imported in a timely manner.',
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
                            window.location = serverRoot + "/app/template/PacsSessionFinder.vm/project/"+that.project;
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
};