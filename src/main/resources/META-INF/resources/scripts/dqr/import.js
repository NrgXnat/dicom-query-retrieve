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

    function resetResults(){
        dqr.searchResults = [];
        dqr.allSearchResults = {};
        dqr.resultsTableData = [];
        $pacsSearchFields.find('input').val('');
        $pacsNoResults.empty().html($noResultsTemplate.html());
        $searchResultsHeader.empty();
        $searchResultsBody.empty();
        $searchResultsSubmit.empty();
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

    var DATE_TODAY = new Date();

    var $studyDateFrom = $('#study-date-from');
    var $studyDateTo = $('#study-date-to');

    var $studyDateToday = $('#study-date-today');

    // initialize date fields *after* DOM loads
    $(function(){

        // $studyDateFrom.off().datepicker(datepickerOpts());
        // $studyDateTo.off().datepicker(datepickerOpts());

        $($studyDateFrom, $studyDateTo).off()
                                       .mask('9999-99-99', { placeholder: '    -  -  ' })
                                       .datepicker({
                                           language: 'en',
                                           maxDate: DATE_TODAY,
                                           todayButton: DATE_TODAY,
                                           // range: true,
                                           // multipleDatesSeparator: '-',
                                           dateFormat: 'yyyy-mm-dd'
                                       })
        ;

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

    function randomFromArray(arr){
        return arr[Math.floor(Math.random() * (arr.length))]
    }

    function importSelected(){

    }

    function selectScanTypes(){

    }

    function getScanTypes(pacsId, uids){
        var UIDS = [].concat(uids).join(',');
        var URL = XNAT.url.restUrl('/xapi/dqr/seriesInfo/pacs/' + pacsId + '/studies/' + UIDS);
        return XNAT.xhr.get(URL);
    }

    function collectScanTypes(json){

        console.log('collectScanTypes');

        dqr.seriesDescriptions = {};

        forOwn(json, function(uid, obj){
            forEach(obj.results, function(item){
                var seriesDescriptionItem = dqr.seriesDescriptions[item.seriesDescription] || {};
                seriesDescriptionItem.name = (item.seriesDescription || NONE);
                seriesDescriptionItem.count = (seriesDescriptionItem.count || 0);
                seriesDescriptionItem.count++;
                seriesDescriptionItem.uids = seriesDescriptionItem.uids || [];
                if (seriesDescriptionItem.uids.indexOf(item.studyInstanceUid) === -1) {
                    seriesDescriptionItem.uids.push(item.studyInstanceUid);
                }
                dqr.seriesDescriptions[item.seriesDescription] = seriesDescriptionItem;
            });
        });

        // RESET SCAN TYPES LIST
        dqr.scanTypesList = [];

        forOwn(dqr.seriesDescriptions, function(name, obj){
            dqr.scanTypesList.push(obj);
        });

        return dqr.scanTypesList

    }

    function scanTypesList(){
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
                            ['input.selectable-select-all|type=checkbox', {
                                checked: true
                            }]
                        ])
                    },
                    apply: function(){
                        var item = this;
                        return spawn('div.center', [
                            ['input.selectable-select-one.select-scan-type|type=checkbox', {
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
                count: 'Count'
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
            XNAT.dialog.message(false, 'Please select at least one series to import.');
            return false;
        }

        var jsonDataExample = {
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

        var jsonData = {
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

        console.log('to submit:');
        console.log(jsonData);

        XNAT.xhr.postJSON({
            url: XNAT.url.restUrl('/xapi/dqr/csvimport/newImportFromJson', [
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
                        return '<div style="margin:20px;">' +
                            '<p style="font-size:15px;line-height:20px;">' +
                            'PACS data has been queued for import. You may close ' +
                            'this dialog to start a new search.</p>' +
                            '<p style="font-size:15px;line-height:20px;">' +
                            'You can also ' +
                            '<a class="link" href="' + XNAT.url.rootUrl('/app/template/XDATScreen_prearchives.vm') + '">' +
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
        getScanTypes(pacsId, uids).done(function(json){
            console.log('getScanTypes');
            collectScanTypes(json);
            var scanTypesTable = scanTypesList();
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
                            var ckbx = spawn('input#toggle-all-sessions.selectable-select-all|type=checkbox', {
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
                            title: window.subjectDisplay,
                            style: { width: WIDTHS.xnatSubject }
                        }
                    }),
                    xnatSession: extend(true, {}, relabelColumn, {
                        label: '' +
                            // '<i class="fa fa-angle-double-right"></i>&nbsp;&nbsp;&nbsp;&nbsp;' +
                            window.sessionDisplay +
                            '',
                        th: {
                            title: window.sessionDisplay,
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
                        ['click', 'td.accessionNumber, td.patientName, td.studyId, td.studyDate', function(e){
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
                            return spawn('div.truncate', { title: accNum }, accNum);
                        }
                    },
                    patientName: {
                        label: false,
                        td: { style: { width: WIDTHS.name } },
                        apply: function(){
                            // var patientName = this.patient.name.lastNameCommaFirstName.replace(/,/, '^');
                            var patientName = this.patient.name.lastNameCommaFirstName || '';
                            return spawn('div.truncate', {
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
                            return spawn('div.truncate', { title: studyId }, studyId);
                        }
                    },
                    studyDate: {
                        label: false,
                        td: {
                            style: { width: WIDTHS.date }
                        },
                        apply: function(){
                            var studyDateStr = this.studyDate + '';
                            return spawn('div.center.mono', this.studyDate ? [
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
                            return [].concat(this.modalitiesInStudy).join(', ');
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
                                title: window.subjectDisplayLower,
                                value: this.relabelMap ? (this.relabelMap[window.subjectDisplayLower] || '') : ''
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
                                title: window.sessionDisplayLower,
                                value: this.relabelMap ? (this.relabelMap[window.sessionDisplayLower] || '') : ''
                            })
                        }
                    })
                }
            });
        }

        renderBody();

        // init the selectable stuff
        XNAT.app.selectableItems($pacsSearchResults);

        XNAT.app.searchableItems($pacsSearchResults);

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