// simpler selectable table behavior
// init on a <table> or *any* container element
XNAT.app.selectableItems = function(container){

    var $container = $$(container || document);

    var CKBX_ALL = '.selectable-select-all';
    var CKBX_ONE = '.selectable-select-one';
    var INDET    = 'indeterminate';

    // namespaced event name
    var CLICK    = 'click.multicheck';

    var $ckbxAll = $container.find(CKBX_ALL);
    var $ckbxs   = $container.find(CKBX_ONE);

    function toggleAll(checked){
        $ckbxAll.prop(INDET, false);
        $ckbxs.prop('checked', checked);
    }

    function multichecker(){
        // if all checkboxes are checked now, check the 'all' checkbox
        var checkedCount = $ckbxs.filter(':checked').length;
        if (checkedCount === 0 || checkedCount === $ckbxs.length) {
            $ckbxAll.prop(INDET, false).prop('checked', !!checkedCount);
        }
        else {
            $ckbxAll.prop('checked', false).prop(INDET, true);
        }
    }
    // fire this on init to set the initial
    // state of the 'all' checkbox
    multichecker();

    // unbind any existing 'multicheck' click handlers
    $container.off(CLICK);

    $container.on(CLICK, CKBX_ALL, function(e){
        toggleAll(!!this.checked);
    });

    $container.on(CLICK, CKBX_ONE, function(e){
        multichecker();
    });

};
