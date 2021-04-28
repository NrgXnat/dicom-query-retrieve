/*
 * dicom-query-retrieve: queue.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/**
 * View queued imports
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

    console.log('dqr/queue.js');

    var dqr, undef;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.dqr = dqr =
        getObject(XNAT.plugin.dqr || {});


    dqr.adminView = window.isAdmin && getQueryStringValue('role') === 'admin';

    // objects to hold data and metadata for queue and history tables
    dqr.queue   = {};
    dqr.history = {};

    var PAGE = +getQueryStringValue('page') || +getUrlHashValue('page=');
    var SIZE = +getQueryStringValue('size') || +getUrlHashValue('size=') || 100;

    // shortcuts for basic element selection
    function getById(id){
        return document.getElementById(id);
    }
    function getById$(id){
        return $(getById(id));
    }
    function getByClassName(cls){
        return document.getElementsByClassName(cls);
    }
    function getByClassName$(cls){
        return $(getByClassName(cls));
    }

    function getPacsList(){
        return (dqr.getPacsList = XNAT.xhr.get(XNAT.url.rootUrl('/xapi/pacs')));
    }

    getPacsList();

    dqr.pacsList = {};

    function setupPACSList(data){
        data.forEach(function(PACS, i){
            dqr.pacsList[PACS.id + ''] = PACS;
        });
    }

    /*
    var pacsInfoSample = {
        "host": "10.1.1.1",
        "label": "ORTHANC",
        "aeTitle": "ORTHANC",
        "defaultStoragePacs": true,
        "defaultQueryRetrievePacs": true,
        "queryable": true,
        "ormStrategySpringBeanId": "dicomOrmStrategy",
        "storable": true,
        "queryRetrievePort": 4242,
        "supportsExtendedNegotiations": false,
        "timestamp": 1559097735082,
        "enabled": true,
        "created": 1559097735082,
        "id": 1,
        "disabled": 0
    };
    */

    function formatDate(timestamp){
        var dateString = new Date(timestamp);
        if (dateString) {
            return dateString.toISOString().replace('T', ' ').replace('Z', ' ').split('.')[0];
            // return dateString.toLocaleString();
        }
        else {
            return 'Unknown Date';
        }
    }

    function localeDate(time){
        return (new Date(time)).toLocaleString()
    }


    $(function(){


        // render elements for any 'time' cells
        function renderTimeCell(time){
            return spawn('div.center.mono.nowrap', [
                ['span.hidden.time.sort-value', (time + '')],
                ['span.locale-string', localeDate(time).replace(', ', '<br>')]
            ])
        }


        function renderDayCell(str){
            // make sure there are no hyphens
            var dateStr = (str || '').replace(/-/g, '') || 0;
            var isoDate = dateStr ? dateStr.slice(0, 4) + '-' + dateStr.slice(4, 6) + '-' + dateStr.slice(6, 8) : '&ndash;';
            // var studyDay = new Date(isoDate + 'T00:00');
            // var time = studyDay.getTime();
            return spawn('div.center.mono', [
                ['span.hidden.date.sort-value', (dateStr + '')],
                ['span.iso-date', isoDate]
            ])
        }


        function resolvePACSLabel(id){
            var pacsInfo = dqr.pacsList[id + ''];
            return pacsInfo ?
                pacsInfo.label || pacsInfo.aeTitle || '-' :
                '-'
        }


        var dataDisplayModel = {
            // config for the actual <table> element
            table: { title: 'Ur Datas' },
            // config for header
            headerO: {
                tr: { addClass: 'ur-datas-header' },
                key: {
                    addClass: 'left',
                    html: '<b>Key</b>'
                },
                value: {
                    addClass: 'left',
                    html: '<b>Value</b>'
                }
            },
            // text for header row cells
            header: ['Key', 'Value'],
            // do not render a header
            header_: false,
            // `items` can be a simple comma-separated list...
            items: 'item1, item2, item3',
            // ...or an actual array (safer)
            items_: ['item1', 'item2', 'item3'],
            // if there's no `items` property, then all
            // object properties will be rendered in the table
            rows: {
                item1: 'Foo',
                item2: 'Bar',
                item3: 'Baz'
            }
        };


        function dataDisplay(opts){

            var displayTable = XNAT.table(extend(true, {
                className: 'xnat-table compact',
                style: { 'width': '100%' }
            }, opts.table));

            if (opts.header) {
                displayTable.tr(opts.header.tr || {});
                displayTable.th({ classes: 'left' }, [spawn('b', opts.header.key || opts.header[0] || 'Key')]);
                displayTable.th({ classes: 'left' }, [spawn('b', opts.header.value || opts.header[1] || 'Value')]);
            }

            var rowData = opts.rows || opts.data;

            var rows = (function(){
                if (opts.items) {
                    if (Array.isArray(opts.items)) {
                        return opts.items
                    }
                    else {
                        return opts.items.split(',').map(function(item, i){
                            return item.trim()
                        })
                    }
                }
                else {
                    return Object.keys(rowData);
                }
            })();


            rows.length && forEach(rows, function(key, i){

                console.log(key);

                displayTable.tr(opts.trs || opts.tr || {});
                displayTable.td('<b>' + key + '</b>');

                var value = rowData[key];
                var cell  = extend(true, {
                    html: ''
                }, opts.tds, opts.td);

                if (stringable(value)) {
                    cell.html = value + '';
                }
                else if (Array.isArray(value)) {
                    cell.html = value.join('<br>');
                }
                else {
                    try {
                        cell.textContent = (JSON.stringify(value));
                    }
                    catch(e) {
                        console.warn(e);
                    }
                }

                displayTable.td(cell);

            });

            return displayTable.get();

        }


        function showItemData(url, callback){
            return XNAT.xhr.get({
                url: url,
                success: function(data){
                    console.log(data);
                    data = getObject(data);
                    if (data.seriesIds) {
                        data.seriesIds = spawn('pre|style=margin:0', data.seriesIds.join(',\n')).outerHTML
                    }
                    if (data.remappingScript) {
                        data.remappingScript = spawn('pre|style=margin:0', data.remappingScript).outerHTML
                    }
                    XNAT.dialog.open({
                        width: 800,
                        title: 'Query to ' + resolvePACSLabel(data.pacsId) + ' on ' + localeDate(data.queuedTime),
                        content: dataDisplay({
                            header: {
                                key: 'Key',
                                value: 'Value'
                            },
                            tds: {
                                addClass: 'mono'
                            },
                            rows: data
                        }),
                        buttons: [
                            {
                                label: 'Close',
                                close: true,
                                isDefault: true
                            }
                        ]
                    });
                }
            });
        }

        // var containerSelector = '#pacs-queue-history-tabs > .xnat-tab-container';
        // var $tabsContainer    = $(containerSelector);

        // `/xapi/dqr/query/queue/user/ordered`
        var queueItemSample = [
            {
                'queue_location': 1,
                'id': 3,
                'created': 1558034448618,
                'disabled': 0,
                'enabled': true,
                'timestamp': 1558034448618,
                'destination_ae_title': 'XNAT',
                'pacs_id': 1,
                'priority': 1,
                'queued_time': 1558034448617,
                'series_ids': '1.3.46.670589.11.5730.5.0.3144.2010043014121521435',
                'status': 'QUEUED',
                'study_instance_uid': '1.3.46.670589.11.5730.5.0.1744.2010043012343685002',
                'username': 'admin',
                'xnat_project': 'foo'
            }
        ];


        function showQueuedItemData(e){
            e.preventDefault();
            e.stopImmediatePropagation();
            var itemId = $(this).closest('tr').attr('data-id');
            showItemData(XNAT.url.restUrl('/xapi/dqr/query/queue/request/' + itemId));
        }


        function removeQueuedItem(itemId){
            return XNAT.xhr
                       .delete('/xapi/dqr/query/queue/request/' + itemId)
                       .done(function(){
                           XNAT.ui.banner.top(2000, 'Item removed from queue.', 'success');
                           // re-render queue table
                           spawnImportQueuePanel(false, 0, SIZE);
                       })
                       .fail(function(){
                           console.warn(arguments);
                           XNAT.ui.banner.top(3000, 'An error occured. Item not removed');
                       });
        }


        function removeItemDialog(e){
            e.preventDefault();

            var itemId = this.getAttribute('href').split('#id=')[1];

            XNAT.dialog.open({
                title: 'Remove queued item?',
                width: 400,
                content: '' +
                    'Would you like to remove the item from the queue? This will abort the import ' +
                    'for this item and shift the remaining queued items up in the queue.',
                buttons: [
                    {
                        label: 'Remove',
                        isDefault: true,
                        close: false,
                        action: function(dlg){
                            removeQueuedItem(itemId).always(function(){
                                dlg.close();
                            });
                        }
                    },
                    {
                        label: 'Cancel',
                        close: true
                    }
                ]
            });

        }


        /**
         * Get size of queue or history list to setup UI elements for table paging
         * @param part string - url part for request ('queue' or 'history')
         * @param [fn] Function - optional callback function
         * @returns {*}
         */
        function getListSize(part, fn){
            var listSizeReq = XNAT.xhr.get(XNAT.url.rootUrl('/xapi/dqr/query/' + part + (dqr.adminView ? '/all' : '/user') + '/count'));
            if (fn && isFunction(fn)) {
                listSizeReq.done(fn)
            }
            return listSizeReq;
        }


        // `/xapi/dqr/query/queue/user/ordered`
        var queueItemSample = [
            {
                "queue_location": 1,
                "id": 26,
                "created": 1564174012066,
                "disabled": 0,
                "enabled": true,
                "timestamp": 1564174012066,
                "destination_ae_title": "XNAT",
                "pacs_id": 1,
                "priority": 1,
                "queued_time": 1564174012065,
                "series_ids": "2.25.8386701303819023546100169065602519914,2.25.269396136096240831489002806637088868931,2.25.248440088715515049566137981943890644330",
                "status": "QUEUED",
                "study_instance_uid": "2.25.327336864675041433324955161893738398200",
                "username": "admin",
                "xnat_project": "foo",
                "patient_id": "Chong",
                "patient_name": "Chong",
                "study_date": "20171128"
            }
        ];


        function setupQueueUrl(all, page, size){
            var queueUrl = '/xapi/dqr/query/queue' + (dqr.adminView ? '/all' : '/user') + '/ordered';
            var queueUrlQuery = [];
            if (!all) {
                queueUrl += ('/paged');
                if (page) queueUrlQuery.push('page=' + (page || PAGE));
                if (size) queueUrlQuery.push('size=' + (size || SIZE));
            }
            queueUrlQuery.push('t=' + Date.now());
            return XNAT.url.rootUrl(queueUrl + '?' + queueUrlQuery.join('&'));
        }


        function setupImportQueuePanel(queueData, statusText, xhrObj){

            window.jsdebug && console.log(queueData);

            var hasData = queueData && queueData.length;

            return {
                importQueuePanel: {
                    tag: 'div#pacs-import-queue-panel-container.table-container|title=PACS Import Queue',
                    contents: !hasData ? {
                        importQueueMessage: {
                            tag: 'div#pacs-import-queue-message.info',
                            content: 'There are no queued items to display.'
                        }
                    } : {
                        // importQueueTableNav: setupTableNav('queue'),
                        importQueueTable: {
                            kind: 'table.dataTable',
                            data: hasData ? queueData : [],
                            // messages: {
                            //     noData: '<div class="message">There are no queued items to display.</div>'
                            // },
                            apply: function(data){
                                var output = data;
                                if (!data.length) {
                                    console.log('nothing');
                                    return []
                                }
                                if (data.length > SIZE) {
                                    return output.slice(0, SIZE);
                                }
                                return output.map(function(item, i){
                                    // (item.series_ids && item.series_ids.length) &&
                                    // (item.series_ids = item.series_ids.split(','));
                                    return item;
                                });
                                // return data.map(function(item, i){
                                //     data.series_ids = data.series_ids.split(',')
                                // })
                            },
                            table: {
                                classes: 'highlight click-rows',
                                // click events will be delegated to the parent <table> element
                                // since the elements they apply to are dynamically rendered
                                on: [
                                    ['click', 'td.show-data', showQueuedItemData],
                                    // ['click', 'a.show-queue-item-data', showQueuedItemData],
                                    ['click', 'a.remove-queue-item', removeItemDialog]
                                ]
                            },
                            order: [
                                // 'CKBX',
                                'queue_location',
                                'id',
                                // 'priority',
                                // 'status',
                                'queued_time',
                                'patient_name',
                                'study_date',
                                'xnat_project',
                                'pacs_id',
                                'destination_ae_title',
                                'REMOVE'
                            ],
                            items: {
                                _id: '~data-id',
                                _pacs_id: '~data-pacs-id',
                                // TODO: select multiple items for deletion
                                // CKBX: {
                                //     label: '<input type="checkbox" id="select-all-queue-items" class="selectable-all">',
                                //     th: { style: { width: '50px' } },
                                //     td: { className: 'center' },
                                //     apply: function(){
                                //         return spawn('input.selectable-one|type=checkbox', {
                                //             value: (this.id + '')
                                //         })
                                //     }
                                // },
                                queue_location: {
                                    label: 'Position',
                                    sort: true,
                                    th: { style: { width: '80px' } },
                                    td: { className: 'show-data' },
                                    apply: function(loc){
                                        return spawn('div.center.mono', [
                                            ['span.hidden.sort.sort-value', zeroPad(loc, 8)],
                                            loc
                                        ])
                                    }
                                },
                                id: {
                                    label: 'ID',
                                    sort: true,
                                    th: { style: { width: '80px' } },
                                    td: { className: 'show-data' },
                                    apply: function(id){
                                        return spawn('div.center.mono', [
                                            ['span.hidden.sort.sort-value', zeroPad(id, 8)],
                                            ['a.link.show-queue-item-data', {
                                                attr: { href: '#id=' + id }
                                            }, id + '']
                                        ])
                                    }
                                },
                                // priority: {
                                //     label: 'Priority',
                                //     sort: true,
                                //     th: { style: { width: '80px' } },
                                //     td: { className: 'center mono show-data'}
                                // },
                                // status: {
                                //     label: 'Status',
                                //     sort: true,
                                //     td: { className: 'center show-data' }
                                // },
                                queued_time: {
                                    label: 'Queued',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'queued-time show-data queued_time' },
                                    apply: renderTimeCell
                                },
                                patient_name: {
                                    label: 'Patient Name',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'patient-name show-data patient_name' },
                                    apply: function(name){
                                        return spawn('div.truncate', name);
                                    }
                                },
                                study_date: {
                                    label: 'Study Date',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'study-date show-data nowrap study_date' },
                                    apply: renderDayCell
                                },
                                xnat_project: {
                                    label: 'Project',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'show-data xnat_project' }
                                },
                                USER: dqr.adminView ? {
                                    label: 'User',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'show-data USER' },
                                    apply: function(){
                                        return spawn('div.nowrap', this.username);
                                    }
                                } : '~!',
                                pacs_id: {
                                    label: 'PACS',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'center show-data pacs_id' },
                                    apply: function(id){
                                        return spawn('span.pacs-label', resolvePACSLabel(id))
                                    }
                                },
                                destination_ae_title: {
                                    label: 'Dest. AE',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'center show-data destination_ae_title' }
                                },
                                REMOVE: {
                                    label: 'Remove',
                                    th: { style: { width: '70px' } },
                                    td: { className: 'center remove-data' },
                                    apply: function(){
                                        return spawn('a.remove-queue-item.nolink.btn-hover', {
                                            attr: { href: '#id=' + this.id },
                                            title: 'Remove queued item'
                                        }, [
                                            ['b.x', '&times;']
                                        ])
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }


        function spawnImportQueuePanel(all, page, size){

            var queueDisplayContainer$ = getById$('pacs-import-queue-display').html('loading...');

            XNAT.xhr.get(setupQueueUrl(all, page, size)).done(function(queueData){

                getListSize('queue').done(function(queueSize){

                    // render queue
                    XNAT.spawner
                        .spawn(setupImportQueuePanel(queueData))
                        .done(function(){
                            console.log(arguments);

                            var spawneri = this;

                            // only render a 'note' if there are more than 100 items
                            var note = queueSize > size ? spawn('div.info', {
                                style: {
                                    marginBottom: '20px',
                                    lineHeight: '28px',
                                    verticalAlign: 'middle'
                                }
                            }, [
                                ['i', "Only the " + size + " most recent queued imports are shown below. Click 'Show More' to view more queue entries."],
                                ['button.pull-right.float-right|type=button', {
                                    on: [
                                        ['click', function(e){
                                            // a 'wait' dialog will show if rendering the dialog takes more than 1 second
                                            var waiting = window.setTimeout(XNAT.dialog.loading.open, 1000);
                                            XNAT.xhr.get(setupQueueUrl(false, 0, 10000)).done(function(allQueueData){
                                                XNAT.dialog.open({
                                                    width: 1200,
                                                    title: 'Queued PACS Imports',
                                                    content: '' ||
                                                        XNAT.spawner
                                                            .spawn(setupImportQueuePanel(allQueueData))
                                                            .done(function(){
                                                                window.clearTimeout(waiting);
                                                                XNAT.dialog.loading.close();
                                                            })
                                                            .get(),
                                                    buttons: [
                                                        {
                                                            label: 'Close',
                                                            isDefault: true,
                                                            close: true
                                                        }
                                                    ]
                                                })
                                            })
                                        }]
                                    ]
                                }, 'Show More'],
                                ['div.clear.clearfix']
                            ]) : '';

                            // XNAT.plugins.dqr.selectableItems(queueDisplayContainer$);
                            // XNAT.plugins.dqr.filterableItems(queueDisplayContainer$);

                            queueDisplayContainer$.empty().append(note).append(spawneri.done(function(){
                                window.jsdebug && console.log(this);
                                window.jsdebug && console.log(arguments);
                            }).get())

                        });

                })

            });

        }
        dqr.spawnImportQueuePanel = spawnImportQueuePanel;


        function filterInput(name){
            return spawn('input.filter-input|type=text', {
                title: 'filter:' + name,
                style: { padding: '4px 6px', border: '1px solid #ccc' },
                data: { filter: name }
            });
        }

        // queue panel rendered below

        // `/xapi/dqr/history/user`
        var historyItemSample = [
            {
                "executedTime": 1557865056364,
                "username": "bob",
                "status": "ISSUED",
                "pacsId": 1,
                "destinationAeTitle": "XNAT",
                "xnatProject": "foo",
                "studyInstanceUid": "1.3.46.670589.11.5730.5.0.7888.2010041913494343000",
                "seriesIds": "1.3.46.670589.11.5730.5.0.10204.2010041914553321484",
                "queuedTime": 1557865039948,
                "enabled": true,
                "created": 1557865056364,
                "timestamp": 1557865056364,
                "id": 2,
                "disabled": 0
            }
        ];


        function showHistoryItemData(e){
            e.preventDefault();
            e.stopImmediatePropagation();
            var itemId = $(this).closest('tr').attr('data-id');
            showItemData(XNAT.url.restUrl('/xapi/dqr/query/history/request/' + itemId));
        }


        function setupHistoryUrl(all, page, size){
            var historyUrl = '/xapi/dqr/query/history' + (dqr.adminView ? '/all' : '/user');
            var historyUrlQuery = [];
            if (!all) {
                historyUrl += ('/paged');
                if (page) historyUrlQuery.push('page=' + (page || PAGE));
                if (size) historyUrlQuery.push('size=' + (size || SIZE));
            }
            historyUrlQuery.push('t=' + Date.now());
            return XNAT.url.rootUrl(historyUrl + '?' + historyUrlQuery.join('&'));
        }


        function getHistory(all, fn){
            var historyReq = XNAT.xhr.get(setupHistoryUrl())
        }


        function setupImportHistoryPanel(historyData, statusText, xhrObj){

            window.jsdebug && console.log(historyData);

            var hasData = !!(historyData && historyData.length);

            return {
                importHistoryPanel: {
                    tag: 'div#pacs-import-history-panel-container.table-container|title=PACS Import History',
                    contents: !hasData ? {
                        importHistoryMessage: {
                            tag: 'div#pacs-import-history-message.info',
                            content: 'There are no import history records to display.'
                        }
                    } : {
                        // importHistoryTableNav: hasData && historyData.length > SIZE ? setupTableNav('history') : {},
                        importHistoryTable: {
                            kind: 'table.dataTable',
                            // data: hasData ? sortObjectsNumeric(historyData, 'executedTime') : [],
                            data: historyData || [],
                            apply: function(data){
                                console.log('history table data');
                                console.log(data);
                                return data;
                            },
                            table: {
                                classes: 'highlight click-rows',
                                on: [
                                    // ['click', '.show-history-item-data', showHistoryItemData],
                                    ['click', 'td.show-data', showHistoryItemData]
                                ]
                            },
                            items: {
                                _id: '~data-id',
                                _pacsId: '~data-pacs-id',
                                id: {
                                    label: 'ID',
                                    sort: true,
                                    td: { className: 'show-data' },
                                    apply: function(id){
                                        return spawn('div.center.mono', [
                                            ['span.hidden.sort.sort-value', zeroPad(id, 8)],
                                            ['a.link.show-history-item-data', {
                                                attr: { href: '#id=' + id }
                                            }, id + '']
                                        ])
                                    }
                                },
                                status: {
                                    label: 'Status',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center show-data status' }
                                },
                                queuedTime: {
                                    label: 'Queued',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'center show-data queuedTime' },
                                    apply: renderTimeCell
                                },
                                executedTime: {
                                    label: 'Executed',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'center show-data executedTime' },
                                    apply: renderTimeCell
                                },
                                patientName: {
                                    label: 'Patient Name',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'show-data patientName' },
                                    apply: function(name){
                                        return spawn('div.truncate', name);
                                    }
                                },
                                studyDate: {
                                    label: 'Study Date',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'show-data nowrap studyDate' },
                                    apply: renderDayCell
                                },
                                xnatProject: {
                                    label: 'Project',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'show-data xnatProject' }
                                },
                                // only render "User" column for admin view
                                USER: dqr.adminView ? {
                                    label: 'User',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'show-data USER' },
                                    apply: function(){
                                        return spawn('div.nowrap', this.username);
                                    }
                                } : '~!',
                                pacsId: {
                                    label: 'PACS',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center show-data pacsId' },
                                    apply: function(pacsId){
                                        return spawn('span.pacs-label', resolvePACSLabel(pacsId))
                                    }
                                },
                                destinationAeTitle: {
                                    label: 'Dest. AE',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center show-data destinationAeTitle' }
                                }
                            }
                        }
                    }
                }
            };
        }


        function spawnImportHistoryPanel(all, page, size){

            var historyDisplayContainer$ = getById$('pacs-import-history-display').html('loading...');

            // get the data BEFORE rendering the table
            XNAT.xhr.get(setupHistoryUrl(all, page, size)).done(function(historyData){

                getListSize('history').done(function(historySize){

                    // render history
                    XNAT.spawner
                        .spawn(setupImportHistoryPanel(historyData))
                        .done(function(){

                            var spawneri = this;

                            // only render a 'note' if there are more than 100 items
                            var note = historySize > size ? spawn('div.info', {
                                style: {
                                    marginBottom: '20px',
                                    lineHeight: '28px',
                                    verticalAlign: 'middle'
                                }
                            }, [
                                ['i', "Only the " + size + " most recent imports are shown below. Click 'Show More' to view more history entries."],
                                ['button.pull-right.float-right|type=button', {
                                    on: [
                                        ['click', function(e){
                                            // a 'wait' dialog will show if rendering the dialog takes more than 1 second
                                            var waiting = window.setTimeout(XNAT.dialog.loading.open, 1000);
                                            XNAT.xhr.get(setupHistoryUrl(false, 0, 10000)).done(function(allHistoryData){
                                                XNAT.dialog.open({
                                                    title: 'PACS Import History',
                                                    content: '' ||
                                                        XNAT.spawner
                                                            .spawn(setupImportHistoryPanel(allHistoryData))
                                                            .done(function(){
                                                                window.clearTimeout(waiting);
                                                                XNAT.dialog.loading.close();
                                                            })
                                                            .get(),
                                                    width: 1200,
                                                    buttons: [
                                                        {
                                                            label: 'Close',
                                                            isDefault: true,
                                                            close: true
                                                        }
                                                    ]
                                                })
                                            })
                                        }]
                                    ]
                                }, 'Show More'],
                                ['div.clear.clearfix']
                            ]) : '';

                            historyDisplayContainer$.empty().append(note).append(spawneri.done(function(){
                                console.log(this);
                                console.log(arguments);
                            }).get())

                        });


                });

            })

        }
        dqr.spawnImportHistoryPanel = spawnImportHistoryPanel;


        dqr.getPacsList.done(function(json){

            // var pacsList = json && json.ResultSet && json.ResultSet.Result ? json.ResultSet.Result : [];
            var pacsList = json || [];

            if (pacsList.length) {

                setupPACSList(pacsList);

                spawnImportQueuePanel(false, 0, SIZE);
                spawnImportHistoryPanel(false, PAGE, SIZE);

            }
            else {
                console.warn('There are no PACS configured for import.')
            }

        });



        // replace ANY part of the url hash with another value
        function updateHashPart(hash, key, value, delim){

            var oldPart, newPart;

            hash = hash || window.location.hash || '#';
            hash = '#' + hash.split(/#+/).slice(1).join('#');

            // both key and value are REQUIRED
            if (!key || value === undef) return hash;

            if (hash.indexOf(key) === -1) {
                hash = (hash + key + value);
            }
            else {
                delim   = delim !== undef ? delim : /\/*#\/*|#+/;
                oldPart = key + hash.split(key)[1].split(delim)[0];
                newPart = key + value;
                hash    = hash.replace(oldPart, newPart);
            }

            hash = hash.replace(/^#*/, '#'); // only one '#' at the beginning, please

            return (window.location.hash = hash)

        }


        // update parameter(s) stored in the url hash in the format
        // #foo=bar
        function updateHashQuery(key, value){

            var hashParts = window.location.hash.split(/#+/).map(function(query, i){

                if (!query) return '';

                var queryParts = query.split(/=+/);
                var paramName, paramValue;

                if (queryParts.length) {
                    paramName  = queryParts[0].trim();
                    paramValue = paramName === key ? '' + firstDefined(value, '') : queryParts[1].trim();
                }

                return [paramName, paramValue].join('=');

            });

            return (window.location.hash = hashParts.join('#'));

            // make sure key starts with '#' and ends with '='
            // key = key.replace(/^#*/, '#').replace(/=*$/, '=');
            // return updateHashPart(window.location.hash, key, value);
        }


        // only update the hash query if it's *not* already present
        !getUrlHashValue('tab=') && updateHashQuery('tab', 'queue');


        // tabSpawn.done(function(){
        //
        //     console.log(this);
        //     console.log(arguments);
        //
        //     tabSpawn.render($tabsContainer, function(){
        //
        //         console.log(this);
        //         console.log(arguments);
        //
        //         var selectTab   = getUrlHashValue('tab=');
        //         var tabSelector = containerSelector + ' ' + (selectTab ? 'li[data-tab="' + selectTab + '"]' : 'li[data-tab]');
        //
        //         // XNAT.ui.tab.activate(getUrlHashValue('tab='), spawned);
        //
        //         waitForElement(1, tabSelector, function(){
        //             $('#tabs-loading').remove();
        //             $tabsContainer.find(tabSelector).first().trigger('click');
        //         });
        //
        //     })
        // });

    });

    return (XNAT.plugin.dqr = dqr)

}));

//# sourceURL=browsertools://scripts/dqr/queue.js