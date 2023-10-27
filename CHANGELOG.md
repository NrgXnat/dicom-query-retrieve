# XNAT, DICOM Query Retrieve (DQR) Plugin Changelog - _Latest Release [1.2.0](#1.2.0)_

The Dicom Query-Retrieve (DQR) plugin allows XNAT users to connect directly to
a PACS or other DICOM Application Entity, send queries to find studies, and
import them to their XNAT with custom relabeling applied en route. Users can
also send image data from XNAT to the PACS.

## <a name="1.2"></a>XNAT DQR Version 1.2 Release Notes

### 1.2.x Versions

[1.2.0](#1.2.0)

### <a name="1.2.0"></a>DQR Plugin Version: 1.2.0

#### 1.2.0 - General Improvements

   * [PLUGINS-86](https://radiologics.atlassian.net/browse/PLUGINS-86) DQR: Store error message when PACS request fails
   * [PLUGINS-87](https://radiologics.atlassian.net/browse/PLUGINS-87) DQR: Add a request id to identify pacs requests across queue and restarts

## <a name="1.1"></a>XNAT DQR Version 1.1 Release Notes

### 1.1.x Versions

[1.1.0](#1.1.0)

### <a name="1.1.0"></a>DQR Plugin Version: 1.1.0

#### 1.1.0 - General Improvements

   * [PLUGINS-83](https://radiologics.atlassian.net/browse/PLUGINS-83) Settings: Notifications settings apply to both admins and users
   * [PLUGINS-82](https://radiologics.atlassian.net/browse/PLUGINS-82) PACS Requests: Don't require a port for C-MOVE
   * [PLUGINS-79](https://radiologics.atlassian.net/browse/PLUGINS-79) PACS Requests: Configurable retry logic for C-MOVE request from the PACS

#### 1.1.0 - Fixes

   * [PLUGINS-81](https://radiologics.atlassian.net/browse/PLUGINS-81) PACS Requests: Fixed bug that caused DQR to not properly abide the “threads” availability setting

