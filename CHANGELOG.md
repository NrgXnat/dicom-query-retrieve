# XNAT, DICOM Query Retrieve (DQR) Plugin Changelog

The Dicom Query-Retrieve (DQR) plugin allows XNAT users to connect directly to
a PACS or other DICOM Application Entity, send queries to find studies, and
import them to their XNAT with custom relabeling applied en route. Users can
also send image data from XNAT to the PACS.

## <a name="3.0"></a>XNAT DQR Version 3.0 Release Notes

### <a name="3.0.0"></a>DQR Plugin Version: 3.0.0

Version 3.0.0 of the DQR Plugin requires XNAT 1.10.0 or newer and is compiled on JDK21. 

#### 3.0.0 - Improvements

* [PLUGINS-289](https://radiologics.atlassian.net/browse/PLUGINS-289) Update and validate core functions to use dcm4che5 
* [PLUGINS-291](https://radiologics.atlassian.net/browse/PLUGINS-291) Prevent thread leak from QrClient 
* [XNAT-8256](https://radiologics.atlassian.net/browse/XNAT-8256) Update build architecture to use JDK21

## <a name="2.4"></a>XNAT DQR Version 2.4 Release Notes

### <a name="2.4.0"></a>DQR Plugin Version: 2.4.0

#### 2.4.0 - General Improvements

* [PLUGINS-295](https://radiologics.atlassian.net/browse/PLUGINS-295) Add subject and experiment label fields to PacsRequest/QueuedPacsRequest routing, enabling incoming DICOM studies to be routed to a specific subject and experiment within a project at queue pickup time.
* [PLUGINS-296](https://radiologics.atlassian.net/browse/PLUGINS-296) Split the single DICOMweb HTTP read timeout into two configurable preferences, so binary retrieval (WADO-RS) is no longer constrained by the shorter metadata-query timeout.
    * `dicomWebMetadataReadTimeoutSeconds` (default 20) — socket read timeout in seconds for DICOMweb metadata requests: QIDO-RS searches, instance metadata fetches, and PACS ping checks.
    * `dicomWebRetrieveReadTimeoutSeconds` (default 20) — socket read timeout in seconds for DICOMweb binary retrieval (WADO-RS multipart responses); raise on PACS implementations whose server-side response-assembly delay before the first byte exceeds the default.


## <a name="2.3"></a>XNAT DQR Version 2.3 Release Notes

### <a name="2.3.2"></a>DQR Plugin Version: 2.3.2

#### 2.3.2 - Bug Fixes

* [PLUGINS-291](https://radiologics.atlassian.net/browse/PLUGINS-291) Fixed a thread leak in QrClient when connection failures were not properly cleaning up resources

### <a name="2.3.1"></a>DQR Plugin Version: 2.3.1

#### 2.3.1 - Fixes

* [PLUGINS-288](https://radiologics.atlassian.net/browse/PLUGINS-288) Fixed thread-safety issue in `DicomWebHttpClient` where a shared `HttpClientContext` caused sporadic HTTP 401 errors under concurrent DICOMweb access. Each request now uses its own context.

### <a name="2.3.0"></a>DQR Plugin Version: 2.3.0

#### 2.3.0 - General Improvements

* [PLUGINS-287](https://radiologics.atlassian.net/browse/PLUGINS-287) Improve DQR C-MOVE efficiency via swap to dcm4che3 implementation
* [PLUGINS-280](https://radiologics.atlassian.net/browse/PLUGINS-280) Allow NumberOfStudyRelatedInstances and NumberOfStudyRelatedSeries in C-FIND results when caller-provided tags are returned
* [PLUGINS-281](https://radiologics.atlassian.net/browse/PLUGINS-281) Optimize queue deletion API call


## <a name="2.2"></a>XNAT DQR Version 2.2 Release Notes

### <a name="2.2.2"></a>DQR Plugin Version: 2.2.2

#### 2.2.2 - General Improvements

* [PLUGINS-267](https://radiologics.atlassian.net/browse/PLUGINS-267) Fixed an issue that caused series imports to fail for certain PACS vendors

### <a name="2.2.1"></a>DQR Plugin Version: 2.2.1

#### 2.2.1 - General Improvements

* [PLUGINS-240](https://radiologics.atlassian.net/browse/PLUGINS-240) Added logic to properly handle HTTP 204 status code in DicomWebHttpClient
* [PLUGINS-218](https://radiologics.atlassian.net/browse/PLUGINS-218) Fixed an issue that caused the PACS schedule modal to not load

### <a name="2.2.0"></a>DQR Plugin Version: 2.2.0

#### 2.2.0 - General Improvements

  * [PLUGINS-212](https://radiologics.atlassian.net/browse/PLUGINS-212) Add a method to delete queued PACS requests that are associated with a request ID and status
  * Updated the required XNAT version to 1.9.1


## <a name="2.1"></a>XNAT DQR Version 2.1 Release Notes

### <a name="2.1.0"></a>DQR Plugin Version: 2.1.0

#### 2.1.0 - General Improvements

  * [XNAT-7990](https://radiologics.atlassian.net/browse/XNAT-7990) Update Hibernate and Spring dependencies for XNAT 1.9.0 compatibility
  * Improve unit tests and validation

#### 2.1.0 - Fixes

  * [XNAT-8163](https://radiologics.atlassian.net/browse/XNAT-8163) Remove "Host" as a constraint when configuring a DICOMweb connection to PACS


## <a name="2.0"></a>XNAT DQR Version 2.0 Release Notes

### 2.0.x Versions

  * [2.0.2](#2.0.2)
  * [2.0.1](#2.0.1)
  * [2.0.0](#2.0.0)

### <a name="2.0.2"></a>DQR Plugin Version: 2.0.2

#### 2.0.2 - General Improvements

  * [PLUGINS-187](https://radiologics.atlassian.net/browse/PLUGINS-187) Improve support for split QIDO and WADO PACS systems.
    Always retrieve series by querying for + retrieving from `RetrieveURL` rather than as a fallback.

#### 2.0.2 - Fixes

  * [PLUGINS-186](https://radiologics.atlassian.net/browse/PLUGINS-186) Tweak DICOMweb HTTP connection pool settings to enable more concurrent connections

### <a name="2.0.1"></a>DQR Plugin Version: 2.0.1

#### 2.0.1 - Fixes
  * [PLUGINS-185](https://radiologics.atlassian.net/browse/PLUGINS-185) Support DICOMweb operations for PACS vendors that split QIDO and WADO operations into separate root URL paths

### <a name="2.0.0"></a>DQR Plugin Version: 2.0.0

DQR now fully supports Query and Retrieve over DICOMweb.

#### 2.0.0 - General Improvements

  * [PLUGINS-96](https://radiologics.atlassian.net/browse/PLUGINS-96) Support configuring a PACS connection for DICOMweb,
    and performing query and retrieve (import) operations over DICOMweb.

#### 2.0.0 - Fixes
  * [PLUGINS-157](https://radiologics.atlassian.net/browse/PLUGINS-157) No longer restrict capitalization of Patient Name input on Query page

## <a name="1.3"></a>XNAT DQR Version 1.3 Release Notes

### 1.3.x Versions

[1.3.0](#1.3.0)

### <a name="1.3.0"></a>DQR Plugin Version: 1.3.0

This release updates the minimum supported XNAT version from 1.8.5 to 1.8.10.

#### 1.3.0 - General Improvements

  * [PLUGINS-22](https://radiologics.atlassian.net/browse/PLUGINS-22) Trigger session building and archiving when all study data is received, bypassing default archive timeout.
  * [PLUGINS-91](https://radiologics.atlassian.net/browse/PLUGINS-91) Delete partially received study data from the prearchive when an import request fails
  * [PLUGINS-93](https://radiologics.atlassian.net/browse/PLUGINS-93) Add internal API to find queued requests by study instance UID
  * [PLUGINS-100](https://radiologics.atlassian.net/browse/PLUGINS-100) [PLUGINS-102](https://radiologics.atlassian.net/browse/PLUGINS-102) Refactor internal service APIs to prepare for more substantial DICOMweb support

#### 1.3.0 - Fixes
  * [PLUGINS-94](https://radiologics.atlassian.net/browse/PLUGINS-94) DICOMweb: Fix Ping button in PACS configuration UI

## <a name="1.2"></a>XNAT DQR Version 1.2 Release Notes

### 1.2.x Versions

  * [1.2.0](#1.2.0)
  * [1.2.1](#1.2.1)

### <a name="1.2.1"></a>DQR Plugin Version: 1.2.1

#### 1.2.1 - General Improvements

  * [PLUGINS-92](https://radiologics.atlassian.net/browse/PLUGINS-92) DICOM web: support preemptive auth

#### 1.2.1 - Fixes
  * [PLUGINS-89](https://radiologics.atlassian.net/browse/PLUGINS-89) DQR: code refactor to clarify wait times for utilization and retrying
  * [PLUGINS-90](https://radiologics.atlassian.net/browse/PLUGINS-90) DQR: count "attempts" rather than "retries"


### <a name="1.2.0"></a>DQR Plugin Version: 1.2.0

#### 1.2.0 - General Improvements

  * [PLUGINS-86](https://radiologics.atlassian.net/browse/PLUGINS-86) DQR: Store error message when PACS request fails
  * [PLUGINS-87](https://radiologics.atlassian.net/browse/PLUGINS-87) DQR: Add a request id to identify pacs requests across queue and restarts

## <a name="1.1"></a>XNAT DQR Version 1.1 Release Notes

### 1.1.x Versions

  * [1.1.0](#1.1.0)

### <a name="1.1.0"></a>DQR Plugin Version: 1.1.0

#### 1.1.0 - General Improvements

  * [PLUGINS-83](https://radiologics.atlassian.net/browse/PLUGINS-83) Settings: Notifications settings apply to both admins and users
  * [PLUGINS-82](https://radiologics.atlassian.net/browse/PLUGINS-82) PACS Requests: Don't require a port for C-MOVE
  * [PLUGINS-79](https://radiologics.atlassian.net/browse/PLUGINS-79) PACS Requests: Configurable retry logic for C-MOVE request from the PACS

#### 1.1.0 - Fixes

  * [PLUGINS-81](https://radiologics.atlassian.net/browse/PLUGINS-81) PACS Requests: Fixed bug that caused DQR to not properly abide the “threads” availability setting