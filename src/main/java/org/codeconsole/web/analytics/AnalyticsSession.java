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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class AnalyticsSession implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final long creationTime = System.currentTimeMillis();
    private final String referer;
    private final String ip;
    private final int maxHistorySize; 
    
    private Serializable userDetails;

    private Queue<AnalyticsHttpRequest> history = new LinkedList<AnalyticsHttpRequest>();
    
    public AnalyticsSession(int maxHistorySize, String referer, String ip) {
        this.maxHistorySize = maxHistorySize;
        this.referer = referer;
        this.ip = ip;
    }
    
    private Map<String, Object> properties = new HashMap<String, Object>();
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    public String getReferer() {
        return referer;
    }
    public String getIp() {
        return ip;
    }
    public long getCreationTime() {
        return creationTime;
    }
    public Serializable getUserDetails() {
		return userDetails;
	}
	public void setUserDetails(Serializable userDetails) {
		this.userDetails = userDetails;
	}
	public void appendHistory(AnalyticsHttpRequest url) {
        history.add(url);
        if (history.size() > maxHistorySize)
            history.remove();
    }
	public Queue<AnalyticsHttpRequest> getHistory() {
		return history;
	}
	public AnalyticsHttpRequest getLastRequest() {
		AnalyticsHttpRequest last = null;
		for (AnalyticsHttpRequest request: history) {
			last = request;
		}
		return last;
	}
	public AnalyticsHttpRequest getLastException() {
		AnalyticsHttpRequest last = null;
		for (AnalyticsHttpRequest request: history) {
			if (request.getWrappedException() != null) {
				last = request;
			}
		}
		return last;
	}	
	public void clear() {
		history.clear();
	}
	public String toString() {
		StringBuilder report = new StringBuilder();
		report.append("Analytics Report\n");
		report.append("\nSession Start Time: " + new SimpleDateFormat("MM-dd-yyyy h:mm:ss a").format(new Date(this.getCreationTime())));
		report.append("\nTotal Online Time: " + new DecimalFormat("#.##").format((System.currentTimeMillis() - this.getCreationTime()) / (60.0 * 1000.0)) + " minutes");
		report.append("\nIP Address: " + this.getIp());
		report.append("\nReferer: " + this.getReferer());
		report.append("\nHistory");
		AnalyticsHttpRequest last = null;
		for (AnalyticsHttpRequest request : history) {
			report.append("\n");
			report.append(request.toString());
	        if (request.getWrappedException() != null) {
	        	report.append("\n"+request.getWrappedException().getException().toString());
	        }			
			last = request;
		}
		if (last != null && last.getWrappedException() != null) {
			report.append("\nLast Exception\n\n");
			report.append(last.getWrappedException().getStackTrace());
		}
		if (last != null && last.getSourceRevision() != null) {
			report.append("\n\nSource Revision\n");
			report.append(last.getSourceRevision());
		}			
		if (userDetails != null) {
			report.append("\n\nUser Information\n");
			report.append(userDetails.toString());
		}		
		return report.toString();		
	}
	public String getHtmlString() {
		return this.toHtmlString();
	}
	
	public String toHtmlString() {
		StringBuilder report = new StringBuilder();
		report.append("<html><body>");
		report.append("<h1>Analytics Report</h1>");
		report.append("<div>");
		report.append("Session Start Time: " + new SimpleDateFormat("MM-dd-yyyy h:mm:ss a").format(new Date(this.getCreationTime())));
		report.append("</div><div>");
		report.append("Total Online Time: " + new DecimalFormat("#.##").format((System.currentTimeMillis() - this.getCreationTime()) / (60.0 * 1000.0)) + " minutes");
		report.append("</div><div>");
		report.append("IP Address: " + this.getIp());
		report.append("</div><div>");
		report.append("Referer: " + this.getReferer());
		report.append("</div>");
		report.append("<h3>History</h3>");
		report.append("<ul>");
		AnalyticsHttpRequest last = null;
		for (AnalyticsHttpRequest request : history) {
			report.append("<li>");
			report.append(request.toHtmlString());
	        if (request.getWrappedException() != null) {
	        	report.append(" <strong>"+request.getWrappedException().getException().toString()+"</strong>");
	        }			
			report.append("</li>");
			if (request.getWrappedException() != null) {
				last = request;
			}
		}
		report.append("</ul>");

		if (last != null && last.getWrappedException() != null) {
			report.append("<h3>Last Exception</h3><pre>");
			report.append(last.getWrappedException().getStackTrace());
			report.append("</pre>");
		}
		
		if (last != null && last.getSourceRevision() != null) {
			report.append("<h3>Source Revision</h3><pre>");
			report.append(last.getSourceRevision());
			report.append("</pre>");
		}		
		
		if (userDetails != null) {
			report.append("<h3>User Information</h3><pre>");
			report.append(userDetails.toString());
			report.append("</pre>");
		}
		report.append("</body></html>");
		return report.toString();		
	}	
}
