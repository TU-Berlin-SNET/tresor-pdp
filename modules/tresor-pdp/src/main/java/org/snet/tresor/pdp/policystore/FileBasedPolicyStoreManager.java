package org.snet.tresor.pdp.policystore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PolicyStoreManager for file based policy store, NOT thread-safe
 * @author malik
 */
public class FileBasedPolicyStoreManager implements PolicyStoreManager {
	private static Log log = LogFactory.getLog(FileBasedPolicyStoreManager.class);
	private File baseDirectory;
	
	public FileBasedPolicyStoreManager(String directoryPath) {		
		this.baseDirectory = new File(directoryPath);
		
		if (!this.baseDirectory.isDirectory())
			this.baseDirectory.mkdir();
	}
	
	public FileBasedPolicyStoreManager(String... directoryPath) {
		this(directoryPath[0]);
	}
	
	public Map<String, String> getAll(String domain) {
		Map<String, String> policyMap = new HashMap<String, String>();
		File file = new File(this.baseDirectory + File.separator + domain);

		if (file.isDirectory()) {
			putPolicies(file, policyMap);
		}
		
		return policyMap;
	}

	public String getPolicy(String domain, String service) {
		String policy = null;
		
		File file = new File(this.baseDirectory + File.separator + domain + File.separator + service);
		if (file.isFile())
			policy = readFile(file);
		
		return policy;
	}

	public String addPolicy(String domain, String service, String policy) {
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

	public int deletePolicy(String domain, String service) {
		int response = 0;
				
		File file = new File(this.baseDirectory + File.separator + domain + File.separator + service);
		if (file.isFile())
			response = (file.delete()) ? 1 : 0;
			
		return response;
	}

	public void close() { }
	
	private void putPolicies(File dir, Map<String, String> policyMap) {
		for (File f : dir.listFiles()) {			
			if (f.isFile())
				policyMap.put(f.getName(), readFile(f));
		}
	}
	
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
	
	private void writeFile(File f, String s) throws IOException {
		PrintWriter writer = new PrintWriter(f);
		writer.print(s);
		writer.flush();
		writer.close();
	}

}
