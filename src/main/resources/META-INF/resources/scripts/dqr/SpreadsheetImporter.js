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

    var csvimporter, undefined;

    XNAT.app.dqr = getObject(XNAT.app.dqr || {});

    XNAT.app.dqr.csvimporter = csvimporter =
        getObject(XNAT.app.dqr.csvimporter || {});

    csvimporter.queryParams = {};
    csvimporter.scpReceivers = {};
    csvimporter.installedProcessors = {};

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
        // present the results as an selectable table organized by query that allows users to confirm data to import


        // initialize the table - we'll add to it below
        var resultsTable = XNAT.table({
            className: 'xnat-table selectable alt1',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        resultsTable.thead().tr()
            .th({
                addClass: 'toggle-all',
                style: { width: '45px' },
                html: '<input type="checkbox" class="selectable-select-all" id="toggle-all-scans" title="Toggle All Scans" />'
            })
            .th('<b>Patient Name</b>')
            .th('<b>Session Date</b>')
            .th('<b>Accession Num</b>')
            .th('<b>Study ID</b>')
            .th('<b>Study Description</b>');


        data.forEach(function(result){
            if (result.criteria) {
                // remove static metadata about the search and focus on the actual search criteria
                var criteria = result.criteria;
                delete criteria['atLeastOneKeyCriterionSpecified'];
                delete criteria['firstNamePartial'];
                delete criteria['firstNamePresent'];
                delete criteria['lastNamePartial'];

                // build an array of formatted keys and values and add that to the content
                var criteriaLabel = [];
                Object.keys(criteria).forEach(function(c){
                    if (c === "studyDateRange") {
                        criteriaLabel.push(c + ': "' + dateFormatter(criteria[c]['start']) + '&ndash;'+ dateFormatter(criteria[c]['end']) + '"');
                    }
                    else {
                        criteriaLabel.push(c + ': "' + criteria[c] + '"');
                    }
                });

                resultsTable.tbody().tr({ data: { criteria: JSON.stringify(result.criteria) } })
                    .th()
                    .th({
                        colSpan: 5,
                        addClass: 'left',
                        html: 'Criteria: ' + criteriaLabel.join(', ')
                    });

                if (result.studies.length) {
                    var listItems = [];

                    result.studies.forEach(function(study){
                        var studyDate = dateFormatter(study.studyDate);

                        resultsTable.tbody().tr({ data: { json: JSON.stringify(study) }})
                            .td({
                                html: '<input type="checkbox" class="selectable-select-one sessionSelector" data-id="'+study.studyId+'" />'
                            })
                            .td( study.patient.name.lastNameCommaFirstName )
                            .td( studyDate )
                            .td( study.accessionNumber )
                            .td( study.id )
                            .td( study.studyDescription )

                        // listItems.push(spawn('li',[
                        //     spawn('label', [
                        //         spawn('input|type=checkbox|checked=checked',{addClass: 'sessionSelector', data: { id: study.studyId }}),
                        //         spawn('span.label', studyDate + ': '+ study.studyId + " (" + study.studyDescription + ")")
                        //     ]),
                        //     spawn('span.hidden', { addClass: 'sessionData id-'+study.studyId }, JSON.stringify(study))
                        // ]));
                    });
                } else {
                    resultsTable.tr()
                        .td({ colSpan: 6, html: 'No matching sessions found' })
                }
            }
        });

        XNAT.ui.dialog.open({
            title: 'Select Sessions To Import',
            width: 600,
            content: spawn('div.data-table-container'),
            beforeShow: function(obj){
                obj.$modal.find('.data-table-container').append(resultsTable.table);
            },
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
            var query = JSON.parse($('span.id-'+studyId).html());
            dataToImport.push(query);
        });

        if (dataToImport.length) {
            XNAT.xhr.ajax({
                url: csvimporter.importJsonUrl(),
                method: 'POST',
                data: JSON.stringify(dataToImport),
                contentType: 'application/json',
                fail: function(e){
                    queryErrorHandler(e)
                },
                success: function(data){
                    XNAT.ui.dialog.message({
                        title: 'Request Succesful',
                        content: 'Your request for data was successful. Proceed to the <a href="'+XNAT.url.rootUrl('/app/template/XDATScreen_prearchives.vm')+'">Prearchive</a>?',
                        okAction: function(){
                            XNAT.ui.dialog.closeAll();
                        }
                    });

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

    /* --- INIT --- */

    function scpSanityChecks(scpId){
        var receiver = csvimporter.scpReceivers[scpId], checks = [];
        var receiverLabel = receiver['aeTitle']+':'+receiver['port'];

        XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/xapi/processors/site/enabled/receiver/'+receiverLabel),
            fail: function(e){
                errorHandler(e, 'Could not retrieve processors for this receiver')
            },
            success: function(data){
                var enabledScpProcessors = data,
                    $processorList = $('#installedProcessorList'),
                    processorItems = [];

                if (enabledScpProcessors.length) {
                    enabledScpProcessors.forEach(function(processor){
                        processorItems.push(
                            spawn('li',[
                                spawn('a.processor-info',{ href:'#!',data: {'id': processor.id, 'json': JSON.stringify(processor) }}, processor.label)
                            ])
                        )
                    });
                    $processorList.empty().append(
                        spawn('ul',processorItems)
                    )
                }
                else {
                    $processorList.empty().append('No processors enabled for this SCP receiver')
                }
            }
        });

        // check custom processing on SCP receiver
        if (receiver.customProcessing === true) {
            // checks.push( spawn('div.success.sanity-check','<b>Custom processing:</b> Enabled.') );
            $('#scpProcessingStatus').empty().html('Enabled')
        }
        else {
            // checks.push( spawn('div.warning.sanity-check','<b>Custom processing:</b> Disabled. DICOM remapping will not be allowed.') );
            $('#scpProcessingStatus').empty().html('Disabled. DICOM remapping will not be allowed.');
        }

        // check Dicom Object Identifier setting
        if (receiver.identifier !== undefined && receiver.identifier !== 'dicomObjectIdentifier') {
            // checks.push(spawn('div.message.sanity-check', '<b>DICOM Object Identifier:</b> '+ receiver.identifier +'. Special handling may be applied to imported sessions.'));
            $('#scpDicomIdentifier').empty().html(receiver.identifier + '. Special handling may be applied.')
        }
        else {
            // checks.push(spawn('div.success', '<b>DICOM Object Identifier:</b> Default. No special handling defined.'))
            $('#scpDicomIdentifier').empty().html('Default. No special handling defined.');
        }

    }

    $(document).on('change','select#ae',function(){
        var scpId = $(this).find('option:selected').data('id');
        scpSanityChecks(scpId);
    });

    $(document).on('click','a.processor-info',function(){
        var processor = $(this).data('json');
        XNAT.ui.dialog.message({
            title: 'Installed Processor: '+processor.label,
            content: '<table class="xnat-table alt1 condensed" style="width:100%"></table>',
            width: 600,
            beforeShow: function(obj){
                var $table = obj.$modal.find('table');
                $table.append(
                    spawn('tr',[
                        spawn('th','Attribute'),
                        spawn('th','Value')
                    ])
                );

                var keys = Object.keys(processor).sort(function(a,b){ return (a>b) ? 1: -1 })

                keys.forEach(function(key){
                    $table.append(
                        spawn('tr',[
                            spawn('th',key),
                            spawn('td',processor[key])
                        ])
                    );
                })
            }
        })
    });



    csvimporter.init = csvimporter.refresh = function(){
        // reset the form
        var $form = $('#pacsSeriesFinderForm');
        $form.resetForm();

        // populate the SCP receiver list if not already populated
        if (Object.keys(csvimporter.scpReceivers).length === 0) {
            XNAT.xhr.getJSON({
                url: XNAT.url.restUrl('/xapi/dicomscp'),
                fail: function(e){
                    errorHandler(e, 'Could not retrieve SCP Receivers')
                },
                success: function(data){
                    // transform the array into an object sorted by ID
                    if (data.length && isArray(data)) {
                        data.forEach(function(receiver){
                            csvimporter.scpReceivers[receiver.id] = receiver;
                        })
                    }
                }
            })
        }

        // populate the list of known processors if not already populated
        if (isObject(csvimporter.installedProcessors) && Object.keys(csvimporter.installedProcessors).length === 0) {
            XNAT.xhr.getJSON({
                url: XNAT.url.restUrl('/xapi/processors/site/enabled'),
                fail: function(e){
                    errorHandler(e, 'Could not retrieve installed processors')
                },
                success: function(data){
                    // transform the data into an object sorted by ID
                    if (data.length && isArray(data)) {
                        data.forEach(function(processor){
                            csvimporter.installedProcessors[processor.id] = processor;
                        })
                    }
                    else {
                        csvimporter.installedProcessors = false;
                    }
                }
            })
        }
    };
    csvimporter.init();

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
