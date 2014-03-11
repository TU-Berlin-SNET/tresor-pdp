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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ParsingException;
import org.wso2.balana.PolicyMetaData;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.Status;
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
    
    private EvaluationResult createProcessingError(String msg) {
        ArrayList<String> code = new ArrayList<String>();
        code.add(Status.STATUS_PROCESSING_ERROR);
        return new EvaluationResult(new Status(code, msg));
    }
    
    /**
     * Tries to find attribute values based on the given selector data. The result, if successful,
     * always contains a <code>BagAttribute</code>, even if only one value was found. If no values
     * were found, but no other error occurred, an empty bag is returned.
     * 
     * @param path
     *            the XPath expression to search against
     * @param namespaceNode
     *            the DOM node defining namespace mappings to use, or null if mappings come from the
     *            context root
     * @param type
     *            the datatype of the attributes to find
     * @param context
     *            the representation of the request data
     * @param xpathVersion
     *            the XPath version to use
     * 
     * @return the result of attribute retrieval, which will be a bag of attributes or an error
     */
    public EvaluationResult findAttribute(String path, Node namespaceNode, URI type,
            EvaluationCtx context, String xpathVersion) {

        if (!xpathVersion.equals(PolicyMetaData.XPATH_1_0_IDENTIFIER))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));

        // get the DOM root of the request document
        Node root = context.getRequestRoot();

        // if we were provided with a non-null namespace node, then use it
        // to resolve namespaces, otherwise use the context root node
        Node nsNode = (namespaceNode != null) ? namespaceNode : root;

        // setup the root path (pre-pended to the context path), which...
        String rootPath = "";

        // ...only has content if the context path is relative
        if (path.charAt(0) != '/') {
            String rootName = root.getLocalName();

            // see if the request root is in a namespace
            String namespace = root.getNamespaceURI();

            if (namespace == null) {
                // no namespacing, so we're done
                rootPath = "/" + rootName + "/";
            } else {
                // namespaces are used, so we need to lookup the correct
                // prefix to use in the search string
                NamedNodeMap nmap = namespaceNode.getAttributes();
                rootPath = null;

                for (int i = 0; i < nmap.getLength(); i++) {
                    Node n = nmap.item(i);
                    if (n.getNodeValue().equals(namespace)) {
                        // we found the matching namespace, so get the prefix
                        // and then break out
                        String name = n.getNodeName();
                        int pos = name.indexOf(':');

                        if (pos == -1) {
                            // the namespace was the default namespace
                            rootPath = "/";
                        } else {
                            // we found a prefixed namespace
                            rootPath = "/" + name.substring(pos + 1);
                        }

                        // finish off the string
                        rootPath += ":" + rootName + "/";

                        break;
                    }
                }

                // if the rootPath is still null, then we don't have any
                // definitions for the namespace
                if (rootPath == null)
                    return createProcessingError("Failed to map a namespace"
                            + " in an XPath expression");
            }
        }

        // now do the query, pre-pending the root path to the context path
        NodeList matches = null;
        try {
            // NOTE: see comments in XALAN docs about why this is slow
            //matches = XPathAPI.selectNodeList(root, rootPath + path, nsNode);
        } catch (Exception e) {
            // in the case of any exception, we need to return an error
            return createProcessingError("error in XPath: " + e.getMessage());
        }

        if (matches.getLength() == 0) {
            // we didn't find anything, so we return an empty bag
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        // there was at least one match, so try to generate the values
        try {
            ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
            AttributeFactory attrFactory = AttributeFactory.getInstance();

            for (int i = 0; i < matches.getLength(); i++) {
                String text = null;
                Node node = matches.item(i);
                short nodeType = node.getNodeType();

                // see if this is straight text, or a node with data under
                // it and then get the values accordingly

                AttributeValue attrValue = null;

                if ((nodeType == Node.CDATA_SECTION_NODE) || (nodeType == Node.COMMENT_NODE)
                        || (nodeType == Node.TEXT_NODE) || (nodeType == Node.ATTRIBUTE_NODE)) {
                    // there is no child to this node
                    text = node.getNodeValue();
                    attrValue = attrFactory.createValue(type, text);
                } else if (nodeType == Node.DOCUMENT_NODE || nodeType == Node.ELEMENT_NODE) {
                    attrValue = attrFactory.createValue(node, type);
                } else {
                    // the data is in a child node
                    text = node.getFirstChild().getNodeValue();
                    attrValue = attrFactory.createValue(type, text);
                }

                list.add(attrValue);
            }

            return new EvaluationResult(new BagAttribute(type, list));
        } catch (ParsingException pe) {
            return createProcessingError(pe.getMessage());
        } catch (UnknownIdentifierException uie) {
            return createProcessingError("unknown attribute type: " + type);
        }
    }
    
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
    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer,
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
    public boolean isDesignatorSupported() {
        return true;
    }

}
