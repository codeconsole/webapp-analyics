/* Copyright 2010 Scott Murphy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalyticsHttpRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Date requestTime = new Date();
	private long completionTime;
	private final String url;
	private final String method;
	private final String queryString;
	private int status = 0;
	private final Map<String, String[]> parameterMap;
	private WrappedException exception;
	private final String sourceRevision;
		
	public AnalyticsHttpRequest(String method, String url, String queryString, Map<String, String[]> parameterMap, String sourceRevision) {
		super();
		this.method = method;
		this.url = url;
		this.queryString = queryString;
		this.parameterMap = new HashMap<String, String[]>();
		this.parameterMap.putAll(parameterMap);
		if (queryString != null) {
			Pattern pattern = Pattern.compile("(\\w+)=");
			Matcher matcher = pattern.matcher(queryString);
			while (matcher.find()) {
				this.parameterMap.remove(matcher.group(1));
			}
		}
		this.sourceRevision = sourceRevision;
	}

	public Date getRequestTime() {
		return requestTime;
	}

	public String getMethod() {
		return method;
	}
	
	public String getUrl() {
		return url;
	}

	public String getUrlWithQueryString() {
		return url + (queryString != null? "?" + queryString : "");
	}
	
	public String getQueryString() {
		return queryString;
	}
	
	public Map<String, String[]> getParameterMap() {
		return parameterMap;
	}

	public void setCompletionTime(long completionTime) {
		this.completionTime = completionTime;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
	
	public WrappedException getWrappedException() {
		return exception;
	}

	public String getSourceRevision() {
		return sourceRevision;
	}

	public void setException(Exception exception) {
		this.exception = new WrappedException(exception);
	}

	public String toString() {
    	StringBuilder url = new StringBuilder(new SimpleDateFormat("MM-dd-yyyy h:mm:ss a").format(requestTime));
        url.append(" (");
        url.append(getMethod());
        url.append(") - ");
        url.append(getUrlWithQueryString());
        url.append(" ");
        for (Iterator<Map.Entry<String, String[]>> it = getParameterMap().entrySet().iterator(); it.hasNext();) {
        	url.append('{');
        	Map.Entry<String, String[]> entry = it.next();
        	url.append(entry.getKey());
        	url.append(":");
        	for (int i = 0; i < entry.getValue().length;) {
        		url.append(entry.getValue()[i]);
        		if (i++ < entry.getValue().length)
        			url.append(',');
        	}
        	url.deleteCharAt(url.length()-1);
        	url.append("}");
        }
        url.append(" (" + getStatus() + " - " + (completionTime-requestTime.getTime()) + " ms)");            
        return url.toString();		
	}	
	
	public String toHtmlString() {
		String s = toString();
		int start = s.indexOf(") -") + 4;
	   	StringBuilder url = new StringBuilder();
		url.append(s.substring(0, start));
	   	url.append("<a href=\"");
        url.append(getUrlWithQueryString());
        url.append("\">");
        url.append(getUrlWithQueryString());
        url.append("</a>");		
        url.append(s.substring(s.indexOf(" ", start)));
        return url.toString();
	}
}
