// better table filter that works on a
// non-standard table container element
XNAT.app.searchableItems = function(container){

    var $container = $$(container);
    var $filterInputs = $container.find('input.filter-data');

    // detach previously bound listeners
    $filterInputs.off('focus, keyup');

    var $dataRows;

    function cacheRows(){
        return $dataRows = $container.find('.table-body').find('tr');
    }

    $filterInputs.on('focus', function(){
        $(this).select();
        // clear all filters on focus
        //$table.find('input.filter-data').val('');
        // save reference to the data rows on focus
        // (should make filtering slightly faster)
        // $dataRows = $table.find('tr[data-filter]');
        cacheRows();
    });

    function filterRows(val, name){
        if (!val) { return false }
        val = val.toLowerCase();
        var filterClass = 'filter-' + name;
        // cache the rows if not cached yet
        // cacheRows();
        $dataRows.addClass(filterClass).filter(function(){
            return $(this).find('td.' + name).containsNC(val).length
        }).removeClass(filterClass);
    }

    $filterInputs.on('keyup', function(e){
        var name = this.title.split(':')[0];
        var val = this.value;
        var key = e.which;
        // don't do anything on 'tab' keyup
        if (key === 9) return false;
        if (key === 27){ // key 27 = 'esc'
            this.value = val = '';
        }
        if (!val || key === 8) {
            $dataRows.removeClass('filter-' + name);
        }
        if (!val) {
            // no value, no filter
            return false
        }
        filterRows(val, name);
    });


}
