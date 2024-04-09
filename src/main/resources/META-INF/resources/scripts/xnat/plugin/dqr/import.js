/*
 * dicom-query-retrieve: import.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/**
 * Import scans from a remote PACS
 */

var XNAT = getObject(XNAT || {});

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

    console.log('dqr/import.js');

    var dqr, undef;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.dqr = dqr =
        getObject(XNAT.plugin.dqr || {});

    // set conditional values
    dqr.canQuery = true;
    dqr.canImport = true;
    dqr.usePacsNameFormatting = false;
    dqr.pacsObj = {};

    // cache DOM elements when script loads for faster access later
    var $selectPacsMenu      = $('#select-pacs');
    var $pacsSearchFields    = $('#pacs-search-fields');
    var $searchSubmit        = $('#submit-pacs-search');
    var $pacsSearchResults   = $('#pacs-search-results');
    var $searchResultsHeader = $pacsSearchResults.find('.results-header');
    var $searchResultsBody   = $pacsSearchResults.find('.results-body');
    var $searchResultsSubmit = $pacsSearchResults.find('.results-submit');
    var $pacsNoResults       = $('#pacs-no-results');
    var $pacsQueryMsg        = $('#pacs-query-msg');
    var $noResultsTemplate   = $('#no-search-results');

    // string 'constants'
    var NONE        = 'none';
    var EMPTY_DATE  = '    -  -  ';
    var DATE_FORMAT = 'yyyy-mm-dd';

    function renderPacsMenu(items){
        var pacsMenu = $selectPacsMenu[0];
        forEach(items || [], function(item, i){
            dqr.pacsObj[item.id] = item;
            if (item.queryable) {
                pacsMenu.add(spawn('option', {
                    value: item.id,
                    title: item.aeTitle,
                    data: {
                        title: item.aeTitle,
                        port: item.queryRetrievePort
                    }
                }, item.label || item.aeTitle));
            }
            if (item.defaultQueryRetrievePacs) {
                dqr.selectedPacs = item.id;
                $selectPacsMenu.changeVal(item.id);
            }
        });
        // menuUpdate($selectPacsMenu);
    }

    function getPacsList(fn){
        return XNAT.xhr.get({
            url: XNAT.url.restUrl('/xapi/pacs'),
            success: function(json){
                if (isFunction(fn)) {
                    fn.apply(this, arguments);
                }
            }
        });
    }

    getPacsList(function fn(json){
        if (json.length) {
            renderPacsMenu(json);
        } else {
            dqr.canQuery = false;
            $('#pacs-status-indicator').append(
                spawn('span.pacs-warning',
                    '<i class="fa fa-warning"></i>&nbsp;No PACS configured'
                )
            );
            dqr.disableQueryForm($(document).find('#pacs-search-fields').find('input'));
        }
    });

    var $studyDateFromContainer = $('#study-date-from-container');
    var $studyDateToContainer   = $('#study-date-to-container');
    var $studyDateToday         = $('#study-date-today');

    function dateInputSetup$(id, name){
        return $.spawn('input|type=text', {
            id: id,
            name: name,
            className: 'study-date mono',
            size: '10',
            autocomplete: 'off',
            attr: { tabindex: '1' },
            placeholder: DATE_FORMAT
        });
        // return $(document).find('input[name='+name+']');
    }

    var DATE_MIN   = new Date('1900-01-01T00:00');
    var DATE_TODAY = new Date(XNAT.data.todaysDate.ISO + 'T00:00');

    var dateFromI, dateToI;

    function dateMask($input){
        $input.mask('9999-99-99', {
            placeholder: EMPTY_DATE,
            autoclear: false
        }).attr('autocomplete', 'off').select();
    }

    function validDate(dateVal){
        var dateSplit;
        if (dateVal) {
            dateSplit = new SplitDate(dateVal);
            return new Date(dateSplit.iso + 'T00:00');
        }
        return null;
    }

    function resolveInputValue(input){
        return (!input.value || input.value === EMPTY_DATE) ? '' : input.value;
    }

    // initialize or reset date range UI...
    // ...a very ham-fisted approach to handle this
    function initDatePickers(){

        var $studyDateFrom = dateInputSetup$('study-date-from', 'startDate');
        var $studyDateTo   = dateInputSetup$('study-date-to', 'endDate');

        $studyDateFromContainer.empty().append($studyDateFrom);
        $studyDateToContainer.empty().append($studyDateTo);

        $.fn.datepicker.language.en.dateFormat = DATE_FORMAT;

        function datepickerOpts(obj){
            return $.extend({
                language: 'en',
                minDate: DATE_MIN,
                maxDate: DATE_TODAY,
                // todayButton: DATE_TODAY,
                autoClose: false,
                keyboardNav: false,
                // range: true,
                // multipleDatesSeparator: '-',
                dateFormat: DATE_FORMAT
            }, obj || {});
        }

        // hold variable values in this object
        var dateRange = {
            $fromInput: $studyDateFrom,
            $toInput: $studyDateTo,
            fromInput: $studyDateFrom[0],
            toInput: $studyDateTo[0],
            fromValue: '',
            toValue: '',
            fromDate: DATE_MIN,
            toDate: DATE_TODAY,
            MIN: DATE_MIN,
            MAX: DATE_TODAY
        };


        function updateDateRangeValues(opts){

            opts = opts !== undef ? opts : {};

            // if a value is explicitly passed, use that to update the input
            if (opts.fromValue !== undef) {
                dateRange.fromValue       = opts.fromValue;
                dateRange.fromInput.value = opts.fromValue;
            }
            else {
                dateRange.fromValue = resolveInputValue(dateRange.fromInput);
            }
            //
            if (opts.toValue !== undef) {
                dateRange.toValue       = opts.toValue;
                dateRange.toInput.value = opts.toValue;
            }
            else {
                dateRange.toValue = resolveInputValue(dateRange.toInput);
            }

            // if a date object is explicitly passed, use that for dates
            if (opts.fromDate && opts.fromDate instanceof Date) {
                dateRange.fromDate = opts.fromDate;
            }
            else {
                dateRange.fromDate = validDate(dateRange.fromValue) || dateRange.fromDate;
            }
            //
            if (opts.toDate && opts.toDate instanceof Date) {
                dateRange.toDate = opts.toDate;
            }
            else {
                dateRange.toDate = validDate(dateRange.toValue) || dateRange.toDate;
            }

            // the dateRange object has been updated
        }


        // setup datpicker 'From' instance
        dateFromI                  = $studyDateFrom.off().datepicker(datepickerOpts({
            // startDate: DATE_TODAY,
            // minDate: DATE_MIN,
            // maxDate: validDate($studyDateTo.val()) || DATE_TODAY,
            // todayButton: true,
            // clearButton: true,
            onShow: function(fromPicker, done){

                updateDateRangeValues();

                if (!done) {
                    fromPicker.update({
                        minDate: dateRange.MIN,
                        maxDate: dateRange.toDate
                    });
                }
                else {
                    $studyDateFrom.val(dateRange.fromValue);
                    console.log('show "from" datepicker');
                }
                // dateMask($studyDateFrom);
            },
            onSelect: function(formattedDate, dateObj, picker){

                updateDateRangeValues({
                    fromValue: formattedDate
                });

                console.log('select "from" datepicker');
                // dateToI.update({
                //     minDate: dateObj
                // });
            }
        })).data('datepicker');
        //
        dqr.studyImportDateFrom = dateFromI;



        // setup datepicker 'To' instance
        dateToI = $studyDateTo.off().datepicker(datepickerOpts({
            // startDate: DATE_TODAY,
            // minDate: validDate($studyDateFrom.val()) || DATE_MIN,
            // maxDate: DATE_TODAY,
            // todayButton: true,
            // clearButton: true,
            onShow: function(toPicker, done){

                updateDateRangeValues();

                if (!done) {
                    toPicker.update({
                        minDate: dateRange.fromDate,
                        maxDate: dateRange.MAX
                    });
                }
                else {
                    // toPicker.date = validDate(dateToValue) || DATE_TODAY;
                    // toPicker.selectDate(validDate(dateToValue) || DATE_TODAY);
                    $studyDateTo.val(dateRange.toValue);

                    // dateMask($studyDateTo);

                    console.log('show "to" datepicker');
                }
            },
            onSelect: function(formattedDate, dateObj, picker){

                updateDateRangeValues({
                    toValue: formattedDate
                });

                console.log('select "to" datepicker');
                // dateFromI.update({
                //     maxDate: dateObj
                // });
            }
        })).data('datepicker');

        dqr.studyImportDateTo = dateToI;

        // $studyDateFrom.mask('9999-99-99');
        // $studyDateTo.mask('9999-99-99');


        // handle manual date field edits on a single date input
        function verifyDateInput(input){

            console.log('verifyDateInput');

            var inputValue = resolveInputValue(input);
            var inputDate  = validDate(inputValue);

            if (inputValue && !XNAT.validate.value(inputValue).is('date', 'iso').check()) {
                XNAT.dialog.message('Invalid Date', 'Please enter a valid date in the format <b>YYYY-MM-DD</b>.');
                $(input).focus().select();
                return false;
            }
            if (inputDate.getTime() > DATE_TODAY.getTime()) {
                XNAT.dialog.message('Invalid Date Range', 'Please enter a date between 1900-01-01 and today.');
                $(input).focus().select();
                return false;
            }

            return true;

        }

        // sanity check for both date inputs
        function verifyDateRange(){

            // make sure the 'from' date is not after the 'to' date and vice-versa
            var newFromValue = resolveInputValue(dateRange.fromInput);
            var newFromDate  = validDate(newFromValue) || dateRange.fromDate;

            var newToValue = resolveInputValue(dateRange.toInput);
            var newToDate  = validDate(newToValue) || dateRange.toDate;

            if (newFromDate.getTime() > newToDate.getTime()) {
                XNAT.dialog.message('Invalid Date Range', 'The "From" date cannot come after the "To" date.');
                return false;
            }

            if (newToDate.getTime() < newFromDate.getTime()) {
                XNAT.dialog.message('Invalid Date Range', 'The "To" date cannot come before the "From" date.');
                return false;
            }

            // if we've made it this far, we should be good to go.
            return true;

        }


        // The 'Date From' input
        $studyDateFrom.on('focusin', function(e){
            console.log('dateFrom focusin');

            dateToI.hide();

            updateDateRangeValues();

            dateRange.$fromInput.select();

        });
        //
        $studyDateFrom.on('change', function(e){
            console.log('dateFrom change');

            e.stopImmediatePropagation();

            if (verifyDateInput(this) && verifyDateRange()) {
                // all valid...
                updateDateRangeValues({
                    fromValue: this.value
                });
                dateFromI.selectDate(dateRange.fromDate);
                return true;
            }

            // this will always be 'false' for an 'onchange' event
            // if (dateRange.fromValue === resolveInputValue(this)) {
            //     return false;
            // }

            return false;

        });
        //
        $studyDateFrom.on('focusout', function(e){
            console.log('dateFrom focusout');

            if (dateRange.fromValue === resolveInputValue(this)) {
                return false;
            }

            updateDateRangeValues();

            // if (dateRange.fromValue) {
            //     if (verifyDateInput(this)) {
            //         return true;
            //     }
            //     else {
            //         return false;
            //     }
            // }
            // else {
            //     return false;
            // }
        });



        // The 'Date To' input
        $studyDateTo.on('focusin', function(e){
            console.log('dateTo focusin');
            // make sure the 'from' selector is closed
            dateFromI.hide();

            updateDateRangeValues();

            dateRange.$toInput.select();
        });
        //
        $studyDateTo.on('change', function(e){
            console.log('dateTo change');
            e.stopImmediatePropagation();

            if (verifyDateInput(this) && verifyDateRange()) {
                updateDateRangeValues({
                    toValue: this.value
                });
                dateToI.selectDate(dateRange.toDate);
                return true;
            }

            // this will always be 'false' for an 'onchange' event
            // if (dateRange.toValue === resolveInputValue(this)) {
            //     return false;
            // }

            return false;

        });
        //
        $studyDateTo.on('focusout', function(e){
            console.log('dateTo focusout');
            if (dateRange.toValue === resolveInputValue(this)) {
                return false;
            }

            updateDateRangeValues();

            // if (dateRange.toValue) {
            //     if (verifyDateInput(this)) {
            //         // this.value = dateToValue;
            //         return true;
            //     }
            //     else {
            //         return false;
            //     }
            // }
            // else {
            //     return false;
            // }
        });

    }

    dqr.pacsDate = pacsDate = function(dateString){
        // accepts YYYY-MM-DD and returns YYYYMMDD, which is a non-standard date format anywhere else but is accepted by PACS
        return dateString.replace(/-/g,'');
    };

    dqr.resetResults = resetResults = function(clear){

        dqr.searchResults    = [];
        dqr.allSearchResults = {};
        dqr.resultsTableData = [];
        dqr.selectedStudies  = {};

        dqr.scanTypesList = [];

        // collect study UIDs by series description
        dqr.studiesBySeriesDesc = {};

        if (clear) {
            $pacsSearchFields.find('input').val('');
            DATE_MIN = new Date('1900-01-01T00:00');
            initDatePickers();
        }

        // Since we're RESETTING the results...
        // ...show the query 'info' message...
        // ...and hide the 'no results' message.
        $pacsQueryMsg.hidden(!clear);
        $pacsNoResults.hidden(true);

        $searchResultsHeader.empty();
        $searchResultsBody.empty();
        $searchResultsSubmit.empty();
        // if these are defined, they should be initialized
        // dateFromI && dateFromI.update({ maxDate: DATE_TODAY, minDate: DATE_MIN });
        // dateToI && dateToI.update({ maxDate: DATE_TODAY, minDate: DATE_MIN });
    };

    // immediately render the 'query info' message
    resetResults(true);

    // reset the search results if changing source PACS...
    // ...but warn the user first
    $selectPacsMenu.on('change', function(e){
        const oldPacsId = dqr.selectedPacs;
        const pacsId = this.value;
        if (!pacsId || pacsId === oldPacsId) return false;
        var PACS = this.options[this.selectedIndex].textContent;

        function commitChange() {
            dqr.selectedPacs = pacsId;

            // ping the PACS to ensure it is up
            XNAT.xhr.getJSON(XNAT.url.restUrl('/xapi/pacs/'+pacsId+'/status'))
                .success(function(data){
                    if (data.successful) {
                        pingSuccess();
                    } else {
                        pingFailure(false);
                    }
                })
                .fail(function(e){
                    pingFailure(true);
                });

            // When they select a PACS, also update the SCP receiver selector
            dqr.initReceivers.updateForPacs(dqr.pacsObj[pacsId]);
        }

        function confirmChangePacsAndResetSearch(){
            XNAT.dialog.open({
                width: 400,
                title: 'Change Source PACS?',
                content: '' +
                    'Would you like to change the source PACS to <b>' + PACS + '</b>? Doing so will ' +
                    'reset all search results and clear the download list.',
                okLabel: 'Change PACS',
                okAction: function(){
                    resetResults(true);
                    commitChange();
                },
                cancelAction: function(){
                    console.log('not changing PACS from ' + oldPacsId + ' to ' + pacsId);
                    $selectPacsMenu.changeVal(oldPacsId);
                }
            });
        }

        function pingSuccess() {
            dqr.canQuery = true;
            dqr.enableQueryForm($(document).find('#pacs-search-fields').find('input'));
            $('#pacs-status-indicator').empty().css('color','#393').append(
                spawn('!',[
                    spawn('i.fa.fa-check'),
                    spawn('span', { style: { padding: '0 4px' }}, 'Ready to Query')
                ])
            );
        }

        function pingFailure(resetSearch) {
            dqr.canQuery = false;
            if (resetSearch) {
                dqr.resetResults(true);
            }
            dqr.disableQueryForm($(document).find('#pacs-search-fields').find('input'));
            $('#pacs-status-indicator').empty().css('color','#933').append(
                spawn('!',[
                    spawn('i.fa.fa-cancel'),
                    spawn('span', { style: { padding: '0 4px' }}, 'Error: PACS Not Available')
                ])
            );
            XNAT.dialog.message("Error: PACS is not responding to network ping. Contact your " + XNAT.app.siteId + " administrator");
        }

        // Show confirmation dialog if there are existing search results
        if (Object.keys(dqr.allSearchResults).length) {
            confirmChangePacsAndResetSearch();
        } else {
            commitChange();
        }
    });

    var relabelColumn = {
        td: {
            classes: 'nowrap',
            style: {
                background: '#f0f0f0'
            }
        },
        filter: function(){
            return '&nbsp;' || spawn('a.link.remap-auto-fill|href=#!', 'Auto-fill');
        }
    };

    function randomizer(length, prefix){
        var pre   = (isDefined(prefix)) ? prefix : 'rndx';
        var newId = pre + (Math.random() + 1).toString(36).substr(2, 8);
        if (isDefined(length)) {
            if (newId.length > length) {
                return newId.slice(0, length);
            }
            else {
                return randomizer(length, newId);
            }
        }
        return newId;
    }

    // //keep this arround for future reference
    // function getBySeriesDesc(studies){
    //     console.log(studies);
    //     dqr.selectedStudies = {};
    //     forOwn(studies, function(siuid, study){
    //         dqr.selectedStudies[siuid] = study.results.map(function(series, i){
    //             var seriesDesc = series.seriesDescription;
    //             dqr.studiesBySeriesDesc[seriesDesc] = dqr.studiesBySeriesDesc[seriesDesc] || [];
    //             if (dqr.studiesBySeriesDesc[seriesDesc].indexOf(siuid) === -1) {
    //                 dqr.studiesBySeriesDesc[seriesDesc].push(siuid);
    //             }
    //             return {
    //                 studyInstanceUid: siuid,
    //                 seriesInstanceUid: series.seriesInstanceUid,
    //                 seriesDescription: series.seriesDescription,
    //                 seriesNumber: series.seriesNumber,
    //                 modality: series.modality
    //             }
    //         });
    //     })
    // }

    function getStudies(pacsId, studyUIDs){
        var UIDS = [].concat(studyUIDs);
        var requestData = {
            pacsId: pacsId,
            studyInstanceUids: UIDS
        };
        // var URL  = XNAT.url.restUrl('/xapi/dqr/seriesInfo/pacs/' + pacsId + '/studies');
        var URL = XNAT.url.csrfUrl('/xapi/dqr/query/series');
        return XNAT.xhr.postJSON({
            url: URL,
            data: JSON.stringify(requestData),
            success: function(studies){
                console.log(studies);
            }
        });
    }

    function collectScanTypes(json){

        console.log('collectScanTypes');
        console.log(json);

        dqr.seriesDescriptions = {};

        var studyCount = 0;

        // getBySeriesDesc(json);

        forOwn(json, function(uid, obj){
            forEach(obj.results, function(item){
                var seriesDescriptionKey    = item.seriesDescription || NONE;
                var seriesDescriptionItem   = dqr.seriesDescriptions[seriesDescriptionKey] || {};
                seriesDescriptionItem.name  = (seriesDescriptionKey);
                seriesDescriptionItem.count = (seriesDescriptionItem.count || 0);
                seriesDescriptionItem.count++;
                seriesDescriptionItem.studyUIDs = seriesDescriptionItem.studyUIDs || [];
                if (seriesDescriptionItem.studyUIDs.indexOf(item.study.studyInstanceUid) === -1) {
                    seriesDescriptionItem.studyUIDs.push(item.study.studyInstanceUid);
                }
                seriesDescriptionItem.seriesUIDs             = (seriesDescriptionItem.seriesUIDs || []).concat(item.seriesInstanceUid);
                dqr.seriesDescriptions[seriesDescriptionKey] = seriesDescriptionItem;
            });
            studyCount += 1;
        });

        // RESET SCAN TYPES LIST
        dqr.scanTypesList = [];

        forOwn(dqr.seriesDescriptions, function(name, obj){
            dqr.scanTypesList.push(obj);
        });

        return dqr.scanTypesList;

    }

    function scanTypesListDisplay(){
        var scanTypesList = sortObjects(dqr.scanTypesList, 'name');
        function itemId(item){
            return 'study_desc_' + (item.name || '').replace(/[\W\s]/g, '_');
        }
        var scanTypesTable = XNAT.table.dataTable(scanTypesList, {
            id: 'scan-types-list',
            table: {
                classes: 'rows-only compact highlight'
            },
            sortable: 'name, count',
            columns: {
                CHECKBOX: {
                    label: '&nbsp;',
                    td: {
                        classes: 'center'
                    },
                    filter: function(){
                        return spawn('div.center', [
                            ['input.selectable-all|type=checkbox', {
                                value: '*',
                                checked: true,
                                style: { width: 'inherit' }
                            }]
                        ]);
                    },
                    apply: function(){
                        var item = this;
                        return spawn('div.center', [
                            ['input.selectable-one.select-scan-type|type=checkbox', {
                                value: item.name,
                                id: itemId(item),
                                checked: true
                            }]
                        ]);
                    }
                },
                name: {
                    label: 'Series Descriptions',
                    filter: !!(scanTypesList.length > 6),
                    apply: function(){
                        var item = this;
                        return spawn('label.scan-type-label', {
                            attr: { 'for': itemId(item) }
                        }, item.name);
                    }
                },
                COUNT: {
                    label: 'Study Count',
                    apply: function(){
                        var item = this;
                        return spawn('div.center.mono', item.studyUIDs.length + '');
                    }
                }
            }
        });

        return scanTypesTable.get();

    }


    function importSessionsOfSelectedTypeToProject(scanTypesDialog){

        var $searchResultsTable = $('#all-search-results');
        var $selectedSessions   = $searchResultsTable.find('input.select-session:checked').filter(':visible');

        var studyUIDs = $selectedSessions.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        console.log(studyUIDs);

        // Receiver AE Title will be in #ae-menu if PACS is set to DIMSE,
        //   else pacs.aeTitle if DICOMweb
        const pacsId = dqr.selectedPacs;
        const pacs = dqr.pacsObj[pacsId];
        const ae = pacs.dicomWebEnabled ? [pacs.aeTitle, "0"] : $('#ae-menu').val().split(':'); // ae produces an array [ title, port ]

        var projectId = window.projectId || getQueryStringValue('project');

        var selectedScanTypes = $('#scan-types-list').find('input.select-scan-type:checked').filter(':visible');

        var scanTypes = selectedScanTypes.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        if (!scanTypes.length) {
            XNAT.dialog.message(false, 'Please select at least one series type to import.');
            return false;
        }

        var jsonModel = {
            "aeTitle": "string",
            "port": 0,
            "forceImport": true,
            "pacsId": 0,
            "projectId": "string",
            "studies": [
                {
                    "anonScript": "string",
                    "relabelMap": {
                        "additionalProp1": "string",
                        "additionalProp2": "string",
                        "additionalProp3": "string"
                    },
                    "seriesDescriptions": [
                        "string"
                    ],
                    "seriesInstanceUids": [
                        "string"
                    ],
                    "studyInstanceUid": "string"
                }
            ]
        };


        var jsonData = {
            pacsId: pacsId,
            aeTitle: ae[0],
            port: ae[1],
            projectId: projectId,
            forceImport: true,
            studies: []
        };

        // SETUP THE FINAL SUBMISSION JSON
        forEach(studyUIDs, function(uid){

            var importObj = {
                studyInstanceUid: uid,
                seriesDescriptions: [],
                seriesInstanceUids: []
            };

            importObj.seriesDescriptions = scanTypes.filter(function(type){
                return dqr.seriesDescriptions[type].studyUIDs.indexOf(uid) !== -1;
            }).map(function(type){
                return type === NONE ? '' : type;
            });

            // don't include this item if there are no associated series descriptions
            if (importObj.seriesDescriptions.length) {

                forEach(importObj.seriesDescriptions, function(type){
                    importObj.seriesInstanceUids =
                        importObj.seriesInstanceUids.concat(dqr.seriesDescriptions[type || NONE].seriesUIDs || []);
                });

                importObj.relabelMap = (function(){
                    var relabelMapTemp = {};
                    var $importRow     = $searchResultsTable.find('tr[data-uid="' + uid + '"]');
                    $importRow.find('input.relabel').each(function(){
                        // only add to the relabelMap object if there's a value
                        this.value && (relabelMapTemp[this.title] = this.value || '');
                    });
                    return relabelMapTemp;
                })();

                jsonData.studies.push(importObj);
            }

        });

        console.log(jsonData);

        XNAT.xhr.postJSON({
            url: XNAT.url.csrfUrl('/xapi/dqr/import'),
            data: JSON.stringify(jsonData),
            success: function(){
                window.jsdebug && console.log(arguments);
                XNAT.dialog.message({
                    title: ' ',
                    width: 400,
                    content: (function(){
                        return '' +
                            '<div class="success">PACS data has been queued for import.</div>' +
                            '<p style="margin:1em;">' +
                            'Close this dialog to return to the search results. You can also ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/app/template/Page.vm?view=dqr/queue&role=dqr#tab=queue') + '">' +
                            'check on the import progress in the queue</a> or ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/data/projects/' + projectId) + '">' +
                            'go back to the project page.</a>' +
                            '</p>';
                    })(),
                    okLabel: 'Close',
                    okAction: function(obj){
                        scanTypesDialog.close();
                        dqr.resetResults(true);
                        // XNAT.dialog.loading.open();
                        // window.location.reload(true);
                    }
                });
            },
            failure: function(){
                XNAT.dialog.message('Error', 'An error has occurred during data import.');
                console.warn(arguments);
            }
        });

    }


    function scanTypesDialog(pacsId, studyUIDs){
        getStudies(pacsId, studyUIDs).done(function(studies){
            collectScanTypes(studies);
            var scanTypesTable = scanTypesListDisplay();

            // hide loading indicator and reset import button
            xmodal.loading.close();
            $(document).find('#import-selected-sessions')
                .removeClass('disabled')
                .prop('disabled',false)
                .data('clicked',false);

            XNAT.dialog.open({
                title: 'Import from PACS',
                content: scanTypesTable,
                afterShow: function(dlg){
                    XNAT.plugin.dqr.selectableItems(dlg.body$.find('#scan-types-list'));
                },
                buttons: [
                    {
                        label: 'Import Selected',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            importSessionsOfSelectedTypeToProject(obj);
                        }
                    },
                    {
                        label: 'Cancel',
                        close: true
                    }
                ]
            });
        });

    }

    dqr.renderResultsTable = renderResultsTable = function(json){

        // console.log(json);

        dqr.searchResults    = json;
        dqr.allSearchResults = {};

        // hide the 'query info' message since we've just done a query
        $pacsQueryMsg.hidden(true);
        $pacsNoResults.hidden(true);
        $searchResultsBody.empty();

        if (json === false) {
            resetResults();
            $pacsQueryMsg.hidden(false);
            return undef;
        }

        // `json` must be an array containing SOMETHING
        // in order to render the results table
        if (!isArray(json) || !json.length) {
            resetResults();
            $pacsNoResults.hidden(false);
            return undef;
        }

        // $pacsNoResults.hidden(true);

        forEach(json, function(item){
            item.relabelMap = item.relabelMap || {};
            dqr.allSearchResults[item.studyInstanceUid] = item;
        });

        // console.log(dqr.allSearchResults);

        function ckbxLabel(ckbx){
            return spawn('label.center', {
                style: { display: 'block', textAlign: 'center' }
            }, ckbx);
        }

        function filterInput(name){
            return spawn('input.filter-input|type=text', {
                title: 'filter:' + name,
                style: { padding: '4px 6px', border: '1px solid #ccc' },
                data: { filter: name }
            });
        }

        var WIDTHS = {
            select: '4%',
            name: '14%',
            studyId: '14%',
            studyDescription: '14%',
            xnatSubject: '16%',
            xnatSession: '16%',
            date: '12%',
            mod: '10%',
            acc: '14%'
        };

        var FILTER_TH_PADDING = '5px 7px';

        function renderHeader(){
            return XNAT.table.dataTable([], {
                container: $searchResultsHeader,
                body: false,
                table: {
                    classes: 'compact table-group-member',
                    style: { tableLayout: 'fixed' }
                },
                overflowY: 'scroll',
                columns: {
                    SELECT_SESSIONS: {
                        label: '<label for="toggle-all-sessions">&nbsp;</label>',
                        th: {
                            style: { width: WIDTHS.select }
                        },
                        filter: function(){
                            var ckbx = spawn('input#toggle-all-sessions.selectable-all|type=checkbox', {
                                checked: false,
                                value: '*',
                                style: { width: 'inherit' }
                            });
                            return ckbxLabel(ckbx);
                        }
                    },
                    accessionNumber: {
                        label: 'Accession Number',
                        filter: function(){
                            return filterInput('accessionNumber');
                        },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.acc, padding: FILTER_TH_PADDING }
                        }
                    },
                    patientName: {
                        label: 'Patient Name',
                        filter: function(){
                            return filterInput('patientName');
                        },
                        // <input class="filter-data" type="text" title="patientName:filter" placeholder="Filter by Patient Name">
                        // filter: function(){
                        //     return spawn('input')
                        // },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.name, padding: FILTER_TH_PADDING }
                        }
                    },
                    // studyId: {
                    //     label: 'Study ID',
                    //     filter: function(){
                    //         return filterInput('studyId');
                    //     },
                    //     sort: true,
                    //     th: {
                    //         style: { width: WIDTHS.studyId, padding: FILTER_TH_PADDING }
                    //     }
                    // },
                    studyDescription: {
                        label: 'Study Description',
                        filter: function(){
                            return filterInput('studyDescription');
                        },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.studyDescription, padding: FILTER_TH_PADDING }
                        }
                    },
                    studyDate: {
                        label: 'Study Date',
                        filter: function(){
                            return filterInput('studyDate');
                        },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.date, padding: FILTER_TH_PADDING }
                        }
                    },
                    modalitiesInStudy: {
                        label: 'Modality',
                        filter: function(){
                            return filterInput('modalitiesInStudy');
                        },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.mod, padding: FILTER_TH_PADDING }
                        }
                    },
                    xnatSubject: extend(true, {}, relabelColumn, {
                        label: '' +
                            // '<i class="fa fa-angle-double-right"></i>&nbsp;&nbsp;&nbsp;&nbsp;' +
                            window.subjectDisplay +
                            '',
                        th: {
                            title: 'Subject',
                            style: { width: WIDTHS.xnatSubject }
                        }
                    }),
                    xnatSession: extend(true, {}, relabelColumn, {
                        label: '' +
                            // '<i class="fa fa-angle-double-right"></i>&nbsp;&nbsp;&nbsp;&nbsp;' +
                            window.sessionDisplay +
                            '',
                        th: {
                            title: 'Session',
                            style: { width: WIDTHS.xnatSession }
                        }
                    })
                }

            });
        }

        if ($searchResultsHeader.find('table').length === 0) {
            renderHeader();
        }

        function renderBody(){
            return XNAT.table.dataTable(dqr.allSearchResults, {
                container: $searchResultsBody,
                header: false,
                table: {
                    id: 'all-search-results',
                    classes: 'compact table-data highlight',
                    style: { tableLayout: 'fixed' },
                    on: [
                        // ['change', 'input.select-session', function(){
                        //     var uid = this.value;
                        //     console.log(uid);
                        //     dqr.allSearchResults[uid].checked = this.checked;
                        // }],
                        ['blur', 'input.relabel', function(e){
                            var uid = $(this).closest('tr').data('uid');
                            console.log(uid);
                            // if (this.value) {
                                dqr.allSearchResults[uid].relabelMap[this.title] = this.value;
                            // }
                        }],
                        ['click', 'td:has(.select-row)', function(e){
                            if (!$(e.target).is('input.select-session')) {
                                $(this).closest('tr').find('input.select-session').trigger('click');
                            }
                        }]
                    ]
                },
                overflowY: 'scroll',
                maxHeight: '650px',
                trs: function(tr, data){
                    $(tr).addClass('filter-data-row');
                },
                columns: {
                    _studyInstanceUid: '~data-uid',  // this will add a [data-uid="1.234.567890"] attribute to each row's <tr> element
                    SELECT_SESSIONS: {
                        label: false,
                        td: { style: { width: WIDTHS.select } },
                        apply: function(){
                            var uid  = this.studyInstanceUid;
                            var ckbx = spawn('input.select-session.selectable-one|type=checkbox', {
                                value: uid,
                                checked: firstDefined(dqr.allSearchResults[uid].checked, false)
                            });
                            return ckbxLabel(spawn('div.center.select-row', [ckbx]));
                        }
                    },
                    accessionNumber: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.acc }
                        },
                        apply: function(){
                            var accNum = this.accessionNumber || '';
                            return spawn('div.truncate.select-row.filter-data-item', {
                                title: accNum,
                                data: { filter: 'accessionNumber' }
                            }, accNum);
                        }
                    },
                    patientName: {
                        label: false,
                        td: { style: { width: WIDTHS.name } },
                        apply: function(){
                            // var patientName = this.patient.name.lastNameCommaFirstName.replace(/,/, '^');
                            var patientName = this.patient.name || '';
                            /^null$/.test(patientName) && (patientName = '');
                            return spawn('div.truncate.select-row.filter-data-item', {
                                title: patientName,
                                data: { filter: 'patientName' }
                            }, patientName);
                        }
                    },
                    studyDescription: {
                        label: false,
                        td: { width: WIDTHS.studyDescription },
                        apply: function(){
                            var studyDescription = this.studyDescription;
                            return spawn('div.truncate.select-row.filter-data-item',{
                                title: studyDescription,
                                data: { filter: 'studyDescription' }
                            }, studyDescription)
                        }
                    },
                    // studyId: {
                    //     label: false,
                    //     td: {
                    //         style: { width: WIDTHS.studyId }
                    //     },
                    //     apply: function(){
                    //         var studyId = this.studyId;
                    //         return spawn('div.truncate.select-row.filter-data-item', {
                    //             title: studyId,
                    //             data: { filter: 'studyId' }
                    //         }, studyId);
                    //     }
                    // },
                    studyDate: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.date }
                        },
                        apply: function(){
                            var studyDateStr = this.studyDate + '';
                            studyDateStr = studyDateStr.replace(/-/g,'');
                            return spawn('div.center.mono.select-row.filter-data-item', {
                                data: { filter: 'studyDate' }
                            }, this.studyDate ? [
                                // ['i.hidden.sort', studyDateStr],
                                studyDateStr.slice(0, 4) + '-' + studyDateStr.slice(4, 6) + '-' + studyDateStr.slice(6, 8)
                            ] : '<i class="hidden">0</i>&ndash;');
                        }
                    },
                    modalitiesInStudy: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.mod }
                        },
                        apply: function(){
                            return spawn('div.select-row.filter-data-item', {
                                data: { filter: 'modalitiesInStudy' }
                            }, [].concat(this.modalitiesInStudy).join(', '));
                        }
                    },
                    xnatSubject: extend(true, {}, relabelColumn, {
                        label: false,
                        td: {
                            style: { width: WIDTHS.xnatSubject }
                        },
                        filter: false,
                        apply: function(){
                            return spawn('input.relabel.relabel-patient-name|type=text|tabindex=1', {
                                title: 'Subject',
                                value: this.relabelMap ? (this.relabelMap['Subject'] || '') : ''
                            });
                        }
                    }),
                    xnatSession: extend(true, {}, relabelColumn, {
                        label: false,
                        td: {
                            style: { width: WIDTHS.xnatSession }
                        },
                        filter: false,
                        apply: function(){
                            return spawn('input.relabel.relabel-study-id|type=text|tabindex=1', {
                                title: 'Session',
                                value: this.relabelMap ? (this.relabelMap['Session'] || '') : ''
                            });
                        }
                    })
                }
            });
        }

        renderBody();

        // init the selectable stuff
        XNAT.plugin.dqr.selectableItems($pacsSearchResults);

        // init new filter method
        XNAT.plugin.dqr.filterableItems($pacsSearchResults);

        var resetSearchButton = spawn('button#clear-search-results.clear-search-results.btn|type=button',{
            html: 'Clear Search',
            on: [
                ['click', function(e){
                    e.preventDefault();
                    resetResults(true);
                }]
            ]
        });

        var beginImportButton = spawn('button#import-selected-sessions.btn.btn1|type=button', {
            html: 'Begin Import',
            data: { 'clicked': false },
            on: [
                ['click', function(e){
                    e.preventDefault();
                    if ($(this).data('clicked') === 'clicked') return false;
                    var studyUIDs = [];
                    $pacsSearchResults.find('input.select-session:checked').filter(':visible').each(function(){
                        studyUIDs.push(this.value);
                    });
                    if (!studyUIDs.length) {
                        XNAT.dialog.message(false, 'Please select at least one study to import.');
                        return false;
                    }
                    // if series are selected, disable search button to prevent repeated clicks and show loading indicator
                    $(this).prop('disabled','disabled').addClass('disabled').data('clicked','clicked');
                    dqr.selectedPacs = dqr.selectedPacs || $selectPacsMenu.val();
                    xmodal.loading.open('Querying '+ dqr.pacsObj[dqr.selectedPacs].aeTitle +' for series information');

                    scanTypesDialog(dqr.selectedPacs, studyUIDs);
                }]
            ]
        });

        var importBlockedButton = spawn('button.btn.btn1.disabled|type=button',{
            html: 'Cannot Import',
            on: [
                ['click',function(e){
                    e.preventDefault();
                    XNAT.ui.dialog.message('Cannot import data with your current configuration. Please contact a site administrator.');
                }]
            ]
        });

        $searchResultsSubmit.empty().spawn('div.pull-right', {
            style: { 'padding-top': '20px' }
        }, [
            resetSearchButton,
            '&nbsp;',
            (dqr.canImport)? beginImportButton : importBlockedButton
        ]);

    };

    function pingPACS(id, callback){
        if (!id) {
            console.warn('id required');
            return;
        }
        return XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/xapi/pacs/' + id + '/status'),
            success: function(data){
                if (data && data.enabled) {
                    if (isFunction(callback)) {
                        callback.call(this, id);
                    }
                }
            },
            failure: function(){
                console.warn('PACS ping failed');
                console.warn(arguments);
            }
        });
    }

    dqr.showReceiverWarning = showReceiverWarning = function(e){
        e.preventDefault();
        XNAT.dialog.message({
            title: 'SCP Receiver Configuration Error',
            content: '<p>There is no SCP receiver configured to allow DQR imports from a PACS system. A system adminstrator must configure a DQR-enabled receiver.</p>'+
                '<p>You can still query PACS data in this configuration.</p>' +
                '<p><a href="https://wiki.xnat.org/xnat-tools/dicom-query-retrieve-plugin" target="_blank">See documentation</a></p>'
        });
    };

    dqr.showPacsWarning = showPacsWarning = function(e){
        e.preventDefault();
        XNAT.dialog.message({
            title: 'DICOM AE Configuration Error',
            content: '<p>There is no DICOM AE or PACS configured to allow DQR queries from this system. A system adminstrator must configure a queryable DICOM AE.</p>'+
                '<p><a href="https://wiki.xnat.org/xnat-tools/dicom-query-retrieve-plugin" target="_blank">See documentation</a></p>'
        });
    };

    dqr.initReceivers = initReceivers = function(){
        function renderScpSelector(receivers){

            var aeMenu$         = $.spawn('select#ae-menu');
            var aeMenu0         = aeMenu$[0];
            var defaultReceiver = receivers[0];
            var defaultLabel    = defaultReceiver.aeTitle + ':' + defaultReceiver.port;
            var hasReceiver     = false;
            var receiverMap     = {};

            // toggleRemapping(/^true$/.test(defaultReceiver.customProcessing));

            forEach(receivers, function(item, i){
                var AE = item.aeTitle + ':' + item.port;
                window.jsdebug && console.log(AE);
                receiverMap[AE] = item;
                // only add 'dqr' receivers to the menu
                if (/dqr/i.test(item.identifier) && item.customProcessing) {
                    !hasReceiver && (hasReceiver = item.enabled);
                    aeMenu$.spawn('option.receiver', {
                        title: AE,
                        value: AE,
                        data: {
                            id: (item.id + ''),
                            identifier: item.identifier,
                            processing: item.customProcessing
                        },
                        disabled: !item.enabled
                    }, AE);
                }
            });

            var relabelInputs$ = $pacsSearchResults.find('input.relabel');

            function toggleRemapping(e){

                try {
                    var selectedOption = this.value;
                    var doProcessing   = (hasReceiver && selectedOption) ?
                        /^true$/.test(receiverMap[selectedOption].customProcessing) :
                        false;

                    if (window.jsdebug) {
                        console.log(relabelInputs$);
                        console.log(selectedOption);
                    }

                    if (!doProcessing) {
                        relabelInputs$.prop('disabled', true).css('opacity', '0.6').val('');
                    }
                    else {
                        relabelInputs$.prop('disabled', false).css('opacity', '1');
                    }
                }
                catch(e) {
                    window.jsdebug && console.warn(e);
                }

            }

            dqr.initReceivers.updateForPacs = function(pacs) {
                if (pacs === undef || pacs.dicomWebEnabled) {
                    dqr.canImport = pacs !== undef;
                    $('#scp-receiver-selector').empty();
                    relabelInputs$.prop('disabled', false).css('opacity', '1');
                } else if (!hasReceiver) {
                    // aeMenu$.spawn('option.disabled|disabled|selected|value=""', '(none available)');
                    // aeMenu0.disabled = true;
                    dqr.canImport = false;

                    $('#scp-receiver-selector').empty().append(spawn('span.receiver-warning', [
                        '<i class="fa fa-warning"></i>&nbsp;Error: Cannot import'
                    ]));
                } else {
                    dqr.canImport = true;
                    toggleRemapping.call(aeMenu0);
                    $('#scp-receiver-selector').empty().append(spawn('span', {
                        on: [['change', '#ae-menu', toggleRemapping]]
                    }, [
                        'Select SCP Receiver: ',
                        aeMenu0
                    ]));
                }
            }

            dqr.initReceivers.updateForPacs(dqr.pacsObj[$selectPacsMenu.val()]);

        }

        XNAT.xhr.get({
            url: XNAT.url.restUrl('/xapi/dicomscp'),
            success: function(json){
                var aeExample = {
                    'enabled': true,
                    'identifier': 'dqrObjectIdentifier',
                    'port': 8104,
                    'id': 1,
                    'aeTitle': 'XNAT',
                    'customProcessing': true
                };
                renderScpSelector(json);
            },
            failure: function(e){
                console.warn('Could not retrieve SCP Receivers');
            }
        });
    };

    $(document).on('click','.receiver-warning',function(e){
        dqr.showReceiverWarning(e);
    });

    $(document).on('click','.pacs-warning',function(e){
        dqr.showPacsWarning(e);
    });

    $(document).on('click','.clear-search-results',function(e){
        dqr.resetResults(true);
    });

    dqr.enableQueryForm = enableQueryForm = function(inputs){
        inputs.prop('disabled',false);
        $searchSubmit.removeClass('disabled').prop('disabled',false);
        $('#import-csv').find('button').removeClass('disabled').prop('disabled',false);
        dqr.initSearchPage('refresh');
    };

    dqr.disableQueryForm = disableQueryForm = function(inputs){
        inputs.prop('disabled',true);
        $searchSubmit.addClass('disabled').prop('disabled','disabled').off('click');
        $('#import-csv').off('click').find('button').addClass('disabled').prop('disabled','disabled');
    };

    dqr.searchPacs = searchPACS = function(id){

        var selectedPacs = id || dqr.selectedPacs || $selectPacsMenu.val();

        var searchCriteria = {
            pacsId: selectedPacs,
            studyDateRange: {}
        };

        $pacsSearchFields.find('input').not('.ignore').serializeArray().forEach(function(param, i){
            // skip fields that start with '!'
            if (param.name.charAt(0) !== '!' && param.value) {
                searchCriteria[param.name] = param.value || '';
            }
        });

        searchCriteria.pacsId = selectedPacs;

        // enable partial matches on name searches by default
        if (searchCriteria.patientName) {
            searchCriteria.patientName = '*' + searchCriteria.patientName + '*';
        }

        // transform date to expected format and reposition in object structure
        if (searchCriteria.startDate) {
            searchCriteria.studyDateRange.start = pacsDate(searchCriteria.startDate);
            delete searchCriteria.startDate;
        }
        if (searchCriteria.endDate) {
            searchCriteria.studyDateRange.end = pacsDate(searchCriteria.endDate);
            delete searchCriteria.endDate;
        }

        console.log(searchCriteria);

        // var searchUrl = XNAT.url.csrfUrl('/xapi/pacs/' + selectedPacs + '/studies', {}, false);
        var searchUrl = XNAT.url.csrfUrl('/xapi/dqr/query/studies');

        // console.log(searchUrl);

        return XNAT.xhr.postJSON({
            url: searchUrl,
            data: JSON.stringify(searchCriteria),
            success: function(json){
                renderResultsTable(json);
            },
            failure: function(){
                console.warn('Error:');
                console.warn(arguments);
                renderResultsTable(null);
                XNAT.dialog.message(false, 'No results were found for the specified search criteria.');
            }
        });

    }

    // process anon script and insert it into `dqr.allSearchResults` object
    function processAnonScript(anonScript){
        // TODO: write this function
    }

    XNAT.plugin.dqr.submitCsvForm = function(formData){
        XNAT.dialog.closeAll();

        XNAT.xhr.post({
            url: XNAT.url.csrfUrl('/xapi/dqr/query/batch'),
            data: formData,
            async: false,
            cache: false,
            contentType: false,
            processData: false,
            success: function(data){
                xmodal.loading.close();
                var results = [];
                if (data.length) {
                    // pluck the data out of the response
                    forEach(data, function(item){
                        var searchId                   = randomizer(16, 'dqrx' + (Date.now() + '').slice(-8, -2) + 'x');
                        dqr.searchIds                  = dqr.searchIds || {};
                        dqr.searchIds[searchId]        = item;
                        dqr.csvSearchResults           = dqr.csvSearchResults || {};
                        dqr.csvSearchResults[searchId] = item;
                        // dqr.allSearchResults[searchId] = {
                        //     anonScript: item.anonScript || ''
                        // };
                        console.log(dqr.csvSearchResults);
                        forEach(item.studies, function(study){
                            study.searchId   = searchId;
                            study.relabelMap = item.relabelMap;
                            results.push(study);
                        });
                    });
                    console.log(results);
                    renderResultsTable(results);
                }
                else {
                    XNAT.ui.dialog.message(
                        'CSV Query Results',
                        'No matching data was found for your CSV query.'
                    );
                }
            },
            failure: function(e){
                xmodal.loading.close();
                console.warn('error importing CSV');
                console.warn(arguments);
                if (e.status === 400) {
                    XNAT.dialog.message('Error', e.responseText);
                } else {
                    XNAT.dialog.message('Error', 'An error occurred processing the CSV file. Please ensure that it\'s a valid CSV file and formatted properly for PACS queries.');
                }
            }
        });
    };

    dqr.initSearchPage = function(refresh){
        if (!refresh) initReceivers();

        // initialize date fields *after* DOM loads
        $(document).ready(function(){
            initDatePickers();
        });

        // handle CSV import
        $(document).off('click', '#import-csv').on('click', '#import-csv', function(e){
            e.preventDefault();
            // check for selected PACS
            if (!$selectPacsMenu.val()) {
                XNAT.dialog.message('PACS Selection Error','Please select a PACS to query before uploading a CSV file.');
                return false;
            }

            // form with file input to render
            var fileForm = spawn('form#csv-upload|name=csv_upload', {
                style: { padding: '10px', fontSize: '13px' },
                onsubmit: function(e){
                    e.preventDefault();
                    var formData = new FormData(this);
                    XNAT.plugin.dqr.submitCsvForm(formData);
                }
            }, [
                ['p', 'This interface lets you upload a CSV-formatted list of queries to run on your PACS. <a href="https://wiki.xnat.org/xnat-tools/dicom-query-retrieve-plugin/using-dqr-bulk-querying-and-importing-via-csv-file" target="_blank">See DQR Documentation</a> for assistance with this feature.'],
                ['div.padded-block',[
                    ['p', 'Select a CSV file to upload:'],
                    ['input|type=hidden|name=pacsId', {
                        // pick up value of selected PACS
                        value: $('#select-pacs').val()
                    }],
                    ['input|type=file|name=csv_to_store|accept=.csv']
                ]]
            ]);
            XNAT.dialog.open({
                title: 'Start New Search via CSV File',
                width: 480,
                content: fileForm,
                okLabel: 'Upload',
                okClose: false,
                okAction: function(obj){
                    dqr.resetResults(true);
                    xmodal.loading.open('Querying PACS...');
                    $(document).find('#csv-upload').submit();
                }
            });
        });

        // enable search submission
        $searchSubmit.off('click').on('click', function(e){
            e.preventDefault();

            var self = this;

            var searchResultsTable$ = $('#all-search-results');

            function doSearch(){
                dqr.selectedPacs = $selectPacsMenu.val();
                if (!dqr.selectedPacs) {
                    XNAT.dialog.message(false, 'Please select a PACS to query.', {
                        okAction: function(obj){
                            // $selectPacsMenu.click();
                            // menuUpdate($selectPacsMenu);
                        }
                    });
                    return;
                }
                pingPACS(dqr.selectedPacs, function(){
                    renderResultsTable(false);
                    searchPACS.apply(self, arguments);
                });
            }

            // confirm clearing of search results before doing new search
            if (searchResultsTable$.find('tr[data-uid]').length) {
                XNAT.dialog.confirm({
                    title: 'Perform New Search?',
                    content: '' +
                        'Performing a new search will clear the results table below. ' +
                        'Would you like to clear the current results and continue?',
                    okLabel: 'Continue',
                    okAction: function(dialog){
                        doSearch();
                    }
                })
            }
            // if there are no results displayed, just do it
            else {
                doSearch()
            }

        });
    };

    XNAT.plugin.dqr = dqr;

    XNAT.plugin.dqr.initSearchPage();

}));

//# sourceURL=browsertools://scripts/xnat/plugin/dqr/import.js