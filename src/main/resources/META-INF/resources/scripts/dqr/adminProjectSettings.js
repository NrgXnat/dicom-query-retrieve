/*
 * dicom-query-retrieve: adminProjectSettings.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage DQR settings for a project
 */

var XNAT = getObject(XNAT);

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

    console.log('/scripts/dqr/adminProjectSettings.js');

    var undef, dqr;
    var projectId = XNAT.data.project || XNAT.data.projectId || window.projectId;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.dqr = dqr =
        getObject(XNAT.plugin.dqr || {});

    var enableProjectDqr$ = $('#enable-project-dqr');
    var enableProjectDqr0 = enableProjectDqr$[0];

    function adminProjectSettingsSubmit(){
        var isChecked = enableProjectDqr0.checked;
        return XNAT.xhr.post({
            url: XNAT.url.rootUrl('/xapi/dqr/adminProjectSettings'),
            contentType: 'application/json',
            data: JSON.stringify({
                projectId: projectId,
                enabled: isChecked
            }),
            processData: false,
            success: function(){
                XNAT.ui.banner.top(2000, 'DQR <b>' + (isChecked ? 'enabled' : 'disabled') + '</b> for project "' + projectId + '"', 'success');
            },
            failure: function(){
                console.warn(arguments);
            }
        });
    }

    // check for a configuration when the script loads
    XNAT.xhr.get({
        url: XNAT.url.restUrl('/xapi/dqr/isDqrProject/' + projectId),
        success: function(data){
            enableProjectDqr0.checked = !!data;
        },
        failure: function(){
            console.warn(arguments);
        },
        always: function(){
            // allow modification only for admins
            if (window.isAdminUser) {
                enableProjectDqr$.removeAttr('disabled');
                enableProjectDqr$.removeAttr('readonly');
                enableProjectDqr$.on('change', function(e){
                    adminProjectSettingsSubmit()
                });
            }
        }
    });

    return XNAT.plugin.dqr = dqr;

}));
