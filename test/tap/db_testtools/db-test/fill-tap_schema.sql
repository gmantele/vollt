BEGIN;


-- Fill the table TAP_SCHEMA.schemas
-- (note: it should already contains the "TAP_SCHEMA" entry)
INSERT INTO "TAP_SCHEMA"."schemas"
        ("schema_name")
  VALUES('public');


-- Fill the table TAP_SCHEMA.tables
-- (note: it should already contains the "TAP_SCHEMA" tables entries)
INSERT INTO "TAP_SCHEMA"."tables"
        ("schema_name", "table_name", "table_type", "description")
  VALUES('public'     , 'hipparcos' , 'TABLE'     , 'Hipparcos subset for JUnit tests of the TAP-Library.');


-- Fill the table TAP_SCHEMA.columns
-- (note: it should already contains all the "TAP_SCHEMA" table's columns entries)
INSERT INTO "TAP_SCHEMA"."columns"
        ("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description"            , "indexed", "principal")
  VALUES('hipparcos' , 1             , 'hip'        , 'INTEGER' , -1         , -1    , NULL    , 'meta.id;meta.main'    , 'Identifier (HIP number)', 1        , 1);
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description", "principal")
  VALUES('hipparcos' , 2             , 'ra'         , 'DOUBLE ' , -1         , -1    , 'deg'   , 'pos.eq.ra;meta.main'  , 'alpha, degrees (ICRS, Epoch=J1991.25)', 1);
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description", "principal")
  VALUES('hipparcos' , 3             , 'dec'        , 'DOUBLE ' , -1         , -1    , 'deg'   , 'pos.eq.dec;meta.main' , 'delta, degrees (ICRS, Epoch=J1991.25)', 1);
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 4             , 'e_ra'       , 'REAL'    , -1         , -1    , 'mas'   , 'stat.error'           , 'Standard error in RA*cos(DEdeg) (at epoch J1991.25; for different epochs, the actual mean error must take into account the proper motion uncertainties)');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 5             , 'e_dec'      , 'REAL'    , -1         , -1    , 'mas'   , 'stat.error'           , 'Standard error in DE (at epoch J1991.25; for different epochs, the actual mean error must take into account the proper motion uncertainties)');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 6             , 'vmag'       , 'REAL'    , -1         , -1    , 'mag'   , 'phot.mag;em.opt.V'    , 'Magnitude in Johnson V');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 7             , 'plx'        , 'REAL'    , -1         , -1    , 'mas'   , 'pos.parallax.trig'    , 'Trigonometric parallax');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 8             , 'e_plx'      , 'REAL'    , -1         , -1    , 'mas'   , 'stat.error'           , 'Standard error in Plx');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 9             , 'pmra'       , 'DOUBLE'  , -1         , -1    , 'mas/yr', 'pos.pm;pos.eq.ra'     , 'Proper motion mu_alpha.cos(delta), ICRS (for J1991.25 epoch)');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 10            , 'e_pmra'     , 'REAL'    , -1         , -1    , 'mas/yr', 'stat.error'           , 'Standard error in pmRA');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 11            , 'e_pmdec'    , 'REAL'    , -1         , -1    , 'mas/yr', 'stat.error'           , 'Standard error in pmDE');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 12            , 'bt_mag'     , 'REAL'    , -1         , -1    , 'mag'   , 'phot.mag;em.opt.B'    , 'Mean BT magnitude');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 13            , 'e_bt_mag'   , 'REAL'    , -1         , -1    , 'mag'   , 'stat.error'           , 'Standard error on BTmag');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 14            , 'vt_mag'     , 'REAL'    , -1         , -1    , 'mag'   , 'phot.mag;em.opt.V'    , 'Mean VT magnitude');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 15            , 'e_vt_mag'   , 'REAL'    , -1         , -1    , 'mag'   , 'stat.error'           , 'Standard error on VTmag');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 16            , 'b_v'        , 'REAL'    , -1         , -1    , 'mag'   , 'phot.color;em.opt.B;em.opt.V', 'Johnson B-V colour');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 17            , 'e_b_v'      , 'REAL'    , -1         , -1    , 'mag'   , 'stat.error'           , 'Standard error on B-V');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 18            , 'v_i'        , 'REAL'    , -1         , -1    , 'mag'   , 'phot.color;em.opt.V;em.opt.I', 'Colour index in Cousins'' system');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 19            , 'e_v_i'      , 'REAL'    , -1         , -1    , 'mag'   , 'stat.error'           , 'Standard error on V-I');
INSERT INTO "TAP_SCHEMA"."columns"("table_name", "column_index", "column_name", "datatype", "arraysize", "size", "unit"  , "ucd"                  , "description")
  VALUES('hipparcos' , 20            , 'sptype'     , 'VARCHAR' , -1         , -1    , NULL    , 'src.spType'           , 'Spectral type');

COMMIT;
