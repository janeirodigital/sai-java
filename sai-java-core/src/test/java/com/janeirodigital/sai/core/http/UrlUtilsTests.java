package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static com.janeirodigital.sai.core.http.UrlUtils.stringToUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlUtilsTests {
    @Test
    @DisplayName("Convert string to URL")
    void convertStringToUrl() throws MalformedURLException, SaiException {
        URL expected = new URL("http://www.solidproject.org");
        assertEquals(expected, stringToUrl("http://www.solidproject.org"));
    }

    @Test
    @DisplayName("Fail to convert string to URL - malformed URL")
    void failToConvertStringToUrl() {
        assertThrows(SaiException.class, () -> stringToUrl("ddd:\\--solidproject_orgZq=something&something=<something+else>"));
    }
}
