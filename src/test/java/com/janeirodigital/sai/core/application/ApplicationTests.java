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
import static org.mockito.Mockito.*;

class ApplicationTests {

    private final String PROJECTRON_ID = "https://projectron.example/id";
    private static URI oidcProviderId;
    private static URI socialAgentId;
    private static URI applicationId;
    private static URI otherApplicationId;

    private static AuthorizedSessionAccessor sessionAccessor;

    @BeforeAll
    static void beforeAll() {
        sessionAccessor = mock(AuthorizedSessionAccessor.class);
        oidcProviderId = URI.create("https://op.example/");
        socialAgentId = URI.create("https://acme.example/id#org");
        applicationId = URI.create("https://projectron.example/id#app");
        otherApplicationId = URI.create("https://othertron.example/id#app");
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

    @Test
    @DisplayName("Initialize an Application Session Accessor")
    void initializeApplicationSessionAccessor() throws SaiException {
        Application app = new Application(URI.create(PROJECTRON_ID), false, true, false, sessionAccessor);
        ApplicationSessionAccessor applicationSessions = new BasicApplicationSessionAccessor();
        AuthorizedSession mockSession = mock(AuthorizedSession.class, CALLS_REAL_METHODS);
        when(mockSession.getSocialAgentId()).thenReturn(socialAgentId);
        when(mockSession.getApplicationId()).thenReturn(applicationId);
        when(mockSession.getOidcProviderId()).thenReturn(oidcProviderId);
        ApplicationSession applicationSession = ApplicationSessionFactory.get(app, mockSession);
        applicationSessions.store(applicationSession);
        applicationSessions.get(applicationSession);
        assertEquals(applicationSession, applicationSessions.get(applicationSession));
        assertEquals(applicationSession, applicationSessions.get(mockSession));
    }

}
