package org.nrg.xnatx.dqr.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dcm4che3.data.Tag;
import org.nrg.framework.utilities.BasicXnatResourceLocator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: This class should be moved into the dicomtools library as org.nrg.dicomtools.utilities.Dcm4cheUtils
@Slf4j
public class Dcm4cheUtils {
    private static final Pattern             TAG_PATTERN                 = Pattern.compile("^(0x|\\()(?<group>[A-F0-9]{4}),?\\s*(?<element>[A-F0-9]{4})\\)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern             HEX_PATTERN                 = Pattern.compile("^[A-Fa-f0-9]{5,8}$", Pattern.CASE_INSENSITIVE);
    public static final  Map<Number, String> DCM4CHE_DICOM_NAMES_BY_TAG  = loadDcm4cheTags();
    public static final  Map<Number, String> EXTENDED_DICOM_NAMES_BY_TAG = loadDicomExtensions();
    private static final Map<Number, String> DICOM_NAMES_BY_TAG          = Stream.concat(DCM4CHE_DICOM_NAMES_BY_TAG.entrySet().stream(), EXTENDED_DICOM_NAMES_BY_TAG.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k2));
    private static final Map<String, Number> DICOM_TAGS_BY_NAME          = DICOM_NAMES_BY_TAG.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    /**
     * This method returns a map of tag names and tags, filtering out any entries that have extended tags that have long
     * values. Compare this to {@link #getDicomTagsByName(Stream)}, which includes <em>all</em> tags, including extended
     * tags with long values.
     * <p>
     * This method is useful for working with methods that presume tags are integers.
     *
     * @param tagNames The list of tag names to retrieve
     *
     * @return A map containing tag names and integer tags.
     *
     * @see #getDicomTagsByName(Stream)
     */
    public static Map<String, Integer> getBoundedDicomTagsByName(final Collection<String> tagNames) {
        return getBoundedDicomTagsByName(tagNames.stream());
    }

    /**
     * This method returns a map of tag names and tags, filtering out any entries that have extended tags that have long
     * values. Compare this to {@link #getDicomTagsByName(Stream)}, which includes <em>all</em> tags, including extended
     * tags with long values.
     * <p>
     * This method is useful for working with methods that presume tags are integers.
     *
     * @param tagNames The list of tag names to retrieve
     *
     * @return A map containing tag names and integer tags.
     *
     * @see #getDicomTagsByName(Stream)
     */
    public static Map<String, Integer> getBoundedDicomTagsByName(final Stream<String> tagNames) {
        return getDicomTagsByName(tagNames).entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof Long))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));
    }

    /**
     * This method returns a map of tag names and tags. Unlike {@link #getBoundedDicomTagsByName(Stream)}, this method
     * includes all entries, including those with extended tags that have long values.
     *
     * @param tagNames The list of tag names to retrieve
     *
     * @return A map containing tag names and tags.
     *
     * @see #getBoundedDicomTagsByName(Stream)
     */
    @SuppressWarnings("unused")
    public static Map<String, Number> getDicomTagsByName(final Collection<String> tagNames) {
        return getDicomTagsByName(tagNames.stream());
    }

    /**
     * This method returns a map of tag names and tags. Unlike {@link #getBoundedDicomTagsByName(Stream)}, this method
     * includes all entries, including those with extended tags that have long values.
     *
     * @param tagNames The list of tag names to retrieve
     *
     * @return A map containing tag names and tags.
     *
     * @see #getBoundedDicomTagsByName(Stream)
     */
    public static Map<String, Number> getDicomTagsByName(final Stream<String> tagNames) {
        return tagNames.map(tagName -> Pair.of(tagName, getTag(tagName)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns the name for the tag value. <em>Most</em> tags only have a single value, e.g. 0x0020000D (or 2,097,165 in
     * decimal) is only assigned to <pre>StudyInstanceUID</pre>. Some tags have multiple names, but these are all
     * retired and are filtered out.
     *
     * @param tag The tag for which you want to retrieve a name
     *
     * @return The name of the tag or null if not valid.
     */
    public static String getTagName(final Number tag) {
        return DICOM_NAMES_BY_TAG.get(tag);
    }

    /**
     * Returns the tag for the specified name. This handles a number of different formats for the tag name:
     *
     * <ul>
     *     <li>Tag name, e.g. <pre>SeriesDescription</pre> or <pre>SeriesNumber</pre></li>
     *     <li>
     *         Group-element notation in the form <pre>(gggg,eeee)</pre>, e.g. <pre>(0008,103E)</pre> for series
     *         description: note that hexadecimal numbers are case insensitive
     *     </li>
     *     <li>
     *         Plain hex values, e.g. <pre>8103E</pre> or <pre>0008103e</pre>: this is case insensitive and supports
     *         padded or un-padded values
     *     </li>
     * </ul>
     *
     * @param tagName The name of the tag to resolve
     *
     * @return The tag for the specified name if it exists, 0 otherwise.
     */
    public static Number getTag(final String tagName) {
        final String cleaned = TAG_PATTERN.matcher(tagName).matches() ? StringUtils.removeStart(RegExUtils.removeAll(tagName, "[,()]"), "0x") : tagName;
        return HEX_PATTERN.matcher(cleaned).matches() ? Integer.parseInt(cleaned, 16) : DICOM_TAGS_BY_NAME.getOrDefault(cleaned, -1);
    }

    /**
     * Formats the tag using DICOM group-element notation in the form <pre>(gggg,eeee)</pre>. For example,
     * <pre>SeriesDescription</pre> has the integer value <pre>528446</pre>, which has the hex value <pre>0x8103E</pre>,
     * which gets formatted to <pre>(0008,103E)</pre>.
     *
     * @param tag The tag to format
     *
     * @return The formatted tag.
     */
    public static String getFormattedTag(final Number tag) {
        final String hex = toExpandedHexString(tag);
        return "(" + StringUtils.substring(hex, 0, 4) + "," + StringUtils.substring(hex, 4, 8) + ")";
    }

    /**
     * Formats the tag for the tag name using DICOM group-element notation in the form <pre>(gggg,eeee)</pre>. This
     * method calls {@link #getTag(String)} to resolve the tag then {@link #getFormattedTag(Number)} to get the formatted
     * tag.
     *
     * @param tagName The name of the tag to format
     *
     * @return The formatted tag.
     *
     * @see #getTag(String)
     * @see #getFormattedTag(Number)
     */
    public static String getFormattedTag(final String tagName) {
        return StringUtils.isNotBlank(tagName) ? getFormattedTag(getTag(tagName)) : null;
    }

    /**
     * Converts the number to an otherwise unformatted (with no zero-padding or hex prefix) hex string. The parameter
     * <code>number</code> should be an integer or long value, although this method will return a value if it is a float
     * or double: anything to the right of the decimal point is ignored during conversion.
     *
     * @param number The number to format as a hex string
     *
     * @return The hex string for the submitted value.
     */
    public static String toHexString(final Number number) {
        return number instanceof Integer ? Integer.toHexString(number.intValue()) : Long.toHexString(number.longValue());
    }

    /**
     * Converts the number to an expanded zero-padded hex string, <em>not</em> including the <code>0x</code> hex prefix.
     * The parameter <code>number</code> should be an integer or long value, although this method will return a value if
     * it is a float or double: anything to the right of the decimal point is ignored during conversion.
     *
     * @param number The number to format as an expanded hex string
     *
     * @return The expanded hex string for the submitted value.
     */
    public static String toExpandedHexString(final Number number) {
        return StringUtils.leftPad(StringUtils.upperCase(toHexString(number)), 8, '0');
    }

    private static Map<Number, String> loadDcm4cheTags() {
        return Arrays.stream(Tag.class.getFields())
                .collect(Collectors.groupingBy(tag -> {
                            try {
                                return (Number) tag.get(null);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        Collectors.mapping(Field::getName, Collectors.toList())))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
    }

    private static Map<Number, String> loadDicomExtensions() {
        try {
            return BasicXnatResourceLocator.getResources("classpath*:config/dicom/*-dicom-extensions.properties").stream().map(resource -> {
                        Properties properties = new Properties();
                        try (final InputStream inputStream = resource.getInputStream()) {
                            properties.load(inputStream);
                        } catch (IOException e) {
                            log.error("Got an error trying to load a DICOM extensions properties file {}", resource, e);
                        }
                        return properties;
                    }).flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(entry -> getTag(entry.getValue().toString()), entry -> entry.getKey().toString(), (k1, k2) -> k1));
        } catch (IOException e) {
            log.error("Got an error trying to load DICOM extensions properties files", e);
            return Collections.emptyMap();
        }
    }
}
