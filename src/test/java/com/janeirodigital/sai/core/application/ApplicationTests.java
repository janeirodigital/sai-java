package com.janeirodigital.sai.core.application;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.authentication.AuthorizedSessionAccessor;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.httputils.SaiHttpException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationTests {

    private final String PROJECTRON_ID = "https://projectron.example/id";
    private static AuthorizedSessionAccessor sessionAccessor;

    @BeforeAll
    static void beforeAll() {
        sessionAccessor = mock(AuthorizedSessionAccessor.class);
    }

    @Test
    @DisplayName("Initialize an Application")
    void initializeApplication() throws SaiException, SaiHttpException {
        Application app = new Application(URI.create(PROJECTRON_ID), false, true, false, sessionAccessor);
        assertEquals(URI.create(PROJECTRON_ID), app.getId());
        assertNotNull(app.getClientFactory());
        assertFalse(app.isValidateSsl());
        assertTrue((app.isValidateShapeTrees()));
    }

    @Test
    @DisplayName("Initialize an Application Session")
    void initializeApplicationSession() throws SaiException, SaiHttpException {
        Application app = new Application(URI.create(PROJECTRON_ID), false, true, false, sessionAccessor);
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        ApplicationSession applicationSession = ApplicationSessionFactory.get(app, mockSession);
        assertNotNull(applicationSession);
        assertNotNull(applicationSession.getSaiSession());
        assertEquals(app, applicationSession.getApplication());
        assertEquals(mockSession, applicationSession.getAuthorizedSession());
        assertEquals(app.getClientFactory(), applicationSession.getClientFactory());
    }

}
