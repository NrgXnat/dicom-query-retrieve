package org.nrg.xnatx.dqr.dicom.json;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.json.JSONReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

/**
 * Test that the DicomCorrectionJsonParser converts noncompliant InlineBinary-valued numeric elements
 * to decimal array values; and that other values are preserved, including potentially tricky cases
 * like SQ (nested objects), PN (JSON object inside the value), AT (hex string encoded 32-bit int)
 * and unsigned numbers (Java doesn't support those).
 */
public class DicomCorrectingJsonParserTest {
    @Test
    public void oneElementNoChanges() {
        final String source = "{\"00080012\": {\n" +
                "      \"vr\": \"DA\",\n" +
                "      \"Value\": [ \"20230103\" ]}}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00080012));
        Assertions.assertEquals("20230103", o.getString(0x00080012));
    }

    @Test
    public void vrSLInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"SL\",\n" +
                "        \"InlineBinary\": \"AQAAAAEAAAA=\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new int[]{1, 1}, o.getInts(0x00700052));
    }

    @Test
    public void vrSLValue() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"SL\",\n" +
                "        \"Value\": [ 2, -3] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new int[]{2, -3}, o.getInts(0x00700052));
    }

    /**
     * For OW, InlineBinary is left in the JSON token stream.
     * Verify that the encoded data comes out correct on the other side.
     * @throws IOException
     */
    @Test
    public void vrOWInlineBinary() throws IOException {
        final String source = "{\"00283006\": {\n" +
                "        \"vr\": \"OW\",\n" +
                "        \"InlineBinary\": \"AQAAAAEAAAA=\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00283006));
        Assertions.assertArrayEquals(new byte[]{1,0,0,0,1,0,0,0}, o.getBytes(0x00283006));
    }

    @Test
    public void multiElementNoConversion() {
        final String source = "{\"00080012\": {\n" +
                "      \"vr\": \"DA\",\n" +
                "      \"Value\": [\n" +
                "        \"20230103\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"00080013\": {\n" +
                "      \"vr\": \"TM\",\n" +
                "      \"Value\": [\n" +
                "        \"093131.000\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"00080016\": {\n" +
                "      \"vr\": \"UI\",\n" +
                "      \"Value\": [\n" +
                "        \"1.2.840.10008.5.1.4.1.1.11.1\"\n" +
                "      ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(3, o.size());
        Assertions.assertEquals("20230103", o.getString(0x00080012));
        Assertions.assertEquals("093131.000", o.getString(0x00080013));
        Assertions.assertEquals("1.2.840.10008.5.1.4.1.1.11.1", o.getString(0x00080016));
    }

    @Test
    public void multiElementsNestedSQNoConversion() {
        final String source = "{\"00281053\": {\n" +
                "      \"vr\": \"DS\",\n" +
                "      \"Value\": [\n" +
                "        1.0\n" +
                "      ]\n" +
                "    },\n" +
                "    \"00281054\": {\n" +
                "      \"vr\": \"LO\",\n" +
                "      \"Value\": [\n" +
                "        \"US\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"00283110\": {\n" +
                "      \"vr\": \"SQ\",\n" +
                "      \"Value\": [\n" +
                "        {\n" +
                "          \"00081140\": {\n" +
                "            \"vr\": \"SQ\",\n" +
                "            \"Value\": [\n" +
                "              {\n" +
                "                \"00081150\": {\n" +
                "                  \"vr\": \"UI\",\n" +
                "                  \"Value\": [\n" +
                "                    \"1.2.840.10008.5.1.4.1.1.7\"\n" +
                "                  ]\n" +
                "                },\n" +
                "                \"00081155\": {\n" +
                "                  \"vr\": \"UI\",\n" +
                "                  \"Value\": [\n" +
                "                    \"1.2.3.4.5.6.7.8\"\n" +
                "                  ]\n" +
                "                }\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          \"00281050\": {\n" +
                "            \"vr\": \"DS\",\n" +
                "            \"Value\": [\n" +
                "              127.0\n" +
                "            ]\n" +
                "          },\n" +
                "          \"00281051\": {\n" +
                "            \"vr\": \"DS\",\n" +
                "            \"Value\": [\n" +
                "              255.0\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o1 = reader.readDataset(null);
        final Sequence sq1 = o1.getSequence(0x00283110);
        Assertions.assertEquals(1, sq1.size());
        final Attributes o2 = sq1.get(0);
        Assertions.assertEquals(3, o2.size());
        final Sequence sq2 = o2.getSequence(0x0081140);
        Assertions.assertEquals(1, sq2.size());
        final Attributes o3 = sq2.get(0);
        Assertions.assertEquals(2, o3.size());
        Assertions.assertEquals("1.2.3.4.5.6.7.8", o3.getString(0x00081155));
    }

    @Test
    public void twoElementsOnePN() {
        final String source = "{    \"00321032\": {\n" +
                "      \"vr\": \"PN\",\n" +
                "      \"Value\": [\n" +
                "        {\n" +
                "          \"Alphabetic\": \"NAME^^\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"00700041\": {\n" +
                "      \"vr\": \"CS\",\n" +
                "      \"Value\": [\n" +
                "        \"N\"\n" +
                "      ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(2, o.size());
        Assertions.assertEquals("NAME^^", o.getString(0x00321032));
        Assertions.assertEquals("N", o.getString(0x00700041));
    }

    @Test
    public void sqWithValueConversion() {
        final String source = "{\"00311101\": {\n" +
                "            \"vr\": \"SQ\",\n" +
                "            \"Value\": [\n" +
                "              {\n" +
                "                \"00311104\": {\n" +
                "                  \"vr\": \"SL\",\n" +
                "                  \"InlineBinary\": \"AQAAAAIAAAADAAAABAAAAA==\"" +
                "                }\n" +
                "              }\n" +
                "            ]\n" +
                "          }," +
                "    \"00700041\": {\n" +
                "      \"vr\": \"CS\",\n" +
                "      \"Value\": [\n" +
                "        \"N\"\n" +
                "      ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(2, o.size());
        final Sequence sq = o.getSequence(0x00311101);
        Assertions.assertArrayEquals(new int[]{1, 2, 3, 4}, sq.get(0).getInts(0x00311104));
        Assertions.assertEquals("N", o.getString(0x00700041));
    }

    /**
     * If there's a Transfer Syntax UID element (this will be first or nearly so), subsequent elements
     * should be decoded using that transfer syntax instead of default Explicit VR Little Endian.
     */
    @Test
    public void explicitVRBigEndian() {
        final String source = "{ \"00020010\": {\n" +
                " \"vr\": \"UI\",\n" +
                " \"Value\": [ \"1.2.840.10008.1.2.2\" ]}," +   // Explicit VR Big Endian
                " \"00070011\": {\n" +
                " \"vr\": \"SL\",\n" +
                " \"InlineBinary\": \"AAAAAQAAAAIAAAADAAAABA==\" }}";  // cf. LE encoding of same values in explicitVRLittleEndian()
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());  // dcm4che3 (correctly) doesn't put Transfer Syntax in the dataset
        Assertions.assertArrayEquals(new int[]{1, 2, 3, 4}, o.getInts(0x00070011));
    }

    @Test
    public void explicitVRLittleEndian() {
        final String source = "{ \"00020010\": {\n" +
                " \"vr\": \"UI\",\n" +
                " \"Value\": [ \"1.2.840.10008.1.2.1\" ]}," +   // Explicit VR Little Endian (i.e., no behavior change)
                " \"00070011\": {\n" +
                " \"vr\": \"SL\",\n" +
                " \"InlineBinary\": \"AQAAAAIAAAADAAAABAAAAA==\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());  // dcm4che3 (correctly) doesn't put Transfer Syntax in the dataset
        Assertions.assertArrayEquals(new int[]{1, 2, 3, 4}, o.getInts(0x00070011));
    }

    @Test
    public void vrATInlineBinary() {
        final String source = "{\"00280009\": {\n" +
                "      \"vr\": \"AT\",\n" +
                "      \"InlineBinary\": \"EABUACAAVAA=\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals("00540010", o.getString(0x00280009, 0));
        Assertions.assertEquals("00540020", o.getString(0x00280009, 1));
    }

    @Test
    public void vrATValue() {
        final String source = "{\"00280009\": {\n" +
                "      \"vr\": \"AT\",\n" +
                "      \"Value\": [ \"00540010\" ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals("00540010", o.getString(0x00280009));
    }

    @Test
    public void vrFLInlineBinary() {
        final String source = "{\"00181320\": {\n" +
                "      \"vr\": \"FL\",\n" +
                "      \"InlineBinary\": \"w/XIQA==\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals(6.28f, o.getFloat(0x00181320, 0), 0.001);
    }

    @Test
    public void vrFLValue() {
        final String source = "{\"00181320\": {\n" +
                "      \"vr\": \"FL\",\n" +
                "      \"Value\": [ 3.14 ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals(3.14f, o.getFloat(0x00181320, 0), 0.001);
    }

    @Test
    public void vrFDInlineBinary() {
        final String source = "{\"00189079\": {\n" +
                "      \"vr\": \"FD\",\n" +
                "      \"InlineBinary\": \"jZduEoPA8z8OLbKd7ycbQA==\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals(1.2345, o.getDouble(0x00189079, 0, 0), 0.001);
        Assertions.assertEquals(6.7890, o.getDouble(0x00189079, 1, 0), 0.001);
    }

    @Test
    public void vrFDValue() {
        final String source = "{\"00189079\": {\n" +
                "      \"vr\": \"FD\",\n" +
                "      \"Value\": [ 0.123, 4.567 ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals(0.123, o.getDouble(0x00189079, 0, 0), 0.001);
        Assertions.assertEquals(4.567, o.getDouble(0x00189079, 1, 0), 0.001);
    }

    /**
     * The dcm4che3 JSON parser does not appear to respect unsigned values;
     * even in you ask for the next width up (long for unsigned int) the
     * returned long value for 0xffffffff is -1.
     * Testing here with Integer.MAX_VALUE.
     * Better test data for when that problem is fixed: "AQAAAP////8=" -> [ 1, 4294967295 ]
     */
    @Test
     public void vrULInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"UL\",\n" +
                "        \"InlineBinary\": \"AQAAAP///38=\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new long[]{1, 2147483647}, o.getLongs(0x00700052));
    }

    /**
     * The dcm4che3 JSON parser does not appear to respect unsigned values;
     * even in you ask for the next width up (long for unsigned int) the
     * returned long value for 0xffffffff is -1.
     * Testing here with Integer.MAX_VALUE.
     * Better test data for when that problem is fixed: [ 2, 4294967295 ]
     */
    @Test
    public void vrULValue() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"UL\",\n" +
                "        \"Value\": [ 2, 2147483647 ] }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new long[]{2, 2147483647}, o.getLongs(0x00700052));
    }

    @Test
    public void vrSSInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"SS\",\n" +
                "        \"InlineBinary\": \"BgAHAAgA\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new int[]{6, 7, 8}, o.getInts(0x00700052));
    }

    /**
     * See the note above about bugs in unsigned handling.
     * Better test case would be "BgAHAP//" -> [6, 7, 65535]
     */
    @Test
    public void vrUSInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"US\",\n" +
                "        \"InlineBinary\": \"BgAHAAgA\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new int[]{6, 7, 8}, o.getInts(0x00700052));
    }

    @Test
    public void vrSVInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"SV\",\n" +
                "        \"InlineBinary\": \"AwAAAAAAAAAAXtCyAAAAAA==\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new long[]{3, 3000000000L}, o.getLongs(0x00700052));
    }

    /**
     * See the note above about bugs in unsigned handling.
     * Better test case would be "BAAAAAAAAAAAAAAAAAAAgA==" -> [4, 9223372036854775807]
     */
    @Test
    public void vrUVInlineBinary() {
        final String source = "{\"00700052\": {\n" +
                "        \"vr\": \"UV\",\n" +
                "        \"InlineBinary\": \"AwAAAAAAAAAAXtCyAAAAAA==\" }}";
        final StringReader sourceReader = new StringReader(source);
        final JsonParser parser = Json.createParser(sourceReader);
        final JsonParser filtered = new DicomCorrectingJsonParser(parser);
        final JSONReader reader = new JSONReader(filtered);
        final Attributes o = reader.readDataset(null);
        Assertions.assertEquals(1, o.size());
        Assertions.assertTrue(o.contains(0x00700052));
        Assertions.assertArrayEquals(new long[]{3, 3000000000L}, o.getLongs(0x00700052));
    }


    /**
     * This method is for investigating file samples during development.
     * Feel free to modify to your needs but remove Test annotation before committing.
     */
    @Test
    @Disabled
    public void tryFiles() throws IOException {
        final String[] paths = new String[]{
                "working/json/sample-1.json", "working/json/sample-2.json"
        };

        for (final String path : paths) {
            System.out.println("loading sample DICOM+JSON " + path);
            try (final FileInputStream f = new FileInputStream(path);
                final JsonParser parser = Json.createParser(f);
                final JsonParser filtered = new DicomCorrectingJsonParser(parser)) {
                final JSONReader reader = new JSONReader(filtered);
                final Attributes o = reader.readDataset(null);
                System.out.println(Arrays.toString(o.getStrings(0x00280009)));
            }
        }
    }
}