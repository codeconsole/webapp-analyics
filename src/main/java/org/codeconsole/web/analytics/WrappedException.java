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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;

public class WrappedException implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final Exception exception;
    private final String fileName;
    private final String methodName;
    private final Integer lineNumber;
    private final String stackTrace;
    private final Date timeStamp;
    
	public WrappedException(Exception exception) {
		this.exception = exception;
		this.timeStamp = new Date();
		StackTraceElement[] stack = exception.getStackTrace();
		if (stack.length > 0) {
		    this.fileName = stack[0].getFileName();
		    this.methodName = stack[0].getMethodName();
		    this.lineNumber = stack[0].getLineNumber();
		} else {
		    this.fileName = null;
		    this.methodName = null;
		    this.lineNumber = null;			
		}
		// writers
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		// update stack
		exception.printStackTrace(pw);
		this.stackTrace = sw.toString();

		// close writers
		pw.close();
		try {
		    sw.close();
		} catch (IOException ex) { }
	}

	public Exception getException() {
		return exception;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMethodName() {
		return methodName;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}
}
