package tap.config;

import java.util.Properties;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import tap.parameters.TestMaxRecController;

@RunWith(Suite.class)
@SuiteClasses({TestTAPConfiguration.class,TestConfigurableServiceConnection.class,TestConfigurableTAPFactory.class,TestMaxRecController.class})
public class AllTests {

	public final static Properties getValidProperties(){
		Properties validProp = new Properties();
		validProp.setProperty("database_access", "jdbc");
		validProp.setProperty("jdbc_url", "jdbc:postgresql:gmantele");
		validProp.setProperty("jdbc_driver", "org.postgresql.Driver");
		validProp.setProperty("db_username", "gmantele");
		validProp.setProperty("db_password", "pwd");
		validProp.setProperty("sql_translator", "postgres");
		validProp.setProperty("metadata", "db");
		validProp.setProperty("file_manager", "local");
		validProp.setProperty("file_root_path", "bin/ext/test/tap");
		return validProp;
	}

}
