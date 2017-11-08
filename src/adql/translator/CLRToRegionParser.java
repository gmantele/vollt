package org.eso.asp.tap.adql.translator;

/*
 * This file is part of ADQLLibrary.
 *
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2017 - European Southern Observatory (ESO)
 */

import adql.db.STCS;
import adql.db.STCS.Region;
import adql.parser.ParseException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that converts MS SQL Server internal geometry format into STCS.Region.
 * MS SQL Server format is described here:
 * https://msdn.microsoft.com/en-us/library/ee320529(v=sql.105).aspx
 *
 * This class currently implements only the geometries used in the ESO
 * databases, i.e.
 * - Point
 * - Polygon (5 points)
 * - GeometryCollection (series of n 5-points polygons)
 *
 * The class also assumes the following values in the preamble:
 * SRD=104001
 * Version=1
 *
 * Author: Vincenzo Forchi`, vforchi@eso.org, vincenzo.forchi@gmail.com
 */
public class CLRToRegionParser {

    public static final STCS.CoordSys coordSys = new STCS.CoordSys(STCS.Frame.J2000, null, null);

    public static Region parseRegion(byte[] bytes) throws ParseException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.rewind();

        int coordSys = buffer.getInt();
        if (coordSys != 104001)
            throw new ParseException("Wrong coordinate system " + coordSys);
        byte version = buffer.get();
        if (version != 0x01) {
            return null;
            //throw new ParseException("Wrong version " + version);
        }

        byte type = buffer.get();

        if (type == 0x0c)
            return parsePoint(buffer);
        else if (type == 0x04) {
            int points = buffer.getInt();
            if (points == 5) {
                return parsePolygon(buffer, 5);
            } else if (points > 200 || (points % 5 != 0)) {
                // CIRCLE does not exist in WKT/WKB, and gets translated into a polygon with many sides
                return parsePolygon(buffer, points);
            } else {
                return parseGeometryCollection(buffer, points/5);
            }
        } else {
            return null;
        }
    }

    private static double[] parseCoordinate(ByteBuffer buffer) {
        double y = buffer.getDouble();
        double x = buffer.getDouble() + 180;
        return new double[]{ x, y };
    }

    private static Region parsePoint(ByteBuffer buffer) {
        return new Region(coordSys, parseCoordinate(buffer));
    }

    private static Region parsePolygon(ByteBuffer buffer, int numPoints) {
        List<double[]> points = new ArrayList<>();
        for (int point = 0; point < numPoints; point++) {
            points.add(parseCoordinate(buffer));
        }
        return new Region(coordSys, points.toArray(new double[points.size()][2]));
    }

    private static Region parseGeometryCollection(ByteBuffer buffer, int numPolygons) {
        List<Region> regions = new ArrayList<>();

        for (int poly = 0; poly < numPolygons; poly++)
            regions.add(parsePolygon(buffer, 5));

        return new Region(STCS.RegionType.UNION, coordSys, regions.toArray(new Region[0]));
    }

}
