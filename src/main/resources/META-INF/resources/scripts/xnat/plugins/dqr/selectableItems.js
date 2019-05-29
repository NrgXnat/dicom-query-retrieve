/**
 * 'Selectable Table' behavior...
 * ...init on *any* container element
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

    var undef;

    XNAT.plugins =
        getObject(XNAT.plugins || {});

    XNAT.plugins.dqr =
        getObject(XNAT.plugins.dqr || {});


    function selectableItems(container){

        var $container = (container && container.jquery) ? container : $(container);

        var CKBX_ALL = '.selectable-all';
        var CKBX_ONE = '.selectable-one';
        var INDET    = 'indeterminate';

        // namespaced event name
        var CLICK    = 'click.multicheck';

        var $ckbxAll = null;
        var $ckbxs   = null;

        function findCheckboxes(){
            $ckbxAll = $container.find(CKBX_ALL);
            $ckbxs   = $container.find(CKBX_ONE);
        }

        // collect checkboxes that are present when initialized
        findCheckboxes();

        function toggleAll(checked){
            findCheckboxes();
            $ckbxAll.prop(INDET, false);
            // uncheck *all* checkboxes first?
            // $ckbxs.prop('checked', false);
            // toggle only *visible* checkboxes
            $ckbxs.filter(':visible').prop('checked', checked);
        }

        function uncheckHidden(){
            $ckbxs.filter(':hidden').prop('checked', false);
        }

        function multichecker(){
            findCheckboxes();
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

        // listen for custom 'multicheck' event on container
        $container.on('multicheck', function(){
            console.log('multicheck');
            multichecker();
        });

    }

    return (XNAT.plugins.dqr.selectableItems = selectableItems)

}));
