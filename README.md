# XNAT DICOM Query-Retrieve Plugin #

Core XNAT includes a DICOM SCP receiver that allows your system to receive image data that has been pushed from a PACS system. The Dicom Query-Retrieve (DQR) plugin allows XNAT users to connect directly to a PACS or other DICOM Application Entity, send queries to find studies, and import them to their XNAT with custom relabeling applied en route. Users can also send image data from XNAT to the PACS. 

With these new capabilities, the DQR Plugin can significantly impact your ability to interact with clinical PACS data. It also offers fine-tuned administrative controls, to help XNAT administrators and PACS administrators set and enforce policies on things such as data transfer rate and availability. 

**With the DQR Plugin, users can:**

* Find patient records by date, by MRN, by name, or other fields
* Import one, some, or all scan series for a given study into XNAT as a new image session
* Apply custom relabeling on every new import as needed
* Batch multiple queries and import requests using a formatted CSV manifest, applying custom relabeling for each import
* Send new scan series data back to PACS from your XNAT

**With the DQR Plugin, XNAT admins can:**

* Set up and manage connections to PACS systems and other DICOM AEs
* Configure availability windows and data throughput rates for each connected PACS
* Fine-tune XNAT's DICOM anonymization and relabeling behavior
* Restrict access to DQR functionality to a set of trusted users 

Full documentation on using and administering this plugin can be found here: https://wiki.xnat.org/xnat-tools/dicom-query-retrieve-plugin


## Building the Plugin Jar ##

You can build the jar for this, including required dependencies by doing:

```bash
./gradlew xnatPluginJar
```

Instructions on installing XNAT plugins can be found here: https://wiki.xnat.org/documentation/xnat-developer-documentation/working-with-xnat-plugins/developing-xnat-plugins
