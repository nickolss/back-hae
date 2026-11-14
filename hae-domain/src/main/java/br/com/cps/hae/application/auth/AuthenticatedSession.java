package br.com.cps.hae.application.auth;

import br.com.cps.hae.application.account.EmployeeProfile;
import br.com.cps.hae.application.port.SessionTokenIssuer.SessionToken;

public record AuthenticatedSession(EmployeeProfile profile, SessionToken token) {
}
