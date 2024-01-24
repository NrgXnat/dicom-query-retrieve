package org.nrg.xnatx.dqr.utils;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Slf4j
public class Dcm4cheUtilsTest {
    private static final int SERIES_DESCRIPTION_TAG = 528446;
    private static final String SERIES_DESCRIPTION_NAME = "SeriesDescription";
    private static final String SERIES_DESCRIPTION_HEX_TAG_BASIC = "8103E";
    private static final String SERIES_DESCRIPTION_HEX_TAG_PADDED = "0008103E";
    private static final String SERIES_DESCRIPTION_HEX_TAG_QUALIFIED = "0x0008103E";
    private static final String SERIES_DESCRIPTION_HEX_TAG_FORMATTED = "(0008,103E)";
    private static final List<String> TAG_STRINGS = Arrays.asList(
            "(0008,0020)", "(0008,0030)", "(0008,0050)", "(0008,0060)", "(0008,0061)", "(0008,0062)", "(0008,0063)",
            "(0008,0090)", "(0008,041F)", "(0008,1030)", "(0008,1032)", "(0008,103E)", "(0008,1060)", "(0008,1080)",
            "(0008,1110)", "(0008,1120)", "(0010,0010)", "(0010,0020)", "(0010,0021)", "(0010,0030)", "(0010,0032)",
            "(0010,0040)", "(0010,1001)", "(0010,1002)", "(0010,1010)", "(0010,1020)", "(0010,1030)", "(0010,2160)",
            "(0010,2180)", "(0010,21B0)", "(0010,4000)", "(0020,000D)", "(0020,000E)", "(0020,0010)", "(0020,0011)",
            "(0020,1070)", "(0020,1200)", "(0020,1202)", "(0020,1204)", "(0020,1206)", "(0020,1208)", "(0020,1209)"
    );
    private static final List<Number> TAG_INTS = Arrays.asList(
            0x00080020, 0x00080030, 0x00080050, 0x00080060, 0x00080061, 0x00080062, 0x00080063,
            0x00080090, 0x0008041F, 0x00081030, 0x00081032, 0x0008103E, 0x00081060, 0x00081080,
            0x00081110, 0x00081120, 0x00100010, 0x00100020, 0x00100021, 0x00100030, 0x00100032,
            0x00100040, 0x00101001, 0x00101002, 0x00101010, 0x00101020, 0x00101030, 0x00102160,
            0x00102180, 0x001021B0, 0x00104000, 0x0020000D, 0x0020000E, 0x00200010, 0x00200011,
            0x00201070, 0x00201200, 0x00201202, 0x00201204, 0x00201206, 0x00201208, 0x00201209
    );
    private static final List<String> TAG_NAMES = Arrays.asList(
            "AccessionNumber", "AdditionalPatientHistory", "AdmittingDiagnosesDescription", "AnatomicRegionsInStudyCodeSequence", "EthnicGroup", "IssuerOfPatientID",
            "ModalitiesInStudy", "Modality", "NameOfPhysiciansReadingStudy", "NumberOfPatientRelatedInstances", "NumberOfPatientRelatedSeries", "NumberOfPatientRelatedStudies",
            "NumberOfSeriesRelatedInstances", "NumberOfStudyRelatedInstances", "NumberOfStudyRelatedSeries", "Occupation", "OtherPatientIDsSequence", "OtherPatientNames",
            "OtherStudyNumbers", "PatientAge", "PatientBirthDate", "PatientBirthTime", "PatientComments", "PatientID", "PatientName", "PatientSex", "PatientSize", "PatientWeight",
            "ProcedureCodeSequence", "ReferencedPatientSequence", "ReferencedStudySequence", "ReferringPhysicianName", "SOPClassesInStudy", "SeriesDescription", "SeriesInstanceUID",
            "SeriesNumber", "StudyDate", "StudyDescription", "StudyID", "StudyInstanceUID", "StudyTime", "StudyUpdateDateTime"
    );
    private static final Map<String, Number> TAGS;
    static {
        final HashMap<String, Number> tags = new HashMap<>();
        tags.put("BodyPartExamined", 1572885);
        tags.put("BurnedInAnnotation", 2622209);
        tags.put("Columns", 2621457);
        tags.put("CompressionCode", 2621536);
        tags.put("Decoupling", 1609817);
        tags.put("FlipAngle", 1577748);
        tags.put("InstanceNumber", 2097171);
        tags.put("PatientID", 1048608);
        tags.put("PatientName", 1048592);
        tags.put("Rows", 2621456);
        tags.put("SOPInstanceUID", 524312);
        tags.put(SERIES_DESCRIPTION_NAME, SERIES_DESCRIPTION_TAG);
        tags.put("SeriesInstanceUID", 2097166);
        tags.put("SeriesNumber", 2097169);
        TAGS = Collections.unmodifiableMap(tags);
    }

    @Test
    public void testGetTagNamesAndValues() {
        final List<Number> tags = TAG_STRINGS.stream().map(Dcm4cheUtils::getTag).collect(Collectors.toList());
        assertThat(tags)
                .isNotNull()
                .isNotEmpty()
                .containsExactlyInAnyOrderElementsOf(TAG_INTS);

        final List<String> names = tags.stream().map(Dcm4cheUtils::getTagName).collect(Collectors.toList());
        assertThat(names)
                .isNotNull()
                .isNotEmpty()
                .containsExactlyInAnyOrderElementsOf(TAG_NAMES);

        final List<String> formatted = names.stream().map(Dcm4cheUtils::getFormattedTag).collect(Collectors.toList());
        assertThat(formatted)
                .isNotNull()
                .isNotEmpty()
                .hasSize(TAG_STRINGS.size())
                .containsExactlyInAnyOrderElementsOf(TAG_STRINGS);

        final Number notATag = Dcm4cheUtils.getTag("NotATag");
        assertThat(notATag)
                .isNotNull()
                .isInstanceOf(Integer.class)
                .asInstanceOf(InstanceOfAssertFactories.INTEGER)
                .isEqualTo(-1);
    }

    @Test
    public void testTagNamesAndValues() {
        final List<Number> tags = TAGS.keySet().stream().map(Dcm4cheUtils::getTag).collect(Collectors.toList());
        assertThat(tags)
                .isNotNull()
                .isNotEmpty()
                .hasSize(TAGS.size())
                .containsExactlyInAnyOrderElementsOf(TAGS.values());

        final List<String> names = TAGS.values().stream().map(Dcm4cheUtils::getTagName).collect(Collectors.toList());
        assertThat(names)
                .isNotNull()
                .isNotEmpty()
                .hasSize(TAGS.size())
                .containsExactlyInAnyOrderElementsOf(TAGS.keySet());

        assertThat(Dcm4cheUtils.getTag(SERIES_DESCRIPTION_NAME)).isEqualTo(SERIES_DESCRIPTION_TAG);
        assertThat(Dcm4cheUtils.getTag(SERIES_DESCRIPTION_HEX_TAG_BASIC)).isEqualTo(SERIES_DESCRIPTION_TAG);
        assertThat(Dcm4cheUtils.getTag(SERIES_DESCRIPTION_HEX_TAG_PADDED)).isEqualTo(SERIES_DESCRIPTION_TAG);
        assertThat(Dcm4cheUtils.getTag(SERIES_DESCRIPTION_HEX_TAG_QUALIFIED)).isEqualTo(SERIES_DESCRIPTION_TAG);
        assertThat(Dcm4cheUtils.getTag(SERIES_DESCRIPTION_HEX_TAG_FORMATTED)).isEqualTo(SERIES_DESCRIPTION_TAG);
    }
}
