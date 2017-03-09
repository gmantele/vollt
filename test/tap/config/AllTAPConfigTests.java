package tap.config;

import java.util.Properties;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import tap.db_testtools.DBTools;
import tap.parameters.TestMaxRecController;

@RunWith(Suite.class)
@SuiteClasses({TestTAPConfiguration.class,TestConfigurableServiceConnection.class,TestConfigurableTAPFactory.class,TestMaxRecController.class})
public class AllTAPConfigTests {

	public final static Properties getValidProperties(){
		Properties validProp = new Properties();
		validProp.setProperty("database_access", "jdbc");
		validProp.setProperty("jdbc_url", DBTools.DB_TEST_URL);
		validProp.setProperty("jdbc_driver", DBTools.DB_TEST_JDBC_DRIVER);
		validProp.setProperty("db_username", DBTools.DB_TEST_USER);
		validProp.setProperty("db_password", DBTools.DB_TEST_PWD);
		validProp.setProperty("sql_translator", "{" + DBTools.DB_TEST_TRANSLATOR + "}");
		validProp.setProperty("metadata", "db");
		validProp.setProperty("file_manager", "local");
		validProp.setProperty("file_root_path", "bin/ext/test/tap");
		return validProp;
	}

}
