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

    var undef, dqr;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.dqr = dqr =
        getObject(XNAT.plugin.dqr || {});

    function adminProjectSettings(){}

    adminProjectSettings.submit = function(e){
        e.preventDefault();
        console.log('adminProjectSettings.submit');
        var $form = $(this);
        var URL = XNAT.url.rootUrl('/xapi/dqr/adminProjectSettings');
    };

    dqr.adminProjectSettings = adminProjectSettings;

    return XNAT.plugin.dqr = dqr;

}));
