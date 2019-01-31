// simpler selectable table behavior
// init on a <table> or *any* container element
XNAT.app.selectableItems = function(container){

    var $container = $$(container || document);

    var CKBX_ALL = '.selectable-all';
    var CKBX_ONE = '.selectable-one';
    var INDET    = 'indeterminate';

    // namespaced event name
    var CLICK    = 'click.multicheck';

    var $ckbxAll = $container.find(CKBX_ALL);
    var $ckbxs   = $container.find(CKBX_ONE);

    function toggleAll(checked){
        $ckbxAll.prop(INDET, false);
        // toggle only *visible* checkboxes
        $ckbxs.filter(':visible').prop('checked', checked);
    }

    function multichecker(){
        // if all *visible* checkboxes are checked, check the 'all' checkbox
        var $checked = $ckbxs.filter(':checked:visible');
        var noneChecked = $checked.length === 0;
        var $actions;
        if (noneChecked || $checked.length === $ckbxs.length) {
            $ckbxAll.prop(INDET, false).prop('checked', !!$checked.length);
        }
        else {
            $ckbxAll.prop('checked', false).prop(INDET, true);
        }
        // if there are 'action' items, disable them if nothing is selected
        if (($actions = $container.find('.selectable-action')).length){
            $actions[noneChecked ? 'addClass' : 'removeClass']('disabled');
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
