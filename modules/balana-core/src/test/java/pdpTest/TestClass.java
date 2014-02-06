package pdpTest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Scanner;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.attr.proxy.GeometryAttributeProxy;
import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.wso2.balana.Balana;
import org.wso2.balana.PDP;

/**
 * Class used for on the fly testing during development, WILL be changed very frequently 
 * @author malik
 */
public class TestClass {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Balana balana = Balana.getInstance();
		System.out.println("got instance of balana");
		
		balana.getAttributeFactory().addDatatype(GeometryAttribute.identifier, new GeometryAttributeProxy());
		System.out.println("extended balana with geometry attribute/datatype");
		
		GeoXACML.initialize();
		System.out.println("initialized geoxacml");
		
		PDP pdp = new PDP(balana.getPdpConfig());
		System.out.println("pdp loaded");
		
		LinkedList<String> reqs = loadRequests("/home/malik/workspace/balana/policies-request/geoxacml-policies/geoxacml-1-request-time-stamp-deny.xml");
		reqs.add(sampleRequest);
		System.out.println("requests loaded");
		
		for (String req : reqs) {
			try {
				System.out.println(pdp.evaluate(req));
			} catch (Exception e) { e.printStackTrace(); }						
		}
		
		System.out.println("finished");
	}
    
	private static LinkedList<String> loadRequests(String path) throws FileNotFoundException {
		LinkedList<String> reqs = new LinkedList<String>();		
		Scanner scanner;
		File file;
		
		file = new File(path);
		
		if (file.isDirectory()) {			
			for (String s : file.list())
				reqs.addAll(loadRequests(file.getPath() + "/" + s));
		}
		
		if (file.isFile()) {
			scanner = new Scanner(file);
			reqs.add(scanner.useDelimiter("\\A").next());
			scanner.close();
		}
		
		return reqs;
	}
	
	private static void singleLineFiles(String path) throws FileNotFoundException {		
		PrintWriter writer;
		Scanner scanner;		
		File file;
		String s;
		
		file = new File(path);
		
		if (file.isDirectory()) {
			for (String s1 : file.list())
				singleLineFiles(file.getPath() + "/" + s1);
		}
		
		if (file.isFile()) {
			scanner = new Scanner(file);
			s = scanner.useDelimiter("\\A").next().replaceAll(">\\s*", ">").replaceAll("\\s*<", "<");
			scanner.close();
			writer = new PrintWriter(file);
			writer.write(s);
			writer.flush();
			writer.close();
		}
		
	}
	
	private static String sampleRequest = 
    					"<Request xmlns='urn:oasis:names:tc:xacml:2.0:context:schema:os' "
    				+ "         xmlns:gml='http://www.opengis.net/gml' "
    				+ "         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
    				+ "         xsi:schemaLocation='urn:oasis:names:tc:xacml:2.0:context:schema:os http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd'>"
    				+ "    <Subject>"
    				+ "        <Attribute AttributeId='urn:oasis:names:tc:xacml:1.0:subject:subject-id' DataType='http://www.w3.org/2001/XMLSchema#string'>"
    				+ "            <AttributeValue>dhzb-arzt</AttributeValue>"
    				+ "        </Attribute>"
    				+ "		</Subject>"
    				+ "		<Resource>"
    				+ "        <Attribute AttributeId='urn:oasis:names:tc:xacml:1.0:resource:resource-id' DataType='http://www.w3.org/2001/XMLSchema#anyURI'>"
    				+ "            <AttributeValue>https://path/to/resource/maybe/with/complete/resource</AttributeValue>"
    				+ "        </Attribute>"
    				+ "    </Resource>"
    				+ "    <Action>"
    				+ "        <Attribute AttributeId='urn:oasis:names:tc:xacml:1.0:action:action-id' DataType='http://www.w3.org/2001/XMLSchema#string'>"
    				+ "            <AttributeValue>GET</AttributeValue>"
    				+ "        </Attribute>"
    				+ "    </Action>"
    				+ "    <Environment>"
    				+ "        <!-- defines the recent position // -->"
    				+ "        <Attribute AttributeId='position' DataType='urn:ogc:def:dataType:geoxacml:1.0:geometry'>"
    				+ "            <AttributeValue>"
    				+ "                <gml:Point srsName='EPSG:4326'>"
    				+ "                    <!-- position Campus Charottenburg //-->"
    				+ "                    <gml:coordinates>52.54317365064372,13.346108794212341</gml:coordinates>"
    				+ "                    <!-- position Campus Dovestr. "
    				+ "                        <gml:coordinates>52.51995523,13.32195282</gml:coordinates>"
    				+ "                    //-->"
    				+ "                    <!-- position Campus Ackerstr."
    				+ "                        <gml:coordinates>52.53885723,13.38465214</gml:coordinates>"
    				+ "                    //-->"
    				+ "                    <!-- position Hackescher Markt    "
    				+ "                        <gml:coordinates>52.522645,13.402234</gml:coordinates>"
    				+ "                    //-->"
    				+ "                </gml:Point>"
    				+ "            </AttributeValue>"
    				+ "        </Attribute>"
    				+ "    </Environment>"
    				+ "</Request>";

}
