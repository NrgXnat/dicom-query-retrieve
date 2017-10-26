Translational Informatics Portal (TIP) Modules
==============================================

This repository contains the code for the modules implementing TIP functionality within XNAT. Pipeline code is hosted at the [tip _ pipelines Bitbucket Repo](https://bitbucket.org/nrg/tip_pipelines "TIP Pipelines Bitbucket Repo").

You'll need to configure the AE title of your XNAT in dicom-import-context.xml, to match what the PACS you'll be talking to know you as.

The following files are part of core XNAT, so changesets affecting these files in xnat_builder will need to be manually merged:

- image-search/src/main/resources/module-resources/conf/dicom-import-context.xml

UNIT TESTING DISCLOSURE
----------------------------------------------------------------------------------------
The unit tests in this project are cheating in that they are not really self-contained...they require some data to be in the test PACS you are hitting.  

Take the DICOM in src/test/resources/sample2-tip-junit.zip and C-STORE it into whatever test PACS you are using.  Then the unit tests should pass.

NOTE: This data was derived from the [NRG "sample2" study](http://nrg.wustl.edu/1.4/sample2.zip) by modifying the following patient/study fields:

- Patient ID
- Patient Name
- Patient Birth Date
- Patient Sex
- Study Date
- Referring Physician Name
- Study ID
- Accession Number
- Study Description
