/*
 * dicom-query-retrieve: irbSettings.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

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

    console.log('irbSettings.js');

    var XNAT =
            getObject(window.XNAT || {});

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.dqr =
        getObject(XNAT.plugin.dqr || {});

    XNAT.plugin.dqr.irbSettings = irbSettings =
        getObject(XNAT.plugin.dqr.irbSettings);

    var irbSettings;
    var projectId = XNAT.data.projectId;

    var baseUrl = '/xapi/dqr/projectSettings/' + projectId;

    var irbUrl = baseUrl + '/irbNumber';

    // set the value asap
    XNAT.xhr.get({
        url: XNAT.url.restUrl(irbUrl),
        dataType: 'text',
        success: function(irbNum){

            // set the value of the input as soon as it's available
            waitForElement(1, '#irb-number', function($el){

                var el = this;

                el.value = irbNum;

                // handle saving the number
                $('#save-irb-number').on('click', function(e){

                    e.preventDefault();

                    var saveNumber = XNAT.xhr.put({
                        url: XNAT.url.rootUrl(irbUrl + '?irbNumber=' + encodeURIComponent(el.value)),
                        contentType: false,
                        processData: false
                    });

                    saveNumber.done(function(){
                        XNAT.ui.banner.top(2000, 'IRB Number Saved', 'success');
                    });

                    saveNumber.fail(function(){
                        XNAT.ui.banner.top(2000, 'An error occurred. IRB number not saved.', 'error');
                        console.error(arguments);
                    });

                });

            });

        },
        failure: function(){
            console.error(arguments);
        }
    });


    XNAT.xhr.get({
        url: XNAT.url.restUrl(baseUrl + '/irbFilename'),
        dataType: 'text',
        success: function(filename){

            // if there's a saved IRB file, show the link with file name
            if (filename) {

                waitForElement(1, '#irb-file-link', function($el){

                    var link = this;

                    link.textContent = filename;
                    link.href        = XNAT.url.rootUrl(baseUrl + '/irbFile/' + filename);
                    link.classList.add('link');

                    var file  = document.getElementById('irb-file-download');
                    file.classList.remove('hidden');

                    var warning = document.getElementById('irb-file-warning');
                    warning.classList.remove('hidden')

                });

            }

        },
        failure: function(){
            console.error(arguments);
        }
    });

    return XNAT.plugin.dqr.irbSettings = irbSettings;


}));