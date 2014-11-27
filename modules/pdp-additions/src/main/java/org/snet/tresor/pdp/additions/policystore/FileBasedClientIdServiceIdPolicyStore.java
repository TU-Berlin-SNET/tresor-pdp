package org.snet.tresor.pdp.additions.policystore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;

/**
 * Thread-safe file-based two-key-value policy store implementation
 * Key1: ClientId, Key2: ServiceId
 */
public class FileBasedClientIdServiceIdPolicyStore implements TwoKeyValuePolicyStore {
	private static final Logger log = LoggerFactory.getLogger(FileBasedClientIdServiceIdPolicyStore.class);
	private File baseDirectory;
	private ReadWriteLock lock;
	// TODO caching!

	/**
	 * Create a new FileBasedClientIdServiceIdPolicyStore, directories are created if not existing
	 * @param directory path to the basedirectory
	 * @param lock the lock
	 */
	public FileBasedClientIdServiceIdPolicyStore(String directory, ReadWriteLock lock) {
		this(new File(directory), lock);
	}

	/**
	 * Create a new FileBasedClientIdServiceIdPolicyStore, directories are created if not existing
	 * @param directory the basedirectory
	 * @param lock the lock
	 */
	public FileBasedClientIdServiceIdPolicyStore(File directory, ReadWriteLock lock) {
		this.baseDirectory = directory;
		this.lock = lock;

		if (!this.baseDirectory.isDirectory() && !this.baseDirectory.mkdirs())
			throw new RuntimeException("Cannot create policy store directory '" + directory.getPath() + "'");
		if (lock == null)
			throw new RuntimeException("Lock may not be null");
	}


	public boolean hasPolicy(String clientId, String serviceId) {
		this.lock.readLock().lock();
		try {
			File file = new File(this.baseDirectory.getPath() + File.separator + clientId + File.separator + serviceId);
			return file.isFile();
		} finally {
			this.lock.readLock().unlock();
		}
	}

	public AbstractPolicy get(EvaluationCtx ctx, PolicyFinder finder) {
		String clientId = XACMLHelper.getClientID(ctx);
		String serviceId = XACMLHelper.getServiceID(ctx);
		String policy = this.get(clientId, serviceId);
		try {
			if (policy != null)
				return XACMLHelper.loadPolicyOrPolicySet(policy, finder);
		} catch (Exception e) {
			log.error("Found corresponding policy for client {} and service {} but failed to load it", clientId, serviceId, e);
		}
		return null;
	}

	public Map<String, String> get(String clientId) {
		Map<String, String> policies = null;

		this.lock.readLock().lock();
		try {
			File file = new File(this.baseDirectory.getPath() + File.separator + clientId);
			// if the directory exists, read all contained files
			if (file.isDirectory()) {
				policies = new HashMap<String, String>();
				File[] files = file.listFiles();
				for (int i = 0; i < files.length; i++)
					policies.put(files[i].getName(), this.readFile(files[i]));
			}
		} finally {
			this.lock.readLock().unlock();
		}

		return policies;
	}

	public String get(String clientId, String serviceId) {
		this.lock.readLock().lock();
		try {
			File file = new File(this.baseDirectory.getPath() + File.separator + clientId + File.separator + serviceId);
			return (file.isFile()) ? this.readFile(file) : null;
		} finally {
			this.lock.readLock().unlock();
		}
	}


	public void put(String clientId, String serviceId, String policy) {
		this.lock.writeLock().lock();
		BufferedWriter writer = null;
		try {
			// make sure the directory exists
			File dir = new File(this.baseDirectory.getPath() + File.separator + clientId);
			if (!dir.isDirectory() && !dir.mkdirs())
				throw new RuntimeException("Cannot create policy store directory '" + dir.getPath() + "'");

			// make sure the file exists
			File file = new File(dir.getPath() + File.separator + serviceId);
			if (!file.isFile() && !file.createNewFile())
				throw new RuntimeException("Cannot create policy store file '" + file.getPath() + "'");

			// write & close
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(policy);
			writer.close();

		} catch (IOException e) {
			log.error("Failed to write policy for {} and {} to policystore", clientId, serviceId, e);
			try { writer.close(); }
			catch (Exception i) {}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public boolean delete(String clientId, String serviceId) {
		this.lock.writeLock().lock();
		try {
			File file = new File(this.baseDirectory.getPath() + File.separator + clientId + File.separator + serviceId);
			return file.delete();
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public void close() { }

	/**
	 * Reads given file into a string
	 * @param f the file to read
	 * @return file contents as string or null if anything goes wrong
	 */
	private String readFile(File f) {
		if (f == null || !f.isFile())
			return null;

		String policy = null;
		Scanner scanner = null;
		try {
			scanner = new Scanner(f).useDelimiter("\\A");
			policy = scanner.next();

			// Scanner class hides exceptions so scan for them
			if (scanner.ioException() != null)
				throw scanner.ioException();

		} catch (IOException e) {
			log.error("Error reading from file '{}'", f.getPath(), e);
			policy = null;
		} finally {
			scanner.close();
		}

		return policy;
	}

}
