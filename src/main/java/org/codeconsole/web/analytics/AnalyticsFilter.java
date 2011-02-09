/* Copyright 2010 Scott Murphy
 *
 * Licensed under the GPLv3 License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     GPLv3: http://www.gnu.org/copyleft/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Scott Murphy
 */

package org.codeconsole.web.analytics;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codeconsole.web.analytics.integration.AnalyticsGateway;
import org.codeconsole.web.analytics.integration.SourceRevisionResolver;
import org.codeconsole.web.analytics.integration.UserDetailsResolver;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet Filter implementation class AnalyticsFilter
 */
public class AnalyticsFilter implements Filter {

    public static final String DEFAULT_ANALTICS_URL = "/analytics";
    public static final String DEFAULT_SPRING_CONFIGURATION = "META-INF/spring/analytics.xml";
	
	private String analyticsUrl = DEFAULT_ANALTICS_URL;
	private String springConfiguration = DEFAULT_SPRING_CONFIGURATION;
	
	private String sessionAttributeName = "analyticsSession";
	private int maxHistorySize = 50;
	private AnalyticsGateway analyticsGateway;
	private List<Pattern> excludedUrlPatterns = new ArrayList<Pattern>(); 
	private List<Pattern> excludedParamPatterns = new ArrayList<Pattern>(); 
	
	private SourceRevisionResolver sourceRevisionResolver;
	private UserDetailsResolver userDetailsResolver;
	
	/**
	 * Default constructor. 
	 */
	public AnalyticsFilter() { }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() { }

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)  throws IOException, ServletException{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpStatusExposingServletResponse httpResponse = new HttpStatusExposingServletResponse((HttpServletResponse) response);

		AnalyticsSession analyticsSession = (AnalyticsSession) httpRequest.getSession().getAttribute(sessionAttributeName);
		if (analyticsSession == null) {
			analyticsSession = new AnalyticsSession(maxHistorySize, httpRequest.getHeader("referer"), getIp(httpRequest));
			httpRequest.getSession().setAttribute(sessionAttributeName, analyticsSession); 
		}	    

		String compareUrl = getComparisonUrl((HttpServletRequest) request);
		if (compareUrl.endsWith(analyticsUrl)) {
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			out.println("<html><head><title>Analytics Report</title></head><body>");
			if (ServletRequestUtils.getBooleanParameter(request, "send", false)) {
				if (analyticsGateway != null) {
					analyticsGateway.sendAnalytics(analyticsSession);
				} else {
					out.println("<div>Warning: Can't send report.  No Spring application context specified/configured.</div>");
				}
				out.println("<div>Message: Report Sent!</div>");
			}
			if (ServletRequestUtils.getBooleanParameter(request, "clear", false)) {
				analyticsSession.clear();
				httpRequest.getSession().setAttribute(sessionAttributeName, analyticsSession);
				out.println("<div>Message: History Cleared!</div>");
			}			
			out.print("<div><a href=\"?\">Refresh</a> &nbsp; <a href=\"?clear=true\">Clear</a> &nbsp; <a href=\"?send=true\">Send</a> &nbsp; <a href=\"?send=true&clear=true\">Send and Clear</a></div>");
			out.print(analyticsSession.toHtmlString());
			out.println("</body></html>");
			out.close();			
		} else {
			String sourceRevision = sourceRevisionResolver == null? null : sourceRevisionResolver.getRevision(httpRequest);
			
			Map<String, String[]> filteredParamMap = request.getParameterMap();
			if (!excludedUrlPatterns.isEmpty()) {
				filteredParamMap = new HashMap<String, String[]>();
				for (Iterator<Map.Entry<String, String[]>> it = request.getParameterMap().entrySet().iterator(); it.hasNext();) {
					Entry<String, String[]> next = it.next();
					for (Pattern exclude : excludedUrlPatterns) {
						if (exclude.matcher(compareUrl).matches()) {
							filteredParamMap.put(next.getKey(), new String[] { "**FILTERED**" });
						} else {
							filteredParamMap.put(next.getKey(), next.getValue());
						}
					}				
				}
			}
				
			@SuppressWarnings("unchecked")
			AnalyticsHttpRequest rq = new AnalyticsHttpRequest(httpRequest.getMethod(), httpRequest.getRequestURL().toString(), httpRequest.getQueryString(), filteredParamMap, sourceRevision);
			try {
				chain.doFilter(request, httpResponse);	        
			} catch (IOException e) {
				rq.setException(e);
				throw e;
			} catch (ServletException e) {
				rq.setException(e);
				throw e;
			} catch (RuntimeException e) {
				rq.setException(e);
				throw e;
			} finally {
				rq.setCompletionTime(System.currentTimeMillis());
				rq.setStatus(httpResponse.getStatus());

				boolean ignore = false;
				if (rq.getStatus() == 200 || rq.getStatus() == 304) {
					for (Pattern exclude : excludedUrlPatterns) {
						if (exclude.matcher(compareUrl).matches()) {
							ignore = true;
							break;
						}
					}
				}
				if (!ignore) {
					analyticsSession.appendHistory(rq);
				}
				
				Serializable userDetails = userDetailsResolver == null? null : 
					userDetailsResolver.getUserDetails(httpRequest);
				if (userDetails != null && 
						(analyticsSession.getUserDetails() == null ||
						!analyticsSession.getUserDetails().equals(userDetails))) {
					analyticsSession.setUserDetails(userDetails);
				}
				
				// in case of clustered sessions, always update the session object to propagate the update.
				if (!httpResponse.isCommitted()) {
					httpRequest.getSession().setAttribute(sessionAttributeName, analyticsSession);
				}
				if (rq.getWrappedException() != null && analyticsGateway != null) {
					analyticsGateway.sendAnalytics(analyticsSession);
				}
			}
		}
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		String springConfigurationParam = fConfig.getInitParameter("spring-context-location");
		if (springConfigurationParam != null) {
			this.springConfiguration = springConfigurationParam;
		}		
		
		ApplicationContext context = null;
		try {
			context = new ClassPathXmlApplicationContext(springConfiguration);
		} catch (BeanDefinitionStoreException notfound) {
			System.out.println("AnalyticsFilter: Could not locate context configuration: "+springConfiguration + ". Attempting to load root Web Application Context.");
			context = WebApplicationContextUtils.getWebApplicationContext(fConfig.getServletContext());
		}
		System.out.println("AnalyticsFilter: Loading bean definitions.");
		if (context != null) {
			if (context.containsBean("analyticsGateway")) {
				analyticsGateway = context.getBean("analyticsGateway", AnalyticsGateway.class);
				System.out.println("AnalyticsFilter: Gateway loaded.");
			} else {
				System.out.println("AnalyticsFilter: Could not a bean named 'analyticsGateway'. Analytics will not be sent!");
			}
			
			if (context.containsBean("userDetailsResolver")) {
				userDetailsResolver = context.getBean("userDetailsResolver", UserDetailsResolver.class); 
				System.out.println("AnalyticsFilter: UserDetailsResolver loaded.");
			}

			if (context.containsBean("sourceRevisionResolver")) {
				sourceRevisionResolver = context.getBean("sourceRevisionResolver", SourceRevisionResolver.class); 
				System.out.println("AnalyticsFilter: SourceRevisionResolver loaded.");
			}
		} else {
			System.out.println("AnalyticsFilter: Could not load root Web Application Context.");
		}
		
		String historySize = fConfig.getInitParameter("history-size");
		if (historySize != null) {
			this.maxHistorySize = Integer.parseInt(historySize);
		}
		String sessionAttribute = fConfig.getInitParameter("session-attribute");
		if (sessionAttribute != null) {
			this.sessionAttributeName = sessionAttribute;
		}
		String excluded = fConfig.getInitParameter("exclude-urls");
		if (excluded != null) {
			String[] excludes = excluded.split("[\r\n]+");
			for (String exclude : excludes) {
				exclude = exclude.trim();
				if (!exclude.isEmpty())
					excludedUrlPatterns.add(Pattern.compile(exclude));
			}
		}
		
		String excludedParms = fConfig.getInitParameter("exclude-params");
		if (excludedParms != null) {
			String[] paramExcludes = excludedParms.split("[\r\n]+");
			for (String exclude : paramExcludes) {
				exclude = exclude.trim();
				if (!exclude.isEmpty())
					excludedParamPatterns.add(Pattern.compile(exclude));
			}
		}			
	}

	private String getIp(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null)
			return request.getRemoteAddr();
		if (ip.indexOf(',') != -1)
			ip = ip.substring(ip.lastIndexOf(',')+1).trim();
		return ip;
	}
	
    private String getComparisonUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int pathParamIndex = uri.indexOf(';');

        if (pathParamIndex > 0) {
            // strip everything after the first semi-colon
            uri = uri.substring(0, pathParamIndex);
        }

        return uri;
    }	
}
