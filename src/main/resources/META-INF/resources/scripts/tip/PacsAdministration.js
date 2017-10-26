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
    var imagePath;

    // We'll keep the edit form in JavaScript, adding it to the DOM upon request
    // Just cleaner to define the form initially in the Velocity template, then slurp it in on page load
    var editModalContent;

    var currentOperation;

    function getImageURI(imageName) {
        return imagePath + imageName;
    }

    function getPacsThatWasClickedOn(image) {
        var nTr = jq(image).parents('tr')[0];
        var oTable = jq(constants.PACS_TABLE).dataTable();
        var pacs = oTable.fnGetData(nTr);
        return pacs;
    }

    function AddOperation(pButton) {
        this.button = pButton;
        this.type = constants.OPERATION_CREATE;
    }


    AddOperation.prototype.disable = function () {
    };
    AddOperation.prototype.enable = function () {
    };

    function ModifyOperation(pImage, pImageCssClass, pPacs, pType) {
        this.image = pImage;
        this.imageCssClass = pImageCssClass;
        this.pacs = pPacs;
        this.type = pType;
    }


    ModifyOperation.prototype.disable = function () {
        jq(this.image).removeClass(this.imageCssClass);
    };
    ModifyOperation.prototype.enable = function () {
        jq(this.image).addClass(this.imageCssClass);
    };

    function populateFormFields() {
        jq("#pacsId").val(currentOperation.pacs.id);
        jq("#aeTitle").val(currentOperation.pacs.aeTitle);
        jq("#host").val(currentOperation.pacs.host);
        jq("#queryRetrievePort").val(currentOperation.pacs.queryRetrievePort);
        jq("#storagePort").val(currentOperation.pacs.storagePort);
        jq("#ormStrategySpringBeanId").val(currentOperation.pacs.ormStrategySpringBeanId);
        jq("#extendedNegotiations").prop("checked", currentOperation.pacs.supportsExtendedNegotiations);
        jq("#defaultQueryRetrievePacs").prop("checked", currentOperation.pacs.defaultQueryRetrievePacs);
        jq("#defaultStoragePacs").prop("checked", currentOperation.pacs.defaultStoragePacs);
    }

    function fnFooterCallback(nRow, aaData, iStart, iEnd, aiDisplay) {
        jq(constants.ADD_PACS_LINK_HOLDER).html("<a id='" + constants.ADD_PACS_LINK.substring(1) + "' href='javascript:void(0);'>Add New PACS</a>");
    }

    function bindAddButtonHandler() {
        var addButtonHandler = function () {
            currentOperation = new AddOperation(this);
            xmodal.open({
                width: 600,
                height: 350,
                className: 'addModal',
                title: 'Add PACS',
                content: editModalContent,
                okAction: XNAT.app.PacsAdministration.submitCurrentOperation
            });
        };
        jq(constants.PACS_TABLE).on("click", constants.ADD_PACS_LINK, addButtonHandler);
    }

    function bindEditButtonHandler() {
        var editButtonHandler = function () {
            var pacs = getPacsThatWasClickedOn(this);
            currentOperation = new ModifyOperation(this, "editRow", pacs, constants.OPERATION_EDIT);
            // currentOperation.disable();

            xmodal.open({
                width: 600,
                height: 350,
                className: 'editModal',
                title: 'Modify PACS',
                content: editModalContent,
                okAction: XNAT.app.PacsAdministration.submitCurrentOperation
            });

            populateFormFields();
        };
        jq(constants.PACS_TABLE).on("click", ".editRow", editButtonHandler);
    }

    function bindDeleteButtonHandler() {
        var deleteButtonHandler = function () {
            var pacs = getPacsThatWasClickedOn(this);
            currentOperation = new ModifyOperation(this, "deleteRow", pacs, constants.OPERATION_DELETE);
            // currentOperation.disable();

            xmodal.open({
                width: 400,
                height: 150,
                className: 'deleteModal',
                title: 'Confirm PACS Deletion',
                content: jq('#delete_modal_content').html(),
                okAction: XNAT.app.PacsAdministration.submitCurrentOperation
            });
        };
        jq(constants.PACS_TABLE).on("click", ".deleteRow", deleteButtonHandler);
    }

    function showPacs(data) {
        jq(constants.PACS_DIV).empty().html('<table cellpadding="0" cellspacing="0" border="0" id="' + constants.PACS_TABLE.substring(1) + '"><tfoot><tr><th style="padding-top: 20px;" align="left" id="' + constants.ADD_PACS_LINK_HOLDER.substring(1) + '" colspan="9"></th></tr></tfoot></table>');

        var dataTableOptions = {
            "aaData": data.ResultSet.Result,
            "aoColumns": [
                {
                    "mData": "id",
                    "sTitle": "ID"
                },
                {
                    "mData": "aeTitle",
                    "sTitle": "AE Title"
                },
                {
                    "mData": "host",
                    "sTitle": "Host"
                },
                {
                    "mData": "queryRetrievePort",
                    "sTitle": "Q/R Port"
                },
                {
                    "mData": "storagePort",
                    "sTitle": "STORE Port"
                },
                {
                    "mData": "ormStrategySpringBeanId",
                    "sTitle": "ORM Strategy Spring Bean ID"
                },
                {
                    "mData": function (source, x, y, z) {
                        return source.defaultQueryRetrievePacs ? "yes" : "no";
                    },
                    "sTitle": "Default Q/R Pacs?"
                },
                {
                    "mData": function (source, x, y, z) {
                        return source.defaultStoragePacs ? "yes" : "no";
                    },
                    "sTitle": "Default STORE Pacs?"
                },
                {
                    "mData": function (source, x, y, z) {
                        return source.supportsExtendedNegotiations ? "yes" : "no";
                    },
                    "sTitle": "Extended Negotiations?"
                },
                {
                    "bSearchable": false,
                    "bSortable": false,
                    "mData": null,
                    "sDefaultContent": '<span class="icon icon-sm icon-edit editRow" title="Edit"></span>'
                },
                {
                    "bSearchable": false,
                    "bSortable": false,
                    "mData": null,
                    "sDefaultContent": '<span class="icon icon-sm icon-trash deleteRow" title="Delete"></span>'
                }
            ],
            "bFilter": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bInfo": false,
            "fnFooterCallback": fnFooterCallback
        };

        jq(constants.PACS_TABLE).dataTable(dataTableOptions);

        bindAddButtonHandler();
        bindEditButtonHandler();
        bindDeleteButtonHandler();

        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function handlePacsSearchFailure(jqXHR) {
        jq(constants.PACS_DIV).text("Error " + jqXHR.status + ": " + jqXHR.responseText);
        closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    function getAllPacs() {
        jq.ajax({
            type: "GET",
            url: serverRoot + "/data/pacs?XNAT_CSRF=" + csrfToken,
            dataType: "json",
            success: showPacs,
            error: handlePacsSearchFailure
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function deletePacs() {
        jq.ajax({
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
        jq.ajax({
            type: "PUT",
            url: serverRoot + "/data/pacs/" + currentOperation.pacs.id + "?XNAT_CSRF=" + csrfToken,
            data: $form.serialize(),
            success: function () {
                xmodal.close();
                getAllPacs();
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("PACS modification failed: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    function addPacs($form) {
        jq.ajax({
            type: "POST",
            url: serverRoot + "/data/pacs?XNAT_CSRF=" + csrfToken,
            data: $form.serialize(),
            success: function () {
                xmodal.close();
                getAllPacs();
            },
            error: function (jqXHR) {
                closeModalPanel(constants.MODAL_WINDOW_NAME);
                alert("PACS addition failed: " + jqXHR.status + ": " + jqXHR.responseText);
            }
        });

        openModalPanel(constants.MODAL_WINDOW_NAME, "Loading data...");
    }

    return {
        init: function (pImagePath) {
            imagePath = pImagePath;
            editModalContent = jq("#edit_modal_content").html();
            jq("#edit_modal_content").empty();
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
                editPacs(jq("#editPacsForm"));
            } else if (currentOperation.type === constants.OPERATION_CREATE) {
                addPacs(jq("#editPacsForm"));
            } else {
                alert('Unspported operation type: ' + currentOperation.type);
            }
            currentOperation.enable();
        }
    };
}());

window.xModalSubmit = function () {
    "use strict";
    XNAT.app.PacsAdministration.submitCurrentOperation();
};

window.xModalCancel = function () {
    "use strict";
    xmodal.close();
    XNAT.app.PacsAdministration.cancelCurrentOperation();
};
