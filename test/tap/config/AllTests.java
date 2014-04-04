package tap.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({TestDefaultServiceConnection.class,TestDefaultTAPFactory.class})
public class AllTests {

}
