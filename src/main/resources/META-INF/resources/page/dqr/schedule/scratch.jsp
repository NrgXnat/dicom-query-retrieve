<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<c:set var="SITE_ROOT" value="${sessionScope.siteRoot}"/>

<header id="content-header">
    <div class="pad" style="padding:0;">
        <h2 style="margin:0;">Utilization Schedule for <span class="pacs-id">${pacsId}</span> PACS</h2>
        <div class="info">
            Use these settings to customize transfer load for specified time intervals when importing data
            from PACS. Darker shades indicate a heavier transfer load, lighter shades indicate a lighter load.
        </div>
        <div class="info alt">
            Customize the data transfer utilization rate between {XNAT_name} and {PACS_name} by creating a
            weekly schedule for uptime, downtime, and throttling of data transfer speeds
        </div>
    </div>
</header>

<style>

    #pacs-schedule-view { width: 1100px; }
    #pacs-schedule-view > .pad { padding: 20px 40px; }

    #pacs-schedule-times,
    #pacs-schedule-days {
        position: relative;
    }

    /*.day-row-container > .pad { padding-bottom: 20px; }*/

    .day-row-table { margin-bottom: 20px; border-collapse: collapse; }

    .day-row-table.hours { position: relative; left: 18px; }

    .day-row-table .day-label { width: 50px; font-size: 16px; line-height: 70px; vertical-align: middle; background: #fff; }

    .day-row-table .day-row { background: #5eb7dc; }

    .day-row-table .time-block { padding: 0 }

    .time-block-color { width: 36px; height: 70px; background: #5cc4ef; }

    .time-block-label { width: 36px; height: 20px; background: #fff; text-align: center; }

</style>

<div id="pacs-schedule-view">
    <div class="pad">

        <header id="pacs-schedule-times">
            <%--
            <!--<table></table>-->
            <!--<span class="day-label">-->
            <!--&lt;!&ndash; this stays empty but is needed so the layout matches &ndash;&gt;-->
            <!--</span>-->
            <!--<span>12</span>-->
            <!--<span>1</span>-->
            <!--<span>2</span>-->
            <!--<span>3</span>-->
            <!--<span>4</span>-->
            <!--<span>5</span>-->
            <!--<span>6</span>-->
            <!--<span>7</span>-->
            <!--<span>8</span>-->
            <!--<span>9</span>-->
            <!--<span>10</span>-->
            <!--<span>11</span>-->
            <!--<span>12</span>-->
            <!--<span>1</span>-->
            <!--<span>2</span>-->
            <!--<span>3</span>-->
            <!--<span>4</span>-->
            <!--<span>5</span>-->
            <!--<span>6</span>-->
            <!--<span>7</span>-->
            <!--<span>8</span>-->
            <!--<span>9</span>-->
            <!--<span>10</span>-->
            <!--<span>11</span>-->
            <!--<span>12</span>-->
            --%>
        </header>

        <div id="pacs-schedule-days">
            <%--

            <!--<div id="sunday-schedule" class="day-row">-->
            <!--<header class="day-label">Sun</header>-->
            <!--<div class="day-hours">-->
            <!--<label title="12 AM">-->

            <!--<input class="select-time" type="checkbox">-->
            <!--<b class="time-block-color">-->
            <!--&lt;!&ndash; empty div for this hour's color block &ndash;&gt;-->
            <!--</b>-->

            <!--<input name="id" type="hidden" value="1">-->
            <!--<input name="pacsId" type="hidden" value="1">-->
            <!--<input name="dayOfWeek" type="hidden" value="0">-->

            <!--<input name="threads" type="hidden" value="1">-->
            <!--<input name="utilizationPercent" type="hidden" value="100">-->

            <!--<input name="availabilityStart" type="hidden" value="0:00">-->
            <!--<input name="availabilityEnd" class="time-value" type="hidden" value="1:00">-->

            <!--</label>-->
            <!--<label title="1 AM"></label>-->
            <!--<label title="2 AM"></label>-->
            <!--<label title="3 AM"></label>-->
            <!--<label title="4 AM"></label>-->
            <!--<label title="5 AM"></label>-->
            <!--<label title="6 AM"></label>-->
            <!--<label title="7 AM"></label>-->
            <!--<label title="8 AM"></label>-->
            <!--<label title="9 AM"></label>-->
            <!--<label title="10 AM"></label>-->
            <!--<label title="11 AM"></label>-->
            <!--<label title="12 PM"></label>-->
            <!--</div>-->
            <!--</div>-->

            --%>
        </div>
    </div>
</div>
<!--<script src="../../../scripts/lib/array-fill.js"></script>-->
<script src="../../../scripts/lib/xx.js"></script>
<!--<script src="../../../scripts/lib/xx.spawnElement.js"></script>-->
<script>
    (function(xx){

        var NBSP = '&nbsp;';

        var scheduleTimes$ = $('#pacs-schedule-times');
        var scheduleDays$ = $('#pacs-schedule-days');

        var daysConfig = [
            ['<div class="day-label">&nbsp;</div>', 'times-row'],
            ['Sun', 'sunday-schedule'],
            ['Mon', 'monday-schedule'],
            ['Tue', 'tuesday-schedule'],
            ['Wed', 'wednesday-schedule'],
            ['Thu', 'thursday-schedule'],
            ['Fri', 'friday-schedule'],
            ['Sat', 'saturday-schedule']
        ];

        // convert to 12-hour format
        function get12HourTime(hour){
            return (hour > 11) ? (((hour - 12) || 12) + ':00 PM') : ((hour || 12) + ':00 AM');
        }

        function createTimeBlock(id, day, hour, pct, threads){
            return {

                id: id || 1,          // increments globally for all entries across all days

                dayOfWeek: day || 0,       // increments for each day
                hour: (hour = hour || 0),  // increments for each hour for the current day

                time: get12HourTime(hour),  // convert to 12-hour format

                availabilityStart: hour + ':00',
                availabilityEnd: (hour + 1) + ':00',

                utilizationPercent: pct || 100,
                threads: threads || 1

            };
        }

        // start id for all entries
        var entryId = 1;

        // all the hours
        var hours = (function(hrs){

            var i   = -1;
            var arr = [];

            while (++i < hrs) {
                arr[i] = i;
            }

            return arr;

        })(24);

        var tmpFrag = document.createDocumentFragment();

        // iterate days and build the display rows
        daysConfig.forEach(function(day, iday){

            var dayIndex = (iday - 1);

            var dayLabel = day[0];
            var dayClass = day[1];

            // var dayRowContainer = spawn('div.day-row-container', { data: { rowIndex: dayIndex } });
            // var dayRowPad       = spawn('div.pad');

            // dayRowContainer.appendChild(dayRowPad);

            // if it's the hour label row, add another hour for midnight
            if (dayIndex === -1) {
                console.log('time label row');
                // hours.push(0);
                scheduleTimes$[0].appendChild(spawn('table.day-row-table.hours', {}, [
                    ['tr', { className: 'hours-row ' + dayClass }, [['td', {}, dayLabel]].concat([].concat(hours, 0).map(function(hour, ihour){
                        var time = get12HourTime(hour);
                        var cell = ['td.time-block', { title: time }, [
                            ['div.time-block-label', {}, time.split(':')[0]]
                        ]];
                        // console.log(cell);
                        return cell;
                    }))]
                ]));
            }
            else {
                tmpFrag.appendChild(spawn('table.day-row-table', {}, [
                    ['tr', { className: 'day-row ' + dayClass }, [['td.day-label', {}, dayLabel]].concat(hours.map(function(hour, ihour){
                        var timeBlock = createTimeBlock(entryId++, dayIndex, ihour, 100, 1);
                        var cell      = ['td.time-block', { title: timeBlock.time }, dayIndex > -1 ? [
                            ['input|type=hidden|name=id', { value: timeBlock.id }],
                            ['input|type=hidden|name=pacsId', { value: 1 }],
                            ['input|type=hidden|name=dayOfWeek', { value: timeBlock.dayOfWeek }],
                            ['input|type=hidden|name=hour', { value: timeBlock.hour }],
                            ['input|type=hidden|name=availabilityStart', { value: timeBlock.availabilityStart }],
                            ['input|type=hidden|name=availabilityEnd', { value: timeBlock.availabilityEnd }],
                            ['input|type=hidden|name=threads', { value: timeBlock.threads }],
                            ['input|type=hidden|name=utilizationPercent', { value: timeBlock.utilizationPercent }],
                            ['div.time-block-color', { style: { opacity: timeBlock.utilizationPercent / 100 } }, NBSP],
                            ''
                        ] : [
                            ['div.time-block-label', {}, NBSP]
                        ]];
                        // console.log(cell);
                        return cell;
                    }))]
                ]));
            }
            // tmpFrag.appendChild(dayRowContainer);
        });

        scheduleDays$.append(tmpFrag);

        $('#page-body').hidden(false);

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

    })(window.xx);
</script>