package org.codeconsole.web.analytics.integration;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

public interface UserDetailsResolver {
	Serializable getUserDetails(HttpServletRequest request);
}
