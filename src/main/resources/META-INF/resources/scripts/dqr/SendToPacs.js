/*
 * D:/Development/DQR/dqr/image-push/src/main/resources/module-resources/scripts/dqr/SendToPacs.js
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
        xmodal.message(sessionId + ' Sent', 'The request to store your session to the requested PACS system has been sent.', 'OK');
    };

    this.exportFailure = function(results) {
        xmodal.loading.close();
        xmodal.message('Error', 'An unexpected error has occurred while processing ' + sessionId + '. Please contact your administrator. Status code: ' + results.status, 'OK');
    };
};