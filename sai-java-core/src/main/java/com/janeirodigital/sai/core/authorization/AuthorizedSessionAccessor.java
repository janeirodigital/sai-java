package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;

// TODO - AUTH-REFACTOR - Documentation
public interface AuthorizedSessionAccessor {

    AuthorizedSession get(AccessToken accessToken);

    AuthorizedSession get(AuthorizedSession session) throws SaiException;

    AuthorizedSession refresh(AuthorizedSession session) throws SaiException;

}
