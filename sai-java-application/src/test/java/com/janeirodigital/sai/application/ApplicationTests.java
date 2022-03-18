package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.authentication.AuthorizedSessionAccessor;
import com.janeirodigital.sai.core.exceptions.SaiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationTests {

    private final String PROJECTRON_ID = "https://projectron.example/id";
    private final String SOCIAL_AGENT_ID = "https://alice.example/id#me";
    private static AuthorizedSessionAccessor sessionAccessor;

    @BeforeAll
    static void beforeAll() {
        sessionAccessor = mock(AuthorizedSessionAccessor.class);
    }

    @Test
    @DisplayName("Initialize an Application")
    void initializeApplication() throws SaiException {
        Application app = new Application(stringToUrl(PROJECTRON_ID), false, true, false, sessionAccessor);
        assertEquals(stringToUrl(PROJECTRON_ID), app.getId());
        assertNotNull(app.getClientFactory());
        assertFalse(app.isValidateSsl());
        assertTrue((app.isValidateShapeTrees()));
    }

    @Test
    @DisplayName("Initialize an Application Session")
    void initializeApplicationSession() throws SaiException {
        Application app = new Application(stringToUrl(PROJECTRON_ID), false, true, false, sessionAccessor);
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        ApplicationSession applicationSession = ApplicationSessionFactory.get(app, mockSession);
        assertNotNull(applicationSession);
        assertNotNull(applicationSession.getSaiSession());
        assertEquals(app, applicationSession.getApplication());
        assertEquals(mockSession, applicationSession.getAuthorizedSession());
        assertEquals(app.getClientFactory(), applicationSession.getClientFactory());
    }

}
