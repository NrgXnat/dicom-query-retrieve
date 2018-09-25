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

    function submitErrorHandler(e){
        if (e.status === 400){
            return errorHandler(e, 'Missing Parameters Found');
        }
        if (e.status === 500){
            if (e.responseText.toLowerCase().indexOf('remapping') >= 0) {
                e.responseText = 'This XNAT SCP Receiver cannot perform the custom remapping you are requesting.';
                csvimporter.forceQuerySubmit();
                return false;
            }

            if (e.responseText.toLowerCase().indexOf('criteria') >= 0) {
                e.responseText = 'Invalid or empty search criteria. Each query in your request must include at least one search criteria.';
                return errorHandler(e, 'Invalid Search Criteria');
            }

            if (e.responseText.toLowerCase().indexOf('scp') >= 0) {
                e.responseText = 'Invalid or Inactive SCP Receiver ID was selected. Please check your selection and try again.';
                return errorHandler(e, 'SCP Receiver Error');
            }
        }
        // if no matches were identified, handle the error generically
        return errorHandler(e);
    }

    function dateFormatter(datestring){
        // all dates for PACS queries should be formatted as YYYYMMDD and must be parsed before they can be processed as date objects.
        var ydate;
        if (!datestring || !datestring.length) {
            return '';
        }
        if (datestring.length === 8){
            ydate = datestring.toString();
            ydate = ydate.slice(0,4)+ '-' +ydate.slice(4,6)+'-'+ydate.slice(6,8);
        }
        else {
            ydate = datestring;
        }

        return (new Date(ydate)).toISOString().substring(0,10);
    }

    /* ============ */

    var csvimporter,
        projectId = XNAT.data.context.projectID,
        undefined;

    XNAT.app.dqr = getObject(XNAT.app.dqr || {});

    XNAT.app.dqr.csvimporter = csvimporter =
        getObject(XNAT.app.dqr.csvimporter || {});

    var terms = getObject(XNAT.app.dqr.terms || {
        termForPatient: "Patient",
        termForStudy: "Study"
    });

    csvimporter.queryParams = {};
    csvimporter.scpReceivers = {};
    csvimporter.selectedReceiver = {};
    csvimporter.installedProcessors = {};
    csvimporter.projectAnon = false;

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

    csvimporter.submitJsonToImport = function($form,force){
        force = force || false;
        var $selected = $form.find('.sessionSelector:checked');
        var dataToImport = JSON.parse($form.find('.json-to-send').html());

        if ($selected.length) {
            csvimporter.submitQuery($selected, dataToImport, force);
        }
        else {
            XNAT.ui.dialog.message('No sessions selected. Nothing to import.');
        }
    };

    csvimporter.forceQuerySubmit = function(){
        XNAT.ui.dialog.open({
            title: 'Error found: Cannot Apply DICOM Remapping',
            content: spawn('!', [
                spawn('p',{ style: { 'font-weight': 'bold' }}, 'Error 500: Internal Server Error'),
                spawn('p','This XNAT SCP Receiver cannot perform the custom remapping you are requesting. If you choose, you can import the requested sessions but remapping will not take place.')
            ]),
            buttons: [
                {
                    label: 'Cancel',
                    isDefault: 'true',
                    close: true,
                    action: csvimporter.refresh
                },
                {
                    label: 'Import Without Remapping',
                    close: true,
                    action: function(){
                        var $form = $(document).find('#submitJsonForm');
                        csvimporter.submitJsonToImport($form, true);
                    }
                }
            ]
        })
    };

    csvimporter.displayQueryResults = function(data){
        // receive data as JSON blob, with an array of query results as the outer layer
        // present the results as an selectable table organized by query that allows users to confirm data to import

        // clean the JSON to send without assuming that any sessions have been selected
        var jsonToSend = [], requiresRemapping = false;

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
            .th('<b>'+terms.termForPatient+' Name</b>')
            .th('<b>'+terms.termForStudy+' Date</b>')
            .th('<b>Accession Num</b>')
            .th('<b>'+terms.termForStudy+' ID</b>')
            .th('<b>'+terms.termForStudy+' Description</b>');

        var resultsTableBody = resultsTable.tbody();

        function showSessionLink(linkText,json){
            return spawn('a.show-session-info',{
                href: '#!',
                html: linkText,
                data: {
                    'result-data': JSON.stringify(json)
                },
                style: { 'font-weight': 'bold' }
            })
        }


        data.forEach(function(result, i){
            if (result.criteria) {
                var anonScript = result.anonScript || '';
                if (anonScript.length) requiresRemapping = true;
                // add the criteria and anon to the JSON we'll send, without adding any sessions
                jsonToSend.push({ 'criteria': result.criteria, 'anonScript': anonScript, 'studies': [] });

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
                        criteriaLabel.push(c + ': "' + (dateFormatter(criteria[c]['start']) || 'Unknown ') + ' to '+ (dateFormatter(criteria[c]['end']) || ' Unknown') + '"');
                    }
                    else {
                        criteriaLabel.push(c + ': "' + criteria[c] + '"');
                    }
                });

                resultsTableBody.tr({ data: { 'criteria-index': i } })
                    .th('Criteria')
                    .th({
                        colSpan: 5,
                        addClass: 'left',
                        style: { 'font-weight': 'normal', 'white-space': 'normal' },
                        html: criteriaLabel.join(', ')
                    });

                if (anonScript) {
                    var remapEls = [];
                    if (anonScript.indexOf('(0010,0010)') >= 0) remapEls.push(terms.termForPatient + ' Name');
                    if (anonScript.indexOf('(0010,0030)') >= 0) remapEls.push(terms.termForPatient + ' Birth Date');
                    if (anonScript.indexOf('(0010,0020)') >= 0) remapEls.push(terms.termForPatient + ' ID');
                    if (anonScript.indexOf('(0020,0010)') >= 0) remapEls.push(terms.termForStudy + ' ID');
                    if (anonScript.indexOf('(0008,0020)') >= 0) remapEls.push(terms.termForStudy + ' Date');
                    if (anonScript.indexOf('(0008,0050)') >= 0) remapEls.push('Accession Number');

                    resultsTableBody.tr()
                        .th('Remap')
                        .th({
                            colSpan: 5,
                            addClass: 'left',
                            style: { 'font-weight': 'normal', 'max-width': '600px' },
                            html: remapEls.join(', ')
                        });
                }

                if (result.studies.length) {
                    result.studies.forEach(function(study){

                        var studyDate = dateFormatter(study.studyDate || '');

                        if (!studyDate || studyDate.trim().toLowerCase() === 'invalid date') {
                            studyDate = "Unknown";
                        }

                        resultsTableBody.tr()
                            .td({
                                html: '<input type="checkbox" class="selectable-select-one sessionSelector" data-id="'+study.studyId+'" data-criteria="'+i+'" data-json=\''+JSON.stringify(study) +'\' />'
                            })
                            .td([ showSessionLink(study.patient.name.lastNameCommaFirstName, study) ])
                            .td( studyDate )
                            .td({ style: { 'max-width':'140px', 'word-wrap':'break-word' }}, study.accessionNumber )
                            .td( study.studyId )
                            .td( study.studyDescription )
                    });
                } else {
                    resultsTable.tr()
                        .td({ colSpan: 6, html: 'No matching sessions found' })
                }
            }
        });

        XNAT.ui.dialog.open({
            title: 'Select Sessions To Import',
            width: 960,
            maxBtn: true, // show 'maximize' button in title bar
            content: spawn('div#submitJsonForm.data-table-container.form-data'),
            beforeShow: function(obj){
                var $container = obj.$modal.find('.data-table-container');
                $container.append(resultsTable.table);
                $container.append(spawn('div.hidden.json-to-send', JSON.stringify(jsonToSend) ));

                if (requiresRemapping && !csvimporter.selectedReceiver['can-remap']) {
                    $container.prepend(spawn('div.alert','Warning: You have requested field remapping on these sessions but your selected SCP Receiver and processor settings cannot support remapping.'))
                }
            },
            buttons: [
                {
                    label: 'Import Selected Sessions',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('.form-data');
                        csvimporter.submitJsonToImport($form);
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        });
    };

    csvimporter.submitQuery = function($sessions, dataToImport, force){
        force = force || false; // only force import without remapping if explicitly specified

        $sessions.each(function(){
            var criteriaIndex = $(this).data('criteria');
            var study = $(this).data('json');
            dataToImport[criteriaIndex].studies.push(study);
        });

        if (dataToImport.length) {
            xmodal.loading.open({ title: 'Submitting studies to PACS...' });
            XNAT.xhr.ajax({
                url: csvimporter.importJsonUrl(force),
                method: 'POST',
                data: JSON.stringify(dataToImport),
                contentType: 'application/json',
                fail: function(e){
                    submitErrorHandler(e);
                    xmodal.loading.close();
                },
                success: function(){
                    xmodal.loading.close();
                    XNAT.ui.dialog.message({
                        title: 'Request Succesful',
                        width: 540,
                        content: 'The selected series have been requested from the PACS and should be available in the system prearchive shortly. Contact your PACS administrator if your requested series are not imported in a timely manner.',
                        buttons: [
                            {
                                label: 'Go to the Prearchive',
                                isDefault: true,
                                close: true,
                                action: function (obj) {
                                    xmodal.loading.open({title: 'Please wait...'});
                                    window.location = serverRoot + "/app/template/XDATScreen_prearchives.vm";
                                }
                            },
                            {
                                label: 'Start Another Import',
                                isDefault: false,
                                close: true,
                                action: csvimporter.refresh
                            }
                        ]
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

    csvimporter.scpSanityChecks = function(scpId){
        var selectedReceiver = csvimporter.selectedReceiver = csvimporter.scpReceivers[scpId], checks = [];
        var receiverLabel = selectedReceiver['aeTitle']+':'+selectedReceiver['port'];

        XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/xapi/processors/site/canRemap/receiver/'+receiverLabel),
            fail: function(e){
                errorHandler(e, 'Could not determine remapping state for '+receiverLabel)
            },
            success: function(data){
                csvimporter.selectedReceiver['can-remap'] = data;
            }
        });

        // XNAT.xhr.getJSON({
        //     url: XNAT.url.restUrl('/xapi/processors/site/enabled/receiver/'+receiverLabel),
        //     fail: function(e){
        //         errorHandler(e, 'Could not retrieve processors for this receiver')
        //     },
        //     success: function(data){
        //         var enabledScpProcessors = data,
        //             $processorList = $('#installedProcessorList'),
        //             processorItems = [];
        //
        //         if (enabledScpProcessors.length) {
        //             enabledScpProcessors.forEach(function(processor){
        //                 processorItems.push(
        //                     spawn('li',[
        //                         spawn('a.processor-info',{ href:'#!',data: {'id': processor.id, 'json': JSON.stringify(processor) }}, processor.label)
        //                     ])
        //                 )
        //             });
        //             $processorList.empty().append(
        //                 spawn('ul',processorItems)
        //             )
        //         }
        //         else {
        //             var msg = 'No processors enabled for this SCP receiver.';
        //             if (!csvimporter.projectAnon) msg = '<div class="alert">'+ msg + ' No anonymization or remapping will be performed.</div>';
        //             $processorList.empty().append(msg);
        //         }
        //     }
        // });

        // check custom processing on SCP receiver
        if (selectedReceiver.customProcessing === true) {
            // checks.push( spawn('div.success.sanity-check','<b>Custom processing:</b> Enabled.') );
            $('#scpProcessingStatus').empty().html('Enabled')
        }
        else {
            // checks.push( spawn('div.warning.sanity-check','<b>Custom processing:</b> Disabled. DICOM remapping will not be allowed.') );
            $('#scpProcessingStatus').empty().html('Disabled. DICOM remapping will not be allowed.');
        }

        // check Dicom Object Identifier setting
        if (selectedReceiver.identifier !== undefined && selectedReceiver.identifier !== 'dicomObjectIdentifier') {
            // checks.push(spawn('div.message.sanity-check', '<b>DICOM Object Identifier:</b> '+ receiver.identifier +'. Special handling may be applied to imported sessions.'));
            $('#scpDicomIdentifier').empty().html(selectedReceiver.identifier + '. Special handling may be applied.')
        }
        else {
            // checks.push(spawn('div.success', '<b>DICOM Object Identifier:</b> Default. No special handling defined.'))
            $('#scpDicomIdentifier').empty().html('Default. No special handling defined.');
        }

        // $('#customProcessingStatus').show();
    };

    function validateCsvForm($form){
        var canSubmit = true,
            formErrors = [];

        $form.find('.required').each(function(){
            if (this.nodeName.toLowerCase() === 'select') {
                if (!$(this).find('option:selected').val()) {
                    canSubmit = false;
                    $(this).addClass('invalid');
                    formErrors.push('Please choose a value for '+$(this).prop('name'));
                }
            }
            if (this.nodeName.toLowerCase() === 'input') {
                if (this.type === 'text' && !$(this).val()) {
                    canSubmit = false;
                    $(this).addClass('invalid');
                    formErrors.push('Please enter a value for '+$(this).prop('name'));
                }
                else if (this.type === 'file' && !$(this).val()) {
                    canSubmit = false;
                    $(this).addClass('invalid');
                    formErrors.push('Please select a file to upload.');
                }
                else if (this.type === 'hidden' && !$(this).val()) {
                    canSubmit = false;
                    XNAT.dialog.message('Internal configuration error: No value set for '+$(this).prop('name'));
                }
            }
        });

        if (!canSubmit) {
            var errorLis = [];
            formErrors.forEach(function(error){
                errorLis.push(spawn('li',error));
            });
            XNAT.dialog.message({
                title: false,
                content: spawn('!',[
                    spawn('p','Errors Found: '),
                    spawn('ul',errorLis)
                ])
            });
        }

        return canSubmit;
    }

    function submitCsvForm($form){
        // capture values from selects to be used in second query
        // formData appears as an empty object, so each value must be retrieved individually.
        $form.find('select').each(function(){
            var key = $(this).prop('name');
            csvimporter.queryParams[key] = $(this).val()
        });
        csvimporter.queryParams['project'] = $form.find('input#project').val();


        var formData = new FormData($form[0]);

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
    }

    function displayDicomInfo(dicomObj){
        // build nice-looking history entry table
        var dsrTable = XNAT.table({
            className: 'xnat-table compact',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        dsrTable.tr()
            .th({ addClass: 'left', html: '<b>Key</b>' })
            .th({ addClass: 'left', html: '<b>Value</b>' });

        for (var key in dicomObj){
            var val = dicomObj[key], formattedVal = '';
            if (key === 'seriesIds') val = val.split(',');

            if (Array.isArray(val)) {
                var items = [];
                val.forEach(function(item){
                    if (typeof item === 'object') item = JSON.stringify(item);
                    items.push(spawn('li',[ spawn('code',item) ]));
                });
                formattedVal = spawn('ul',{ style: { 'list-style-type': 'none', 'padding-left': '0' }}, items);
            } else if (typeof val === 'object' ) {
                formattedVal = spawn('code', JSON.stringify(val));
            } else if (!val) {
                formattedVal = spawn('code','false');
            } else {
                formattedVal = spawn('code',val);
            }

            dsrTable.tr()
                .td('<b>'+key+'</b>')
                .td([ spawn('div',{ style: { 'word-break': 'break-all','max-width':'600px' }}, formattedVal) ]);
        }

        // display PACS info on queried session
        XNAT.ui.dialog.open({
            title: 'Query Result: '+dicomObj.studyId,
            width: 800,
            scroll: true,
            content: dsrTable.table,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true
                }
            ]
        });
    }

    /* -- User Event Handlers -- */

    $(document).on('change','select#ae',function(){
        var scpId = $(this).find('option:selected').data('id');
        csvimporter.scpSanityChecks(scpId);
    });

    $(document).on('click','a.show-session-info',function(){
        var dicomObj = $(this).data('result-data');
        displayDicomInfo(dicomObj);
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

    $('.invalid').on('focus',function(){ $(this).removeClass('invalid') });

    $(document).ready(function () {
        $(document).off('submit', 'form#pacsSeriesFinderForm');
        $(document).off('click','#submit-csv-form');
        $(document).on('click', '#submit-csv-form', function (e) {
            e.preventDefault();
            var $form = $('form#pacsSeriesFinderForm');

            // validate form before proceeding
            if (validateCsvForm($form)) {
                submitCsvForm($form);
            } else {
                return false;
            }
        });
    });

    /* --- INIT --- */

    csvimporter.init = csvimporter.refresh = function(scpId){
        if (!projectId) {
            XNAT.ui.dialog.message('Page configuration error: No project context defined. Please return to your project page and begin the Import From PACS process again.');
            return false;
        }

        // if an scpId is fed as a parameter, we should do a sanity check on that receiver on startup
        scpId = scpId || false;

        // close all dialogs and reset the form
        XNAT.ui.dialog.closeAll(); 
        var $form = $('#pacsSeriesFinderForm');
        $form.find('.invalid').removeClass('invalid');
        $form.resetForm();

        // reset the AE-specific data import settings
        $('#customProcessingStatus').hide();

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
                    if (scpId) csvimporter.scpSanityChecks(scpId);
                }
            })
        }

        // populate the list of known processors if not already populated
        if (isObject(csvimporter.installedProcessors) && Object.keys(csvimporter.installedProcessors).length === 0) {
            XNAT.xhr.getJSON({
                url: XNAT.url.restUrl('/xapi/processors/site/enabled/summary'),
                fail: function(e){
                    errorHandler(e, 'Could not retrieve installed processors')
                },
                success: function(data){
                    // transform the data into an object sorted by ID
                    if (data.length && isArray(data)) {
                        data.forEach(function(processor){
                            csvimporter.installedProcessors[processor.processorClass] = processor;
                        })
                    }
                    else {
                        csvimporter.installedProcessors = false;
                    }
                }
            })
        }

        // check for project anonymization script
        XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/data/config/edit/projects/'+projectId+'/image/dicom/script'),
            fail: function(e){
                if (e.status.toString() === '404') {
                    $('#projectAnonSetting').html('Not Active');
                }
                else errorHandler(e, 'Could not retrieve project anonymization script');
            },
            success: function(data){
                if (data.ResultSet.Result.length) {
                    csvimporter.projectAnon = true;
                }
            }
        })
    };

    // if the page loads with only a single default SCP receiver initialized, perform the sanity check on that receiver on startup
    var presetScpId = $('#default-scp-id').val();
    presetScpId = presetScpId || false;
    csvimporter.init(presetScpId);

}));
