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
    var $searchInputs = $pacsSearchFields.find('.pacs-search-item input');
    var $searchSubmit = $('#submit-pacs-search');
    var $pacsSearchResults = $('#pacs-search-results');
    var $noResultsTemplate = $('#no-search-results');

    function renderPacsMenu(items){
        var pacsMenu = $selectPacsMenu[0];
        forEach(items || [], function(item, i){
            // this effectively allows all PACS to show
            // in the menu - if only 'queryable' PACS
            // should be displayed, remove `|| true`
            if (item.queryable || true) {
                pacsMenu.add(spawn('option', {
                    value: item.id,
                    title: item.aeTitle
                }, item.label || item.aeTitle))
            }
            if (item.defaultQueryRetrievePacs) {
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
        $pacsSearchResults.empty().html($noResultsTemplate.html());
    }

    // immediately render the 'no results' message
    resetResults();

    // and bind to the 'Clear Search Results' button
    $('#clear-search-results').on('click', resetResults);

    var $studyDateFrom = $('#study-date-from');
    var $studyDateTo = $('#study-date-to');

    // initialize date fields
    $pacsSearchFields.find('.study-date')
                     .attr('autocomplete', 'off')
                     .mask('99/99/9999', { placeholder: 'MM/DD/YYYY' })
                     .datetimepicker({
                         timepicker: false,
                         // today is max date, disallow future date selection
                         maxDate:    XNAT.data.todaysDate.ISO,
                         format:     'm/d/Y'
                         // format: 'Y-m-d' // ISO standard date format
                     })
    ;

    // click the 'today' checkbox to fill in today's date
    $('#study-date-today').on('click', function(e){
        if (this.checked) {
            $studyDateFrom.val(XNAT.data.todaysDate.US)
                          // .attr('readonly', 'readonly')
                          // .addClass('disabled')
            ;
            $studyDateTo.val(XNAT.data.todaysDate.US)
                        // .attr('readonly', 'readonly')
                        // .addClass('disabled')
            ;
        }
        else {
            $studyDateFrom.val('')
                          // .removeAttr('readonly')
                          // .removeClass('disabled')
            ;
            $studyDateTo.val('')
                        // .removeAttr('readonly')
                        // .removeClass('disabled')
            ;
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
            return spawn('a.link.remap-auto-fill|href=#!', 'Auto-fill');
        }
    };

    function randomizer(length, prefix) {
        var pre = (isDefined(prefix)) ? prefix : 'rndx' ;
        var newId = pre + (Math.random() + 1).toString(36).substr(2,8);
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

    function renderResultsTable(json){

        // console.log(json);

        // `json` must be an array containing SOMETHING
        // in order to render the results table
        if (!isArray(json) || !json.length) {
            return undef;
        }

        forEach(json, function(item){
            if (!dqr.allSearchResults.hasOwnProperty(item.studyInstanceUid)) {
                dqr.allSearchResults[item.studyInstanceUid] = item;
            }
        });

        // console.log(dqr.allSearchResults);

        function ckbxLabel(ckbx){
            return spawn('label.center', {
                style: { display: 'block', width: '40px' }
            }, ckbx);
        }

        var resultsTable = XNAT.table.dataTable(dqr.allSearchResults, {
            table: {
                id: 'all-search-results',
                classes: 'compact'
            },
            items: {
                _studyInstanceUid: '~data-uid',
                SELECT_SESSIONS: {
                    label: '<label for="toggle-all-sessions">&nbsp;</label>',
                    filter: function(){
                        var ckbx = spawn('input#toggle-all-sessions.selectable-select-all|type=checkbox', {
                            checked: true,
                            value: '*',
                            on: [
                                ['click', function(){
                                    var ckbx = this;
                                    forEach(Object.keys(dqr.allSearchResults), function(uid){
                                        dqr.allSearchResults[uid].checked = !!ckbx.checked;
                                    });
                                }]
                            ]
                        });
                        return ckbxLabel(ckbx);
                    },
                    apply: function(){
                        var uid = this.studyInstanceUid;
                        var ckbx = spawn('input.select-session.selectable-select-one|type=checkbox', {
                            value: uid,
                            on: [
                                ['click', function(){
                                    dqr.allSearchResults[uid].checked = this.checked
                                }]
                            ]
                        });
                        ckbx.checked = firstDefined(dqr.allSearchResults[uid].checked, true);
                        return ckbxLabel(ckbx);
                    }
                },
                patientName: {
                    label: 'Patient Name',
                    filter: true,
                    sort: true,
                    apply: function(){
                        var patientName = this.patient.name.lastNameCommaFirstName.replace(/,/, '^');
                        return spawn('div.truncate', {
                            title: patientName
                        }, patientName)
                    }
                },
                RELABEL_PATIENT_NAME: extend(true, {}, relabelColumn, {
                    label: '<i class="fa fa-angle-double-right"></i>&nbsp;&nbsp;&nbsp;&nbsp;Relabel Patient Name',
                    apply: function(){
                        return spawn('input.relabel-patient-name|type=text')
                    }
                }),
                studyId: {
                    label: 'Study ID',
                    filter: true,
                    sort: true,
                    html: '<div class="truncate">__VALUE__</div>'
                },
                RELABEL_STUDY_ID: extend(true, {}, relabelColumn, {
                    label: '<i class="fa fa-angle-double-right"></i>&nbsp;&nbsp;&nbsp;&nbsp;Relabel Study ID',
                    apply: function(){
                        return spawn('input.relabel-study-id|type=text')
                    }
                }),
                studyDate: {
                    label: 'Study Date',
                    filter: true,
                    sort: true,
                    apply: function(){
                        var studyDateStr = this.studyDate + '';
                        return spawn('div.center.mono', this.studyDate ? [
                            // ['i.hidden.sort', studyDateStr],
                            studyDateStr.slice(0, 4) + '-' + studyDateStr.slice(4, 6) + '-' + studyDateStr.slice(6, 8)
                        ] : '<i class="hidden">0</i>&ndash;' )
                    }
                },
                MODALITY: {
                    label: 'Modality',
                    filter: true,
                    sort: true,
                    apply: function(){
                        // TODO: REPLACE MODALITY FILLER WITH VALUE WHEN AVAILABLE
                        if (/MR/i.test(this.patient.id)) {
                            return 'MR'
                        }
                        if (/PET/i.test(this.patient.id)) {
                            return 'PET'
                        }
                        if (/CT/i.test(this.patient.id)) {
                            return 'CT'
                        }
                        return randomFromArray(['MR', 'MR', 'CT', 'PET', 'PET', 'MR', 'MR', 'CT', 'PET', 'PET'])
                    }
                },
                // patientId: {
                //     label: 'Patient ID',
                //     filter: true,
                //     sort: true,
                //     apply: function(){
                //         return this.patient.id
                //     }
                // },
                accessionNumber: 'Accession Number'
            }
        });
        // .done(function(){
        //     if ($pacsSearchResults.has('table.data-table')) {
        //         return $pacsSearchResults;
        //     }
        //     this.render($pacsSearchResults.empty());
        // })
        resultsTable.render($pacsSearchResults.empty());

        // init the selectable stuff
        XNAT.app.selectableItems(resultsTable.table);

        $pacsSearchResults.spawn('br');

        $pacsSearchResults.spawn('button#import-selected-sessions.btn.btn1.pull-right|type=button', {
            html: 'Import Selected',
            on: [
                ['click', function(e){
                    e.preventDefault();
                    console.log('importing...');
                    var UIDs = [];
                    $pacsSearchResults.find('input.select-session:checked').each(function(){
                        UIDs.push(this.value);
                    });
                    XNAT.dialog.open({
                        title: 'Import from Pacs',
                        content: (function(){
                            console.log('confirm...');
                            return spawn('div#pacs-import-confirm', [
                                ['p', 'Sessions with the following study instance UIDs will be imported:'],
                                ['ul', '<li>' + UIDs.join('</li><li>') + '</li>']
                            ])
                        })(),
                        okLabel: 'Import'
                    })
                }]
            ]
        })

    }

    $searchSubmit.on('click', function(e){

        console.log('PACS search...');

        var selectedPacs = $selectPacsMenu.val();

        if (!selectedPacs) {
            XNAT.dialog.message('Error', 'Please select a PACS to query.', {
                okAction: function(obj){
                    // $selectPacsMenu.click();
                    // menuUpdate($selectPacsMenu);
                }
            });
            return;
        }

        var searchCriteria = $pacsSearchFields.getValues();
        searchCriteria.pacsId = selectedPacs;

        // console.log(searchCriteria);

        var searchUrl = XNAT.url.csrfUrl('/data/services/pacs/' + selectedPacs + '/search/studies');

        // console.log(searchUrl);

        XNAT.xhr.post({
            url: searchUrl,
            data: searchCriteria,
            success: function(json){
                renderResultsTable(json.ResultSet.Result);
            },
            failure: function(){
                console.warn('Error:');
                console.warn(arguments);
                XNAT.dialog.message('Error', 'No sessions were found for the specified search criteria.')
            }
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
            ['input|type=file|name=csv_to_store']
        ]);
        XNAT.dialog.open({
            title: 'Upload CSV',
            width: 320,
            content: fileForm,
            okLabel: 'Upload',
            okClose: false,
            okAction: function(obj){
                var postCsv = XNAT.xhr.post({
                    url: XNAT.url.csrfUrl('/xapi/dqr/csvimport/uploadCsv'),
                    data: (new FormData(fileForm)),
                    cache: false,
                    contentType: false,
                    processData: false,
                    success: function(data){
                        var results = [];
                        if (data.length) {
                            // pluck the data out of the response
                            forEach(data, function(item){
                                // since each search criteria returns its own
                                // results, it's necessary to
                                forEach(item.studies, function(study){
                                    results.push(study)
                                })
                            });
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