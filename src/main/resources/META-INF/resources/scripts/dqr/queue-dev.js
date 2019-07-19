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
            return spawn('div.center.mono', [
                ['span.hidden.time.sort-value', (time + '')],
                ['span.locale-string', (new Date(time)).toLocaleString().replace(', ', '<br>')]
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
            var rows;

            if (opts.items) {
                if (Array.isArray(opts.items)) {
                    rows = opts.items
                }
                else {
                    rows = opts.items.split(',').map(function(item, i){
                        return item.trim()
                    })
                }
            }
            else {
                rows = Object.keys(rowData);
            }

            forEach(rows, function(key, i){

                displayTable.tr(opts.tr);
                displayTable.td('<b>' + key + '</b>');

                var value = rowData[key];
                var cell = extend(true, {
                    html: ''
                }, opts.td);

                if (stringable(value)) {
                    cell.html = value + '';
                }
                else if (Array.isArray(value)) {
                    cell.html = value.join(', ');
                }
                else {
                    try {
                        cell.html = (JSON.stringify(value))
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
                    XNAT.dialog.open({
                        width: 800,
                        content: dataDisplay({
                            header: {
                                key: 'Key',
                                value: 'Value'
                            },
                            td: {
                                addClass: 'mono'
                            },
                            rows: extend(data, {
                                seriesIds: data.seriesIds.split(',').join('<br>')
                            })
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

        // `/xapi/dqr/query/queueWithOrder/user`
        var queueSample = [
            {
                "queue_location": 1,
                "id": 3,
                "created": 1558034448618,
                "disabled": 0,
                "enabled": true,
                "timestamp": 1558034448618,
                "destination_ae_title": "XNAT",
                "pacs_id": 1,
                "priority": 1,
                "queued_time": 1558034448617,
                "series_ids": "1.3.46.670589.11.5730.5.0.3144.2010043014121521435",
                "status": "QUEUED",
                "study_instance_uid": "1.3.46.670589.11.5730.5.0.1744.2010043012343685002",
                "username": "admin",
                "xnat_project": "foo"
            }
        ];


        function showQueuedItemData(e){
            e.preventDefault();
            var itemId = this.getAttribute('href').split('#id=')[1];
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


        function spawnImportQueuePanel(admin){

            var queueDisplayContainer$ = getById$('pacs-import-queue-display');

            // render queue
            XNAT.spawner
                .spawn(setupImportQueuePanel(admin))
                .render(queueDisplayContainer$.empty())
                .done(function(){
                    XNAT.plugins.dqr.selectableItemsDev(queueDisplayContainer$);
                    // XNAT.plugins.dqr.filterableItems(queueDisplayContainer$);
                });

        }


        function setupImportQueuePanel(admin){
            return {
                importQueuePanel: {
                    tag: 'div',
                    element: { title: 'PACS Import Queue' },
                    contents: {
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: '*/xapi/dqr/query/queueWithOrder' + (admin ? '' : '/user'),
                            apply: function(data){
                                if (!data.length) {
                                    console.log('nothing')
                                }
                            },
                            table: {
                                // click events will be delegated to the parent <table> element
                                // since the elements they apply to are dynamically rendered
                                on: [
                                    ['click', 'a.show-queue-item-data', showQueuedItemData],
                                    ['click', 'a.remove-queue-item', removeItemDialog]
                                ]
                            },
                            items: {
                                _id: '~data-id',
                                _pacs_id: '~data-pacs-id',
                                CKBX: {
                                    label: '<input type="checkbox" id="select-all-queue-items" class="selectable-all">',
                                    th: { style: { width: '50px' } },
                                    td: { className: 'center' },
                                    apply: function(){
                                        return spawn('input.selectable-one|type=checkbox', {
                                            value: (this.id + '')
                                        })
                                    }
                                },
                                queue_location: {
                                    label: 'Position',
                                    sort: true,
                                    th: { style: { width: '80px' } },
                                    td: { className: 'center mono' }
                                    // apply: function(loc){
                                    //     return spawn('div.center', loc)
                                    // }
                                },
                                priority: {
                                    label: 'Priority',
                                    sort: true,
                                    th: { style: { width: '80px' } },
                                    td: { className: 'center mono' }
                                },
                                status: {
                                    label: 'Status',
                                    sort: true,
                                    td: { className: 'center' }
                                },
                                id: {
                                    label: 'ID',
                                    sort: true,
                                    th: { style: { width: '80px' } },
                                    td: { className: 'center mono' },
                                    apply: function(id){
                                        return spawn('a.link.show-queue-item-data', {
                                            attr: { href: '#id=' + id }
                                        }, id + '')
                                    }
                                },
                                xnat_project: {
                                    label: 'Project',
                                    sort: true,
                                    td: { className: 'center' }
                                },
                                pacs_id: {
                                    label: 'PACS',
                                    sort: true,
                                    td: { className: 'center' },
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
                                    td: { className: 'center' }
                                },
                                REMOVE: {
                                    label: 'Remove',
                                    th: { style: { width: '70px' } },
                                    td: { className: 'center' },
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


        function adminImportQueuePanel(){
            return $.extend(true, {}, setupImportQueuePanel(), {
                importQueuePanel: {
                    contents: {
                        pacsQueueTable: {
                            load: '*/xapi/dqr/query/queueWithOrder'
                        }
                    }
                }
            })
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
            var itemId = this.getAttribute('data-id') || this.getAttribute('href').split('#id=')[1];
            showItemData(XNAT.url.restUrl('/xapi/dqr/query/history/request/' + itemId));
        }


        function spawnImportHistoryPanel(admin){

            var historyDisplayContainer$ = getById$('pacs-import-history-display');

            // render history
            XNAT.spawner
                .spawn(setupImportHistoryPanel(admin))
                .render(historyDisplayContainer$.empty());

        }


        function setupImportHistoryPanel(admin){
            return {
                userImportHistoryPanel: {
                    tag: 'div',
                    element: { title: 'PACS Import History' },
                    contents: {
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: '*/xapi/dqr/query/history' + (admin ? '' : '/user'),
                            apply: function(data){
                                console.log(data);
                                return data
                            },
                            table: {
                                on: [
                                    ['click', '.show-history-item-data', showHistoryItemData],
                                    ['click', 'tr[data-id]', showHistoryItemData]
                                ]
                            },
                            items: {
                                _id: '~data-id',
                                _pacsId: '~data-pacs-id',
                                id: {
                                    label: 'ID',
                                    sort: true,
                                    td: { className: 'center mono' },
                                    apply: function(id){
                                        return spawn('a.link.show-history-item-data', {
                                            attr: { href: '#id=' + id }
                                        }, id + '')
                                    }
                                },
                                status: {
                                    label: 'Status',
                                    filter: true,
                                    sort: true//,
                                    // td: { className: 'center' }
                                },
                                queuedTime: {
                                    label: 'Queued',
                                    sort: true,
                                    apply: renderTimeCell
                                },
                                executedTime: {
                                    label: 'Executed',
                                    sort: true,
                                    apply: renderTimeCell
                                },
                                // username: {
                                //     label: 'User',
                                //     filter: true,
                                //     sort: true,
                                //     td: { className: 'center' }
                                // },
                                xnatProject: {
                                    label: 'Project',
                                    filter: true,
                                    sort: true//,
                                    // td: { className: 'center' }
                                },
                                pacsId: {
                                    label: 'PACS',
                                    filter: true,
                                    sort: true,
                                    // td: { className: 'center' },
                                    apply: function(id){
                                        return spawn('span.pacs-label', resolvePACSLabel(id))
                                    }
                                },
                                destinationAeTitle: {
                                    label: 'Dest. AE',
                                    filter: true,
                                    sort: true//,
                                    // td: { className: 'center' }
                                }
                            }
                        }
                    }
                }
            };
        }

        dqr.getPACSData.done(function(json){

            setupPACSList(json.ResultSet.Result);

            spawnImportQueuePanel();
            spawnImportHistoryPanel();

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


        //
        updateHashQuery('tab', 'queue');


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