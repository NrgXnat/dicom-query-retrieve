/*
 * D:/Development/DQR/dqr/src/main/resources/module-resources/scripts/dqr/PacsAdministration.js
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
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

    var pacsAdministration, pacsObj, pacsList;

    XNAT.app.dqr = getObject(XNAT.app.dqr || {});

    XNAT.app.dqr.pacsObj = pacsObj = {};
    XNAT.app.dqr.pacsList = pacsList = [];

    XNAT.app.dqr.PacsAdministration = pacsAdministration =
        getObject(XNAT.app.dqr.PacsAdministration);



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
                            label: 'Label'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'extendedNegotiations',
                            label: 'Extended Negotiations',
                            onText: 'Supported',
                            offText: 'Not Supported',
                            value: false
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'queryable',
                            label: 'Queryable',
                            onText: 'Yes',
                            offText: 'No',
                            value: false,
                            addClass: 'toggle-query'
                        }),
                        spawn('div.toggle-query-selector',{ style: { display: 'none' }},[
                            XNAT.ui.panel.input.text({
                                name: 'queryRetrievePort',
                                label: 'Q/R Port'
                            }),
                            XNAT.ui.panel.input.switchbox({
                                name: 'defaultQueryRetrievePacs',
                                label: 'Default Q/R AE',
                                onText: 'Yes',
                                offText: 'No'
                            })
                        ]),
                        XNAT.ui.panel.input.switchbox({
                            name: 'storable',
                            label: 'Storable',
                            onText: 'Yes',
                            offText: 'No',
                            value: false,
                            addClass: 'toggle-store'
                        }),
                        spawn('div.toggle-store-selector',{ style: { display: 'none' }},[
                            XNAT.ui.panel.input.text({
                                name: 'storagePort',
                                label: 'Storage Port'
                            }),
                            XNAT.ui.panel.input.switchbox({
                                name: 'defaultStoragePacs',
                                label: 'Default Storage AE',
                                onText: 'Yes',
                                offText: 'No',
                                value: false
                            })
                        ]),
                        XNAT.ui.panel.input.text({
                            name: 'availabilityStart',
                            label: 'Availability Start Time',
                            description: 'Time is specified in military time, <br>aka "20:00" rather than "10:00 pm"'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'availabilityEnd',
                            label: 'Availability End Time',
                            description: 'Time is specified in military time, <br>aka "20:00" rather than "10:00 pm"'
                        })
                    ])
                );

                if (pacs && doWhat.toLowerCase() === 'modify') {
                    $form.setValues(pacs);
                    if (pacs.storable && pacs.storable !== 'false') $form.find('.toggle-store-selector').show();
                    if (pacs.queryable && pacs.queryable !== 'false') $form.find('.toggle-query-selector').show();
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

                        $form.find('.validate').each(function(){
                            if (!XNAT.validate($(this)).check()) {
                                $(this).addClass('invalid');
                                invalidFields.push($(this).prop('name'));
                            }
                        });

                        if (invalidFields.length) {
                            XNAT.ui.dialog.open({
                                title: 'Form Validation Errors Found',
                                content: 'Please fix errors found in the following fields: <b>'+invalidFields.join(", ")+'</b>'
                            });
                            return false;
                        }

                        // // validate AE title
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
    $(document).on('click','input.toggle-query',function(){
        var $querySelector = $(this).parents('form').find('.toggle-query-selector');
        if ($(this).is(':checked')) {
            $querySelector.show();
        }
        else {
            $querySelector.hide();
        }
    });
    $(document).on('click','input.toggle-store',function(){
        var $storeSelector = $(this).parents('form').find('.toggle-store-selector');
        if ($(this).is(':checked')) {
            $storeSelector.show();
        }
        else {
            $storeSelector.hide();
        }
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
        var pacsTableData = data.ResultSet.Result;

        // intialize PACS table container and PACS list
        $(constants.PACS_DIV).empty();
        pacsList = [];
        pacsObj = {};

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
            .th('<b>Queryable</b>')
            .th('<b>Storable</b>')
            .th('<b>Status</b>')
            .th('<b>Actions</b>');

        function showDefault(setting,defaultSet){
            defaultSet = defaultSet || false;
            var display = spawn('i',{ className: 'fa fa-check' });
            if (defaultSet) display = spawn('small',{ style: { 'text-transform':'uppercase', 'font-weight': 'bold'} }, 'Default');
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
            if (ae.queryRetrievePort) summary.push( spawn('p', 'Q/R Port: '+ae.queryRetrievePort));
            if (ae.storagePort) summary.push( spawn('p', 'Storage Port: '+ae.storagePort));
            return spawn('!',summary);
        }

        // add data rows
        if (pacsTableData.length) {
            pacsTableData.sort(function(a,b){
                return (a.id > b.id) ? 1 : -1;
            });
            pacsTableData.forEach(function(ae){
                // add AE Title to Pacs List
                pacsList.push(ae.aeTitle.toLowerCase());
                pacsObj[ae.id] = ae;

                // populate table row
                pacsTable.tr({
                    data: {
                        id: ae.id,
                        aeTitle: ae.aeTitle,
                        host: ae.host,
                        label: ae.label,
                        queryable: ae.queryable,
                        queryRetrievePort: ae.queryRetrievePort,
                        defaultQueryRetrievePacs: ae.defaultQueryRetrievePacs,
                        storable: ae.storable,
                        storagePort: ae.storagePort,
                        defaultStoragePacs: ae.defaultStoragePacs,
                        availabilityStart: ae.availabilityStart,
                        availabilityEnd: ae.availabilityEnd,
                        ormStrategySpringBeanId: ae.ormStrategySpringBeanId,
                        supportsExtendedNegotiations: ae.supportsExtendedNegotiations
                    }
                })
                    .td([ displayAeSummary(ae) ])
                    .td({ addClass: 'center' },[ showDefault(ae.queryable, ae.defaultQueryRetrievePacs) ])
                    .td({ addClass: 'center' },[ showDefault(ae.storable, ae.defaultStoragePacs) ])
                    .td({ addClass: 'center'}, '(Ping Status)')
                    .td({ addClass: 'center'}, [ editButton(), spawn('!',' '), deleteButton() ]);
            })

        }
        $(constants.PACS_DIV).append(pacsTable.table);

        $(constants.PACS_DIV).append(
            spawn('p',{ 'id': constants.ADD_PACS_LINK_HOLDER.substring(1), style: { 'margin-top':'1em' } }, [
                spawn('a', { className: 'btn primary', href: 'javascript:void(0)', id: constants.ADD_PACS_LINK.substring(1) },'Add New DICOM AE')
            ])
        );

        bindAddButtonHandler();
        bindEditButtonHandler();
        bindDeleteButtonHandler();

        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function handlePacsSearchFailure(jqXHR) {
        $(constants.PACS_DIV).text("Error " + jqXHR.status + ": " + jqXHR.responseText);
        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function getAllPacs() {
        XNAT.xhr.ajax({
            type: "GET",
            url: XNAT.url.csrfUrl("/data/pacs"),
            dataType: "json",
            success: showPacs,
            error: handlePacsSearchFailure
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function deletePacs() {
        XNAT.xhr.ajax({
            type: "DELETE",
            url: XNAT.url.csrfUrl("/data/pacs/" + currentOperation.pacs.id),
            success: getAllPacs,
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("Could not delete DICOM AE deletion: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function editPacs($form) {
        XNAT.xhr.ajax({
            type: "PUT",
            url: XNAT.url.csrfUrl("/data/pacs/" + currentOperation.pacs.id),
            data: $form.serialize(),
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

    function addPacs($form) {
        XNAT.xhr.ajax({
            type: "POST",
            url: XNAT.url.csrfUrl("/data/pacs"),
            data: $form.serialize(),
            success: function () {
                xmodal.close();
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

    pacsAdministration.init = function() {
        getAllPacs();
    };

    pacsAdministration.cancelCurrentOperation = function() {
        currentOperation.enable();
    };

    pacsAdministration.submitCurrentOperation = function() {
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

    pacsAdministration.ormStrategies = ormStrategies;

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
        var appended = (id) ? '/request/'+id : '';
        return XNAT.url.rootUrl('/xapi/dqr/query/history' + appended);
    }

    function getQueryQueueUrl(id){
        var appended = (id) ? '/request/'+id : '';
        return XNAT.url.rootUrl('/xapi/dqr/query/queue' + appended);
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

    function sortQueueData(callback){
        callback = isFunction(callback) ? callback : function(){};

        var URL = getQueryQueueUrl();
        return XNAT.xhr.getJSON(URL)
            .success(function(data){
                if (data.length){
                    // sort data by ID
                    data = data.sort(function(a,b){ return (a.id > b.id) ? 1 : -1 });

                    // copy the history listing into an object for individual reference
                    data.forEach(function(queueEntry){
                        queryQueue[queueEntry.id] = queueEntry;
                    });

                    return data;
                }
                callback.apply(this, arguments);
            })
    }

    function sortHistoryData(callback){
        callback = isFunction(callback) ? callback : function(){};

        var URL = getQueryHistoryUrl();
        return XNAT.xhr.getJSON(URL)
            .success(function(data){
                if (data.length){
                    // sort data by ID
                    data = data.sort(function(a,b){ return (a.id > b.id) ? 1 : -1 });

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
        }
        else {
            return 'Unknown Date';
        }
    }

    function spawnHistoryTable(sortedHistoryObj,tableType){

        var $dataRows = [];
        tableType = (tableType || 'history');

        return {
            kind: 'table.dataTable',
            name: 'dqr-'+tableType,
            id: 'dqr-'+tableType,
            // load: URL,
            data: sortedHistoryObj,
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
            sortable: 'id, pacs, dataRequested, user, DATE, PROJECT',
            filter: 'pacs, dataRequested, user, DATE, PROJECT',
            items: {
                // by convention, name 'custom' columns with ALL CAPS
                // 'custom' columns do not correspond directly with
                // a data item
                id: {
                    label: 'ID',
                    td: { className: 'center' },
                    filter: false,
                    apply: function(){
                        return this['id'];
                    }
                },
                pacs: {
                    label: 'DICOM AE',
                    filter: true,
                    apply: function(){
                        return (pacsObj[this.pacsId]) ? pacsObj[this.pacsId].aeTitle : 'unknown'
                    }
                },
                DATE: {
                    label: 'Date',
                    th: { className: 'dqr-query center' },
                    td: { className: 'dqr-query center mono'},
                    filter: function(table){
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
                        var dateString = formatDate(this['timestamp']);

                        return spawn('!',[
                            spawn('span', dateString ),
                            spawn('input.hidden.query-timestamp.filtering|type=hidden', { value: this['timestamp'] } )
                        ])
                    }
                },
                dataRequested: {
                    label: 'Data Requested',
                    filter: true, // add filter: true to individual items to add a filter,
                    apply: function(){
                        var sessionID = this['studyInstanceUid'];
                        var scans = this['seriesIds'].split(',');
                        return spawn (
                            'a',
                            { href: '#!', title: sessionID, className: 'view-'+tableType+'-entry', data: { id: this['id'] } },
                            '1 Session with '+scans.length+' Scans'
                        );
                    }
                },
                user: {
                    label: 'User',
                    filter: true,
                    apply: function(){
                        return this['username']
                    }
                },
                PROJECT: {
                    label: 'Project',
                    filter: true,
                    apply: function(){
                        var projectId = this['xnatProject'];
                        if (projectId) {
                            return spawn('a',{ href: '/data/projects/'+ projectId + '?format=html', html: projectId });
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
                    label: 'OK',
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
            XNAT.ui.dialog.open({
                content: 'Sorry, could not display this history item.',
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true
                    }
                ]
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
                            'This request is currently queued. Items in the queue will be processed during the PACS\'s availability window. ' +
                            'The availability window for '+pacsObj[queueEntry['pacsId']].aeTitle+' opens at <strong>'+
                            pacsObj[queueEntry['pacsId']].availabilityStart+ '</strong> and closes at <strong>'+
                            pacsObj[queueEntry['pacsId']].availabilityEnd+'</strong>.'
                        )
                    );
                },
                buttons: queueDialogButtons
            });
        } else {
            console.log(id);
            XNAT.ui.dialog.open({
                content: 'Sorry, could not display this queue entry.',
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true
                    }
                ]
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

        sortHistoryData().done(function(data){
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

            sortQueueData().done(function(data){
                if (data.length){
                    setTimeout(function(){
                        _queueTable = XNAT.spawner.spawn({
                            queueTable: spawnHistoryTable(data,'queue')
                        });
                        _queueTable.done(function(){
                            var queueLength = (data.length === 1) ? "Query" : "Queries";
                            $queueContainer.empty().append(
                                spawn('h3', { style: { 'margin-bottom': '1em' }}, data.length + ' ' + queueLength + ' Have Been Queued')
                            );
                            this.render($queueContainer, 20);
                        });
                    }, 20);
                    $queueContainer.css('margin-bottom','2em');
                }
            })
        });

    };

    historyTable.init();

}));
