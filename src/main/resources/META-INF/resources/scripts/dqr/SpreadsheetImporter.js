console.log('SpreadsheetImporter.js');

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

}(function() {

    /* ================ *
     * GLOBAL FUNCTIONS *
     * ================ */

    function spacer(width) {
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function errorHandler(e, title, closeAll) {
        console.log(e);
        title = (title) ? 'Error Found: ' + title : 'Error' + e.status;
        closeAll = (closeAll === undefined) ? true : closeAll;
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': ' + e.statusText + '</strong></p><p>' + e.responseText + '</p>' : e;
        XNAT.dialog.open({
            width: 450,
            title: title,
            content: errormsg,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true,
                    action: function () {
                        if (closeAll) {
                            xmodal.closeAll();

                        }
                    }
                }
            ]
        });
    }

    function queryErrorHandler(e){
        if (e.status === 400){
            e.responseText = 'Please upload a valid CSV file.';
            return errorHandler(e, 'Incorrect File Format');
        }
        if (e.status === 404){
            e.responseText = 'Please specify a valid PACS ID to query.';
            return errorHandler(e, 'Invalid PACS ID');
        }
        if (e.status === 500){
            if (e.responseText.indexOf('SocketTimeoutException') >= 0) {
                e.responseText = 'Timeout error. Could not communicate with PACS. Please ensure PACS is running.';
                return errorHandler(e, 'Timeout Error');
            }

            if (e.responseText.indexOf('csv_to_store') >= 0) {
                e.responseText = 'No file uploaded. Please upload a valid CSV file.';
                return errorHandler(e, 'No CSV File');
            }

            if (e.responseText.indexOf('criteria') >= 0) {
                e.responseText = 'Invalid search criteria. Each query in your CSV file must include at least one search criteria.';
                return errorHandler(e, 'Invalid CSV Data')
            }
        }
        // if no matches were identified, handle the error generically
        return errorHandler(e);
    }

    function dateFormatter(timestamp){
        return (new Date(timestamp)).toLocaleDateString();
    }

    /* ============ */

    var csvimporter;

    XNAT.app.dqr = getObject(XNAT.app.dqr || {});

    XNAT.app.dqr.csvimporter = csvimporter =
        getObject(XNAT.app.dqr.csvimporter || {});

    csvimporter.queryParams = {};

    csvimporter.importJsonUrl = function(force){
        force = force || false;

        // required: pacsId, ae, project
        // optional: importEvenIfCustomProcessingIsOff
        var queryParams = [];
        Object.keys(csvimporter.queryParams).forEach(function(key){
            queryParams.push(key+'='+csvimporter.queryParams[key])
        });

        if (force) queryParams.push('importEvenIfCustomProcessingIsOff=true');

        return XNAT.url.csrfUrl('/xapi/dqr/csvimport/importFromJson?'+queryParams.join('&'));
    };

    csvimporter.displayQueryResults = function(data){
        // receive data as JSON blob, with an array of query results as the outer layer
        // present the results as an indexed selectable list that allows users to confirm data to import
        var content = [];

        data.forEach(function(result){
            if (result.criteria) {
                // remove static metadata about the search and focus on the actual search criteria
                delete result.criteria['atLeastOneKeyCriterionSpecified'];
                delete result.criteria['firstNamePartial'];
                delete result.criteria['firstNamePresent'];
                delete result.criteria['lastNamePartial'];

                // build an array of formatted keys and values and add that to the content
                var criteria = [];
                Object.keys(result.criteria).forEach(function(c){
                    if (c === "studyDateRange") {
                        criteria.push(c + ': "' + dateFormatter(result.criteria[c]['start']) + '&ndash;'+ dateFormatter(result.criteria[c]['end']) + '"');
                    }
                    else {
                        criteria.push(c + ': "' + result.criteria[c] + '"');
                    }
                });

                content.push(spawn('p.criteria',{ style: {'font-weight': 'bold'}}, 'Search Criteria: '+criteria.join(', ') ));

                if (result.studies.length) {
                    var listItems = [];

                    result.studies.forEach(function(study){
                        var studyDate = dateFormatter(study.studyDate);

                        listItems.push(spawn('li',[
                            spawn('label', [
                                spawn('input|type=checkbox|checked=checked',{addClass: 'sessionSelector', data: { id: study.studyId }}),
                                spawn('span.label', studyDate + ': '+ study.studyId + " (" + study.studyDescription + ")")
                            ]),
                            spawn('span.hidden', { addClass: 'sessionData id-'+study.studyId }, JSON.stringify(study))
                        ]));
                    });
                    content.push(spawn('ul.resultList', listItems ))
                }
            }
        });

        XNAT.ui.dialog.open({
            title: 'Select Sessions To Import',
            width: 600,
            content: spawn('form', content),
            buttons: [
                {
                    label: 'Import Selected Sessions',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        var $selected = $form.find('.sessionSelector:checked');
                        if ($selected.length) {
                            csvimporter.submitQuery($selected);
                        }
                        else {
                            XNAT.ui.dialog.message('No sessions selected. Nothing to import.');
                        }
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        });
    };

    csvimporter.submitQuery = function($sessions){
        var dataToImport = [];
        $sessions.each(function(){
            var studyId = $(this).data('id');
            var query = $('span.id-'+studyId).html();
            dataToImport.push(JSON.parse(query))
        });

        if (dataToImport.length) {
            XNAT.xhr.ajax({
                url: importJsonUrl(),
                method: 'POST',
                data: dataToImport,
                contentType: 'application/json',
                fail: function(e){
                    queryErrorHandler(e)
                },
                success: function(data){
                    XNAT.ui.dialog.message({ title: 'success', content: JSON.stringify(data) });
                }
            })
        }
        else {
            XNAT.ui.dialog.message('No session data found. Nothing to import.');
        }
    };

    csvimporter.noQueryResults = function(){
        XNAT.ui.dialog.message({
            title: "Error",
            content: spawn('p','No results were found on your selected PACS that matched your query.')
        })
    };

    $(document).ready(function () {
        $(document).off('submit', 'form#pacsSeriesFinderForm');
        $(document).on('submit', 'form#pacsSeriesFinderForm', function (e) {
            e.preventDefault();
            var formData = new FormData(this);

            // validate form before proceeding

            // capture values from selects to be used in second query
            // formData appears as an empty object, so each value must be retrieved individually. 
            $(this).find('select').each(function(){
                var key = $(this).prop('name');
                csvimporter.queryParams[key] = $(this).val()
            });

            XNAT.xhr.ajax({
                url: XNAT.url.csrfUrl('/xapi/dqr/csvimport/uploadCsv'),
                method: 'POST',
                data: formData,
                cache: false,
                contentType: false,
                processData: false,
                fail: function (e) {
                    queryErrorHandler(e);
                },
                success: function (data) {
                    if (data.length) {
                        csvimporter.displayQueryResults(data);
                    }
                    else csvimporter.noQueryResults();
                }
            });
        });
    });

}));
