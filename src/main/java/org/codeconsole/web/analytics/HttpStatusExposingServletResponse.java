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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HttpStatusExposingServletResponse extends HttpServletResponseWrapper {

    private int status = HttpServletResponse.SC_OK;
    
    public HttpStatusExposingServletResponse(HttpServletResponse response) {
        super(response);
    }
    
    @Override 
    public void sendError(int sc) throws IOException { 
        status = sc; 
        super.sendError(sc); 
    } 
 
    @Override 
    public void sendError(int sc, String msg) throws IOException { 
        status = sc; 
        super.sendError(sc, msg); 
    } 
 
 
    @Override 
    public void setStatus(int sc) { 
        status = sc; 
        super.setStatus(sc); 
    } 
 
    public int getStatus() { 
        return status; 
    } 
}