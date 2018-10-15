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

    // set the value of the input as soon as it's available
    waitForElement(1, '#irb-number', function($el){

        var el = this;

        // set the value asap
        XNAT.xhr.get({
            url: XNAT.url.restUrl(irbUrl),
            dataType: 'text',
            success: function(irbNum){
                el.value = irbNum;
            },
            failure: function(){
                console.error(arguments);
            }
        });

        // handle saving the number
        $('#save-irb-number').on('click', function(e){

            e.preventDefault();

            var saveNumber = XNAT.xhr.put(XNAT.url.restUrl(irbUrl, { irbNumber: el.value }, false));

            saveNumber.done(function(){
                XNAT.ui.banner.top(2000, 'IRB Number Saved', 'success');
            });

            saveNumber.fail(function(){
                console.error(arguments);
            });

        });

    });

    // if there's a saved IRB file, show the link with file name
    waitForElement(1, '#irb-file-link', function($el){

        var link = this;
        var div = document.getElementById('irb-file-download');

        // set the value asap
        XNAT.xhr.get({
            url: XNAT.url.restUrl(baseUrl + '/irbFilename'),
            dataType: 'text',
            success: function(filename){
                if (filename) {
                    link.textContent = filename;
                    link.href = XNAT.url.rootUrl(baseUrl + '/irbFile');
                    link.classList.add('link');
                    div.classList.remove('hidden');
                }
            },
            failure: function(){
                console.error(arguments);
            }
        });

    });

    return XNAT.plugin.dqr.irbSettings = irbSettings;


}));