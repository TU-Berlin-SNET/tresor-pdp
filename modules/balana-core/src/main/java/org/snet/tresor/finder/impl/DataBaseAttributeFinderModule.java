
package org.snet.tresor.finder.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;

/**
 *
 * @author zequeira
 */
public class DataBaseAttributeFinderModule extends AttributeFinderModule {
    
    /**
     * Standard environment variable that represents the current time
     */
    public static final String supportedAttributes = "org:snet:tresor:db";
    
    @Override
    public boolean isDesignatorSupported() {
        return true;
    }
    
    private EvaluationResult makeBag(URI attributeType, String attributeValue) throws UnknownIdentifierException, ParsingException {
        ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
        AttributeFactory attrFactory = AttributeFactory.getInstance();

        AttributeValue attrValue = null;
        attrValue = attrFactory.createValue(attributeType, attributeValue);

        list.add(attrValue);
        return new EvaluationResult(new BagAttribute(attributeType, list));
    }

    public static String getAttributeFromDB(String driver, String field, String issuer) throws Exception {
        
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
          // this will load the DB driver, each DB has its own driver
          Class.forName(driver);
          // setup the connection with the DB.
          connect = DriverManager
              .getConnection("jdbc:mysql://localhost:3306/tresor", "root", "root");

          // statements allow to issue SQL queries to the database
          statement = connect.createStatement();
          
          //String query = "SELECT "+field+" FROM attributes WHERE issuer = '"+issuer+"'";          
          resultSet = statement.executeQuery("SELECT "+field+" FROM attributes WHERE issuer = '"+issuer+"'");
          
          while (resultSet.next()) {
            return resultSet.getString(field);
          }
          return null;
          
        } catch (ClassNotFoundException e) {
          throw e;
        } catch (SQLException e) {
            throw e;
        } finally {
            connect.close();
        }
    }
    
    public static String getAttributeFromMongoDB(String field, String issuer) throws UnknownHostException{
      
        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
        DB db = mongoClient.getDB( "tresor" );
        //boolean auth = db. authenticate("root", "root");

        DBCollection coll = db.getCollection("attributes");
        mongoClient.setWriteConcern(WriteConcern.NORMAL);

        BasicDBObject query = new BasicDBObject("issuer", issuer);

        DBCursor cursor = coll.find(query);
        try {
          while(cursor.hasNext()) {
              //System.out.println(cursor.next().get(field));
              //System.out.println(cursor.next());
              return cursor.next().get(field).toString();
          }
        return null;
       } finally {
          cursor.close();
       }
    }
    
    
    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer,
            URI category, EvaluationCtx context) {
        // here only undestand how to find attributes from a sql DB 
        if (!attributeId.toString().contains(supportedAttributes)){
            return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
        }
        
        Map<String, String> driverMap = new HashMap<String, String>();
        Map<String, String> dbField = new HashMap<String, String>();
        
        driverMap.put("mysql"       , "com.mysql.jdbc.Driver");
        driverMap.put("oracle"      , "oracle.jdbc.driver.OracleDriver");
        driverMap.put("postgresql"  , "org.postgresql.Driver");
        driverMap.put("mongodb"     , "mongodb");
        
        // figure out which DB are we dealing with
        String attrName = attributeId.toString();
        String[] temp = attrName.split(":");
        
        dbField.put("currentDB"   , temp[4]);
        dbField.put("field"       , temp[5]);
       
        try{
            if (attrName.contains("mysql")) {
                String attributeValue = getAttributeFromDB(driverMap.get("mysql"), dbField.get("field"), "bob");
                return makeBag(attributeType, attributeValue);
            } else if (attrName.contains("mongodb")) {
                String attributeValue = getAttributeFromMongoDB(dbField.get("field"), "bob");
                return makeBag(attributeType, attributeValue);
            }
        
        } catch (Exception ex) {
            Logger.getLogger(DataBaseAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        }

        // if we got here, then it's an attribute that we don't know
        return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }
    
}
