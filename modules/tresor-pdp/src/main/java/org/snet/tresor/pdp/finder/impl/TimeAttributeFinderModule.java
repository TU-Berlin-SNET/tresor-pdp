package org.snet.tresor.finder.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.time.TimeTCPClient;
import org.apache.commons.net.time.TimeUDPClient;
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.XACMLConstants;
import org.wso2.balana.attr.AttributeFactory;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.DateAttribute;
import org.wso2.balana.attr.DateTimeAttribute;
import org.wso2.balana.attr.TimeAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.impl.CurrentEnvModule;

/**
 * Time Attribute Finder Module to get the time, date or date-time from a specified NTP server
 * @author zequeira
 */
public class TimeAttributeFinderModule extends CurrentEnvModule {
    
    /**
     * Server's url to make the request
     */
    public static final String url = "hora.roa.es";
    
    /**
     * Standard environment variable that represents the current timestamp
     */
    public static final String ENVIRONMENT_CURRENT_TIMESTAMP = "org:snet:tresor:time:current-timestamp";
    
    /**
     * Private method to get the time from the NTP specified server through the url parameter.
     */
    private static Date getTimeDate(Date timeDate) throws IOException{
        String[] args = url.split(" ");
                
        if (args.length == 1)
        {
            TimeTCPClient client = new TimeTCPClient();
            try {
                // We want to timeout if a response takes longer than 60 seconds
                client.setDefaultTimeout(60000);
                client.connect(args[0]);
                
                timeDate = client.getDate();
            } finally {
                client.disconnect();
            }
            
        }
        else if (args.length == 2 && args[0].equals("-udp"))
        {
            TimeUDPClient client = new TimeUDPClient();
            try
            {
                // We want to timeout if a response takes longer than 60 seconds
                client.setDefaultTimeout(60000);
                client.open();
                
                timeDate = client.getDate(InetAddress.getByName(args[1]));
            } finally {
                client.close();            
            }
        }
        else
        {
            System.err.println("Usage: TimeClient [-udp] <hostname>");
            return timeDate;
        }
        return timeDate;
    }
    
    /**
     * Private helper that makes a bag containing only the given attribute.
     */
    private EvaluationResult makeBag(URI attributeType, String attributeValue) throws UnknownIdentifierException, ParsingException {
        ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
        AttributeFactory attrFactory = AttributeFactory.getInstance();

        AttributeValue attrValue = null;
        attrValue = attrFactory.createValue(attributeType, attributeValue);

        list.add(attrValue);
        return new EvaluationResult(new BagAttribute(attributeType, list));
    }
    
    
    /**
     * Handles requests for the current Time.
     */
    private EvaluationResult handleTime(URI type,  Date timeDate) throws UnknownIdentifierException, ParsingException {
        // make sure they're asking for a time attribute
        if (!type.toString().equals(TimeAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", new Locale("en","US"));
        
        return makeBag(type, formatter.format(timeDate));
    }
    
    /**
     * Handles requests for the current Date.
     */
    private EvaluationResult handleDate(URI type,  Date timeDate) throws UnknownIdentifierException, ParsingException {
        // make sure they're asking for a date attribute
        if (!type.toString().equals(DateAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", new Locale("en","US"));
        
        return makeBag(type, formatter.format(timeDate));
    }
    
    /**
     * Handles requests for the current DateTime.
     */
    private EvaluationResult handleDateTime(URI type,  Date timeDate) throws UnknownIdentifierException, ParsingException {
        // make sure they're asking for a dateTime attribute
        if (!type.toString().equals(DateTimeAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale("en","US"));
        
        return makeBag(type, formatter.format(timeDate));
    }
    
    /**
     * Handles requests for the current Timestamp.
     */
    private EvaluationResult handleTimestamp(URI type,  Date timeDate) throws UnknownIdentifierException, ParsingException {
        String unixTime = String.valueOf(timeDate.getTime()/1000);
        
        System.out.println("The string is: "+unixTime);
        return makeBag(type, unixTime);
    }
    
    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, String issuer,
            URI category, EvaluationCtx context) {
        // we only know about environment attributes
        if (!XACMLConstants.ENT_CATEGORY.equals(category.toString())){
            return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
        }
        
        Date timeDate = null;
        // figure out which attribute we're looking for
        String attrName = attributeId.toString();
        
        try{
            if (attrName.equals(ENVIRONMENT_CURRENT_TIME)) {
                timeDate = getTimeDate(timeDate);
                return handleTime(attributeType, timeDate);
            } else if (attrName.equals(ENVIRONMENT_CURRENT_DATE)) {
                timeDate = getTimeDate(timeDate);
                return handleDate(attributeType, timeDate);
            } else if (attrName.equals(ENVIRONMENT_CURRENT_DATETIME)) {
                timeDate = getTimeDate(timeDate);
                return handleDateTime(attributeType, timeDate);
            } else if (attrName.equals(ENVIRONMENT_CURRENT_TIMESTAMP)) {
                timeDate = new Date();
                return handleTimestamp(attributeType, timeDate);
            }
        
        }
        catch (IOException ex) {
                    Logger.getLogger(TimeAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnknownIdentifierException ex) {
            Logger.getLogger(TimeAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParsingException ex) {
            Logger.getLogger(TimeAttributeFinderModule.class.getName()).log(Level.SEVERE, null, ex);
        }

        // if we got here, then it's an attribute that we don't know
        return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }
}