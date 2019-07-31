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
            if (item.queryable) {
                pacsMenu.add(spawn('option', {
                    value: item.id,
                    title: item.aeTitle
                }, item.label || item.aeTitle));
            }
            if (item.defaultQueryRetrievePacs) {
                dqr.selectedPacs = item.id;
                $selectPacsMenu.changeVal(item.id);
            }
        });
        menuUpdate($selectPacsMenu);
    }

    function getPacsList(fn){
        return XNAT.xhr.get({
            url: XNAT.url.restUrl('/data/pacs'),
            success: function(json){
                if (isFunction(fn)) {
                    fn.apply(this, arguments);
                }
            }
        });
    }

    getPacsList(function fn(json){
        renderPacsMenu(json.ResultSet.Result);
    });

    var $studyDateFromContainer = $('#study-date-from-container');
    var $studyDateToContainer   = $('#study-date-to-container');
    var $studyDateToday = $('#study-date-today');

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

        var $studyDateFrom = dateInputSetup$('study-date-from', 'studyDateFrom');
        var $studyDateTo   = dateInputSetup$('study-date-to', 'studyDateTo');

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

            console.log(dateRange);

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
        window.studyImportDateFrom = dateFromI;



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

        window.studyImportDateTo = dateToI;

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

        // // click the 'today' button to fill in today's date
        // $studyDateToday.off().on('click', function(e){
        //
        //     e.preventDefault();
        //
        //     $studyDateFrom.val(XNAT.data.todaysDate.ISO);
        //     // dateFromI.date = DATE_TODAY;
        //     // dateFromI.update({
        //     //     startDate: DATE_TODAY
        //     // });
        //
        //     $studyDateTo.val(XNAT.data.todaysDate.ISO);
        //     // dateToI.date = DATE_TODAY;
        //     // dateToI.update({
        //     //     startDate: DATE_TODAY
        //     // });
        //
        // });

    }


    function resetResults(clear){

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
    }

    // immediately render the 'query info' message
    resetResults(true);

    // and bind to the 'Clear Search Results' button
    $('#clear-search-results').on('click', function(){
        resetResults(true);
    });

    // reset the search results if changing source PACS...
    // ...but warn the user first
    $selectPacsMenu.on('change', function(e){
        var pacsId = this.value;
        if (!pacsId || pacsId === dqr.selectedPacs) return false;
        var PACS = this.options[this.selectedIndex].textContent;
        // only show the dialog if there are are items in the dqr['allSearchResults'] object
        if (Object.keys(dqr.allSearchResults).length) {
            XNAT.dialog.open({
                width: 400,
                title: 'Change Source PACS?',
                content: '' +
                    'Would you like to change the source PACS to <b>' + PACS + '</b>? Doing so will ' +
                    'reset all search results and clear the download list.',
                okLabel: 'Change PACS',
                okAction: function(){
                    dqr.selectedPacs = pacsId;
                    resetResults(true);
                },
                cancelAction: function(){
                    console.log('not changing PACS');
                    // revert menu if cancelling
                    $selectPacsMenu.changeVal(dqr.selectedPacs);
                    menuUpdate($selectPacsMenu);
                }
            });
        }
    });

    // initialize date fields *after* DOM loads
    $(function(){
        initDatePickers();
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
        var UIDS = [].concat(studyUIDs).join(',');
        var URL  = XNAT.url.restUrl('/xapi/dqr/seriesInfo/pacs/' + pacsId + '/studies');
        return XNAT.xhr.postJSON({
            url: URL,
            data: UIDS,
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
                                checked: true,
                                attr: { checked: 'checked' }
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
                        return item.studyUIDs.length + '';
                    }
                }
            }
        }).get();
        XNAT.plugins.dqr.selectableItems(scanTypesTable);
        return scanTypesTable;
    }


    function importSessionsOfSelectedTypeToProject(){

        var $searchResultsTable = $('#all-search-results');
        var $selectedSessions   = $searchResultsTable.find('input.select-session:checked').filter(':visible');

        var studyUIDs = $selectedSessions.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        console.log(studyUIDs);

        var ae = $('#ae-menu').val();

        var projectId = window.projectId || getQueryStringValue('project');

        var selectedScanTypes = $('#scan-types-list').find('input.select-scan-type:checked').filter(':visible');

        var scanTypes = selectedScanTypes.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        if (!scanTypes.length) {
            XNAT.dialog.message(false, 'Please select at least one series type to import.');
            return false;
        }

        var jsonDataOldExample = {
            'importRows': [
                {
                    'relabelMap': {},
                    'studyInstanceUIDs': [
                        'string'
                    ]
                }
            ],
            'seriesDescriptions': [
                'string'
            ]
        };

        var jsonDataOld = {
            importRows: studyUIDs.map(function(uid, i){
                var relabelMap = {};
                var $importRow = $searchResultsTable.find('tr[data-uid="' + uid + '"]');
                $importRow.find('input.relabel').each(function(){
                    relabelMap[this.title] = this.value || '';
                });
                return {
                    relabelMap: relabelMap,
                    studyInstanceUIDs: [].concat(uid)
                };
            }),
            seriesDescriptions: scanTypes
        };

        var jsonDataExample = {
            '1.234.567890987654321': {
                'seriesInstanceUIDs': [
                    '1.23.456.7890',
                    '1.23.789.0234'
                ],
                'seriesDescriptions': [
                    'string'
                ],
                'relabelMap': {
                    'Subject': 'SUBJ1',
                    'Session': 'SUBJ1_001'
                }
            }
        };



        var jsonData = {};

        // SETUP THE FINAL SUBMISSION JSON
        forEach(studyUIDs, function(uid){

            jsonData[uid] = jsonData[uid] || {};

            jsonData[uid].seriesDescriptions = scanTypes.filter(function(type){
                return dqr.seriesDescriptions[type].studyUIDs.indexOf(uid) !== -1;
            }).map(function(type){
                return type === NONE ? '' : type;
            });

            jsonData[uid].seriesInstanceUids = jsonData[uid].seriesInstanceUids || [];

            forEach(jsonData[uid].seriesDescriptions, function(type){
                jsonData[uid].seriesInstanceUids =
                    jsonData[uid].seriesInstanceUids.concat(dqr.seriesDescriptions[type || NONE].seriesUIDs || []);
            });

            jsonData[uid].relabelMap = (function(){
                var relabelMapTemp = {};
                var $importRow     = $searchResultsTable.find('tr[data-uid="' + uid + '"]');
                $importRow.find('input.relabel').each(function(){
                    // only add to the relabelMap object if there's a value
                    this.value && (relabelMapTemp[this.title] = this.value || '');
                });
                return relabelMapTemp;
            })();

        });

        console.log('SUBMIT...');
        console.log(jsonData);

        XNAT.xhr.postJSON({
            url: XNAT.url.restUrl('/xapi/dqr/csvimport/generalImportFromJson', [
                'pacsId=' + $selectPacsMenu.val(),
                'ae=' + ae,
                'project=' + projectId
            ], false),
            data: JSON.stringify(jsonData),
            success: function(){
                window.jsdebug && console.log(arguments);
                XNAT.dialog.message({
                    title: ' ',
                    width: 400,
                    content: (function(){
                        return '<div style="margin:30px;">' +
                            '<p>' +
                            'PACS data has been queued for import. You may close ' +
                            'this dialog to start a new search.</p>' +
                            '<p>' +
                            'You can also ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/app/template/Page.vm?view=dqr/queue&role=dqr#tab=queue') + '">' +
                            'check on the import progress in the queue</a> or ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/data/projects/' + projectId) + '">' +
                            'go back to the project page.</a></p>' +
                            '</div>';
                    })(),
                    okLabel: 'Close',
                    okAction: function(){
                        XNAT.dialog.loading.open();
                        window.location.reload(true);
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
        console.log('scanTypesDialog');
        getStudies(pacsId, studyUIDs).done(function(studies){
            console.log('studies');
            console.log(studies);
            collectScanTypes(studies);
            var scanTypesTable = scanTypesListDisplay();
            XNAT.dialog.open({
                title: 'Import from PACS',
                content: scanTypesTable,
                buttons: [
                    {
                        label: 'Import Selected',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            importSessionsOfSelectedTypeToProject();
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

    function renderResultsTable(json){

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
        if ((json !== false && !isArray(json)) || !json.length) {
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
                                value: '*'
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
                    studyId: {
                        label: 'Study ID',
                        filter: function(){
                            return filterInput('studyId');
                        },
                        sort: true,
                        th: {
                            style: { width: WIDTHS.studyId, padding: FILTER_TH_PADDING }
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
                            if (this.value) {
                                dqr.allSearchResults[uid].relabelMap[this.title] = this.value;
                            }
                        }],
                        ['click', 'td:has(.select-row)', function(e){
                            $(this).closest('tr').find('input.select-session').trigger('click');
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
                            var uid      = this.studyInstanceUid;
                            var ckbx     = spawn('input.select-session.selectable-one|type=checkbox', {
                                value: uid
                            });
                            ckbx.checked = firstDefined(dqr.allSearchResults[uid].checked, false);
                            return ckbxLabel(ckbx);
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
                            var patientName = this.patient.name.lastNameCommaFirstName || '';
                            return spawn('div.truncate.select-row.filter-data-item', {
                                title: patientName,
                                data: { filter: 'patientName' }
                            }, patientName);
                        }
                    },
                    studyId: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.studyId }
                        },
                        apply: function(){
                            var studyId = this.studyId;
                            return spawn('div.truncate.select-row.filter-data-item', {
                                title: studyId,
                                data: { filter: 'studyId' }
                            }, studyId);
                        }
                    },
                    studyDate: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.date }
                        },
                        apply: function(){
                            var studyDateStr = this.studyDate + '';
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
        XNAT.plugins.dqr.selectableItems($pacsSearchResults);

        // init new filter method
        XNAT.plugins.dqr.filterableItems($pacsSearchResults);

        $searchResultsSubmit.empty();

        function renderBottom(receivers){

            var aeMenu$ = $.spawn('select#ae-menu');
            var aeMenu0 = aeMenu$[0];
            var defaultReceiver = receivers[0];
            var defaultLabel = defaultReceiver.aeTitle + ':' + defaultReceiver.port;
            var receiverMap = {};

            // toggleRemapping(/^true$/.test(defaultReceiver.customProcessing));

            forEach(receivers, function(item, i){
                var AE = item.aeTitle + ':' + item.port;
                window.jsdebug && console.log(AE);
                receiverMap[AE] = item;
                // only add 'dqr' receivers to the menu
                if (/dqr/i.test(item.identifier)) {
                    aeMenu0.appendChild(spawn('option.receiver', {
                        title: AE,
                        value: AE,
                        data: {
                            id: (item.id + ''),
                            identifier: item.identifier,
                            processing: item.customProcessing
                        },
                        disabled: !item.enabled
                    }, AE));
                }
            });

            var relabelInputs$ = null;

            function toggleRemapping(e){

                var selectedOption = this.value;
                var doProcessing = /^true$/.test(receiverMap[selectedOption].customProcessing);

                !relabelInputs$ && (relabelInputs$ = $pacsSearchResults.find('input.relabel'));

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

            toggleRemapping.call(aeMenu0);

            $searchResultsSubmit.spawn('div.pull-right', {
                on: [['change', '#ae-menu', toggleRemapping]]
            }, [
                ['br'],
                'Select SCP Receiver: ',
                aeMenu0,
                '&nbsp;&nbsp;',
                ['button#import-selected-sessions.btn.btn1|type=button', {
                    html: 'Begin Import',
                    on: [
                        ['click', function(e){
                            e.preventDefault();
                            console.log('importing...');
                            var studyUIDs = [];
                            $pacsSearchResults.find('input.select-session:checked').filter(':visible').each(function(){
                                studyUIDs.push(this.value);
                            });
                            if (!studyUIDs.length) {
                                XNAT.dialog.message(false, 'Please select at least one study to import.');
                                return false;
                            }
                            dqr.selectedPacs = dqr.selectedPacs || $selectPacsMenu.val();
                            scanTypesDialog(dqr.selectedPacs, studyUIDs);
                        }]
                    ]
                }]
            ]);

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
                renderBottom(json);
            },
            failure: function(e){
                console.warn('Could not retrieve SCP Receivers');
            }
        });

    }

    function pingPACS(id, callback){
        if (!id) {
            console.warn('id required');
            return;
        }
        return XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/xapi/dqr/pacsStatus/ping/' + id),
            success: function(json){
                if (json && json.successful) {
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

    function searchPACS(id){

        console.log('PACS search...');

        var selectedPacs = id || dqr.selectedPacs || $selectPacsMenu.val();

        var searchCriteria = {};

        $pacsSearchFields.find('input').not('.ignore').serializeArray().forEach(function(param, i){
            // skip fields that start with '!'
            if (param.name.charAt(0) !== '!') {
                searchCriteria[param.name] = param.value || '';
            }
        });
        searchCriteria.pacsId = selectedPacs;

        // transform date to expected format
        searchCriteria.studyDateFrom = (searchCriteria.studyDateFrom ? (new SplitDate(searchCriteria.studyDateFrom)).US : '');
        searchCriteria.studyDateTo   = (searchCriteria.studyDateTo ? (new SplitDate(searchCriteria.studyDateTo)).US : '');

        // console.log(searchCriteria);

        var searchUrl = XNAT.url.csrfUrl('/data/services/pacs/' + selectedPacs + '/search/studies', {}, false);

        // console.log(searchUrl);

        return XNAT.xhr.post({
            url: searchUrl,
            data: searchCriteria,
            success: function(json){
                renderResultsTable(json.ResultSet.Result);
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

    $searchSubmit.on('click', function(e){
        e.preventDefault();
        var self         = this;
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
    });

    // handle CSV import
    $(document).on('click', '#import-csv', function(e){
        e.preventDefault();
        // form with file input to render
        var fileForm = spawn('form#csv-upload', {
            style: { padding: '10px', fontSize: '13px' }
        }, [
            ['p', 'Select a CSV file to upload:'],
            ['input|type=hidden|name=pacsId', {
                // pick up value of selected PACS
                value: $('#select-pacs').val()
            }],
            ['input|type=file|name=csv_to_store|accept=.csv']
        ]);
        XNAT.dialog.open({
            title: 'Upload CSV',
            width: 320,
            content: fileForm,
            okLabel: 'Upload',
            okClose: false,
            okAction: function(obj){
                var postCsv = XNAT.xhr.post({
                    url: XNAT.url.csrfUrl('/xapi/dqr/csvimport/newUploadCsv'),
                    data: (new FormData(fileForm)),
                    cache: false,
                    contentType: false,
                    processData: false,
                    success: function(data){
                        console.log(data);
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
                        obj.close();
                        // XNAT.ui.banner.top(3000, 'Saved changes to DICOM AE connection', 'success');
                    },
                    failure: function(){
                        console.warn('error importing CSV');
                        console.warn(arguments);
                    }
                });
            }
        });
    });

    XNAT.plugin.dqr = dqr;

}));