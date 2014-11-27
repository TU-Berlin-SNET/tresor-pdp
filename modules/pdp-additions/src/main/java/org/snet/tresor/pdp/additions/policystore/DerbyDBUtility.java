
package org.snet.tresor.pdp.additions.policystore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snet.tresor.pdp.additions.XACMLHelper;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinder;

/**
 * Class to handle all the operations with a Derby Data Base
 * @author zequeira
 */
public class DerbyDBUtility implements PolicyStore{
	private static final Logger log = LoggerFactory.getLogger(DerbyDBUtility.class);

    private static final String driver = "org.apache.derby.jdbc.ClientDriver";
    private static final String protocol = "jdbc:derby://localhost:1527/";
    private static final String db = "PolicyStore";
    private static final String dbTable = "Policy";
    private static Connection connection = null;

    private static Statement statement = null;
    private static PreparedStatement psInsert = null;
    private static PreparedStatement psUpdate = null;
    private static ResultSet resultSet = null;

    public DerbyDBUtility() throws SQLException {

        //load the desired JDBC driver
        try {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println(
                        "\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println(
                        "\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }

        try
        {

            connection = DriverManager.getConnection(protocol + db, "root", "root");
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            this.statement = connection.createStatement();
        } catch (SQLException sqle) {
            throw sqle;
        }
    }

    public Map<String, String> get(String domain) {
        try {
            //to prevent the error getting a policy that already was deleted
            this.statement = connection.createStatement(this.resultSet.TYPE_SCROLL_INSENSITIVE, this.resultSet.CONCUR_UPDATABLE);

            this.resultSet = this.statement.executeQuery("SELECT * FROM "+dbTable+" WHERE domain='"+domain+"'");
            Map<String, String> policyMap = new HashMap<String, String>();

            while (this.resultSet.next()) {
                policyMap.put(this.resultSet.getString("service") , this.resultSet.getString("policy"));
            }
            connection.commit();
            return policyMap;
        } catch (SQLException ex) {
        	log.error("", ex);
            return null;
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

    public String get(String domain, String service) {
        try {
            //to prevent the error getting a policy that already was deleted
            this.statement = connection.createStatement(this.resultSet.TYPE_SCROLL_INSENSITIVE, this.resultSet.CONCUR_UPDATABLE);

            this.resultSet = this.statement.executeQuery("SELECT policy FROM "+dbTable+" WHERE domain='"+domain+"' and service='"+service+"'");

            String policy = null;
            while (this.resultSet.next()) {
                policy = this.resultSet.getString("policy");
            }
            connection.commit();
            return policy;
        } catch (SQLException ex) {
        	log.error("", ex);
            return null;
        }
    }

    public String put(String domain, String service, String policy) {
        try {
            this.psInsert = connection.prepareStatement("insert into "+dbTable+" (domain, service, policy) values (?, ?, ?)");

            this.psInsert.setString(1, domain);
            this.psInsert.setString(2, service);
            this.psInsert.setString(3, policy);
            this.psInsert.executeUpdate();

            connection.commit();
            return policy;
        } catch (SQLException ex) {
        	log.error("", ex);
            return null;
        }
    }

    public int delete(String domain, String service) {
        try {
            this.psUpdate = connection.prepareStatement("DELETE FROM "+dbTable+" WHERE domain='"+domain+"' and service='"+service+"'");
            int result = this.psUpdate.executeUpdate();
            connection.commit();

            if ( result > 0) {
                return 1;
            } else {
                return 0;
            }
        } catch (SQLException ex) {
        	log.error("", ex);
            return 0;
        }
    }


    public void close() {
        // release all open resources to avoid unnecessary memory usage
        // ResultSet
        try {
            if (this.resultSet != null) {
                this.resultSet.close();
                this.resultSet = null;
            }
        } catch (SQLException sqle) {
        	log.error("", sqle);
        }

        // Statement and PreparedStatements
        try {
            if (this.statement != null) {
                this.statement.close();
                this.statement = null;
            }
        } catch (SQLException sqle) {
        	log.error("", sqle);
        }

        //Connection
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException sqle) {
        	log.error("", sqle);
        }
    }

}
