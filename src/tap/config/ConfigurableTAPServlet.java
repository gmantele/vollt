package tap.config;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2015 - Astronomisches Rechen Institut (ARI)
 */

import static tap.config.TAPConfiguration.DEFAULT_TAP_CONF_FILE;
import static tap.config.TAPConfiguration.KEY_ADD_TAP_RESOURCES;
import static tap.config.TAPConfiguration.KEY_HOME_PAGE;
import static tap.config.TAPConfiguration.KEY_HOME_PAGE_MIME_TYPE;
import static tap.config.TAPConfiguration.TAP_CONF_PARAMETER;
import static tap.config.TAPConfiguration.getProperty;
import static tap.config.TAPConfiguration.isClassName;
import static tap.config.TAPConfiguration.newInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tap.ServiceConnection;
import tap.TAPException;
import tap.resource.HomePage;
import tap.resource.TAP;
import tap.resource.TAPResource;

/**
 * <p>HTTP servlet fully configured with a TAP configuration file.</p>
 * 
 * <p>
 * 	This configuration file may be specified in the initial parameter named {@link TAPConfiguration#TAP_CONF_PARAMETER}
 * 	of this servlet inside the WEB-INF/web.xml file. If none is specified, the file {@link TAPConfiguration#DEFAULT_TAP_CONF_FILE}
 * 	will be searched inside the directories of the classpath, and inside WEB-INF and META-INF.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (04/2015)
 * @since 2.0
 */
public class ConfigurableTAPServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** TAP object representing the TAP service. */
	private TAP tap = null;

	@Override
	public void init(final ServletConfig config) throws ServletException{
		// Nothing to do, if TAP is already initialized:
		if (tap != null)
			return;

		/* 1. GET THE FILE PATH OF THE TAP CONFIGURATION FILE */
		String tapConfPath = config.getInitParameter(TAP_CONF_PARAMETER);
		if (tapConfPath == null || tapConfPath.trim().length() == 0)
			tapConfPath = null;
		//throw new ServletException("Configuration file path missing! You must set a servlet init parameter whose the name is \"" + TAP_CONF_PARAMETER + "\".");

		/* 2. OPEN THE CONFIGURATION FILE */
		InputStream input = null;
		// CASE: No file specified => search in the classpath for a file having the default name "tap.properties".
		if (tapConfPath == null)
			input = searchFile(DEFAULT_TAP_CONF_FILE, config);
		else{
			File f = new File(tapConfPath);
			// CASE: The given path matches to an existing local file.
			if (f.exists()){
				try{
					input = new FileInputStream(f);
				}catch(IOException ioe){
					throw new ServletException("Impossible to read the TAP configuration file (" + tapConfPath + ")!", ioe);
				}
			}
			// CASE: The given path seems to be relative to the servlet root directory.
			else
				input = searchFile(tapConfPath, config);
		}
		// If no file has been found, cancel the servlet loading:
		if (input == null)
			throw new ServletException("Configuration file not found with the path: \"" + ((tapConfPath == null) ? DEFAULT_TAP_CONF_FILE : tapConfPath) + "\"! Please provide a correct file path in servlet init parameter (\"" + TAP_CONF_PARAMETER + "\") or put your configuration file named \"" + DEFAULT_TAP_CONF_FILE + "\" in a directory of the classpath or in WEB-INF or META-INF.");

		/* 3. PARSE IT INTO A PROPERTIES SET */
		Properties tapConf = new Properties();
		try{
			tapConf.load(input);
		}catch(IOException ioe){
			throw new ServletException("Impossible to read the TAP configuration file (" + tapConfPath + ")!", ioe);
		}finally{
			try{
				input.close();
			}catch(IOException ioe2){}
		}

		/* 4. CREATE THE TAP SERVICE */
		ServiceConnection serviceConn = null;
		try{
			// Create the service connection:
			serviceConn = new ConfigurableServiceConnection(tapConf, config.getServletContext().getRealPath(""));
			// Create all the TAP resources:
			tap = new TAP(serviceConn);
		}catch(Exception ex){
			tap = null;
			if (ex instanceof TAPException)
				throw new ServletException(ex.getMessage(), ex.getCause());
			else
				throw new ServletException("Impossible to initialize the TAP service!", ex);
		}

		/* 4Bis. SET THE HOME PAGE */
		String propValue = getProperty(tapConf, KEY_HOME_PAGE);
		if (propValue != null){
			// If it is a class path, replace the current home page by an instance of this class:
			if (isClassName(propValue)){
				try{
					tap.setHomePage(newInstance(propValue, KEY_HOME_PAGE, HomePage.class, new Class<?>[]{TAP.class}, new Object[]{tap}));
				}catch(TAPException te){
					throw new ServletException(te.getMessage(), te.getCause());
				}
			}
			// If it is a file URI (null, file inside WebContent, file://..., http://..., etc...):
			else{
				// ...set the given URI:
				tap.setHomePageURI(propValue);
				// ...and its MIME type (if any):
				propValue = getProperty(tapConf, KEY_HOME_PAGE_MIME_TYPE);
				if (propValue != null)
					tap.setHomePageMimeType(propValue);
			}
		}

		/* 5. SET ADDITIONAL TAP RESOURCES */
		propValue = getProperty(tapConf, KEY_ADD_TAP_RESOURCES);
		if (propValue != null){
			// split all list items:
			String[] lstResources = propValue.split(",");
			for(String addRes : lstResources){
				addRes = addRes.trim();
				// ignore empty items:
				if (addRes.length() > 0){
					try{
						// create an instance of the resource:
						TAPResource newRes = newInstance(addRes, KEY_ADD_TAP_RESOURCES, TAPResource.class, new Class<?>[]{TAP.class}, new Object[]{tap});
						if (newRes.getName() == null || newRes.getName().trim().length() == 0)
							throw new TAPException("TAP resource name missing for the new resource \"" + addRes + "\"! The function getName() of the new TAPResource must return a non-empty and not NULL name. See the property \"" + KEY_ADD_TAP_RESOURCES + "\".");
						// add it into TAP:
						tap.addResource(newRes);
					}catch(TAPException te){
						throw new ServletException(te.getMessage(), te.getCause());
					}
				}
			}
		}

		/* 6. DEFAULT SERVLET INITIALIZATION */
		super.init(config);

		/* 7. FINALLY MAKE THE SERVICE AVAILABLE */
		serviceConn.setAvailable(true, "TAP service available.");
	}

	/**
	 * Search the given file name/path in the directories of the classpath, then inside WEB-INF and finally inside META-INF.
	 * 
	 * @param filePath	A file name/path.
	 * @param config	Servlet configuration (containing also the context class loader - link with the servlet classpath).
	 * 
	 * @return	The input stream toward the specified file, or NULL if no file can be found.
	 * 
	 * @since 2.0
	 */
	protected final InputStream searchFile(String filePath, final ServletConfig config){
		InputStream input = null;

		// Try to search in the classpath (with just a file name or a relative path):
		input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);

		// If not found, try searching in WEB-INF and META-INF (as this fileName is a file path relative to one of these directories):
		if (input == null){
			if (filePath.startsWith("/"))
				filePath = filePath.substring(1);
			// ...try at the root of WEB-INF:
			input = config.getServletContext().getResourceAsStream("/WEB-INF/" + filePath);
			// ...and at the root of META-INF:
			if (input == null)
				input = config.getServletContext().getResourceAsStream("/META-INF/" + filePath);
		}

		return input;
	}

	@Override
	public void destroy(){
		// Free all resources used by TAP:
		if (tap != null){
			tap.destroy();
			tap = null;
		}
		super.destroy();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		if (tap != null){
			try{
				tap.executeRequest(req, resp);
			}catch(Throwable t){
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
			}
		}else
			resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "TAP service not yet initialized!");
	}

}
