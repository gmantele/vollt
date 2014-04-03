package uws;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Map.Entry;

/**
 * Parser of HTTP Accept header.
 * It takes into account the order of the different MIME types and their respective quality.
 * 
 * @author Brice Gassmann (CDS) & modified by Gr&eacute;gory Mantelet (CDS)
 * @version 12/2010
 */
public class AcceptHeader {

	/** Quality for each extracted MIME type. */
	private Map<String, Float> mMimeTypes;

	/** Association between a quality and a list of MIME types. */
	private Map<Float, List<String>> mSortedMimeTypes;


	/**
	 * Parses the given value of the Accept field of HTTP request header.
	 * 
	 * @param acceptS	The list of MIME types to parse.
	 */
	public AcceptHeader(String acceptS) {
		mMimeTypes = new HashMap<String,Float>();
		// List of MIME-types
		String[] mimeTypes = acceptS.split(",");
		for (String mimeType : Arrays.asList(mimeTypes)) {
			// Get quality
			Float quality = new Float(1);
			String[] split = mimeType.split(";");
			if (split.length > 1) {
				String[] qualitySplit = split[1].split("=");
				quality = Float.parseFloat(qualitySplit[1]);
			}
			mMimeTypes.put(split[0], quality);
		}
		// Sort mimeTypes by requested quality
		mSortedMimeTypes = new HashMap<Float, List<String>>();
		Set<Entry<String, Float>> mimeTypesES = mMimeTypes.entrySet();
		for (Entry<String, Float> mimeType : mimeTypesES) {
			if (!mSortedMimeTypes.containsKey(mimeType.getValue())) {
				List<String> mimeTypesL = new ArrayList<String>();
				mimeTypesL.add(mimeType.getKey());
				mSortedMimeTypes.put(mimeType.getValue(), mimeTypesL);
			} else {
				mSortedMimeTypes.get(mimeType.getValue()).add(
						mimeType.getKey());
			}
		}
	}

	/**
	 * Gets the association between each extracted MIME type and its respective quality.
	 * 
	 * @return	Extracted MIME types and their quality.
	 */
	public final Map<String, Float> getMimeTypes() {
		return mMimeTypes;
	}

	/**
	 * Sets the association between MIME types and their quality.
	 * 
	 * @param mimeTypes	MIME types and their quality.
	 */
	public final void setMimeTypes(Map<String, Float> mimeTypes) {
		mMimeTypes = mimeTypes;
	}

	/**
	 * Gets the preferred MIME type (HTML, JSON or * /*) according to the order and the quality of all extracted MIME types.
	 * 
	 * @return	<i>application/xhtml+xml</i> or <i>text/html</i>
	 * 			or <i>text/xml</i> or <i>application/json</i>
	 * 			or <i>* /*</i>.
	 */
	public String getPreferredMimeType() {
		if (mSortedMimeTypes.size() == 0) {
			return "*/*";
		}
		Float[] qualities = mSortedMimeTypes.keySet().toArray(new Float[0]);
		Arrays.sort(qualities, Collections.reverseOrder());
		String choosenMimeType = null;
		for (Float key : Arrays.asList(qualities)) {
			List<String> mimeTypes = mSortedMimeTypes.get(key);
			String htmlMimeType = null;
			for (String mimeType : mimeTypes) {
				if (mimeType.equals("application/xhtml+xml")
						|| mimeType.equals("text/html")) {
					htmlMimeType = mimeType;
					break;
				}
				if (mimeType.equals("text/xml")
						|| mimeType.equals("application/json")) {
					choosenMimeType = mimeType;
				}
			}
			if (htmlMimeType != null) {
				return htmlMimeType;
			}
			if (choosenMimeType != null) {
				return choosenMimeType;
			}
		}
		return choosenMimeType;
	}

	/**
	 * Gets a list of the extracted MIME types, ordered by their respective quality.
	 * 
	 * @return	The ordered list of the extracted MIME types.
	 */
	public ArrayList<String> getOrderedMimeTypes(){
		Float[] qualities = mSortedMimeTypes.keySet().toArray(new Float[0]);
		Arrays.sort(qualities, Collections.reverseOrder());

		ArrayList<String> orderedMimeTypes = new ArrayList<String>();
		for(int i=0; i<qualities.length; i++){
			List<String> mimeTypes = mSortedMimeTypes.get(qualities[i]);
			if (mimeTypes != null)
				orderedMimeTypes.addAll(mimeTypes);
		}
		return orderedMimeTypes;
	}
}
