package com.janeirodigital.sai.core.enums;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumerated list of applicable HTTP Content-Type values
 */
public enum ContentType {
    TEXT_TURTLE("text/turtle"),
    RDF_XML("application/rdf+xml"),
    N_TRIPLES("application/n-triples"),
    LD_JSON("application/ld+json"),
    TEXT_HTML("text/html"),
    OCTET_STREAM("application/octet-stream"),
    TEXT_PLAIN("text/plain"),
    SPARQL_UPDATE("application/sparql-update"),
    AAC("audio/aac"),
    ABIWORD("application/x-abiword"),
    FREEARC("application/x-freearc"),
    MSVIDEO("video/x-msvideo"),
    AZW("application/vnd.amazon.ebook"),
    BMP("image/bmp"),
    BZIP("application/x-bzip"),
    BZIP2("application/x-bzip2"),
    CDF("application/x-cdf"),
    CSH("application/x-csh"),
    CSS("text/css"),
    CSV("text/csv"),
    MS_WORD("application/msword"),
    OOXML_DOCUMENT("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    MS_FONTOBJECT("application/vnd.ms-fontobject"),
    EPUD("application/epub+zip"),
    GZIP("application/gzip"),
    GIF("image/gif"),
    MS_ICON("image/vnd.microsoft.icon"),
    CALENDAR("text/calendar"),
    JAVA_ARCHIVE("application/java-archive"),
    JPEG("image/jpeg"),
    JAVASCRIPT("text/javascript"),
    JSON("application/json"),
    MIDI("audio/midi"),
    X_MIDI("audio/x-midi"),
    MPEG_AUDIO("audio/mpeg"),
    MPEG4("video/mp4"),
    MPEG_VIDEO("video/mpeg"),
    APPLE_INSTALLER("application/vnd.apple.installer+xml"),
    OPENDOCUMENT_PRESENTATION("application/vnd.oasis.opendocument.presentation"),
    OPENDOCUMENT_SHEET("application/vnd.oasis.opendocument.spreadsheet"),
    OPENDOCUMENT_TEXT("application/vnd.oasis.opendocument.text"),
    OGG_AUDIO("audio/ogg"),
    OGG_VIDEO("video/ogg"),
    OGG("application/ogg"),
    OPUS_AUDIO("audio/opus"),
    OTF("font/otf"),
    PNG("image/png"),
    PDF("application/pdf"),
    HTTPD_PHP("application/x-httpd-php"),
    MS_POWERPOINT("application/vnd.ms-powerpoint"),
    OOXML_PRESENTATION("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    RAR("application/vnd.rar"),
    RTF("application/rtf"),
    SH("application/x-sh"),
    SVG_XML("image/svg+xml"),
    FLASH("application/x-shockwave-flash"),
    TAR("application/x-tar"),
    TIFF("image/tiff"),
    MP2T("video/mp2t"),
    TTF("font/ttf"),
    VISIO("application/vnd.visio"),
    WAV("audio/wav"),
    WEBM_AUDIO("audio/webm"),
    WEBM_VIDEO("video/webm"),
    WEBP("image/webp"),
    WOFF("font/woff"),
    WOFF2("font/woff2"),
    XHTML_XML("application/xhtml+xml"),
    MS_EXCEL("application/vnd.ms-excel"),
    OOXML_SHEET("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    APP_XML("application/xml"),
    XML("text/xml"),
    ATOM_XML("application/atom+xml"),
    XUL_XML("application/vnd.mozilla.xul+xml"),
    ZIP("application/zip"),
    VIDEO_3GPP("video/3gpp"),
    AUDIO_3GPP("audio/3gpp"),
    VIDEO_3GPP2("video/3gpp2"),
    AUDIO_3GPP2("audio/3gpp2"),
    X7Z("application/x-7z-compressed");

    public String getValue() {
        return this.value;
    }

    private final String value;
    
    private static final Map<String,ContentType> ENUM_MAP;
    
    ContentType(String value) {
        this.value = value;
    }

    static {
        Map<String,ContentType> map = new ConcurrentHashMap<>();
        for (ContentType instance : ContentType.values()) {
            map.put(instance.getValue().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static ContentType get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
    
}
