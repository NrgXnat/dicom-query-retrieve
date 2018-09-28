/*
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

console.log('SendToPacs.js');

var XNAT = getObject(XNAT || {});
XNAT.app = getObject(XNAT.app || {});
XNAT.app.dqr = getObject(XNAT.app.dqr || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function(){

    var exportScans;

    XNAT.app.dqr.exportScans = exportScans = {};

    exportScans.SendToPacs = function (pacsId, sessionId, scanIds) {

        try {
            xmodal.open({
                title: 'Send Processed Data To PACS',
                content: 'This operation sends all processed data back to the selected PACS. Click <b>OK</b> to start the update operation or <b>Cancel</b> if you want to wait.',
                okAction: this.sendToPacsOk,
                cancelAction: this.sendToPacsCancel
            });
        } catch (e) {
            xmodal.message('Error', "<p>Couldn't start the PACS send operation. Error message:</p><blockquote>" + e.toString() + "</blockquote>", 'OK');
        }

        this.sendToPacsOk = function(obj) {

            xmodal.close(obj.$modal);
            xmodal.loading.open({title: 'Please wait...'});

            for (var index = 0; index < scanIds.length; index++) {
                var scanId = scanIds[index];
                XNAT.xhr.ajax({
                    type: "POST",
                    url: XNAT.url.csrfUrl("/data/services/pacs/" + pacsId + "/export/experiments/" + sessionId + "/scans/" + scanId),
                    dataType: "json",
                    success: this.exportSuccess,
                    error: this.exportFailure
                });
            }
        };

        this.sendToPacsCancel = function(obj) {
            xmodal.close(obj.$modal);
        };

        this.exportSuccess = function() {
            xmodal.loading.close();
            xmodal.message(sessionId + ' Sent', 'The request to store your session to the requested PACS has been sent.', 'OK');
        };

        this.exportFailure = function(results) {
            xmodal.loading.close();
            xmodal.message('Error', 'An unexpected error has occurred while processing ' + sessionId + '. Please contact your administrator. Status code: ' + results.status, 'OK');
        };
    };

    // Populate Scan Table
    function hasDicomResource(scan){
        // only return true if the scan has DICOM-formatted file resources, regardless of how they are labeled
        if (!scan.file) return false;
        if (isArray(scan.file)) {
            var dicomFound = false;
            scan.file.forEach(function(file){
                if (file['_format'] === 'DICOM') dicomFound = true;
            });
            return dicomFound;
        }
        else return (scan.file['_format'] === 'DICOM');
    }

    function scanCheckbox(scan){
        return spawn('input.selectable-select-one', {
            type: 'checkbox',
            name: 'scansToExport',
            value: scan['_ID'],
            id: 'scan-'+scan['_ID']
        })
    }

    var spawnScanTable = function(scans,sessionTime){
        var scanTable = XNAT.table({
            id: 'scansToExport',
            addClass: 'xnat-table condensed selectable',
            style: { width: '100%' }
        });
        scanTable.thead().tr()
            .th({style: { 'width': '40px' }}, '<input type="checkbox" class="selectable-select-all" id="select-all" title="Select / Deselect All" />')
            .th({addClass: 'left' }, '<b>Series</b>')
            .th('<b>Description</b>')
            .th('<b>Sequence</b>')
            .th('<b>Scan Date</b>');

        var tbody = scanTable.tbody();

        function showSeriesDescription(scan){
            return (scan.series_description) ? scan.series_description.toString() : 'unknown'
        }
        function showScanType(scan){
            if (scan.modality === 'MR' && scan.parameters.scanSequence) {
                return scan.parameters.scanSequence
            }
            else return scan['_type'];
        }
        function showScanDate(scan){
            return (scan.start_date) ? scan.start_date.toString() : 'unknown';
        }

        scans.forEach(function(scan){
            // keep track of which scans are original to the session
            // if (isOriginalScan(scan,sessionTime) && exportScans.originalScans.indexOf(scan['_ID']) < 0) exportScans.originalScans.push(scan['_ID']);

            if (hasDicomResource(scan)) {
                tbody.tr({
                    // addClass: (isOriginalScan(scan,sessionTime)) ? 'original' : '',
                    data: { 'scan-id': scan['_ID'] }
                })
                    .td({ style: { 'width': '40px' }},[ scanCheckbox(scan) ])
                    .td({ addClass: 'scan-id max120' }, scan['_ID'])
                    .td({ addClass: 'max200' },[ showSeriesDescription(scan) ])
                    .td({ addClass: 'max200' },[ showScanType(scan) ])
                    .td([ showScanDate(scan) ] )
            }
            else {
                tbody.tr({
                    addClass: 'disabled',
                    data: { 'scan-id': scan['_ID'] }
                })
                    .td()
                    .td({ addClass: 'scan-id max120' }, scan['_ID'])
                    .td({ colSpan: '3' }, 'Error: No DICOM Resource found. Cannot send scan.')
            }
        });

        return scanTable.table;
    };


    /* User event handlers */

    $('#submitScansToPacs').on('click',function(e){
        e.preventDefault();
        var scansToSubmit = [];

        $('#editPacsForm').find('input[name=scansToExport]').not(':disabled').each(function(){
            if ($(this).prop('checked')) {
                scansToSubmit.push($(this).val());
            }
        });

        if (scansToSubmit.length > 0) {
            $('#editPacsForm').submit();
        }
        else {
            xmodal.message('Error', "<p>Please select a valid scan to send.</p>", 'OK');
        }

    });

    /* Page Init */

    exportScans.init = exportScans.refresh = function(sessionId){
        sessionId = sessionId || XNAT.data.context.ID;
        var $tableContainer = $('#pacsExportScanSelectorContainer').find('.data-table-container');
        $tableContainer.empty();

        // get the XML description of the image session and process it as JSON
        // requires core JS library /lib/x2js/xml2json.js
        var x2js = new X2JS();

        XNAT.xhr.get({
            url: XNAT.url.rootUrl('/data/experiments/'+sessionId+'?format=xml'),
            fail: function(e){
                console.log('Could not load session data for '+sessionId, e);
            },
            success: function(xmlData){
                var sessionJson = x2js.xml2json(xmlData).MRSession;
                var scans = sessionJson.scans.scan;

                if (isArray(scans) && scans.length) {
                    $tableContainer.append(spawnScanTable(scans));
                }
                else if (isObject(scans) && Object.keys(scans).length) {
                    var scanArray = []; scanArray.push(scans);
                    $tableContainer.append(spawnScanTable(scanArray))
                }
                else {
                    $tableContainer.append(spawn('p', 'No scans found to export.'));
                }
            }
        })

    };
    // exportScans.init();

}));