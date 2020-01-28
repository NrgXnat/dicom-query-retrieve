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

    function getPACSData(){
        return (dqr.getPACSData = XNAT.xhr.get('~/data/pacs'));
    }

    getPACSData();

    dqr.PACSData = {};

    function setupPACSList(data){
        data.forEach(function(PACS, i){
            dqr.PACSData[PACS.id + ''] = PACS;
        });
    }

    var PACSDataSample = {
        "ResultSet": {
            "Result": [
                {
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
                }
            ],
            "resultSetSize": 1
        }
    };


    $(function(){


        // render elements for any 'time' cells
        function renderTimeCell(time){
            return spawn('div.center.mono.nowrap', [
                ['span.hidden.time.sort-value', (time + '')],
                ['span.locale-string', (new Date(time)).toLocaleString().replace(', ', '<br>')]
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
            var PACSdata = dqr.PACSData[id + ''];
            return PACSdata ?
                PACSdata.label || PACSdata.aeTitle || '-' :
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
                className: 'xnat-table',
                style: { 'width': '100%' }
            }, opts.table));

            if (opts.header) {
                displayTable.tr(opts.header.tr || {});
                displayTable.th(opts.header.key || opts.header[0] || 'Key');
                displayTable.th(opts.header.value || opts.header[1] || 'Value');
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
                    if (data && data.seriesIds) {
                        data.seriesIds = data.seriesIds.split(',').join('<br>');
                    }
                    XNAT.dialog.open({
                        width: 800,
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
            // var itemId = this.getAttribute('href').split('#id=')[1];
            showItemData(XNAT.url.restUrl('/xapi/dqr/query/queue/request/' + itemId));
        }


        function removeQueuedItem(itemId){
            return XNAT.xhr
                       .delete('/xapi/dqr/query/queue/request/' + itemId)
                       .done(function(){
                           XNAT.ui.banner.top(2000, 'Item removed from queue.', 'success');
                           // re-render queue table
                           spawnImportQueuePanel();
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


        function spawnImportQueuePanel(count){

            var queueDisplayContainer$ = getById$('pacs-import-queue-display').html('loading...');

            // render queue
            XNAT.spawner
                .spawn(setupImportQueuePanel(count))
                .done(function(){
                    console.log(arguments);
                    // XNAT.plugins.dqr.selectableItemsDev(queueDisplayContainer$);
                    // XNAT.plugins.dqr.filterableItemsDev(queueDisplayContainer$);
                })
                .render(queueDisplayContainer$.empty());

        }

        function filterInput(name){
            return spawn('input.filter-input|type=text', {
                title: 'filter:' + name,
                style: { padding: '4px 6px', border: '1px solid #ccc' },
                data: { filter: name }
            });
        }

        // `/xapi/dqr/query/queue/user/ordered`
        var queueSample = [
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

        function setupImportQueuePanel(count){
            return {
                importQueuePanel: {
                    tag: 'div',
                    element: { title: 'PACS Import Queue' },
                    contents: {
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: '*/xapi/dqr/query/queue' + (dqr.adminView ? '/all' : '/user') + '/ordered/paged?t=' + Date.now(),
                            messages: {
                                noData: '<div class="message">There are no queued items to display.</div>'
                            },
                            apply: function(data){
                                var output = data;
                                if (!data.length) {
                                    console.log('nothing');
                                    return []
                                }
                                if (data.length > 100) {
                                    return output.slice(0, 100);
                                }
                                return output.map(function(item, i){
                                    (item.series_ids && item.series_ids.length) &&
                                    (item.series_ids = item.series_ids.split(','));
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
                                    td: { className: 'center show-data xnat_project' }
                                },
                                pacs_id: {
                                    label: 'PACS',
                                    sort: true,
                                    filter: true,
                                    td: { className: 'center show-data pacs_id' },
                                    apply: function(id){
                                        return spawn('span.pacs-label', resolvePACSLabel(id))
                                    }
                                },
                                // username: {
                                //     label: 'User',
                                //     td: { className: 'center' }
                                // },
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

        // queue panel rendered below

        // `/xapi/dqr/history/user`
        var historySample = [
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
            // var itemId = this.getAttribute('data-id') || this.getAttribute('href').split('#id=')[1];
            showItemData(XNAT.url.restUrl('/xapi/dqr/query/history/request/' + itemId));
        }


        function spawnImportHistoryPanel(count){

            var historyDisplayContainer$ = getById$('pacs-import-history-display').html('loading...');

            // render history
            XNAT.spawner
                .spawn(setupImportHistoryPanel(false))
                .done(function(){

                    var spawneri = this;

                    // only render a 'note' if there are more than 100 items
                    var note = count && count >= 100 ? spawn('div.info', {
                        style: {
                            marginBottom: '20px',
                            lineHeight: '28px',
                            verticalAlign: 'middle'
                        }
                    }, [
                        ['i', "Only the last 100 items are shown below. Click 'Show All' to view the entire import history."],
                        ['button.pull-right.float-right|type=button', {
                            on: [
                                ['click', function(e){
                                    XNAT.dialog.open({
                                        title: 'PACS Import History',
                                        content: XNAT.spawner.spawn(setupImportHistoryPanel(true)).get(),
                                        width: 1100,
                                        buttons: [
                                            {
                                                label: 'Close',
                                                isDefault: true,
                                                close: true
                                            }
                                        ]
                                    })
                                }]
                            ]
                        }, 'Show All'],
                        ['div.clear.clearfix']
                    ]) : '';

                    historyDisplayContainer$.empty().append(note).append(spawneri.done(function(){
                        console.log(this);
                        console.log(arguments);
                    }).get())

                });

        }


        function setupImportHistoryPanel(all){
            var historyUrl = '*/xapi/dqr/query/history' + (dqr.adminView ? '/all' : '/user');
            historyUrl += (!all ? '/paged?' : '?');
            historyUrl += ('t=' + Date.now());
            return {
                userImportHistoryPanel: {
                    tag: 'div#user-import-history-panel-container',
                    element: { title: 'PACS Import History' },
                    contents: {
                        // pacsImportHistoryMessage: {
                        //     tag: 'div#pacs-import-history-message.info'
                        // },
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: historyUrl,
                            messages: {
                                noData: '<div class="message">There are no import records to display.</div>'
                            },
                            apply: function(data){
                                console.log(data);
                                var history = (data && data.length) ? data.reverse() : [];
                                return all ? history : history.slice(0, 100);
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
                                // only render "User" column for admin view
                                username: !dqr.adminView ? '~!' : {
                                    label: 'User',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center' }
                                },
                                xnatProject: {
                                    label: 'Project',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center show-data xnatProject' }
                                },
                                pacsId: {
                                    label: 'PACS',
                                    filter: true,
                                    sort: true,
                                    td: { className: 'center show-data pacsId' },
                                    apply: function(id){
                                        return spawn('span.pacs-label', resolvePACSLabel(id))
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


        dqr.getPACSData.done(function(json){

            var pacsData = json && json.ResultSet && json.ResultSet.Result ? json.ResultSet.Result : [];

            setupPACSList(pacsData);

            spawnImportQueuePanel(pacsData.length);
            spawnImportHistoryPanel(pacsData.length);

        });



        // replace ANY part of the url hash with another value
        function updateHashPart(hash, key, value, delim){

            var oldPart, newPart;

            hash = hash || window.location.hash || '#';
            hash = '#' + hash.split('#').slice(1).join('#');

            // both key and value are REQUIRED
            if (!key || value === undef) return hash;

            if (hash.indexOf(key) === -1) {
                hash = (hash + key + value);
            }
            else {
                delim   = delim !== undef ? delim : /#|\/#/;
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
            // make sure key starts with '#' and ends with '='
            key = key.replace(/^#*/, '#').replace(/=*$/, '=');
            return updateHashPart(window.location.hash, key, value);
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