package org.snet.tresor.pdp.policystore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PolicyStore for file based policy store, NOT THREAD SAFE
 * @author malik
 */
public class FileBasedPolicyStore implements PolicyStore {	
	private static final Logger log = LoggerFactory.getLogger(FileBasedPolicyStore.class);
	private File baseDirectory;
	
	/**
	 * Create new FileBasedPolicyStore pointing to given directoryPath
	 * (directory is created if it does not exists)
	 * @param directoryPath path to the directory which is designated to hold the policies
	 */
	public FileBasedPolicyStore(String directoryPath) {		
		this.baseDirectory = new File(directoryPath);
		
		if (!this.baseDirectory.isDirectory())
			this.baseDirectory.mkdirs();
	}
	
	/**
	 * Create new FileBasedPolicyStore pointing to given directoryPath
	 * (directory is created if it does not exists)
	 * Compatibility method, takes first value of array as directory Path
	 * @param directoryPath array containing the directory path
	 */
	public FileBasedPolicyStore(String... directoryPath) {
		this(directoryPath[0]);
	}
	
	public Map<String, String> get(String domain) {
		Map<String, String> policyMap = new HashMap<String, String>();
		File file = new File(this.baseDirectory + File.separator + domain);

		if (file.isDirectory()) {
			putPolicies(file, policyMap);
		}
		
		return policyMap;
	}

	public String get(String domain, String service) {
		String policy = null;
		
		File file = new File(this.baseDirectory + File.separator + domain + File.separator + service);
		if (file.isFile())
			policy = readFile(file);
		
		return policy;
	}

	public String put(String domain, String service, String policy) {
		String response = null;
		
		try {
			File dir = new File(this.baseDirectory + File.separator + domain);
			
			if (!dir.isDirectory())
				dir.mkdir();
			
			File file = new File(this.baseDirectory + File.separator + domain + File.separator + service);			
			
			if (!file.isFile())
				file.createNewFile();
			
			writeFile(file, policy);
			response = service;
		} catch (IOException e) { }
		
		return response;
	}

	public int delete(String domain, String service) {
		int response = 0;
				
		File file = new File(this.baseDirectory + File.separator + domain + File.separator + service);
		if (file.isFile())
			response = (file.delete()) ? 1 : 0;
			
		return response;
	}

	public void close() { }
	
	/**
	 * Loads all files in a given directory to given policyMap as String
	 * (expects to only find policies in the directory)
	 * 
	 * @param dir file directory containing policies
	 * @param policyMap Map to save loaded policies
	 */
	private void putPolicies(File dir, Map<String, String> policyMap) {
		for (File f : dir.listFiles()) {			
			if (f.isFile())
				policyMap.put(f.getName(), readFile(f));
		}
	}
	
	/**
	 * Reads given file into string
	 * @param f the file to read
	 * @return file contents as string
	 */
	private String readFile(File f) {
		String policy = null;
				
		try {
			Scanner scanner = new Scanner(f);
			scanner.useDelimiter("\\A");
			policy = scanner.next();
			scanner.close();			
		} catch (FileNotFoundException e) {	}
		
		return policy;
	}
	
	/**
	 * Writes given string to file
	 * @param f the file to write
	 * @param s the string to write
	 * @throws IOException
	 */
	private void writeFile(File f, String s) throws IOException {
		PrintWriter writer = new PrintWriter(f);
		writer.print(s);
		writer.flush();
		writer.close();
	}

}
