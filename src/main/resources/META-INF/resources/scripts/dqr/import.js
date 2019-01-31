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
    var $selectPacsMenu = $('#select-pacs');
    var $pacsSearchFields = $('#pacs-search-fields');
    var $searchSubmit = $('#submit-pacs-search');
    var $pacsSearchResults = $('#pacs-search-results');
    var $searchResultsHeader = $pacsSearchResults.find('.results-header');
    var $searchResultsBody = $pacsSearchResults.find('.results-body');
    var $searchResultsSubmit = $pacsSearchResults.find('.results-submit');
    var $pacsNoResults = $('#pacs-no-results');
    var $noResultsTemplate = $('#no-search-results');

    // string 'constants'
    var NONE = '(none)';

    function renderPacsMenu(items){
        var pacsMenu = $selectPacsMenu[0];
        forEach(items || [], function(item, i){
            if (item.queryable) {
                pacsMenu.add(spawn('option', {
                    value: item.id,
                    title: item.aeTitle
                }, item.label || item.aeTitle))
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
        renderPacsMenu(json.ResultSet.Result)
    });

    var $studyDateFrom = $('#study-date-from');
    var $studyDateTo = $('#study-date-to');
    var $studyDateToday = $('#study-date-today');

    var DATE_MIN = new Date('1900-01-01T00:00');
    var DATE_TODAY = new Date();
    var dateFrom, dateTo;

    function resetResults(){
        dqr.searchResults = [];
        dqr.allSearchResults = {};
        dqr.resultsTableData = [];
        dqr.selectedStudies = {};

        dqr.scanTypesList = [];

        // collect study UIDs by series description
        dqr.studiesBySeriesDesc = {};

        $pacsSearchFields.find('input').val('');
        $pacsNoResults.empty().html($noResultsTemplate.html());
        $searchResultsHeader.empty();
        $searchResultsBody.empty();
        $searchResultsSubmit.empty();
        // if these are defined, they should be initialized
        // if (dateFrom && dateTo) {
        //     dateFrom.update({
        //         maxDate: DATE_TODAY,
        //         minDate: DATE_MIN
        //     });
        //     dateTo.update({
        //         maxDate: DATE_TODAY,
        //         minDate: DATE_MIN
        //     })
        // }
    }

    // immediately render the 'no results' message
    resetResults();

    // and bind to the 'Clear Search Results' button
    $('#clear-search-results').on('click', resetResults);

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
                    resetResults();
                },
                cancelAction: function(){
                    console.log('not changing PACS');
                    // revert menu if cancelling
                    $selectPacsMenu.changeVal(dqr.selectedPacs);
                    menuUpdate($selectPacsMenu);
                }
            })
        }
    });

    function validDate(dateVal){
        var dateSplit;
        return dateVal && (dateSplit = new SplitDate(dateVal)) && XNAT.validate.value(dateSplit.iso).is('date', 'iso').check() ? new Date(dateVal + 'T00:00') : ''
    }

    function datepickerOpts(obj){
        return $.extend({
            language: 'en',
            maxDate: DATE_TODAY,
            // todayButton: DATE_TODAY,
            autoClose: true,
            // range: true,
            // multipleDatesSeparator: '-',
            dateFormat: 'yyyy-mm-dd'
        }, obj || {});
    }

    // initialize date fields *after* DOM loads
    $(function(){

        dateFrom = $studyDateFrom.off().datepicker(datepickerOpts({
            onShow: function(fromPicker){
                fromPicker.update({
                    minDate: DATE_MIN,
                    maxDate: validDate($studyDateTo.val()) || DATE_TODAY
                })
            },
            onSelect: function(formattedDate, dateObj, picker){
                dateTo.update({
                    minDate: dateObj
                });
            }
        })).data('datepicker');

        dateTo = $studyDateTo.off().datepicker(datepickerOpts({
            onShow: function(toPicker){
                toPicker.update({
                    minDate: validDate($studyDateFrom.val()) || DATE_MIN,
                    maxDate: DATE_TODAY
                })
            },
            onSelect: function(formattedDate, dateObj, picker){
                dateFrom.update({
                    maxDate: dateObj
                })
            }
        })).data('datepicker');

        // handle manual date field edits
        function verifyDates(){
            if (this.value && !XNAT.validate.value(this.value).is('date', 'iso').check()) {
                XNAT.dialog.message('Invalid Date', 'Please enter a valid date in the format <b>YYYY-MM-DD</b>.');
                $(this).focus().select();
                return false;
            }
            dateFrom.update({
                maxDate: validDate($studyDateTo.val()) || DATE_TODAY
            });
            dateTo.update({
                minDate: validDate($studyDateFrom.val()) || DATE_MIN
            })
        }

        $studyDateFrom.on('change', verifyDates);
        $studyDateTo.on('change', verifyDates);

        // $pacsSearchFields.find('.study-date')
        //                  .mask('9999-99-99', { placeholder: '    -  -  ' })
        //                  .attr('autocomplete', 'off')
        // ;

        // click the 'today' button to fill in today's date
        $studyDateToday.off().on('click', function(e){
            e.preventDefault();
            $studyDateFrom.val(XNAT.data.todaysDate.ISO);
            $studyDateTo.val(XNAT.data.todaysDate.ISO);
        });

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
        var pre = (isDefined(prefix)) ? prefix : 'rndx';
        var newId = pre + (Math.random() + 1).toString(36).substr(2, 8);
        if (isDefined(length)) {
            if (newId.length > length) {
                return newId.slice(0, length);
            }
            else {
                return randomizer(length, newId)
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

    function getStudies(pacsId, uids){
        var UIDS = [].concat(uids).join(',');
        var URL = XNAT.url.restUrl('/xapi/dqr/seriesInfo/pacs/' + pacsId + '/studies/' + UIDS);
        return XNAT.xhr.getJSON({
            url: URL,
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
                var seriesDescriptionItem = dqr.seriesDescriptions[item.seriesDescription] || {};
                seriesDescriptionItem.name = (item.seriesDescription || NONE);
                seriesDescriptionItem.count = (seriesDescriptionItem.count || 0);
                seriesDescriptionItem.count++;
                seriesDescriptionItem.studyUIDs = seriesDescriptionItem.studyUIDs || [];
                if (seriesDescriptionItem.studyUIDs.indexOf(item.study.studyInstanceUid) === -1) {
                    seriesDescriptionItem.studyUIDs.push(item.study.studyInstanceUid);
                }
                seriesDescriptionItem.seriesUIDs = (seriesDescriptionItem.seriesUIDs || []).concat(item.seriesInstanceUid);
                dqr.seriesDescriptions[item.seriesDescription] = seriesDescriptionItem;
            });
            studyCount += 1;
        });

        // RESET SCAN TYPES LIST
        dqr.scanTypesList = [];

        forOwn(dqr.seriesDescriptions, function(name, obj){
            dqr.scanTypesList.push(obj);
        });

        return dqr.scanTypesList

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
                                checked: true
                            }]
                        ])
                    },
                    apply: function(){
                        var item = this;
                        return spawn('div.center', [
                            ['input.selectable-one.select-scan-type|type=checkbox', {
                                value: item.name !== NONE ? item.name : '',
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
                        }, item.name)
                    }
                },
                COUNT: {
                    label: 'Study Count',
                    apply: function(){
                        var item = this;
                        return item.studyUIDs.length + ''
                    }
                }
            }
        }).get();
        XNAT.app.selectableItems(scanTypesTable);
        return scanTypesTable;
    }


    function importSessionsOfSelectedTypeToProject(){

        var $searchResultsTable = $('#all-search-results');
        var selectedSessions = $searchResultsTable.find('input.select-session:checked');

        var uids = selectedSessions.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        console.log(uids);

        var ae = $('#ae-menu').val();

        var projectId = window.projectId || getQueryStringValue('project');

        var selectedScanTypes = $('#scan-types-list').find('input.select-scan-type:checked');

        var scanTypes = selectedScanTypes.toArray().map(function(ckbx, i){
            return ckbx.value;
        });

        if (!scanTypes.length) {
            XNAT.dialog.message(false, 'Please select at least one series type to import.');
            return false;
        }

        var jsonDataOldExample = {
            "importRows": [
                {
                    "relabelMap": {},
                    "studyInstanceUIDs": [
                        "string"
                    ]
                }
            ],
            "seriesDescriptions": [
                "string"
            ]
        };

        var jsonDataOld = {
            importRows: uids.map(function(uid, i){
                var relabelMap = {};
                var $importRow = $searchResultsTable.find('tr[data-uid="' + uid +'"]');
                $importRow.find('input.relabel').each(function(){
                    relabelMap[this.title] = this.value || '';
                });
                return {
                    relabelMap: relabelMap,
                    studyInstanceUIDs: [].concat(uid)
                }
            }),
            seriesDescriptions: scanTypes
        };

        var jsonDataExample = {
            "1.234.567890987654321": {
                "seriesInstanceUIDs": [
                    "1.23.456.7890",
                    "1.23.789.0234"
                ],
                "seriesDescriptions": [
                    "string"
                ],
                "relabelMap": {
                    "Subject": "SUBJ1",
                    "Session": "SUBJ1_001"
                }
            }
        };



        var jsonData = {};

        // SETUP THE FINAL SUBMISSION JSON
        forEach(uids, function(uid){
            jsonData[uid] = {
                seriesDescriptions: Object.keys(dqr.seriesDescriptions),
                seriesInstanceUids: (function(){
                    var seriesUidsTemp = [];
                    forOwn(dqr.seriesDescriptions, function(uid, desc){
                        seriesUidsTemp = seriesUidsTemp.concat(desc.seriesUIDs);
                    });
                    return seriesUidsTemp;
                })(),
                relabelMap: (function(){
                    var relabelMapTemp = {};
                    var $importRow = $searchResultsTable.find('tr[data-uid="' + uid +'"]');
                    $importRow.find('input.relabel').each(function(){
                        // only add to the relabelMap object if there's a value
                        this.value && (relabelMapTemp[this.title] = this.value || '');
                    });
                    return relabelMapTemp;
                })()
            }
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
                console.log(arguments);
                XNAT.dialog.message({
                    title: ' ',
                    width: 400,
                    content: (function(){
                        return '<div style="margin:30px;">' +
                            '<p style="font-size:13px;line-height:18px;">' +
                            'PACS data has been queued for import. You may close ' +
                            'this dialog to start a new search.</p>' +
                            '<p style="font-size:14px;line-height:20px;">' +
                            'You can also ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/app/action/XDATActionRouter/xdataction/prearchives/project/' + projectId) + '">' +
                            'check on the import progress in the prearchive</a> or ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/data/projects/' + projectId) +'">' +
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
        })

    }


    function scanTypesDialog(pacsId, uids){
        console.log('scanTypesDialog');
        getStudies(pacsId, uids).done(function(studies){
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
            })
        });

    }

    function renderResultsTable(json){

        // console.log(json);

        // `json` must be an array containing SOMETHING
        // in order to render the results table
        if (!isArray(json) || !json.length) {
            return undef;
        }

        $pacsNoResults.empty();

        forEach(json, function(item){
            item.relabelMap = item.relabelMap || {};
            if (!dqr.allSearchResults.hasOwnProperty(item.studyInstanceUid)) {
                dqr.allSearchResults[item.studyInstanceUid] = item;
            }
        });

        // console.log(dqr.allSearchResults);

        function ckbxLabel(ckbx){
            return spawn('label.center', {
                style: { display: 'block', textAlign: 'center' }
            }, ckbx);
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
                        filter: true,
                        sort: true,
                        th: {
                            style: { width: WIDTHS.acc }
                        }
                    },
                    patientName: {
                        label: 'Patient Name',
                        filter: true,
                        sort: true,
                        th: {
                            style: { width: WIDTHS.name }
                        }
                    },
                    studyId: {
                        label: 'Study ID',
                        filter: true,
                        sort: true,
                        th: {
                            style: { width: WIDTHS.studyId }
                        }
                    },
                    studyDate: {
                        label: 'Study Date',
                        filter: true,
                        sort: true,
                        th: {
                            style: { width: WIDTHS.date }
                        }
                    },
                    modalitiesInStudy: {
                        label: 'Modality',
                        filter: true,
                        sort: true,
                        th: {
                            style: { width: WIDTHS.mod }
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
            $searchResultsBody.empty();
            return XNAT.table.dataTable(dqr.allSearchResults, {
                container: $searchResultsBody,
                header: false,
                table: {
                    id: 'all-search-results',
                    classes: 'compact table-data highlight',
                    style: { tableLayout: 'fixed' },
                    on: [
                        ['change', 'input.select-session', function(){
                            var uid = this.value;
                            console.log(uid);
                            dqr.allSearchResults[uid].checked = this.checked
                        }],
                        ['blur', 'input.relabel', function(e){
                            var uid = $(this).closest('tr').data('uid');
                            console.log(uid);
                            if (this.value) {
                                dqr.allSearchResults[uid].relabelMap[this.title] = this.value
                            }
                        }],
                        ['click', 'td:has(.select-row)', function(e){
                            $(this).closest('tr').find('input.select-session').trigger('click');
                        }]
                    ]
                },
                overflowY: 'scroll',
                maxHeight: '650px',
                columns: {
                    _studyInstanceUid: '~data-uid',  // this will add a [data-uid="1.234.567890"] attribute to each row's <tr> element
                    SELECT_SESSIONS: {
                        label: false,
                        td: { style: { width: WIDTHS.select } },
                        apply: function(){
                            var uid = this.studyInstanceUid;
                            var ckbx = spawn('input.select-session.selectable-select-one|type=checkbox', {
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
                            return spawn('div.truncate.select-row', { title: accNum }, accNum);
                        }
                    },
                    patientName: {
                        label: false,
                        td: { style: { width: WIDTHS.name } },
                        apply: function(){
                            // var patientName = this.patient.name.lastNameCommaFirstName.replace(/,/, '^');
                            var patientName = this.patient.name.lastNameCommaFirstName || '';
                            return spawn('div.truncate.select-row', {
                                title: patientName
                            }, patientName)
                        }
                    },
                    studyId: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.studyId }
                        },
                        apply: function(){
                            var studyId = this.studyId;
                            return spawn('div.truncate.select-row', { title: studyId }, studyId);
                        }
                    },
                    studyDate: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.date }
                        },
                        apply: function(){
                            var studyDateStr = this.studyDate + '';
                            return spawn('div.center.mono.select-row', this.studyDate ? [
                                // ['i.hidden.sort', studyDateStr],
                                studyDateStr.slice(0, 4) + '-' + studyDateStr.slice(4, 6) + '-' + studyDateStr.slice(6, 8)
                            ] : '<i class="hidden">0</i>&ndash;')
                        }
                    },
                    modalitiesInStudy: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.mod }
                        },
                        apply: function(){
                            return spawn('div.select-row', [].concat(this.modalitiesInStudy).join(', '));
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
                            })
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
                            })
                        }
                    })
                }
            });
        }

        renderBody();

        // init the selectable stuff
        XNAT.app.selectableItems($pacsSearchResults);

        XNAT.app.filterableItems($pacsSearchResults);

        $searchResultsSubmit.empty();

        $searchResultsSubmit.spawn('br');

        function renderBottom(receivers){

            var aeMenu = spawn('select#ae-menu');

            forEach(receivers, function(item, i){
                var AE = item.aeTitle + ':' + item.port;
                console.log(AE);
                aeMenu.appendChild(spawn('option.receiver', {
                    title: AE,
                    value: AE,
                    data: {
                        id: (item.id + ''),
                        identifier: item.identifier
                    },
                    disabled: !item.enabled
                }, AE));
            });

            $searchResultsSubmit.spawn('div.pull-right', [
                'Select SCP Receiver: ',
                aeMenu,
                '&nbsp;&nbsp;',
                ['button#import-selected-sessions.btn.btn1|type=button', {
                    html: 'Begin Import',
                    on: [
                        ['click', function(e){
                            e.preventDefault();
                            console.log('importing...');
                            var uids = [];
                            $pacsSearchResults.find('input.select-session:checked').each(function(){
                                uids.push(this.value);
                            });
                            if (!uids.length) {
                                XNAT.dialog.message(false, 'Please select at least one study to import.');
                                return false;
                            }
                            dqr.selectedPacs = dqr.selectedPacs || $selectPacsMenu.val();
                            scanTypesDialog(dqr.selectedPacs, uids)
                        }]
                    ]
                }]
            ]);

        }

        XNAT.xhr.get({
            url: XNAT.url.restUrl('/xapi/dicomscp'),
            success: function(json){
                var aeExample = {
                    "enabled": true,
                    "identifier": "dqrObjectIdentifier",
                    "port": 8104,
                    "id": 1,
                    "aeTitle": "XNAT",
                    "customProcessing": true
                };
                renderBottom(json);
            },
            failure: function(e){
                console.warn('Could not retrieve SCP Receivers')
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
        })
    }

    function searchPACS(id){

        console.log('PACS search...');

        var selectedPacs = id || dqr.selectedPacs || $selectPacsMenu.val();

        var searchCriteria = {};

        $pacsSearchFields.find('input').not('.ignore').serializeArray().forEach(function(param, i){
            // skip fields that start with '!'
            if (param.name.charAt(0) !== '!'){
                searchCriteria[param.name] = param.value || '';
            }
        });
        searchCriteria.pacsId = selectedPacs;

        // transform date to expected format
        searchCriteria.studyDateFrom = (searchCriteria.studyDateFrom ? (new SplitDate(searchCriteria.studyDateFrom)).US : '');
        searchCriteria.studyDateTo = (searchCriteria.studyDateTo ? (new SplitDate(searchCriteria.studyDateTo)).US : '');

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
                XNAT.dialog.message(false, 'No results were found for the specified search criteria.')
            }
        });

    }

    // process anon script and insert it into `dqr.allSearchResults` object
    function processAnonScript(anonScript){
        // TODO: write this function
    }

    $searchSubmit.on('click', function(e){
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
        pingPACS(dqr.selectedPacs, searchPACS)
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
                                var searchId = randomizer(16, 'dqrx' + (Date.now() + '').slice(-8, -2) + 'x');
                                dqr.searchIds = dqr.searchIds || {};
                                dqr.searchIds[searchId] = item;
                                dqr.csvSearchResults = dqr.csvSearchResults || {};
                                dqr.csvSearchResults[searchId] = item;
                                // dqr.allSearchResults[searchId] = {
                                //     anonScript: item.anonScript || ''
                                // };
                                console.log(dqr.csvSearchResults);
                                forEach(item.studies, function(study){
                                    study.searchId = searchId;
                                    study.relabelMap = item.relabelMap;
                                    results.push(study)
                                })
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
                })
            }
        })
    });

    XNAT.plugin.dqr = dqr;

}));