package org.codeconsole.web.analytics.integration;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSecurityUserDetailsResolver implements UserDetailsResolver {

	@Override
	public Serializable getUserDetails(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth == null? null : (Serializable) auth.getPrincipal();
	}

}
