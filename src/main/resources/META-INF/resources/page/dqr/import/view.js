$(function(){

    var pageBody = $('#page-body');

    function showConfigErrorMessage(msg){
        $('.datepickers-container').remove();
        pageBody.removeClass('hidden').find('> .pad').empty().html(msg);
    }

    window.projectId = window.projectId || getQueryStringValue('project');

    XNAT.xhr.get({
        url: XNAT.url.rootUrl('/xapi/dqr/isDqrProject/' + window.projectId),
        success: function(data){
            if (/^true$/i.test(data)) {
                pageBody.removeClass('hidden').show();
            }
            else {
                showConfigErrorMessage('' +
                    '<div class="error">' +
                    'This project is not configured for PACS DICOM import. ' +
                    'Please contact your system administrator if you need PACS DICOM import enabled.' +
                    '</div>');
            }
        },
        failure: function(){
            console.warn(arguments);
            showConfigErrorMessage('' +
                '<div class="error">' +
                'Access denied. Redirecting...' +
                '</div>');
            window.setTimeout(function(){
                window.location.href = XNAT.url.rootUrl()
            }, 2000)
        }
    });

});
