package uws.service.actions;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uws.UWSException;
import uws.job.serializer.UWSSerializer;
import uws.job.user.JobOwner;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.log.UWSLog.LogLevel;

/**
 * <p>The "Show UWS Home Page" action of a UWS.</p>
 * 
 * <p><i><u>Note:</u> The corresponding name is {@link UWSAction#HOME_PAGE}.</i></p>
 * 
 * <p>This action displays the UWS home page.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 4.1 (04/2015)
 */
public class ShowHomePage extends UWSAction {
	private static final long serialVersionUID = 1L;

	public ShowHomePage(UWSService u){
		super(u);
	}

	/**
	 * @see UWSAction#HOME_PAGE
	 * @see uws.service.actions.UWSAction#getName()
	 */
	@Override
	public String getName(){
		return HOME_PAGE;
	}

	@Override
	public String getDescription(){
		return "Shows the UWS home page. (URL: {baseUWS_URL}, Method: HTTP-GET, No parameter)";
	}

	/**
	 * Checks whether there is no jobs list name.
	 * 
	 * @see uws.service.actions.UWSAction#match(uws.service.UWSUrl, uws.job.user.JobOwner, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean match(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request) throws UWSException{
		return !urlInterpreter.hasJobList();
	}

	/**
	 * <p>Writes the UWS home page in the given HttpServletResponse. 3 cases are possible:</p>
	 * <ul>
	 * 	<li><b>Default home page ({@link UWSService#isDefaultHomePage()} returns <i>true</i>):</b>
	 * 			write the appropriate (considering the Accept header of the HTTP-Request) serialization of this UWS.</li>
	 * 	<li><b>Home redirection ({@link UWSService#isHomePageRedirection()} = <i>true</i>):</b> call {@link UWSService#redirect(String, HttpServletRequest, HttpServletResponse)} with the {@link UWSService#getHomePage()} URL.</li>
	 * 	<li><b>Otherwise (({@link UWSService#isHomePageRedirection()} = <i>false</i>)):</b> read the content of the resource at the {@link UWSService#getHomePage()} URL and copy it in the given {@link HttpServletResponse}.</li>
	 * </ul>
	 * 
	 * @throws UWSException	If there is an error, mainly during the redirection.
	 * 
	 * @throws IOException	If there is an error while reading at a custom home page URL
	 * 						or while writing in the given HttpServletResponse.
	 * 
	 * @see uws.service.actions.UWSAction#apply(uws.service.UWSUrl, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * @see UWSService#redirect(String, HttpServletRequest, JobOwner, String, HttpServletResponse)
	 */
	@Override
	public boolean apply(UWSUrl urlInterpreter, JobOwner user, HttpServletRequest request, HttpServletResponse response) throws UWSException, IOException{

		if (uws.isDefaultHomePage()){
			UWSSerializer serializer = uws.getSerializer(request.getHeader("Accept"));
			response.setContentType(serializer.getMimeType());
			// Get a short and simple serialization of this UWS:
			String serialization;
			try{
				serialization = serializer.getUWS(uws);
			}catch(Exception e){
				if (!(e instanceof UWSException)){
					getLogger().logUWS(LogLevel.ERROR, urlInterpreter, "SERIALIZE", "Can't display the default home page, due to a serialization error!", e);
					throw new UWSException(UWSException.NO_CONTENT, e, "No home page available for this UWS service!");
				}else
					throw (UWSException)e;
			}
			// Write the simple UWS serialization in the given response:
			if (serialization != null){
				PrintWriter output = response.getWriter();
				output.print(serialization);
				output.flush();
			}else
				throw new UWSException(UWSException.NO_CONTENT, "No home page available for this UWS service.");

		}else{
			if (uws.isHomePageRedirection())
				uws.redirect(uws.getHomePage(), request, user, getName(), response);
			else{
				URL homePageUrl = new URL(uws.getHomePage());
				BufferedReader reader = new BufferedReader(new InputStreamReader(homePageUrl.openStream()));

				response.setContentType("text/html");
				PrintWriter writer = response.getWriter();
				try{
					String line = null;

					while((line = reader.readLine()) != null){
						writer.println(line);
						writer.flush();
					}

				}finally{
					writer.close();
					reader.close();
				}
			}
		}

		return true;
	}

}
