$(function(){

    var pageBody = $('#page-body');

    function showConfigErrorMessage(){
        $('.datepickers-container').remove();
        pageBody.removeClass('hidden').find('> .pad').empty().html('' +
            '<div class="error">' +
            'This project is not configured for PACS DICOM import. ' +
            'Please contact your system administrator if you need DICOM PACS import enabled.' +
            '</div>' +
            '');
    }

    window.projectId = window.projectId || getQueryStringValue('project');

    XNAT.xhr.get({
        url: XNAT.url.rootUrl('/xapi/dqr/adminProjectSettings/' + window.projectId),
        success: function(data){
            if (data && data.enabled) {
                pageBody.removeClass('hidden').show();
            }
            else {
                showConfigErrorMessage();
            }
        },
        failure: function(){
            console.warn(arguments);
            showConfigErrorMessage();
        }
    });

});
