package testtools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Let's execute any shell command (even with pipes and redirections).
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (09/2014)
 */
public final class CommandExecute {

	/**
	 * SINGLETON CLASS.
	 * No instance of this class can be created.
	 */
	private CommandExecute(){}

	/**
	 * Execute the given command (which may include pipe(s) and/or redirection(s)).
	 * 
	 * @param command	Command to execute in the shell.
	 * 
	 * @return	The string returned by the execution of the command.
	 */
	public final static String execute(final String command){

		String[] shellCmd = new String[]{"/bin/sh","-c",command};

		StringBuffer output = new StringBuffer();

		Process p;
		try{
			p = Runtime.getRuntime().exec(shellCmd);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while((line = reader.readLine()) != null){
				output.append(line + "\n");
			}

		}catch(Exception e){
			e.printStackTrace();
		}

		return output.toString();

	}
}
