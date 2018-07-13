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
    if (typeof jq === 'undefined') var jq = jQuery;

    var originalScans = [];

    jq('#submitScansToPacs').on('click',function(e){
        e.preventDefault();
        var scansToSubmit = [];

        jq('#editPacsForm').find('input[name=scansToExport]').not(':disabled').each(function(){
            if (jq(this).prop('checked')) {
                scansToSubmit.push(jq(this).val());
            }
        });

        if (scansToSubmit.length > 0) {
            jq('#editPacsForm').submit();
        }
        else {
            xmodal.message('Error', "<p>Please select a valid scan to send.</p>", 'OK');
        }

    });

    function getSessionId() {
        var urlParams = window.location.search.substring(1).split('&'),
            params = {};
        urlParams.forEach(function(urlParam){
            var keyPair = urlParam.split('=');
            params[keyPair[0]] = keyPair[1];
        });

        return (params.sessionid) ? params.sessionid : false;
    }

    // A scan's "original" status is populated in the Velocity construction of the scan table by comparing scan attributes to session attributes
    var findOriginalScans = function() {

        $('table#scansToExport').find('tbody').find('tr').each(function(){
            if ($(this).hasClass("original")) {
                var scanId = $(this).find('.scan-id').html();
                originalScans.push(scanId);
            } else {
                canSendScans = true;
            }
        });
    };
    findOriginalScans();

    XNAT.app.disableOriginalScans = function(){

        // iterate over the list of original scans and disable their checkbox
        originalScans.forEach(function(scanId){
            $('input#scan-'+scanId)
                .prop('disabled','disabled')
                .addClass('hidden')
                .parents('tr').addClass('disabled');
        });
        setSelectAll();
        $('#scan-exclusion-warning').removeClass('hidden');

        var allScansLength = $('table#scansToExport').find('tbody').find('tr').length;
        var canSendScans = (allScansLength > originalScans.length);

        // don't allow user to submit form if no scans can be sent
        if (!canSendScans) {
            $('#submitScansToPacs').prop('disabled', 'disabled');
            XNAT.ui.dialog.message({ title: false, content: 'This data only contains scans that were a part of the original image session. To send any scans to PACS, enable original scans to be sent.' });
        }
    };

    XNAT.app.enableOriginalScans = function(){
        // iterate over the list of original scans and enable their checkbox
        originalScans.forEach(function(scanId){
            $('input#scan-'+scanId)
                .prop('disabled',false)
                .removeClass('hidden')
                .parents('tr').removeClass('disabled');
        });
        setSelectAll();
        $('#scan-exclusion-warning').addClass('hidden');
        $('#submitScansToPacs').prop('disabled', false);
    };

    // Select-all Behavior
    $('#selectAll').on('click', function(){
        if ($(this).prop('indeterminate') || $(this).is(':checked')) {
            // if none or some checkboxes are selected, select all
            $('input[name=scansToExport]').prop('checked','checked');
            $(this).prop('indeterminate',false)
        }
        else {
            // otherwise, deselect all
            $('input[name=scansToExport]').prop('checked',false);
            $(this).prop('indeterminate',false)
        }
    });

    // Set Select-all status based on external event
    $('input[name=scansToExport]').on('click',function(){
        // place Select All button in a default state.
        setSelectAll();
    });

    // original scan toggle
    $('#dqrPushSetting').on('click',function(){
        if ($(this).prop('checked')) {
            XNAT.app.enableOriginalScans();
        }
        else {
            XNAT.app.disableOriginalScans();
        }
    });

    function setSelectAll() {
        $('#selectAll')
            .prop('checked',false)
            .prop('indeterminate',true)

        // compare the number of checked checkboxes to N number of checkboxes. '0' = an unchecked, determinate state for Select All. 'N' = a fully checked, determinate state for Select All.
        if (document.querySelectorAll('input[name=scansToExport]:checked').length === 0) {
            $('#selectAll')
                .prop('indeterminate',false);
        } else if (document.querySelectorAll('input[name=scansToExport]:checked').length === document.querySelectorAll('input[name=scansToExport]').length) {
            $('#selectAll')
                .prop('indeterminate',false)
                .prop('checked','checked')
        }
    }

    XNAT.app.SendToPacs = function (pacsId, sessionId, scanIds) {

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
}));