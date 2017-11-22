/*
 * D:/Development/TIP/tip/image-search/src/main/resources/module-resources/scripts/tip/PacsAdministration.js
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

/*jslint white: true, browser: true, vars: true */

console.log('PacsAdministration.js');

XNAT.app.PacsAdministration = ( function () {
    "use strict";

    var constants = {
        "MODAL_WINDOW_NAME": "loadData",
        "PACS_DIV": "#pacsDiv",
        "PACS_TABLE": "#pacsTable",
        "ADD_PACS_LINK": "#addNewPacs",
        "ADD_PACS_LINK_HOLDER": "#addNewPacsHolder",
        "OPERATION_EDIT": "EDIT",
        "OPERATION_DELETE": "DELETE",
        "OPERATION_CREATE": "CREATE"
    };

    var ormStrategies = ['dicomOrmStrategy']; // replace this with a dynamic list

    // We'll keep the edit form in JavaScript, adding it to the DOM upon request
    // Just cleaner to define the form initially in the Velocity template, then slurp it in on page load

    var currentOperation;

    function AddOperation(pButton) {
        this.button = pButton;
        this.type = constants.OPERATION_CREATE;
    }

    function ModifyOperation(pImage, pImageCssClass, pPacs, pType) {
        this.image = pImage;
        this.imageCssClass = pImageCssClass;
        this.pacs = pPacs;
        this.type = pType;
    }


    ModifyOperation.prototype.disable = function () {
        $(this.image).removeClass(this.imageCssClass);
    };
    ModifyOperation.prototype.enable = function () {
        $(this.image).addClass(this.imageCssClass);
    };

    function editPacsDialog(pacs) {
        pacs = pacs || {};
        var doWhat = Object.keys(pacs).length ? 'Modify' : 'Create';
        XNAT.dialog.open({
            title: doWhat + ' PACS',
            width: 600,
            className: doWhat.toLowerCase() + 'Modal',
            content: spawn('form.panel'),
            beforeShow: function(obj){
                var $form = obj.$modal.find('form');
                $form.append(
                    spawn('!', [
                        XNAT.ui.panel.input.hidden({
                            name: 'pacsId'
                        }),
                        XNAT.ui.panel.select.menu({
                            name: 'ormStrategySpringBeanId',
                            label: 'ORM Strategy',
                            options: ormStrategies
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'aeTitle',
                            label: 'AE Title'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'host',
                            label: 'Host'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'queryRetrievePort',
                            label: 'Q/R Port'
                        }),
                        XNAT.ui.panel.input.text({
                            name: 'storagePort',
                            label: 'Storage Port'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'extendedNegotiations',
                            label: 'Extended Negotiations',
                            onText: 'Supported',
                            offText: 'Not Supported',
                            value: 'true'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'defaultQueryRetrievePacs',
                            label: 'Default Q/R PACS',
                            onText: 'Yes',
                            offText: 'No',
                            value: 'true'
                        }),
                        XNAT.ui.panel.input.switchbox({
                            name: 'defaultStoragePacs',
                            label: 'Default Storage PACS',
                            onText: 'Yes',
                            offText: 'No',
                            value: 'true'
                        })
                    ])
                );

                if (pacs && doWhat === 'Modify') {
                    $form.setValues(pacs);
                }
                else {
                    $form.find('select').find('option').first().prop('selected','selected');
                    $form.find('input[type=checkbox]').prop('checked',false);
                }
            },
            buttons: [
                {
                    label: 'Save',
                    isDefault: true,
                    close: true,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        (doWhat === 'Create') ?
                            addPacs($form) :
                            editPacs($form);
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        })
    }

    function bindAddButtonHandler() {
        var addButtonHandler = function () {
            currentOperation = new AddOperation(this);

            editPacsDialog();
        };
        $(document).off("click", constants.ADD_PACS_LINK);
        $(document).on("click", constants.ADD_PACS_LINK, addButtonHandler);
    }

    function bindEditButtonHandler() {
        var editButtonHandler = function () {
            var pacs = $(this).parents('tr').data();
            currentOperation = new ModifyOperation(this, "editRow", pacs, constants.OPERATION_EDIT);
            // currentOperation.disable();

            editPacsDialog(pacs);

        };
        $(constants.PACS_TABLE).on("click", ".editRow", editButtonHandler);
    }

    function bindDeleteButtonHandler() {
        var deleteButtonHandler = function () {
            var pacs = $(this).parents('tr').data();
            currentOperation = new ModifyOperation(this, "deleteRow", pacs, constants.OPERATION_DELETE);
            // currentOperation.disable();

            xmodal.open({
                width: 400,
                height: 150,
                className: 'deleteModal',
                title: 'Confirm PACS Deletion',
                content: spawn('p','Are you sure you want to delete this PACS?'),
                okAction: XNAT.app.PacsAdministration.submitCurrentOperation
            });
        };
        $(constants.PACS_TABLE).on("click", ".deleteRow", deleteButtonHandler);
    }

    function showPacs(data) {
        var pacsTableData = data.ResultSet.Result;
        $(constants.PACS_DIV).empty();

        var pacsTable = XNAT.table({
            className: 'xnat-table',
            style: {
                'width': '100%'
            },
            id: constants.PACS_TABLE.substring(1)
        });

        // add table header row
        pacsTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>AE Title</b>')
            .th('<b>Host</b>')
            .th('<b>Q/R Port</b>')
            .th('<b>Storage Port</b>')
            .th('<b>Q/R Default</b>')
            .th('<b>Storage Default</b>')
            .th('<b>Actions</b>');

        function showDefault(setting){
            return setting ? spawn('i',{ className: 'fa fa-check' }) : '';
        }
        function editButton(){
            return spawn('button',{ className: 'btn editRow', title: 'Edit This PACS' },[
                spawn('i', { className: 'fa fa-pencil' })
            ]);
        }
        function deleteButton(){
            return spawn('button',{ className: 'btn deleteRow', title: 'Delete This PACS' },[
                spawn('i', {className: 'fa fa-trash' })
            ]);
        }

        // add data rows
        if (pacsTableData.length) {
            pacsTableData.forEach(function(ae){
                pacsTable.tr({
                    data: {
                        id: ae.id,
                        aeTitle: ae.aeTitle,
                        host: ae.host,
                        queryRetrievePort: ae.queryRetrievePort,
                        storagePort: ae.storagePort,
                        ormStrategySpringBeanId: ae.ormStrategySpringBeanId,
                        defaultQueryRetrievePacs: ae.defaultQueryRetrievePacs,
                        defaultStoragePacs: ae.defaultStoragePacs,
                        supportsExtendedNegotiations: ae.supportsExtendedNegotiations
                    }
                })
                    .td({ addClass: 'right' }, ae.id )
                    .td( ae.aeTitle )
                    .td( ae.host )
                    .td( ae.queryRetrievePort )
                    .td( ae.storagePort )
                    .td([ showDefault(ae.defaultQueryRetrievePacs) ])
                    .td([ showDefault(ae.defaultStoragePacs) ])
                    .td([ editButton(), spawn('!',' '), deleteButton() ]);
            })

        }
        $(constants.PACS_DIV).append(pacsTable.table);

        $(constants.PACS_DIV).append(
            spawn('p',{ 'id': constants.ADD_PACS_LINK_HOLDER.substring(1), style: { 'margin-top':'1em' } }, [
                spawn('a', { className: 'btn primary', href: 'javascript:void(0)', id: constants.ADD_PACS_LINK.substring(1) },'Add New PACS')
            ])
        );

        bindAddButtonHandler();
        bindEditButtonHandler();
        bindDeleteButtonHandler();

        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function handlePacsSearchFailure(jqXHR) {
        $(constants.PACS_DIV).text("Error " + jqXHR.status + ": " + jqXHR.responseText);
        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function getAllPacs() {
        $.ajax({
            type: "GET",
            url: serverRoot + "/data/pacs?XNAT_CSRF=" + csrfToken,
            dataType: "json",
            success: showPacs,
            error: handlePacsSearchFailure
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function deletePacs() {
        $.ajax({
            type: "DELETE",
            url: serverRoot + "/data/pacs/" + currentOperation.pacs.id + "?XNAT_CSRF=" + csrfToken,
            success: getAllPacs,
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("PACS deletion failed: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function editPacs($form) {
        $.ajax({
            type: "PUT",
            url: serverRoot + "/data/pacs/" + currentOperation.pacs.id + "?XNAT_CSRF=" + csrfToken,
            data: $form.serialize(),
            success: function () {
                xmodal.close();
                getAllPacs();
                XNAT.ui.banner.top(3000, 'Saved changes to PACS definition', 'success');
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("PACS modification failed: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function addPacs($form) {
        $.ajax({
            type: "POST",
            url: serverRoot + "/data/pacs?XNAT_CSRF=" + csrfToken,
            data: $form.serialize(),
            success: function () {
                xmodal.close();
                getAllPacs();
                XNAT.ui.banner.top(3000, 'Created new PACS definition', 'success');
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("PACS addition failed: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    return {
        init: function () {
            getAllPacs();
        },

        cancelCurrentOperation: function () {
            currentOperation.enable();
        },

        submitCurrentOperation: function () {
            if (currentOperation.type === constants.OPERATION_DELETE) {
                xmodal.close();
                deletePacs();
            } else if (currentOperation.type === constants.OPERATION_EDIT) {
                editPacs($("#editPacsForm"));
            } else if (currentOperation.type === constants.OPERATION_CREATE) {
                addPacs($("#editPacsForm"));
            } else {
                alert('Unsupported operation type: ' + currentOperation.type);
            }
            currentOperation.enable();
        },

        ormStrategies: ormStrategies
    };
}());

$(document).ready(function(){
    XNAT.app.PacsAdministration.init();
});

window.xModalSubmit = function () {
    "use strict";
    XNAT.app.PacsAdministration.submitCurrentOperation();
};

window.xModalCancel = function () {
    "use strict";
    xmodal.close();
    XNAT.app.PacsAdministration.cancelCurrentOperation();
};
