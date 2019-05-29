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


    dqr.getPACSData = XNAT.xhr.get('~/data/pacs');

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

        function resolvePACSLabel(id){
            var PACSdata = dqr.PACSData[id + ''];
            return PACSdata ?
                PACSdata.label || PACSdata.aeTitle || '-' :
                '-'
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

        function userImportQueuePanel(){
            return {
                userImportQueuePanel: {
                    tag: 'div',
                    element: { title: 'PACS Import Queue' },
                    contents: {
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: '*/xapi/dqr/query/queueWithOrder/user',
                            apply: function(data){
                                if (!data.length) {
                                    console.log('nothing')
                                }
                            },
                            table: {
                                on: [
                                    ['click', 'a.show-queue-item-data', function(e){
                                        e.preventDefault();
                                        XNAT.dialog.message(false, 'Show the queued item data.')
                                    }],
                                    ['click', 'a.remove-queue-item', function(e){
                                        e.preventDefault();
                                        XNAT.dialog.message(false, 'Remove the queued item now.')
                                    }]
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
                                        return spawn('a.link.show-queue-item-data|href=#!', id + '')
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
                                            href: '#!',
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

        function userImportHistoryPanel(){
            return {
                userImportHistoryPanel: {
                    tag: 'div',
                    element: { title: 'PACS Import History' },
                    contents: {
                        pacsQueueTable: {
                            kind: 'table.dataTable',
                            load: '*/xapi/dqr/query/history/user',
                            apply: function(data){
                                console.log(data);
                                return data
                            },
                            items: {
                                _id: '~data-id',
                                _pacsId: '~data-pacs-id',
                                id: {
                                    label: 'ID',
                                    sort: true,
                                    td: { className: 'center mono' }
                                },
                                status: {
                                    label: 'Status',
                                    filter: true,
                                    sort: true//,
                                    // td: { className: 'center' }
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

            var queueDisplayContainer$ = getById$('pacs-import-queue-display').empty();

            // render queue
            XNAT.spawner
                .spawn(userImportQueuePanel())
                .render(queueDisplayContainer$)
                .done(function(){
                    XNAT.plugins.dqr.selectableItems(queueDisplayContainer$);
                    // XNAT.plugins.dqr.filterableItems(queueDisplayContainer$);
                });


            var historyDisplayContainer$ = getById$('pacs-import-history-display').empty();

            // render history
            XNAT.spawner
                .spawn(userImportHistoryPanel())
                .render(historyDisplayContainer$);

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