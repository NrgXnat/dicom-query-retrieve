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

    function adminProjectSettingsUrl(proj, bustCache){
        return XNAT.url.restUrl('/xapi/dqr/adminProjectSettings' + (proj ? ('/' + proj) : ''), {}, bustCache)
    }

    // check for a configuration when the script loads
    XNAT.xhr.get({
        url: adminProjectSettingsUrl(projectId, true),
        dataType: 'json',
        success: function(data){
            enableProjectDqr0.checked = (data && data.enabled);
        },
        failure: function(){
            console.warn(arguments);
        },
        always: function(){

        }
    });

    function adminProjectSettingsSubmit(){
        var isChecked = enableProjectDqr0.checked;
        return XNAT.xhr.post({
            url: adminProjectSettingsUrl('', false),
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

    enableProjectDqr$.on('change', function(e){
        adminProjectSettingsSubmit()
    });

    return XNAT.plugin.dqr = dqr;

}));
