// More versatile filtering that works on non-table elements
XNAT.app.filterableItems = function filterableItems(container){

    var args   = arguments;
    var argLen = args.length;
    var argi   = -1;
    var multi  = [];

    // initialize multiple containers as separate arguments
    if (argLen > 1) {
        while (++argi < argLen) {
            multi.push(filterableItems(args[argi]));
        }
        return multi;
    }

    // initialize multiple containers as an array
    if (Array.isArray(container)) {
        return container.map(function(cont, i){
            return filterableItems(cont);
        });
    }

    var $container = (container && container.jquery) ? container : $(container);

    // text inputs to use as filters
    var $filterInputs = $container.find('input.filter-input, input.filter-data');

    // <select> menus to use as filters
    var $filterMenus = $container.find('select.filter-menu');

    // don't do anything if there are no filter inputs or menus
    if (!$filterInputs.length && !$filterMenus.length) {
        return;
    }

    function performanceNow(){
        return (window.performance && window.performance.now) ? window.performance.now() : 0;
    }

    var filterValues    = {};
    var filterComposite = '';

    // detach previously bound listeners
    $filterInputs.off('focus.filter keyup.filter');
    $filterMenus.off('change.filter');

    var $allFilters = [].concat($filterInputs.toArray(), $filterMenus.toArray());

    var $dataRows;
    var dataItems = [];

    // filter name string *should* be:
    // 'filter:urFilterName'
    // ...but can also be:
    // 'urFilterName:filter'
    // if you want (for whatever reason)
    function parseFilterName(str){
        return str ? (str.split(/filter:/i)[1] || str.split(/:filter/i)[0] || str || '').trim() : '';
    }

    function cacheItems(){
        var start = performanceNow();
        $dataRows = $container.find('.filter-data-row');
        dataItems = [];
        // cache each item's data
        $dataRows.toArray().forEach(function(row, i){
            var $row     = $(row);
            var rowIndex = i;
            var rowData  = {
                // use 'filter:*' to filter the entire row's content
                '*': {
                    row: row,
                    row$: $row,
                    rowIndex: rowIndex,
                    // name: '*',
                    // index: null,
                    content: (row.textContent || row.innerText || row.innerHTML).trim(),
                    hidden: false
                }
            };
            $row.find('.filter-data-item').toArray().forEach(function(item, i){

                var name = item.dataset && item.dataset.filter ?
                    item.dataset.filter :
                    parseFilterName(item.title);

                var itemIndex = i;

                var itemData = {};

                itemData.rowIndex = rowIndex;
                itemData.index    = itemIndex;
                itemData.name     = name;
                itemData.content  = (item.textContent || item.innerText || item.innerHTML).trim();
                // itemData.item$ = $(item); // cache the jQuery object for the item
                // itemData.row = row;
                // itemData.row$ = $row;
                // save it for later,
                // keyed by the item name
                rowData[name] = itemData;

            });
            dataItems.push(rowData);
        });
        console.log('cache speed: ' + (performanceNow() - start));
        return $dataRows;
    }

    // cache items on init
    cacheItems();

    function resetFilters(){
        filterValues    = {};
        filterComposite = '';
        $filterInputs.val('');
        // $filterMenus.find('option').removeAttr('selected').prop('selected', false);
        // $filterMenus.find('option').first().prop('selected', true);
        $dataRows.show();
        // $dataRows.removeClass('hidden');
    }

    function getFilterValues(){
        filterComposite = '::';
        $allFilters.forEach(function(input, i){

            var name = input.dataset && input.dataset.filter ?
                input.dataset.filter :
                parseFilterName(input.title);

            filterValues[name] = input.value;
            filterComposite += (input.value.trim() + '::');

        });
    }

    $filterInputs.on('focus.filter', function(){
        $(this).select();
        // save reference to the data rows on focus
        // (should make filtering slightly faster)
        // cacheItems();
    });

    // $filterMenus.on('focus.filter', cacheItems);

    function containsNC(str, val){
        // returns true if `str` has a value but `val` does not...
        // ...or consists of only whitespace and/or '*' characters
        if ((str && !val) || /^[*\s]+$/.test(val)) return true;
        if (!str && !val) return false;
        return (str + '').toLowerCase().indexOf((val + '').toLowerCase()) > -1;
    }

    function filterRows(){
        var start = performanceNow();
        getFilterValues();
        // show all rows if there are no filter values, or if they're all '*'
        // if (!filterComposite) {
        //     resetFilters();
        //     // return;
        // }
        dataItems.forEach(function(row, i){
            // shown until hidden
            row['*'].hidden = false;
            // iterate to hit all the filter inputs
            Object.keys(filterValues).forEach(function(name, i){
                var val         = filterValues[name];
                row['*'].hidden = !containsNC(row[name].content, val);
                // row['*'].hidden = row['*'].hidden || !containsNC(row[name].content, val);
            });
            // hide if `hidden`
            // row['*'].row$[row['*'].hidden ? 'hide' : 'show']();
            // row['*'].row$[row['*'].hidden ? 'addClass' : 'removeClass']('hidden');
            row['*'].row.classList[row['*'].hidden ? 'add' : 'remove']('hidden');
        });
        console.log('filter speed: ' + (performanceNow() - start));
        $container.triggerHandler('multicheck');
    }

    $filterInputs.on('keyup.filter', function(e){
        // the <input> [title] attribute should be 'filter:item-name'
        // var name = parseFilterName(this.title);
        var val = this.value;
        var key = e.which;
        // don't do anything on 'tab' keyup
        if (key === 9) return false;
        if (key === 27) { // key 27 = 'esc'
            this.value = val = '';
        }
        // if (!val && key === 8) {
        //     $dataRows.show();
        // }
        // if (!val) {
        //     // no value, no filter
        //     return false
        // }
        filterRows();
    });


    $filterMenus.on('change.filter', function(e){
        // the <select> [title] attribute should be 'filter:item-name'
        filterRows();
    });

    return {
        rows: $dataRows.length ? $dataRows.toArray() : [],
        getRows: function(){
            $dataRows = cacheItems();
            return (this.rows = $dataRows.toArray());
        },
        items: dataItems,
        getItems: function(){
            this.getRows();
            return (this.items = dataItems);
        },
        update: function(){
            return this.getItems();
        }
    };

};
