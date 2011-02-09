package org.codeconsole.web.analytics.integration;

import javax.servlet.http.HttpServletRequest;

public interface SourceRevisionResolver {
	public String getRevision(HttpServletRequest request);
}
