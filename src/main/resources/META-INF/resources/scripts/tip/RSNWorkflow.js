/**
 * Created by whorto01 on 11/19/2014.
 */

    /*
    index - this value will be stored for each study for each workflow
    label - this value will be used in workflow panels in session and assessor report pages
    shortLabel - this value will be used in workflow summary tables
    validation - a list of conditions that must be true for this status to be correct (using made up terms for now)
    actions - this array specifies which workflow-specific actions can be run. each one gets a button placed in the workflow panel.
        label - button text for custom action
        actionType - how to style the button
        actionURL - what should happen when user clicks button. (could be a function)
        actionSrc - on what entity in XNAT can this action appear? (i.e. session, assessor, etc.)
        restrictedRoles - if present, restricts who can perform action
    workflowComplete - boolean value. Only set to true if no more actions needed or possible.
    */

    /*
    suggested attributes that could be tracked to validate these statuses:
    - boldScanCount: number of BOLD scans in MR session
    - t1ScanCount: number of T1 scans in MR session
    - pipelineStartStatusRSN: has the RSN pipeline been launched? Is it currently running?
    - pipelineCompleteStatusRSN: has the RSN pipeline completed? Has it generated a QC assessor?
    - qcStatusRSN: has the RSN QC been reviewed and an approve/reject decision made?
    - qcResultRSN: did the RSN pipeline output pass or fail QC? or "other"?
    - initiateExportRSN: has RSN output been sent to PACS?
    - confirmExportRSN: has PACS received RSN output?
    */

var RSNworkflow = {

    "status" : [
        {
            "index": 0,
            "label": "Cannot start RSN Workflow. T1 and BOLD series must be present.",
            "shortLabel": "Requires T1/BOLD Series",
            "validation": "boldScanCount == 0 || t1ScanCount == 0",
            "actions": []
        },
        {
            "index": 1,
            "label": "Study Imported. Ready for RSN Pipeline.",
            "shortLabel": "Ready to Launch",
            "validation": "boldScanCount > 0 && t1ScanCount > 0 && pipelineStartStatusRSN == false",
            "actions": [
                {
                    "label": "Run RSN Gen",
                    "actionType": "pipeline",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                }
            ]
        },
        {
            "index": 2,
            "label": "RSN Pipeline Running.",
            "shortLabel": "Ready to Launch",
            "validation": "pipelineStartStatusRSN == true && pipelineCompleteStatusRSN == false",
            "actions": []
        },
        {
            "index": 3,
            "label": "RSN Pipeline Complete. Ready for QC.",
            "shortLabel": "Ready for QC",
            "validation": "pipelineCompleteStatusRSN == true && qcStatusRSN == false",
            "actions": [
                {
                    "label": "Review RSN QC",
                    "actionType": "view",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                },
                {
                    "label": "Approve and Export to PACS",
                    "actionType": "approve",
                    "actionSrc": "assessor_imageQC",
                    "actionURL": "?"
                },
                {
                    "label": "Reject",
                    "actionType": "reject",
                    "actionSrc": "assessor_imageQC",
                    "actionURL": "?"
                }
            ]
        },
        {
            "index": 4,
            "label": "QC Approved. Ready to Export to PACS.",
            "shortLabel": "Ready for PACS Export",
            "validation": "qcStatusRSN == true && qcResultRSN == 'pass' && initiateExportRSN == false",
            "actions": [
                {
                    "label": "Export to PACS",
                    "actionType": "approve",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                },
                {
                    "label": "Export to PACS",
                    "actionType": "approve",
                    "actionSrc": "assessor_imageQC",
                    "actionURL": "?"
                }
            ]
        },
        {
            "index": 5,
            "label": "RSN Exported to PACS. Needs Verification of Receipt.",
            "shortLabel": "Confirm Arrival at PACS",
            "validation": "initiateExportRSN == true && confirmExportRSN == false",
            "actions": [
                {
                    "label": "Confirm Arrival at PACS",
                    "actionType": "confirm",
                    "actionURL": "?"
                },
                {
                    "label": "Resend to PACS",
                    "actionType": "resend",
                    "actionSrc": "ImageSession",
                    "actionURL": "?",
                    "restrictedRoles": [
                        "XNATadmin","XNATprojectOwner"
                    ]
                }
            ]
        },
        {
            "index": 6,
            "label": "RSN Exported to PACS. Workflow complete.",
            "shortLabel": "Workflow Complete",
            "validation": "confirmExportRSN == true",
            "actions": [
                {
                    "label": "Mark Study Closed",
                    "actionType": "close",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                }
            ],
            "workflowComplete": true
        },
        {
            "index": 7,
            "label": "RSN Pipeline QC Rejected. QC Pipeline Needs to be Rerun. See Notes.",
            "shortLabel": "QC Rejected",
            "validation": "qcStatusRSN == true && qcResultRSN != 'pass' && qcResultRSN != 'unusable'",
            "actions": [
                {
                    "label": "Run RSN Gen",
                    "actionType": "pipeline",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                }
            ]
        },
        {
            "index": 8,
            "label": "RSN Pipeline Failed to Run. QC Pipeline Needs to be Rerun.",
            "shortLabel": "QC Rejected",
            "validation": "pipelineStartStatusRSN == true && pipelineCompleteStatusRSN == false",
            "actions": [
                {
                    "label": "Run RSN Gen",
                    "actionType": "pipeline",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                }
            ]
        },
        {
            "index": 9,
            "label": "Study Marked Unusable. Workflow complete.",
            "shortLabel": "Study Unusable",
            "validation": "qcStatusRSN == true && qcResultRSN == 'unusable'",
            "actions": [
                {
                    "label": "Mark Study Closed",
                    "actionType": "close",
                    "actionSrc": "ImageSession",
                    "actionURL": "?"
                }
            ]

        }

    ]
}
