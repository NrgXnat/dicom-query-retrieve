# dcm4che2 to dcm4che5 Migration Notes

## Branch: `upgrade-dcm4che5-try`

## Migration Progress

### Completed
- [x] Data model classes (DqrPersonName, Patient, ReferringPhysicianName)
- [x] Utility classes (Dcm4cheUtils - removed dcm4che2 conversion methods)
- [x] BasicPatientNameStrategy - updated to use dcm4che3 PersonName
- [x] build.gradle - removed dcm4che2 dependencies

### In Progress
- [ ] DIMSE commands (C-FIND, C-MOVE, C-ECHO) - requires rewrite with dcm4che3-net API

### Pending
- [ ] CFindSCUSpecificLevel and subclasses
- [ ] DqrCMoveDcmQR
- [ ] Dcm4cheToolCEchoSCU
- [ ] ResultSetLimitStrategy (uses QueryRetrieveLevel)
- [ ] Series, Study domain classes (minor dcm4che2 references)

## Key Findings

### 1. dcm4che2 vs dcm4che3/5 Tool Availability

| Feature | dcm4che2 | dcm4che3/5 |
|---------|----------|------------|
| DcmQR (C-FIND/C-MOVE) | `org.dcm4che2.tool.dcmqr.DcmQR` - usable as library | No equivalent published to Maven |
| DcmEcho (C-ECHO) | `org.dcm4che2.tool.dcmecho.DcmEcho` - usable as library | No equivalent published to Maven |
| CLI tools | Can be used as both CLI and Java API | Primarily CLI, source code only |

### 2. API Differences

#### Data Model
```java
// dcm4che2
org.dcm4che2.data.DicomObject
org.dcm4che2.data.Tag
org.dcm4che2.data.PersonName

// dcm4che3/5
org.dcm4che3.data.Attributes
org.dcm4che3.data.Tag
org.dcm4che3.data.PersonName
```

#### Network Operations
```java
// dcm4che2 - Simple high-level API
DcmQR dcmQR = new DcmQR("LOCAL_AE");
dcmQR.setRemoteHost("pacs.example.com");
dcmQR.setRemotePort(104);
dcmQR.setCalledAET("REMOTE_AE", true);
dcmQR.setQueryLevel(QueryRetrieveLevel.STUDY);
dcmQR.open();
List<DicomObject> results = dcmQR.query();
dcmQR.close();

// dcm4che3/5 - Requires manual setup
Device device = new Device("device");
Connection conn = new Connection();
ApplicationEntity ae = new ApplicationEntity("LOCAL_AE");
// ... extensive configuration required
Association assoc = ae.connect(remoteConn, aarq);
// ... manual DIMSE handling
```

### 3. Required dcm4che3 Classes for DIMSE Implementation

For C-ECHO:
- `org.dcm4che3.net.Device`
- `org.dcm4che3.net.Connection`
- `org.dcm4che3.net.ApplicationEntity`
- `org.dcm4che3.net.Association`
- `org.dcm4che3.net.pdu.AAssociateRQ`
- `org.dcm4che3.data.UID` (for Verification SOP Class)

For C-FIND:
- All of the above plus:
- `org.dcm4che3.net.DimseRSPHandler`
- `org.dcm4che3.net.Priority`
- `org.dcm4che3.data.Attributes` (for query keys)
- SOP Classes: `UID.PatientRootQueryRetrieveInformationModelFind`, etc.

For C-MOVE:
- All of the above plus:
- Move destination AE configuration
- SOP Classes: `UID.PatientRootQueryRetrieveInformationModelMove`, etc.

### 4. QueryRetrieveLevel Mapping

```java
// dcm4che2
org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel.PATIENT
org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel.STUDY
org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel.SERIES
org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel.IMAGE

// dcm4che3/5 - Use string values in Attributes
attributes.setString(Tag.QueryRetrieveLevel, VR.CS, "PATIENT");
attributes.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
attributes.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
attributes.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
```

### 5. Transfer Syntax Configuration

dcm4che3 requires explicit TransferCapability configuration:
```java
String[] TRANSFER_SYNTAXES = {
    UID.ImplicitVRLittleEndian,
    UID.ExplicitVRLittleEndian,
    UID.ExplicitVRBigEndian
};

TransferCapability tc = new TransferCapability(null,
    UID.StudyRootQueryRetrieveInformationModelFind,
    TransferCapability.Role.SCU,
    TRANSFER_SYNTAXES);
ae.addTransferCapability(tc);
```

## Implementation Strategy

1. Create abstract base class `Dcm4che3DicomClient` with common Device/Connection/AE setup
2. Implement `Dcm4che3CEchoSCU` - simplest, good starting point
3. Implement `Dcm4che3CFindSCU` - handle query levels and result parsing
4. Implement `Dcm4che3CMoveSCU` - add move destination handling
5. Update existing classes to use new implementations

## References

- dcm4che3 source: https://github.com/dcm4che/dcm4che
- XNAT DicomSender example: `~/projects/Java21/dicomtools/src/main/java/org/nrg/dcm/DicomSender.java`
- XNAT builders: `org.nrg.dicomtools.builders.NetworkApplicationEntityBuilder`, `NetworkConnectionBuilder`
