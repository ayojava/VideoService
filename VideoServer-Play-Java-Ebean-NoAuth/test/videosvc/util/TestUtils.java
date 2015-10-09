package videosvc.util;

import com.fasterxml.jackson.databind.JsonNode;
import videosvc.models.Video;
import play.Logger;
import play.libs.Json;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class TestUtils {

    private static final Logger.ALogger l = Logger.of(TestUtils.class);


    private static class HttpBodyStream {

        private ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        public HttpBodyStream writeBinary(byte[] data) {
            try {
                bodyStream.write(data);
                bodyStream.flush();
                return this;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public byte[] getBytes() {
            try {
                bodyStream.close();
                return bodyStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public static Long getInvalidVideoId() {

        return -111L;
    }

    public static String separator() {
        return UUID.randomUUID().toString();
    }

    public static Video parseVideoFromJson(String jsonString) {

        JsonNode json = Json.parse(jsonString);
        assertTrue(json.isObject());

        Video v = Json.fromJson(json, Video.class);
        l.debug(v.toString());
        return v;
    }

    public static byte[] multiPartRequestBody(String separator,
                                              String dataPartKey, String dataPartContentType, String charSet, String dataPartString,
                                              String filePartKey, String filePartContentType, String dataFilename) {

        byte[] dataPartBody = dataPartBody(dataPartString, charSet);

        String dataPartHeader = dataPartHeader(separator, dataPartKey, dataPartContentType, charSet, dataPartBody.length);

        byte[] filePartBody = filePartBody(dataFilename);

        String filePartHeader = filePartHeader(separator, filePartKey, filePartContentType, dataFilename, filePartBody.length);

        return new HttpBodyStream()
                .writeBinary(toByteArray(dataPartHeader, charSet))
                .writeBinary(dataPartBody)
                .writeBinary(toByteArray(filePartHeader, charSet))
                .writeBinary(filePartBody)
                .getBytes();
    }

    private static String LINEBREAK = "\r\n";

    private static byte[] dataPartBody(String content, String charSet) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(baos, charSet)) {
            writer.append(content);
            writer.close();
            // l.debug("dataPartBody():\n----->>\n" + new String(baos.toByteArray()) + "\n<<-----");
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String dataPartHeader(String separator, String key, String contentType, String charSet, long contentLength) {
        String header = new StringBuilder("--")
                .append(separator + LINEBREAK)
                .append("Content-Disposition: form-data; name=\"" + key + "\"" + LINEBREAK)
                .append("Content-Type: " + contentType + "; charset=" + charSet + LINEBREAK)
                .append("Content-Length: " + contentLength + LINEBREAK)
                .append("Content-Transfer-Encoding: binary" + LINEBREAK)
                .append(LINEBREAK)
                .toString();
        // l.debug("dataPartHeader():\n----->>\n" + header + "<<-----");
        return header;
    }

    private static byte[] filePartBody(String src) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Files.copy(new File(src).toPath(), out);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String filePartHeader(String separator, String key, String contentType, String filename, long contentLength) {
        String header = new StringBuilder(LINEBREAK + "--")
                .append(separator + LINEBREAK)
                .append("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filename + "\"" + LINEBREAK)
                .append("Content-Type: " + contentType + LINEBREAK)
                .append("Content-Length: " + contentLength + LINEBREAK)
                .append("Content-Transfer-Encoding: binary" + LINEBREAK)
                .append(LINEBREAK)
                .toString();
        // l.debug("filePartHeader():\n----->>\n" + header + "<<-----");
        return header;
    }

    private static byte[] toByteArray(String text, String charSet) {
        return text.getBytes(Charset.forName(charSet));
    }
}
