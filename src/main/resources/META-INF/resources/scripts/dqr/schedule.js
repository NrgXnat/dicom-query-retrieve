/*!
 * DQR Utilization Schedule
 */

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

    var NBSP = '&nbsp;';

    var scheduleTimes$ = $('#pacs-schedule-times');
    var scheduleDays$  = $('#pacs-schedule-days');

    window.pacsId =
        window.pacsId ||
        getQueryStringValue('pacs') ||
        getUrlHashValue('#pacs=');

    window.pacsLabel =
        window.pacsLabel ||
        getQueryStringValue('label') ||
        getUrlHashValue('#label=');

    var daysConfig = [
        ['', ''],  // placeholder for index 0
        ['Sun', 'sunday-schedule', 'Sunday'],
        ['Mon', 'monday-schedule', 'Monday'],
        ['Tue', 'tuesday-schedule', 'Tuesday'],
        ['Wed', 'wednesday-schedule', 'Wednesday'],
        ['Thu', 'thursday-schedule', 'Thursday'],
        ['Fri', 'friday-schedule', 'Friday'],
        ['Sat', 'saturday-schedule', 'Saturday']
    ];

    // all the hours
    var hours = (function(hrs){

        var i   = -1;
        var arr = [];

        while (++i < hrs) {
            arr[i] = i;
        }

        return arr;

    })(24);

    // convert to 12-hour format
    function get12HourTime(time){
        var parts  = (time + '').split(':');
        var hour   = +parts[0];
        var minute = parts[1] || '00';
        hour       = (hour === 24) ? 0 : hour;
        // console.log(hour);
        return (hour > 11) ? (((hour - 12) || 12) + ':' + minute + ' PM') : ((hour || 12) + ':' + minute + ' AM');
    }

    // return a conformed object with calculated values
    function timeBlockData(data){

        var obj = extend({
            id: '',
            dayOfWeek: 1,
            availabilityStart: '0:00',
            availabilityEnd: '24:00',
            utilizationPercent: 0,
            threads: 0
        }, data);

        // aliases and calculated values

        obj.dayIndex = +obj.dayOfWeek;

        obj.dayLabel = daysConfig[obj.dayIndex][0];
        obj.dayClass = daysConfig[obj.dayIndex][1];
        obj.dayName  = daysConfig[obj.dayIndex][2];

        obj.startTime = obj.availabilityStart;
        obj.endTime   = obj.availabilityEnd;

        // start and end labels in 12-hour time format
        obj.startLabel = get12HourTime(obj.startTime);
        obj.endLabel   = get12HourTime(obj.endTime);

        obj.startValue = +(obj.startTime.split(':').join(''));
        obj.endValue   = +(obj.endTime.split(':').join(''));

        obj.startKey = zeroPad(obj.startValue, 4);

        // obj.startKey = '0000';
        // if (obj.startValue < 1000) {
        //     obj.startKey = '0' + obj.startValue;
        // }
        // if (obj.startValue < 100) {
        //     obj.startKey = '00' + obj.startValue;
        // }
        // if (obj.startValue < 10) {
        //     obj.startKey = '000' + obj.startValue;
        // }

        obj.intervalLength = obj.endValue - obj.startValue;

        obj.pct = obj.utilizationPercent;

        // console.log('timeBlockData');
        // console.log(obj);

        return obj;

    }

    function intervalDialog(id, day, start, end){

        XNAT.dialog.open({
            width: 500,
            padding: 0,
            title: 'Edit Utilization Interval for <b>' + daysConfig[+day][2] + '</b>',
            okLabel: 'Save',
            okClose: false,
            okAction: function(dlg){
                var saveInterval = dlg.body$.find('form').submitJSON();
                saveInterval.done(function(){
                    // re-render the interval display
                    renderDayRows();
                    XNAT.ui.banner.top(2000, 'Saved', 'success');
                    dlg.close();
                });
                saveInterval.fail(function(txt){
                    console.error(arguments);
                    XNAT.dialog.message('Error', '' +
                        '<p>An error occurred when saving.</p>' +
                        '<div class="error">' + txt + '</div>' +
                        '');
                });
            },
            beforeShow: function(dlg){
                // bind the delete function to the link
                dlg.body$.on('click', 'a.delete-schedule-entry', function(e){
                    e.preventDefault();
                    XNAT.dialog.open({
                        width: 400,
                        title: 'Delete Entry?',
                        content: 'Delete this schedule entry?',
                        okLabel: 'Delete',
                        okClose: false,
                        okAction: function(dlg2){
                            XNAT.xhr.delete('~/xapi/dqr/pacsAvailability/window/' + id)
                                .done(function(){
                                    renderDayRows();
                                    XNAT.ui.banner.top(2000, 'Deleted', 'success');
                                    dlg2.close();
                                    dlg.close();
                                })
                                .fail(function(e){
                                    XNAT.dialog.message('Error', '' +
                                        '<p>An error occurred while trying to delete a schedule entry.</p>' +
                                        '<div class="error">' + e + '</div>' +
                                        '');
                                });
                        }
                    });
                });
            },
            content: (function(){

                var startMenu = spawn('select.start-time', { name: 'availabilityStart' });
                var endMenu   = spawn('select.end-time', { name: 'availabilityEnd' });

                function setupOption(val, hr){
                    var cfg   = { attr: {} };
                    cfg.value = hr;
                    if (val === hr) {
                        cfg.attr.selected = 'selected';
                        cfg.selected      = true;
                    }
                    return cfg;
                }

                [].concat(hours, [24]).forEach(function(hr, i){
                    var time = hr + ':00';
                    hr < 24 && startMenu.appendChild(spawn('option', setupOption(start, time), get12HourTime(time)));
                    hr > 0 && endMenu.appendChild(spawn('option', setupOption(end, time), hr === 24 ? 'Midnight' : get12HourTime(time)));
                });

                // TODO: more time increments, or manual entry.
                // [].concat(hours, [24]).forEach(function(hr, hri){
                //     ['00', '15', '30', '45'].forEach(function(min, mini){
                //         var time = (hr + ':' + min);
                //         if (hr === 24 && min !== '00') {
                //             return false;
                //         }
                //         (hr < 24) && startMenu.appendChild(spawn('option', setupOption(start, time), get12HourTime(time)));
                //         (time !== '0:00') && endMenu.appendChild(spawn('option', setupOption(end, time), (time === '24:00') ? 'Midnight' : get12HourTime(time)));
                //     });
                // });

                return XNAT.spawner.spawn({
                    intervalEditor: {
                        kind: 'panel.form',
                        header: false,
                        footer: false,
                        borderless: true,
                        border: false,
                        method: id ? 'PUT' : 'POST',
                        contentType: 'json',
                        load: id ? '$? */xapi/dqr/pacsAvailability/window/' + id : false,
                        action: '~/xapi/dqr/pacsAvailability/window' + (id ? '/' + id : ''),
                        contents: {
                            id: {
                                kind: 'input.hidden',
                                name: 'id',
                                value: id ? id : ''
                            },
                            pacsId: {
                                kind: 'input.hidden',
                                name: 'pacsId',
                                value: window.pacsId
                            },
                            dayOfWeek: {
                                kind: 'input.hidden',
                                name: 'dayOfWeek',
                                value: day
                            },
                            availabilityStart: {
                                kind: 'panel.display',
                                name: 'availabilityStart',
                                label: 'Start Time',
                                contents: {
                                    startMenu: {
                                        tag: 'div.start-time-menu',
                                        content: startMenu.outerHTML
                                    }
                                }
                            },
                            availabilityEnd: {
                                kind: 'panel.display',
                                name: 'availabilityEnd',
                                label: 'End Time',
                                contents: {
                                    endMenu: {
                                        tag: 'div.end-time-menu',
                                        content: endMenu.outerHTML
                                    }
                                }
                            },
                            threads: {
                                kind: 'panel.input.number',
                                name: 'threads',
                                label: 'Threads',
                                size: 4,
                                // value: id ? '$? */xapi/dqr/pacsAvailability/window/' + id + ' | :threads' : ''
                                validate: 'onblur gte:0 lt:1000',
                                message: 'Please enter a value between 0 and 999',
                                description: ''
                            },
                            utilizationPercent: {
                                kind: 'panel.input.number',
                                name: 'utilizationPercent',
                                label: 'Utilization',
                                size: 3,
                                // value: id ? '$? */xapi/dqr/pacsAvailability/window/' + id + ' | :utilizationPercent' : ''
                                afterElement: ' %',
                                validate: 'onblur gte:0 lte:100',
                                message: 'Please enter a value between 0 and 100',
                                description: ''
                            },
                            deleteEntry: (function(){
                                return id ? {
                                    kind: 'panel.display',
                                    contents: {
                                        deleteLink: {
                                            tag: 'a.link.delete-schedule-entry|title=Delete Schedule Entry|href=#!',
                                            content: 'Delete Schedule Entry'
                                        }
                                    }
                                } : {
                                    tag: 'div.hidden'
                                };

                            })()
                        }
                    }
                }).get();

            })()
        });
    }

    // render time indicators
    scheduleTimes$[0].appendChild(spawn('div.hours-label-row.pull-left', {}, [
        ['div.hours-labels.pull-left', {}, [['div.day-label.pull-left', {}, [['b', {}, NBSP]]]].concat([].concat(hours, 0).map(function(hour, ihour){
            var time = get12HourTime(hour);
            var cell = ['div.time-block', { title: time }, [
                ['div.time-block-label', {}, time.split(':')[0]]
            ]];
            // console.log(cell);
            return cell;
        }))]
    ]));

    function renderDayRows(){

        // assemble the DOM elements for later insertion all at once
        var tmpFrag = document.createDocumentFragment();

        // var scheduleUrl = '~/page/dqr/schedule-dev/scratch.json';
        var scheduleUrl = '~/xapi/dqr/pacsAvailability/windowsByDay/' + window.pacsId;

        // get data then apply it to each day
        var getSchedule = XNAT.xhr.get(scheduleUrl);

        getSchedule.done(function(data){

            // console.log('done');
            // console.log(data);

            // iterate returned data (by day)
            Object.keys(data).sort().forEach(function(dayKey, i){

                var dayIndex = +dayKey;

                var dayLabel = daysConfig[dayIndex][0];
                var dayClass = daysConfig[dayIndex][1];

                // how wide should each hour block be - in pixels?
                // used for initial position and width
                var hourWidth = 38;

                // return a 'px' string
                function setWidth(mult, suffix){
                    return ((mult / 100) * (hourWidth + 1)) + (suffix || '');
                }

                // each day has its own 'row'
                var dayRow   = spawn('div.day-row.pull-left.' + dayClass);
                var dayHours = spawn('div.day-hours.pull-left', {
                    style: {
                        // width: (39 * 24) + 'px'
                    }
                });

                // the first item is the day label
                dayRow.appendChild(spawn('div.day-label.pull-left', {}, [['b', {}, dayLabel]]));

                // put the hours in the row :P
                dayRow.appendChild(dayHours);

                // for each interval entry, calculate the start point and end point
                // based on the numeric values of `availabilityStart` and `availabilityEnd`

                // dayData is an array of interval config objects
                var dayData = data[dayKey];

                // collect interval 'blocks' keyed from the start time
                var blocks = {};

                if (dayData.length) {

                    // make sure the data entries are in order
                    dayData = dayData.map(function(entry, i){
                        // add a zero-padded string for sorting
                        return timeBlockData(entry);
                    });

                    // console.log('dayData');
                    // console.log(dayData);

                    sortObjects(dayData, 'startKey').forEach(function(entry, i){

                        var block  = entry;
                        var filler = {};

                        // always create a '0' block - it can be overwritten
                        // if there's a block defined that starts at '0:00'
                        if (i === 0) {
                            blocks['0000'] = timeBlockData({
                                availabilityStart: '0:00',
                                availabilityEnd: block.startValue > 0 ? block.startTime : block.endTime,
                                // id: '',
                                dayOfWeek: dayIndex
                            });
                        }

                        var prevBlock = (i > 0) ? timeBlockData(dayData[i - 1]) : blocks['0000'];

                        // if the start time of this block is greater than the
                        // end time of the previous block, create a 'filler' block
                        if (block.startValue >= prevBlock.endValue) {

                            filler = timeBlockData({
                                availabilityStart: prevBlock.availabilityEnd,
                                availabilityEnd: block.availabilityStart,
                                // id: '',
                                dayOfWeek: block.dayOfWeek
                            });

                            blocks[filler.startKey] = filler;

                        }

                        // add this entry to the 'blocks' object
                        // complete with computed and aliased values
                        blocks[block.startKey] = block;

                        // add a filler for the end after the last entry
                        if (dayData.length === i + 1 && block.endValue < 2400) {

                            filler = timeBlockData({
                                availabilityStart: block.endTime,
                                availabilityEnd: '24:00'
                            });

                            blocks[filler.startKey] = filler;

                        }

                        // console.log(blocks);

                    });
                }
                else {
                    // set whole day to 0 utilization if there is nothing configured
                    blocks['0000'] = timeBlockData({
                        dayOfWeek: dayIndex,
                        id: '',
                        utilizationPercent: 0,
                        threads: 0
                    });
                }

                // console.log('blocks');
                // console.log(blocks);

                Object.keys(blocks).sort().forEach(function(startKey, i){

                    // console.log(startKey);

                    // process values needed for display
                    var block = blocks[startKey];

                    var threads = block.threads;
                    var pct     = block.utilizationPercent;

                    var threadCounter = spawn('div.thread-count', (function(){
                        // if there's no id, this thread counter is not for
                        // a defined interval, so show the background as gray
                        return block.id ? {} : {
                            style: {
                                background: '#909090',
                                borderColor: '#808080'
                            }
                        }
                    })(), [
                        threads + '',
                        ['span', {}, ((threads === 1) ? ' thread' : ' threads') + (+threads !== 0 ? (' @ ' + pct + '%') : '')]
                    ]);

                    console.log('time block:');
                    console.log(block);

                    // create the div to represent this interval
                    var timeBlock = spawn('div.time-block', {
                        title: block.startLabel + ' - ' + block.endLabel + ' (double-click to edit)',
                        // style: { left: setWidth(block.startValue, 'px') },
                        on: [['dblclick', function(e){
                            intervalDialog(block.id, dayIndex, block.startTime, block.endTime);
                        }]]
                    }, [
                        ['input|type=hidden|name=id', { value: block.id }],
                        ['input|type=hidden|name=pacsId', { value: block.pacsId }],
                        ['input|type=hidden|name=dayOfWeek', { value: dayIndex }],
                        ['input|type=hidden|name=hour', { value: block.hour }],
                        ['input|type=hidden|name=availabilityStart', { value: block.startTime }],
                        ['input|type=hidden|name=availabilityEnd', { value: block.endTime }],
                        ['input|type=hidden|name=threads', { value: block.threads }],
                        ['input|type=hidden|name=utilizationPercent', { value: block.utilizationPercent }],
                        ['div.time-block-color', {
                            style: {
                                width: setWidth(block.intervalLength, 'px'),
                                background: 'rgba(92, 196, 239, ' + (block.utilizationPercent / 100) + ')'
                            }
                        }, NBSP],
                        threadCounter,
                        ''
                    ]);
                    dayHours.appendChild(timeBlock);
                });

                // append the day's table to the frag
                tmpFrag.appendChild(dayRow);

            });

            scheduleDays$.empty().append(tmpFrag);

            $('#page-body').hidden(false);

        });

    }

    // render the rows when the page loads
    renderDayRows();

    // sample data object
    var sampleData = {
        'availabilityEnd': 'string',
        'availabilityStart': 'string',
        // "created": "2019-02-28T19:25:24.888Z",
        'dayOfWeek': 0,
        'utilizationPercent': 0,
        // "disabled": "2019-02-28T19:25:24.888Z",
        // "enabled": true,
        'id': 0,
        'pacsId': 0,
        'threads': 0,
        // "timestamp": "2019-02-28T19:25:24.888Z"
        '_': 0
    };


}));
