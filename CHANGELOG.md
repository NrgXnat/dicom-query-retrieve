# XNAT - Plugins: DICOM Query Retrieve - Changelog

The Dicom Query-Retrieve (DQR) plugin allows XNAT users to connect directly to
a PACS or other DICOM Application Entity, send queries to find studies, and
import them to their XNAT with custom relabeling applied en route. Users can
also send image data from XNAT to the PACS.

## <a name="1.1"></a>Release Notes - Version: 1.1 (_Latest Version: 1.1.0_)

### 1.1.x Versions

#### <a name="1.1.0"></a>1.1.0

##### General Improvements

  * [PLUGINS-83](https://radiologics.atlassian.net/browse/PLUGINS-83) Settings: Notifications settings apply to both admins and users
  * [PLUGINS-82](https://radiologics.atlassian.net/browse/PLUGINS-82) PACS Requests: Don't require a port for C-MOVE
  * [PLUGINS-79](https://radiologics.atlassian.net/browse/PLUGINS-79) PACS Requests: Configurable retry logic for C-MOVE request from the PACS

##### Fixes

  * [PLUGINS-81](https://radiologics.atlassian.net/browse/PLUGINS-81) PACS Requests: Fixed bug that caused DQR to not properly abide the “threads” availability setting
