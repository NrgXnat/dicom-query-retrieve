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


    $(function(){

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

        function importQueuePanel(){
            return {
                importQueuePanel: {
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
                            items: {
                                _id: '~data-id',
                                _pacs_id: '~data-pacs-id',
                                id: 'ID',
                                status: 'Status',
                                queue_location: 'Queue Location',
                                priority: 'Priority',
                                username: 'User',
                                xnat_project: 'Project',
                                destination_ae_title: 'Dest. AE'
                            }
                        }
                    }
                }
            };
        }

        XNAT.spawner
            .spawn(importQueuePanel())
            .render(getById$('pacs-import-queue-display').empty());


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

        function importHistoryPanel(){
            return {
                importHistoryPanel: {
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
                                    sort: true
                                },
                                status: {
                                    label: 'Status',
                                    filter: true,
                                    sort: true
                                },
                                username: {
                                    label: 'User',
                                    filter: true,
                                    sort: true
                                },
                                xnatProject: {
                                    label: 'Project',
                                    filter: true,
                                    sort: true
                                },
                                destinationAeTitle: {
                                    label: 'AE',
                                    filter: true,
                                    sort: true
                                }
                            }
                        }
                    }
                }
            };
        }

        XNAT.spawner
            .spawn(importHistoryPanel())
            .render(getById$('pacs-import-history-display').empty());


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
                delim = delim !== undef ? delim : /#|\/#/;
                oldPart = key + hash.split(key)[1].split(delim)[0];
                newPart = key + value;
                hash = hash.replace(oldPart, newPart);
            }

            hash = hash.replace(/^#*/,'#'); // only one '#' at the beginning, please

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

    XNAT.plugin.dqr = dqr;

}));