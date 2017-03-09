
-- Configure the geometrical Shapes serialization:
SET JAVA_OBJECT_SERIALIZER '''ari.h2.astro.AstroH2ObjectSerializer''';

-- Adding all the geometrical functions
-- (i.e. Shapes constructors, predicates and other functions):
DROP ALIAS IF EXISTS CONTAINS;
CREATE ALIAS CONTAINS FOR "ari.h2.astro.AstroH2.contains";
DROP ALIAS IF EXISTS BOX;
CREATE ALIAS BOX FOR "ari.h2.astro.AstroH2.box";
DROP ALIAS IF EXISTS DISTANCE;
CREATE ALIAS DISTANCE FOR "ari.h2.astro.AstroH2.distance";
DROP ALIAS IF EXISTS INTERSECTS;
CREATE ALIAS INTERSECTS FOR "ari.h2.astro.AstroH2.intersects";
DROP ALIAS IF EXISTS POINT;
CREATE ALIAS POINT FOR "ari.h2.astro.AstroH2.point";
DROP ALIAS IF EXISTS COORD1;
CREATE ALIAS COORD1 FOR "ari.h2.astro.AstroH2.coord1";
DROP ALIAS IF EXISTS AREA;
CREATE ALIAS AREA FOR "ari.h2.astro.AstroH2.area";
DROP ALIAS IF EXISTS CENTROID;
CREATE ALIAS CENTROID FOR "ari.h2.astro.AstroH2.centroid";
DROP ALIAS IF EXISTS COORD2;
CREATE ALIAS COORD2 FOR "ari.h2.astro.AstroH2.coord2";
DROP ALIAS IF EXISTS CIRCLE;
CREATE ALIAS CIRCLE FOR "ari.h2.astro.AstroH2.circle";
DROP ALIAS IF EXISTS POLYGON;
CREATE ALIAS POLYGON FOR "ari.h2.astro.AstroH2.polygon";
DROP ALIAS IF EXISTS COORDSYS;
CREATE ALIAS COORDSYS FOR "ari.h2.astro.AstroH2.coordsys";

-- Creation of the testing table (here: a small subset of Hipparcos):
CREATE TABLE hipparcos (
    hip integer PRIMARY KEY,
    ra double precision,
    e_ra real,
    dec double precision,
    e_dec real,
    vmag real,
    plx real,
    e_plx real,
    pmra double precision,
    e_pmra real,
    pmdec double precision,
    e_pmdec real,
    btmag real,
    e_btmag real,
    vtmag real,
    e_vtmag real,
    b_v real,
    e_b_v real,
    v_i real,
    e_v_i real,
    sptype character varying(12))
  AS SELECT * FROM CSVREAD('./test/tap/db_testtools/db-test/hipparcos_subset.csv');

-- #############################################################################
-- GENERATION OF "hipparcos_subset" IN POSTGRES:
--
-- CREATE TABLE hipparcos_subset AS SELECT hip, ra, e_ra, dec, e_dec, vmag, plx, e_plx, pmra, e_pmra, pmdec, e_pmdec, btmag, e_btmag, vtmag, e_vtmag, b_v, e_b_v, v_i, e_v_i, sptype FROM extcat.hipparcos LIMIT 1000;
-- 
-- COPY hipparcos_subset TO '/tmp/hipparcos_subset.csv' DELIMITER ',' CSV HEADER;
-- #############################################################################
