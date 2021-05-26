/*
 * dicom-query-retrieve: SendToPacs.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
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


        xmodal.loading.open({title: 'Please wait...'});

        var exportUrl = XNAT.url.restUrl('/xapi/dqr/export', ['pacsId='+pacsId,'session='+sessionId ]);

        scanIds.forEach(function(scan){
            exportUrl += '&scansToExport='+scan;
        });

        XNAT.xhr.put({
            url: exportUrl,
            success: function(data) {
                console.log(data);
                xmodal.loading.close();
                XNAT.dialog.message({
                    title: sessionId + ' Sent',
                    content: 'The request to store your session to the requested PACS has been sent.',
                    okAction: function(){
                        window.location.assign(XNAT.url.rootUrl('/data/experiments/'+sessionId+'?format=html'));
                    }
                });
            },
            error: function(e) {
                xmodal.loading.close();
                xmodal.message('Error', 'An unexpected error has occurred while processing ' + sessionId + '. Please contact your administrator. Status code: ' + e.status, 'OK');
            }
        });

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
        var scansToSubmit = [],
            pacsId = $('#pacsId').find('option:selected').val(),
            sessionId = $('#session').val();

        if (!pacsId) {
            XNAT.dialog.message('Error','Please select a valid DICOM AE destination.');
            return false;
        }

        $('#editPacsForm').find('input[name=scansToExport]').not(':disabled').each(function(){
            if ($(this).prop('checked')) {
                scansToSubmit.push($(this).val());
            }
        });

        if (scansToSubmit.length > 0) {
            exportScans.SendToPacs(pacsId,sessionId,scansToSubmit);
        }
        else {
            XNAT.dialog.message('Error', 'Please select a valid scan to send.');
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
                var dataObject = x2js.xml2json(xmlData);
                var sessionJson = dataObject[Object.keys(dataObject)[0]];

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
//# sourceURL=browsertools://scripts/xnat/plugin/dqr/SendToPacs.js