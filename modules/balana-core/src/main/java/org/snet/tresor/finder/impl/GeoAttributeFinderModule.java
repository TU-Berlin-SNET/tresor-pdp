package org.snet.tresor.finder.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.finder.AttributeFinderModule;

/**
 * 
 * @author Zequeira
 */
public class GeoAttributeFinderModule extends AttributeFinderModule {
    
    /**
     * Attribute Id suported by this Attribute Finder Module
     */
    public static final String suportedAttributeId = "http://wso2.org/claims/emailaddress";
    
    /**
     * Server's url to make the request
     */
    public static final String url = "http://localhost:9090/coordinates";
    
    /**
     * Tries to find attribute values based on the given designator data. The result, if successful,
     * must always contain a <code>BagAttribute</code>, even if only one value was found. If no
     * values were found, but no other error occurred, an empty bag is returned. This method may
     * need to invoke the context data to look for other attribute values, so a module writer must
     * take care not to create a scenario that loops forever.
     * 
     * @param attributeType the datatype of the attributes to find
     * @param attributeId the identifier of the attributes to find
     * @param issuer the issuer of the attributes, or null if unspecified
     * @param category the category of the attribute whether it is Subject, Resource or any thing
     * @param context the representation of the request data
     * 
     * @return the result of attribute retrieval, which will be a bag of attributes or an error
     */
    //@Override
    public EvaluationResult findAttribute_old(URI attributeType, URI attributeId, String issuer,
            URI category, EvaluationCtx context) {
        
        // check if the AttributeId is supported by this Attribute Finder, otherwise go for the next AttributeFinder
        if (!suportedAttributeId.equals(attributeId.toString()))
            return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
        
        ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
        
        Map<String, String> data = new HashMap<String, String>();
        /*data.put("AttributeId", "http://wso2.org/claims/emailaddress");
        data.put("DataType", "http://www.w3.org/2001/XMLSchema#string");
        data.put("Category", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");*/
        
        data.put("AttributeId", attributeId.toString());
        data.put("DataType", attributeType.toString());
        data.put("Category", category.toString());
        
        //System.out.println("La info en AttributeId es: " +data.get("AttributeId"));
        
        //String url = "http://localhost:9090/coordinates";
        String USER_AGENT = "Mozilla/5.0";
        URL  obj;
        //HttpsURLConnection conection;
        try {
            obj = new URL(url);
            HttpURLConnection conection = (HttpURLConnection) obj.openConnection();
            
            //adding request headers
            conection.setRequestMethod("POST");
            conection.setDoOutput(true);
            conection.setDoInput(true);
            conection.setRequestProperty("User-Agent", USER_AGENT);
            conection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            
            
            DataOutputStream out = new DataOutputStream(conection.getOutputStream());
		
            Set keys = data.keySet();
            Iterator keyIter = keys.iterator();
            String content = "";
            for(int i=0; keyIter.hasNext(); i++) {
                    Object key = keyIter.next();
                    if(i!=0) {
                            content += "&";
                    }
                    content += key + "=" + URLEncoder.encode(data.get(key), "UTF-8");
            }
            System.out.println(content);
            out.writeBytes(content);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conection. getInputStream()));
            String line = "";
            String answer[] = null;
            
            StringBuffer response = new StringBuffer();
            
            while((line=in.readLine())!=null) {
                    System.out.println(line);
                    answer = line.split(" ");
            }
            in.close();
            
            
            AttributeFactory attrFactory = AttributeFactory.getInstance();
            
            AttributeValue attrValue = null;
            attrValue = attrFactory.createValue(attributeType, answer[7]);
            
            list.add(attrValue);
            
            
            //print result
            //System.out.println(response.toString());
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(GeoAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GeoAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownIdentifierException ex) {
            Logger.getLogger(GeoAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParsingException ex) {
            Logger.getLogger(GeoAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return new EvaluationResult(new BagAttribute(attributeType, list));
        //return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }
    
    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer,
            URI category, EvaluationCtx context) {
        
        // check if the AttributeId is supported by this Attribute Finder, otherwise go for the next AttributeFinder
        if (!suportedAttributeId.equals(attributeId.toString()))
            return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
        
        ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
        Map<String, String> responseMap = new HashMap<String, String>();
        
        HttpClient client = new HttpClient();
        
        //BufferedReader bufferedReader = null;

        PostMethod method = new PostMethod(url);
        method.addParameter("AttributeId", attributeId.toString());
        method.addParameter("DataType", attributeType.toString());
        method.addParameter("Category", category.toString());
        
        String answerPairs[] = null;
        String readLine = null;
        
        try{
                client.executeMethod(method);

                readLine = method.getResponseBodyAsString();
                answerPairs = readLine.split(" ");
                /*bufferedReader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
                while(((readLine = bufferedReader.readLine()) != null)) {
                  answer = readLine.split(" ");
                }*/
                
                for (int i=0; i<answerPairs.length; i++){
                    String pair = answerPairs[i];
                    String[] keyValue = pair.split(":=");
                    responseMap.put(keyValue[0], keyValue[1]);
                }
                
                
                AttributeFactory attrFactory = AttributeFactory.getInstance();

                AttributeValue attrValue = null;
                attrValue = attrFactory.createValue(attributeType, responseMap.get("AttributeValue"));

                list.add(attrValue);
                
                
            } catch (Exception e) {
              System.err.println(e);
            } finally {
              method.releaseConnection();
              //if(bufferedReader != null) try { bufferedReader.close(); } catch (Exception fe) {}
            }

        return new EvaluationResult(new BagAttribute(attributeType, list));
    }
    
    @Override
    public boolean isDesignatorSupported() {
        return true;
    }

}
