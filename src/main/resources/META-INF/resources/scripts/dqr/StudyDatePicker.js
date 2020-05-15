/*
 * dicom-query-retrieve: StudyDatePicker.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*jslint white: true, browser: true, vars: true */
/*global jq */
function StudyDatePicker(fromDateFieldId, toDateFieldId, useTodaysDateCheckboxId) {
    "use strict";

    var that = this;

    this.getFromDate = function () {
        return this.getDate(fromDateFieldId);
    };

    this.getToDate = function () {
        return this.getDate(toDateFieldId);
    };

    this.getDate = function (fieldId) {
        return Date.parse(jq("#" + fieldId).val());
    };

    this.setFromDate = function (newDate) {
        this.setDate(fromDateFieldId, newDate);
    };

    this.setToDate = function (newDate) {
        this.setDate(toDateFieldId, newDate);
    };

    this.setDate = function (fieldId, newDate) {
        jq("#" + fieldId).val(this.formatDate(newDate));
    };

    this.formatDate = function (parsedDate) {
        if (parsedDate) {
            return parsedDate.toString("MM/dd/yyyy");
        }
        return "";
    };

    this.formatDateFieldInPlace = function (fieldId) {
        this.setDate(fieldId, this.getDate(fieldId));
    };

    this.formatDateFieldsInPlace = function () {
        this.formatDateFieldInPlace(fromDateFieldId);
        this.formatDateFieldInPlace(toDateFieldId);
    };

    this.toggleStudyDateEnabled = function (enabled) {
        jq("#" + fromDateFieldId).prop("disabled", !enabled);
        jq("#" + toDateFieldId).prop("disabled", !enabled);
        var triggerIcons = jq("#" + fromDateFieldId).siblings("button");
        if (enabled) {
            triggerIcons.show();
            jq("#" + fromDateFieldId).focus();
        } else {
            triggerIcons.hide();
        }
    };

    this.getStudyDateValidationGroup = function () {
        return {
            studyDateRangeGroup: fromDateFieldId + " " + toDateFieldId
        };
    };

    this.getStudyDateValidationRules = function () {
        var dateFieldRules = {
            required: false,
            studyDate: true,
            studyDateRange: true
        };
        var datePickerRules = {};
        datePickerRules[fromDateFieldId] = dateFieldRules;
        datePickerRules[toDateFieldId] = dateFieldRules;
        return datePickerRules;
    };

    // "main"

    var datePickerOptions = {
        defaultDate: "+0D",
        maxDate: "+0D",
        showOn: "button",
        buttonText: "<i class='fa fa-calendar'></i>",
        buttonImageOnly: false,
        changeMonth: true,
        changeYear: true
    };
    jq("#" + fromDateFieldId).datepicker(datePickerOptions);
    jq("#" + toDateFieldId).datepicker(datePickerOptions);

    // overriding the default jQuery date validation cuz it sux
    // datejs is abandonware but it still rocks
    jq.validator.addMethod("studyDate", function (value, element) {
        return this.optional(element) || Date.parse(value);
    }, "Please specify a valid date.");

    jq.validator.addMethod("studyDateRange", function () {
        if (that.getFromDate() && that.getToDate()) {
            return Date.compare(that.getFromDate(), that.getToDate()) <= 0;
        }
        // user specified neither date, or an open-ended date range, so skip the comparison
        return true;
    }, "Please specify a valid date range, the first date must come before the second.");

    jq("#" + useTodaysDateCheckboxId).click(function () {
        if (jq(this).is(':checked')) {
            that.toggleStudyDateEnabled(false);
            var now = Date.today();
            that.setToDate(now);
            that.setFromDate(now);
            jq("#" + fromDateFieldId).closest("form").valid();
        } else {
            that.setToDate(null);
            that.setFromDate(null);
            that.toggleStudyDateEnabled(true);
        }
    });
}
