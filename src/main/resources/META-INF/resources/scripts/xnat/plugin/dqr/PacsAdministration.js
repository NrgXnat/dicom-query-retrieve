/*
 * dicom-query-retrieve: PacsAdministration.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*jslint white: true, browser: true, vars: true */

console.log('PacsAdministration.js');

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

}(function(){

/* ================ *
 * GLOBAL FUNCTIONS *
 * ================ */

    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function errorHandler(e, title, closeAll){
        console.log(e);
        title = (title) ? 'Error Found: '+ title : 'Error';
        closeAll = (closeAll === undefined) ? true : closeAll;
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>' : e;
        XNAT.dialog.open({
            width: 450,
            title: title,
            content: errormsg,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true,
                    action: function(){
                        if (closeAll) {
                            xmodal.closeAll();

                        }
                    }
                }
            ]
        });
    }

    /* ============ */

    var PacsAdministration, pacsObj, pacsList;

    XNAT.app.dqr = getObject(XNAT.app.dqr || {});
    XNAT.app.dqr.pacsObj = pacsObj = {};
    XNAT.app.dqr.pacsList = pacsList = [];

    XNAT.app.dqr.PacsAdministration = PacsAdministration =
        getObject(XNAT.app.dqr.PacsAdministration);

    let showDicomWeb = false;

    var constants = {
        "MODAL_WINDOW_NAME": "loadData",
        "PACS_DIV": "#pacsDiv",
        "PACS_TABLE": "#pacsTable",
        "ADD_PACS_LINK": "#addNewPacs",
        "ADD_PACS_LINK_HOLDER": "#addNewPacsHolder",
        "OPERATION_EDIT": "EDIT",
        "OPERATION_DELETE": "DELETE",
        "OPERATION_CREATE": "CREATE"
    };

    var ormStrategies = ['dicomOrmStrategy']; // replace this with a dynamic list

    // We'll keep the edit form in JavaScript, adding it to the DOM upon request
    // Just cleaner to define the form initially in the Velocity template, then slurp it in on page load

    var currentOperation;

    function AddOperation(pButton) {
        this.button = pButton;
        this.type = constants.OPERATION_CREATE;
    }

    function ModifyOperation(pImage, pImageCssClass, pPacs, pType) {
        this.image = pImage;
        this.imageCssClass = pImageCssClass;
        this.pacs = pPacs;
        this.type = pType;
    }


    ModifyOperation.prototype.disable = function () {
        $(this.image).removeClass(this.imageCssClass);
    };
    ModifyOperation.prototype.enable = function () {
        $(this.image).addClass(this.imageCssClass);
    };

    function editPacsDialog(pacs) {
        pacs = pacs || {};
        pacsList = XNAT.app.dqr.pacsList;
        var doWhat = Object.keys(pacs).length ? 'Modify' : 'Add New';
        var originalPacsLabel = (pacs.aeTitle) ? pacs.aeTitle.toLowerCase() : false;

        XNAT.dialog.open({
            title: doWhat + ' DICOM AE Connection ',
            width: 600,
            className: doWhat.toLowerCase() + 'Modal',
            content: spawn('form.panel'),
            beforeShow: function(obj){
                var $form = obj.$modal.find('form');
                var ormSelector;
                if (ormStrategies.length > 1) {
                    ormSelector = XNAT.ui.panel.select.menu({
                        name: 'ormStrategySpringBeanId',
                        label: 'ORM Strategy',
                        options: ormStrategies
                    })
                }
                else {
                    ormSelector = XNAT.ui.panel.input.hidden({
                        name: 'ormStrategySpringBeanId',
                        value: ormStrategies[0]
                    })
                }
                $form.append(
                    spawn('!', [
                        XNAT.ui.panel.input.hidden({
                            name: 'pacsId'
                        }),
                        ormSelector,
                        XNAT.ui.panel.input.text({
                            name: 'aeTitle',
                            label: 'AE Title',
                            addClass: 'aeTitle-input validate',
                            validation: 'required'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'host',
                            label: 'Host',
                            addClass: 'validate',
                            validation: 'required'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'label',
                            label: 'Label',
                            addClass: 'validate',
                            validation: 'required'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'queryRetrievePort',
                            label: 'Port',
                            addClass: 'validate',
                            validation: 'required'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'queryable',
                            label: 'Queryable',
                            onText: 'Yes',
                            offText: 'No',
                            addClass: 'toggle-query'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'defaultQueryRetrievePacs',
                            label: 'Default Q/R AE',
                            onText: 'Yes',
                            offText: 'No'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'storable',
                            label: 'Storable',
                            onText: 'Yes',
                            offText: 'No',
                            addClass: 'toggle-store'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'defaultStoragePacs',
                            label: 'Default Storage AE',
                            onText: 'Yes',
                            offText: 'No'
                        }),
                        spawn('div.dicom-web', [
                            XNAT.ui.panel.input.switchbox({
                                name: 'dicomWebEnabled',
                                label: 'DicomWeb Enabled',
                                onText: 'Yes',
                                offText: 'No'
                            }),
                            XNAT.ui.panel.input.text({
                                name: 'dicomWebRootUrl',
                                label: 'DicomWeb Root Url',
                                addClass: 'validate',
                                validation: 'url',
                                description: 'eg: http://www.orthanc.com:8042/dicom-web/'
                            })
                        ])
                    ])
                );

                XNAT.xhr.getJSON({
                    url: XNAT.url.restUrl('/xapi/dqr/settings'),
                    fail: function (e) {
                        console.log('Could not get DQR settings', e)
                    },
                    success: function (data) {
                        showDicomWeb = Boolean(data.dicomWebEnabled);
                        if (showDicomWeb) {
                            $(document).find('.dicom-web').show();
                        }
                    }
                });

                if (pacs && doWhat.toLowerCase() === 'modify') {
                    $form.setValues(pacs);
                }
                else {
                    $form.find('select').find('option').first().prop('selected','selected');
                    $form.find('input[type=checkbox]').prop('checked',false);
                }
            },
            buttons: [
                {
                    label: 'Save',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        var invalidFields = [];

                        if ($form.find('input[name=dicomWebEnabled]').val().toLowerCase() != 'true') {
                            $form.find('input[name=dicomWebRootUrl]').removeClass('validate');
                        }

                        $form.find('.validate').each(function(){
                            if (!XNAT.validate($(this)).check()) {
                                $(this).addClass('invalid');
                                invalidFields.push($(this).prop('name'));
                            }
                        });

                        if (invalidFields.length) {
                            XNAT.ui.dialog.message({
                                title: false,
                                content: '<h4>Form Validation Errors Found</h4><p>Please fix errors found in the following fields: <b>'+invalidFields.join(", ")+'</b></p>'
                            });
                            return false;
                        }

                        // validate AE title
                        var submittedAeTitle = $form.find('input[name=aeTitle]').val().toLowerCase();
                        if (originalPacsLabel && submittedAeTitle !== originalPacsLabel && pacsList.indexOf(submittedAeTitle) >= 0) {
                            xmodal.alert('<strong>Error:</strong> You cannot save more than one connection to a single AE Title');
                            $form.find('input[name=aeTitle]').addClass('invalid');
                            return false;
                        }
                        else {
                            XNAT.ui.dialog.closeAll();
                        }

                        (doWhat.toLowerCase() === 'modify') ?
                            editPacs($form) :
                            addPacs($form);
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        })
    }

    $(document).on('blur','.validate',function(){
        $(this).removeClass('invalid');
    });

    function bindAddButtonHandler() {
        var addButtonHandler = function () {
            currentOperation = new AddOperation(this);

            editPacsDialog();
        };
        $(document).off("click", constants.ADD_PACS_LINK);
        $(document).on("click", constants.ADD_PACS_LINK, addButtonHandler);
    }

    function bindEditButtonHandler() {
        var editButtonHandler = function () {
            var pacs = $(this).parents('tr').data();
            currentOperation = new ModifyOperation(this, "editRow", pacs, constants.OPERATION_EDIT);
            // currentOperation.disable();

            editPacsDialog(pacs);

        };
        $(constants.PACS_TABLE).on("click", ".editRow", editButtonHandler);
    }

    function bindDeleteButtonHandler() {
        var deleteButtonHandler = function () {
            var pacs = $(this).parents('tr').data();
            currentOperation = new ModifyOperation(this, "deleteRow", pacs, constants.OPERATION_DELETE);
            // currentOperation.disable();

            xmodal.open({
                width: 400,
                height: 150,
                className: 'deleteModal',
                title: 'Confirm DICOM AE Deletion',
                content: 'Are you sure you want to delete this DICOM AE connection?',
                okAction: XNAT.app.dqr.PacsAdministration.submitCurrentOperation
            });
        };
        $(constants.PACS_TABLE).on("click", ".deleteRow", deleteButtonHandler);
    }

    function showPacs(data) {
        var pacsTableData = data;

        // intialize PACS table container and PACS list
        $(constants.PACS_DIV).empty();
        XNAT.app.dqr.pacsList = pacsList = [];

        var pacsTable = XNAT.table({
            className: 'xnat-table',
            style: {
                'width': '100%'
            },
            id: constants.PACS_TABLE.substring(1)
        });

        // add table header row
        pacsTable.tr()
                 .th({ addClass: 'left', html: '<b>DICOM AE</b>' })
                 .th('<b>Schedule</b>')
                 .th('<b>Queryable</b>')
                 .th('<b>Storable</b>')
                 .th('<b>Status</b>')
                 .th('<b>Actions</b>');

        function showDefault(setting,defaultSet){
            defaultSet = defaultSet || false;
            var display = spawn('i',{ className: 'fa fa-check' });
            if (defaultSet) display = spawn('small',{ style: {
                // 'text-transform':'uppercase',
                'font-weight': 'bold'
            }}, 'Default');
            return setting ? display : '';
        }
        function editButton(){
            return spawn('button',{ className: 'btn editRow', title: 'Edit This DICOM AE Connection' },[
                spawn('i', { className: 'fa fa-pencil' })
            ]);
        }
        function deleteButton(){
            return spawn('button',{ className: 'btn deleteRow', title: 'Delete This DICOM AE Connection' },[
                spawn('i', {className: 'fa fa-trash' })
            ]);
        }
        function displayLongLabel(label){
            return spawn('span.truncate', { style: { 'width': '120px' }, title: label }, label);
        }
        function displayAeSummary(ae){
            var summary = [
                spawn('p',[
                    spawn('a.editRow', { href: '#!', style: { 'font-weight': 'bold'} }, ae.aeTitle),
                    spawn('span', ' (IP: ' + ae.host + ')')
                ])
            ];
            if (ae.label) summary.push( spawn('p', ae.label));
            if (ae.queryRetrievePort) summary.push( spawn('p', 'Port: '+ae.queryRetrievePort));
            return spawn('!',summary);
        }
        function pingPacs(id){
            return spawn('button.btn-sm.ping-pacs', { title: 'Check PACS Network Status', data: { 'pacs-id': id }}, 'Ping');
        }

        // add data rows
        if (pacsTableData.length) {
            pacsTableData.sort(function(a, b){
                return (a.id > b.id) ? 1 : -1;
            });
            pacsTableData.forEach(function(ae){
                // add AE Title to Pacs List
                XNAT.app.dqr.pacsList.push(ae.aeTitle.toLowerCase());
                XNAT.app.dqr.pacsObj[ae.id] = ae;

                // these parameters will be stored in
                // [data-*] attributes on the row's <tr> element
                var dataAttrs = {
                    id: ae.id,
                    aeTitle: ae.aeTitle,
                    host: ae.host,
                    label: ae.label,
                    queryable: ae.queryable,
                    queryRetrievePort: ae.queryRetrievePort,
                    defaultQueryRetrievePacs: ae.defaultQueryRetrievePacs,
                    storable: ae.storable,
                    defaultStoragePacs: ae.defaultStoragePacs,
                    ormStrategySpringBeanId: ae.ormStrategySpringBeanId,
                    supportsExtendedNegotiations: ae.supportsExtendedNegotiations,
                    dicomWebEnabled: ae.dicomWebEnabled,
                    dicomWebRootUrl: ae.dicomWebRootUrl
                };

                // populate table row
                pacsTable.tr({ data: dataAttrs })
                         .td([ displayAeSummary(ae) ])
                         .td({ addClass: 'center ' }, [ spawn('a.link.edit-pacs-schedule|href=#!schedule', {
                             on: [['click', function(e){
                                 e.preventDefault();
                                 window.pacsId = ae.id;
                                 window.pacsLabel = ae.label || ae.aeTitle;
                                 var URL = XNAT.url.restUrl('/page/dqr/schedule/view.html.jsp', {
                                     pacs: window.pacsId,
                                     label: window.pacsLabel
                                 });
                                 console.log(URL);
                                 XNAT.dialog.load(URL, {
                                     width: 1150,
                                     maxBtn: false
                                 });
                             }]]
                         }, 'Schedule') ])
                         .td({ addClass: 'center' }, [ showDefault(ae.queryable, ae.defaultQueryRetrievePacs) ])
                         .td({ addClass: 'center' }, [ showDefault(ae.storable, ae.defaultStoragePacs) ])
                         .td({ addClass: 'center' }, [ pingPacs(ae.id)] )
                         // .td({ addClass: 'center' }, [ editButton(), '&nbsp;', deleteButton()] );
                         .td({ addClass: 'center' }, [ editButton()] );
            });

        }

        $(constants.PACS_DIV).append(pacsTable.table);

        $(constants.PACS_DIV).append(
            spawn('p', { 'id': constants.ADD_PACS_LINK_HOLDER.substring(1), style: { 'margin-top': '1em' } }, [
                spawn('a.btn.primary|href=#!', { id: constants.ADD_PACS_LINK.substring(1) }, 'Add New DICOM AE')
            ])
        );

        bindAddButtonHandler();
        bindEditButtonHandler();
        // bindDeleteButtonHandler();

        // initialize history table after pacsObj is populated
        historyTable.init();

        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function handlePacsSearchFailure(jqXHR) {
        $(constants.PACS_DIV).text("Error " + jqXHR.status + ": " + jqXHR.responseText);
        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function getAllPacs() {
        XNAT.xhr.ajax({
            type: "GET",
            url: XNAT.url.csrfUrl("/xapi/pacs"),
            dataType: "json",
            success: showPacs,
            error: handlePacsSearchFailure
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function deletePacs() {
        XNAT.xhr.ajax({
            type: "DELETE",
            url: XNAT.url.csrfUrl("/xapi/pacs/" + currentOperation.pacs.id),
            success: getAllPacs,
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("Could not delete DICOM AE deletion: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function editPacs($form) {
        XNAT.xhr.putJSON({
            url: XNAT.url.csrfUrl("/xapi/pacs/" + currentOperation.pacs.id),
            data: JSON.stringify($form),
            // data: XNAT.xhr.formToJSON($form, true),
            success: function () {
                xmodal.close();
                getAllPacs();
                XNAT.ui.banner.top(3000, 'Saved changes to DICOM AE connection', 'success');
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("Could not modify DICOM AE connection: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function setInitialAvailability(pacsObj){
        var pacsId = pacsObj.id;
        var days = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];
        days.forEach(function(day){
            var availabilityObj = {
                availabilityStart: "0:00",
                availabilityEnd: "24:00",
                dayOfWeek: day,
                pacsId: pacsId,
                threads: 1,
                utilizationPercent: 100
            };
            XNAT.xhr.postJSON({
                url: XNAT.url.csrfUrl('/xapi/pacs/'+pacsId+'/availability'),
                data: JSON.stringify(availabilityObj),
                cache: false,
                processData: false,
                success: function(data){
                    console.log(data)
                },
                fail: function(e){
                    console.warn(e)
                }
            })
        })

    }

    function addPacs($form) {
        XNAT.xhr.postJSON({
            url: XNAT.url.csrfUrl("/xapi/pacs"),
            data: JSON.stringify($form),
            // data: XNAT.xhr.formToJSON($form, true),
            success: function (data) {
                xmodal.close();
                setInitialAvailability(data); // set initial availability at 24/7 by default
                getAllPacs();
                XNAT.ui.banner.top(3000, 'Created new DICOM AE connection', 'success');
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("Could not create new DICOM AE connection: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function populateIdentifierTable(){
        // ensure identifiers are installed
        XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/xapi/dicomscp/identifiers'),
            fail: function(e){ console.log('Could not import DICOM Object Identifier list',e) },
            success: function(data){
                var $container = $(document).find('#dqrDicomIdentifierTableContainer');
                if (isObject(data) && Object.keys(data).length) {
                    var receivers = [], identifierObj = data;

                    // associate SCP receivers with identifiers
                    XNAT.xhr.getJSON({
                        url: XNAT.url.restUrl('/xapi/dicomscp'),
                        fail: function(e){ console.log('Could not retrieve SCP receiver list', e) },
                        success: function(data){
                            receivers = data;

                            var identifierTable = XNAT.table({
                                className: 'xnat-table',
                                style: {
                                    'width': '100%'
                                },
                                id: 'dqrDicomIdentifierTable'
                            });

                            function isDqrEnabled(identifier){
                                if (identifier.toLowerCase().indexOf('dqr') >= 0) {return 'true'}
                            }
                            function associatedReceivers(identifier){
                                var associatedReceivers = [];
                                receivers.forEach(function(receiver){
                                    var label = receiver.aeTitle + ':' + receiver.port;
                                    if (receiver.customProcessing) label += ' (Remapping Enabled)';
                                    if (!receiver.identifier) receiver.identifier = 'dicomObjectIdentifier'; // XNAT default value if an identifier has never been explicitly associated with an SCP receiver
                                    if (receiver.identifier === identifier) associatedReceivers.push(label);
                                });
                                return associatedReceivers.join('<br>');
                            }

                            // add table header row
                            identifierTable.tr()
                                .th({ addClass: 'left', html: '<b>Identifier Label</b>' })
                                .th('<b>Identifier Class</b>')
                                .th('<b>DQR-Enabled</b>')
                                .th('<b>Associated SCP Receivers</b>')

                            Object.keys(identifierObj).forEach(function(key){
                                identifierTable.tr()
                                    .td(key)
                                    .td(identifierObj[key])
                                    .td([ isDqrEnabled(key) ])
                                    .td([ associatedReceivers(key) ])
                            });

                            $container.append(identifierTable.table);

                            // add a link to the SCP Receiver admin panel
                            $container.append('<p><a class="btn primary" href="'+XNAT.url.rootUrl('/app/template/Page.vm?view=admin#tab=dicom-scp-receivers-tab')+'">Configure SCP Receivers</a></p>');
                        }

                    });

                }
                else {
                    $container.append('No identifiers found');
                }
            }
        })
    }

    PacsAdministration.checkPacsStatus = function(id, button){
        id = parseInt(id);
        var $button = $(button);
        var url = XNAT.url.rootUrl('/xapi/pacs/'+id+'/status');

        function showPingError(errorObj){
            xmodal.loading.close();
            console.log('Could not ping PACS',errorObj);
            XNAT.ui.banner.top(3000,'Error: Could not check PACS status','error');
        }

        function showPingStatus(response){
            xmodal.loading.close();
            if (!response || !response.successful) {
                XNAT.ui.banner.top(3000,'Error: No response from PACS','error');
                return false;
            }
            if (!response.enabled) {
                XNAT.ui.banner.top(3000,'PACS Status: Down','alert');
                return true;
            }
            if (response.enabled){
                XNAT.ui.banner.top(3000,'PACS Status: Up','success');
            }
        }

        xmodal.loading.open({ title: 'Checking PACS Status' })
        XNAT.xhr.getJSON({
            url: url,
            fail: function(e) { showPingError(e) },
            success: function(d) { showPingStatus(d) }
        });
    };

    PacsAdministration.init = function() {
        getAllPacs();
        populateIdentifierTable();
    };

    PacsAdministration.cancelCurrentOperation = function() {
        currentOperation.enable();
    };

    PacsAdministration.submitCurrentOperation = function() {
        if (currentOperation.type === constants.OPERATION_DELETE) {
            xmodal.close();
            deletePacs();
        } else if (currentOperation.type === constants.OPERATION_EDIT) {
            editPacs($("#editPacsForm"));
        } else if (currentOperation.type === constants.OPERATION_CREATE) {
            addPacs($("#editPacsForm"));
        } else {
            alert('Unsupported operation type: ' + currentOperation.type);
        }
        currentOperation.enable();
    };

    PacsAdministration.ormStrategies = ormStrategies;

    $(document).on('click','.ping-pacs', function() {
        var id = $(this).data('pacs-id');
        XNAT.app.dqr.PacsAdministration.checkPacsStatus(id, this);
    });

    $(document).ready(function(){
        XNAT.app.dqr.PacsAdministration.init();
    });

    window.xModalSubmit = function () {
        "use strict";
        XNAT.app.dqr.PacsAdministration.submitCurrentOperation();
    };

    window.xModalCancel = function () {
        "use strict";
        xmodal.close();
        XNAT.app.dqr.PacsAdministration.cancelCurrentOperation();
    };

/* ================ *
 * AE Query History *
 * ================ */

    console.log('commandHistory.js');

    var historyTable, queryHistory, queryQueue;

    XNAT.app.dqr.historyTable = historyTable =
        getObject(XNAT.app.dqr.historyTable || {});

    XNAT.app.dqr.queryHistory = queryHistory =
        getObject(XNAT.app.dqr.queryHistory || {});

    XNAT.app.dqr.queryQueue = queryQueue =
        getObject(XNAT.app.dqr.queryQueue || {});

    function getQueryHistoryUrl(id){
        var appended = (id) ? '/'+id : '?page=1&pageSize=100&sort=DESC&all=true';
        return XNAT.url.restUrl('/xapi/dqr/import/history' + appended);
    }

    function getQueryQueueUrl(id){
        var appended = (id) ? '/'+id : '?page=1&pageSize=100&sort=DESC&all=true';
        return XNAT.url.restUrl('/xapi/dqr/import/queue' + appended);
    }

    function viewHistoryDialog(e, onclose){
        e.preventDefault();
        var historyId = $(this).data('id') || $(this).closest('tr').prop('title');
        XNAT.app.dqr.historyTable.viewHistory(historyId);
    }
    function viewQueueEntryDialog(e, onclose){
        e.preventDefault();
        var queueEntryId = $(this).data('id') || $(this).closest('tr').prop('title');
        XNAT.app.dqr.historyTable.viewQueueEntry(queueEntryId);
    }
    function removeQueueEntry(id){
        return XNAT.xhr.ajax({
            url: getQueryQueueUrl(id),
            method: 'DELETE'
        });
    }
    historyTable.removeQueueEntry = removeQueueEntry;

    function getQueueData(callback){
        callback = isFunction(callback) ? callback : function(){};

        var URL = getQueryQueueUrl();
        return XNAT.xhr.getJSON(URL)
            .success(function(data){
                if (data.length){
                    // sort data by ID
                    data = data.sort(function(a,b){ return (a.id < b.id) ? 1 : -1 });

                    // copy the history listing into an object for individual reference
                    data.forEach(function(queueEntry){
                        queryQueue[queueEntry.id] = queueEntry;
                    });

                    return data;
                }
                callback.apply(this, arguments);
            })
    }

    function getHistoryData(callback){
        callback = isFunction(callback) ? callback : function(){};

        var URL = getQueryHistoryUrl();
        return XNAT.xhr.getJSON(URL)
            .success(function(data){
                if (data.length){
                    // sort data by ID
                    data = data.sort(function(a,b){ return (a.id < b.id) ? 1 : -1 });

                    // copy the history listing into an object for individual reference
                    data.forEach(function(historyEntry){
                        queryHistory[historyEntry.id] = historyEntry;
                    });

                    return data;
                }
                callback.apply(this, arguments);
            })
    }

    function formatDate(timestamp){
        var dateString = new Date(timestamp);
        if (dateString) {
            return dateString.toISOString().replace('T',' ').replace('Z',' ').split('.')[0];
            // return dateString.toLocaleString();
        }
        else {
            return 'Unknown Date';
        }
    }

    function spawnHistoryTable(sortedHistoryObj,tableType){

        var $dataRows = [];
        tableType = (tableType || 'history');
        pacsObj = XNAT.app.dqr.pacsObj;

        return {
            kind: 'table.dataTable',
            name: 'dqr-'+tableType,
            id: 'dqr-'+tableType,
            // load: URL,
            data: sortedHistoryObj,
            before: {
                filterCss: {
                    tag: 'style|type=text/css',
                    content: '\n' +
                    '#dqr-history tr.filter-timestamp { display: none } \n'
                }
            },
            table: {
                classes: 'highlight hidden',
                on: [
                    ['click', 'a.view-history', viewHistoryDialog],
                    ['click', 'a.view-queueEntry', viewQueueEntryDialog]
                ]
            },
            trs: function(tr, data){
                tr.id = data.id;
                addDataAttrs(tr, { filter: '0' });
            },
            sortable: 'id, pacs, DATE, dataRequested, user, PROJECT',
            filter: 'id, pacs, DATE, dataRequested, user, PROJECT',
            // `order` is a comma-separated list (or array) of the
            // keys in `items` to make sure the columns are rendered
            // in the correct order, since object property iteration
            // is not guaranteed to be in the order as defined below
            order: 'id, pacs, DATE, dataRequested, user, PROJECT',
            items: {
                // by convention, name 'custom' columns with ALL CAPS
                // 'custom' columns do not correspond directly with
                // a data item,
                id: {
                    seq: '01',
                    label: 'ID',
                    th: { style: { width: '80px' }},
                    td: { style: { width: '80px' }},
                    filter: function(table){
                        return spawn('div.center', [ XNAT.ui.input({
                            element: {
                                placeholder: 'ID',
                                width: '80',
                                size: '5',
                                on: { keyup: function(){
                                    var FILTERCLASS = 'filter-id';
                                    var selectedValue = parseInt(this.value);
                                    $dataRows = $dataRows.length ? $dataRows : $$(table).find('tbody').find('tr');
                                    if (!selectedValue && selectedValue.toString() !== '0') {
                                        $dataRows.removeClass(FILTERCLASS);
                                    }
                                    else {
                                        $dataRows.addClass(FILTERCLASS).filter(function(){
                                            // remove zero-padding
                                            var queryId = parseInt($(this).find('td.id .sorting').html()).toString();
                                            return (queryId.indexOf(selectedValue) >= 0);
                                        }).removeClass(FILTERCLASS)
                                    }
                                } }
                            }

                        }).element ])
                    },
                    apply: function(){
                        return '<i class="hidden sorting">'+ zeroPad(this.id, 6) +'</i>'+ this.id.toString();
                    }
                },
                pacs: {
                    seq: '02',
                    label: 'DICOM AE',
                    filter: true,
                    apply: function(){
                        return (pacsObj[this.pacsId]) ? pacsObj[this.pacsId].aeTitle : 'unknown'
                    }
                },
                DATE: {
                    seq: '03',
                    label: 'Date',
                    th: { className: 'dqr-query center' },
                    td: { className: 'dqr-query'},
                    filter: true,
                    filterDisabled: function(table){
                        var MIN = 60*1000;
                        var HOUR = MIN*60;
                        var X8HRS = HOUR*8;
                        var X24HRS = HOUR*24;
                        var X7DAYS = X24HRS*7;
                        var X30DAYS = X24HRS*30;
                        return spawn('div.center', [XNAT.ui.select.menu({
                            value: 0,
                            options: {
                                all: {
                                    label: 'All',
                                    value: 0,
                                    selected: true
                                },
                                lastHour: {
                                    label: 'Last Hour',
                                    value: HOUR
                                },
                                last8hours: {
                                    label: 'Last 8 Hrs',
                                    value: X8HRS
                                },
                                last24hours: {
                                    label: 'Last 24 Hrs',
                                    value: X24HRS
                                },
                                lastWeek: {
                                    label: 'Last Week',
                                    value: X7DAYS
                                },
                                last30days: {
                                    label: 'Last 30 days',
                                    value: X30DAYS
                                }
                            },
                            element: {
                                id: 'filter-select-query-timestamp',
                                on: {
                                    change: function(){
                                        var FILTERCLASS = 'filter-timestamp';
                                        var selectedValue = parseInt(this.value, 10);
                                        var currentTime = Date.now();
                                        $dataRows = $dataRows.length ? $dataRows : $$(table).find('tbody').find('tr');
                                        if (selectedValue === 0) {
                                            $dataRows.removeClass(FILTERCLASS);
                                        }
                                        else {
                                            $dataRows.addClass(FILTERCLASS).filter(function(){
                                                var timestamp = this.querySelector('input.query-timestamp');
                                                var queryDate = +(timestamp.value);
                                                return selectedValue === queryDate-1 || selectedValue > (currentTime - queryDate);
                                            }).removeClass(FILTERCLASS);
                                        }
                                    }
                                }
                            }
                        }).element])
                    },
                    apply: function(){
                        var timestamp = this['timestamp'];
                        var dateString = formatDate(timestamp);
                        return spawn('!',[
                            spawn('i.hidden.sorting', timestamp),
                            spawn('span', dateString ),
                            spawn('input.hidden.query-timestamp.filtering|type=hidden', { value: timestamp } )
                        ])
                    }
                },
                dataRequested: {
                    seq: '04',
                    label: 'Data Requested',
                    filter: true, // add filter: true to individual items to add a filter,
                    apply: function(){
                        var sessionID = this['studyInstanceUid'];
                        var scans = this['seriesIds'];
                        return spawn (
                            'a',
                            { href: '#!', title: sessionID, className: 'view-'+tableType+'-entry', data: { id: this['id'] } },
                            '1 Session with '+scans.length+' Scans'
                        );
                    }
                },
                user: {
                    seq: '05',
                    label: 'User',
                    filter: true,
                    apply: function(){
                        return this['username']
                    }
                },
                PROJECT: {
                    seq: '06',
                    label: 'Project',
                    filter: true,
                    apply: function(){
                        var projectId = this['xnatProject'];
                        var projectUrl = XNAT.url.rootUrl('/data/projects/' + projectId + '?format=html');
                        if (projectId) {
                            return spawn('a', { href: projectUrl, html: projectId });
                        } else {
                            return 'Unknown';
                        }
                    }
                }
            }
        }
    }

    historyTable.viewHistory = function(id){
        if (queryHistory[id]) {
            var historyEntry = XNAT.app.dqr.queryHistory[id];
            var historyDialogButtons = [
                {
                    label: 'Close',
                    isDefault: true,
                    close: true
                }
            ];

            // build nice-looking history entry table
            var qheTable = XNAT.table({
                className: 'xnat-table compact',
                style: {
                    width: '100%',
                    marginTop: '15px',
                    marginBottom: '15px'
                }
            });

            // add table header row
            qheTable.tr()
                .th({ addClass: 'left', html: '<b>Key</b>' })
                .th({ addClass: 'left', html: '<b>Value</b>' });

            for (var key in historyEntry){
                var val = historyEntry[key], formattedVal = '';

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

                qheTable.tr()
                    .td('<b>'+key+'</b>')
                    .td([ spawn('div',{ style: { 'word-break': 'break-all','max-width':'600px' }}, formattedVal) ]);
            }

            // display history
            XNAT.ui.dialog.open({
                title: 'Query to '+pacsObj[historyEntry['pacsId']].aeTitle+' on '+formatDate(historyEntry['timestamp']),
                width: 800,
                scroll: true,
                content: qheTable.table,
                buttons: historyDialogButtons
            });
        } else {
            console.log(id);
            XNAT.ui.dialog.message({
                title: false,
                content: 'Sorry, could not display this history item.'
            });
        }
    };

    $(document).on('click','.view-history-entry',function(e){
        e.preventDefault();
        var historyEntryId = $(this).data('id');
        if (historyEntryId) {
            XNAT.app.dqr.historyTable.viewHistory(historyEntryId)
        }
        else {
            console.log('No history item ID provided');
        }
    });

    historyTable.viewQueueEntry = function(id){
        pacsObj = XNAT.app.dqr.pacsObj;

        if (queryQueue[id]) {
            var queueEntry = XNAT.app.dqr.queryQueue[id];
            var queueDialogButtons = [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true
                },
                {
                    label: 'Remove From Queue',
                    isDefault: false,
                    close: false,
                    action: function(){
                        XNAT.ui.dialog.confirm({
                            title: false,
                            content: 'Are you sure you want to remove this DICOM request from the queue?',
                            buttons: [
                                {
                                    label: 'Confirm Queue Removal',
                                    isDefault: true,
                                    close: true,
                                    action: function(){
                                        xmodal.loading.open({
                                            title: 'Removing DICOM request from queue'
                                        });
                                        window.setTimeout(function(){
                                            historyTable.removeQueueEntry(id).done(function(){
                                                historyTable.refresh();
                                                xmodal.loading.close();
                                                XNAT.ui.dialog.closeAll();
                                                XNAT.ui.banner.top(3000, 'Removed queue entry', 'success');
                                            })
                                        }, 500)
                                    }
                                },
                                {
                                    label: 'Cancel',
                                    close: true
                                }
                            ]
                        })
                    }
                }
            ];

            // build nice-looking history entry table
            var qheTable = XNAT.table({
                className: 'xnat-table compact',
                style: {
                    width: '100%',
                    marginTop: '15px',
                    marginBottom: '15px'
                }
            });

            // add table header row
            qheTable.tr()
                .th({ addClass: 'left', html: '<b>Key</b>' })
                .th({ addClass: 'left', html: '<b>Value</b>' });

            for (var key in queueEntry){
                var val = queueEntry[key], formattedVal = '';

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

                qheTable.tr()
                    .td('<b>'+key+'</b>')
                    .td([ spawn('div',{ style: { 'word-break': 'break-all','max-width':'600px' }}, formattedVal) ]);
            }

            // display queue entry
            XNAT.ui.dialog.open({
                title: 'Queued: Query to '+pacsObj[queueEntry['pacsId']].aeTitle,
                width: 800,
                scroll: true,
                content: qheTable.table,
                beforeShow: function(obj){
                    obj.$modal.find('.xnat-dialog-content').prepend(
                        spawn(
                            'div.message',
                            'This request is currently queued.'
                        )
                    );
                },
                buttons: queueDialogButtons
            });
        } else {
            console.log(id);
            XNAT.ui.dialog.message({
                title:false,
                content: 'Sorry, could not display this queue entry.',
            });
        }
    };

    $(document).on('click','.view-queue-entry',function(e){
        e.preventDefault();
        var queueEntryId = $(this).data('id');
        if (queueEntryId) {
            XNAT.app.dqr.historyTable.viewQueueEntry(queueEntryId)
        }
        else {
            console.log('No queue entry ID provided');
        }
    });

    historyTable.init = historyTable.refresh = function(){

        var $historyContainer = $('#dqr-history-container'),
            $queueContainer = $('#dqr-queue-container'),
            _historyTable, _queueTable;

        getHistoryData().done(function(data){

            if (data.length) {

                setTimeout(function(){
                    $historyContainer.html('loading...');
                }, 1);
                setTimeout(function(){
                    _historyTable = XNAT.spawner.spawn({
                        historyTable: spawnHistoryTable(data)
                    });
                    _historyTable.done(function(){
                        var queryLength = (data.length === 1) ? "DICOM Query" : "DICOM Queries";
                        $historyContainer.empty().append(
                            spawn('h3', { style: { 'margin-bottom': '1em' }}, data.length + ' ' + queryLength + ' Performed From This Site')
                        );
                        this.render($historyContainer, 20);
                    });
                }, 10);
            }

        }).always(function(){

            getQueueData().done(function(data){
                if (data.length){
                    setTimeout(function(){
                        _queueTable = XNAT.spawner.spawn({
                            queueTable: spawnHistoryTable(data,'queue')
                        });
                        _queueTable.done(function(){
                            var queueLength = (data.length === 1) ? "Query Has" : "Queries Have";
                            $queueContainer.empty().append(
                                spawn('h3', { style: { 'margin-bottom': '1em' }}, data.length + ' ' + queueLength + ' Been Queued')
                            );
                            this.render($queueContainer, 20);
                        });
                    }, 20);
                    $queueContainer.css('margin-bottom','2em');
                }
            })

        });

    };

}));

//# sourceURL=browsertools://scripts/dqr/PacsAdministration.js