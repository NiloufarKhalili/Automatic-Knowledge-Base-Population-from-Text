package KBExtraction;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class KBExtraction {
	public static Map<String, String> entityMap = new HashMap<String, String>();
	public static ArrayList<ArticleSlots> slots = new ArrayList<ArticleSlots>();

	public static void main(String[] args) throws Exception {
		String filePath = "C:/Users/Niloufar/Desktop/sampleKB4.xml";
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true); 
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		Document doc = builder.parse(filePath);

		getEntityMap(doc);

		for (String entityId : entityMap.keySet()) {
			ArticleSlots a = readAnEnity(entityId, doc);
			if (a != null) {
				slots.add(a);
			}
		}
		// FeatureFactory f = new FeatureFactory();
		System.out.println(FeatureFactory.set);
		for (List<String> list : FeatureFactory.listlist) {
			ArrayList<String> arrayList = (ArrayList<String>) FeatureFactory
					.checkListAndSet(FeatureFactory.set, list);
			System.out.println("");
			for (String s : arrayList) {
				System.out.print(s + ",");
			}
		}
		generateArffForAFile(slots);
		
	}

	public static ArticleSlots readAnEnity(String entityId, Document doc) {
		try {

			XPathFactory factory = XPathFactory.newInstance();

			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile("knowledge_base//entity[@id='"
					+ entityId + "']/facts/fact");
			
			NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			Map<String, String> map = new HashMap<String, String>();
			for (int i = 0; i < nl.getLength(); i++) {
				String slot = nl.item(i).getAttributes().getNamedItem("name")
						.getNodeValue();
				String slotText = nl.item(i).getTextContent();
				map.put(slot, slotText);
			}
			String entity = entityMap.get(entityId);
			map.put("name", entityMap.get(entityId));
			System.out.println("entity read successfully");
			XPath xpath2 = factory.newXPath();

			String entityContent = (String) xpath2
					.evaluate("knowledge_base//entity[@id='" + entityId
							+ "']/wiki_text/text()", doc, XPathConstants.STRING);
			
			entityContent = entityContent.substring(entity.length()+1);

			openNLPNER ner = new openNLPNER();

			ArticleSlots a = ner.mainMethod(entityContent, map);

			// generateArff(a.LocationFeaures,"location.arff");
			// generateArff(a.DateOfBithFeatures, "dob.arff");
			return a;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	private static void printLoop(ArrayList<String> collection) {
		for (String c : collection) {
			System.out.println(c);
		}
		System.out.println("");
	}

	public static void loopList(ArrayList<String> list, String name) {
		System.out.println(name);
		for (String s : list) {
			System.out.println(s);
		}
	}

	public static void getEntityMap(Document doc) {

		try {
			NodeList nList = doc.getElementsByTagName("entity");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					entityMap.put(eElement.getAttribute("id"),
							eElement.getAttribute("name"));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void generateArffForAFile(ArrayList<ArticleSlots> slots) {

		BufferedWriter br = createFileHeader("location.arff");

		// // System.out.println(slots.size());

		BufferedWriter br2 = createFileHeader("dob.arff");
		 for (ArticleSlots a : slots) {
			 generateArffLines(br2, a.DateOfBithFeatures);
		 }
		 
		for (ArticleSlots a : slots) {
			generateArffLines(br, a.LocationFeaures);
		}
		try {
			br.close();
			br2.close();
		} catch (IOException e) {
			System.out.println("Error:" + e.getMessage());
		}
	}


	public static void generateArffLines(BufferedWriter br,
			ArrayList<ExtractedFeature> features) {

		try {

			for (ExtractedFeature f : features) {

				if (f.WordsBetween != null) {
					ArrayList<String> lf = (ArrayList<String>) FeatureFactory
							.checkListAndSet(FeatureFactory.set, f.WordsBetween);
					br.newLine();
					br.write("{");
//					for (String item : lf) {
//						
//						br.write(item + ",");
//					}
					for(int index = 0 ; index < lf.size(); index++){
						if(lf.get(index)!= "0"){
							br.write(index+" 1,");
						}
					}
					int size = lf.size();
					br.write(size+" "+(f.HasLocationBetween ?"\"yes\"" : "\"no\"" )+ ",");
					br.write(size+1+" "+(f.HasDateBetween ? "\"yes\"" : "\"no\"" )+ ",");
					br.write(size+2+" "+(f.IsEntityMentionedFirst ? "\"yes\"" : "\"no\"")+ ",");
					br.write(size+3+" "+f.WordsBetween.size() + ",");
					br.write(size+4+" "+(f.IsPositive ? "\"Positive\"" : "\"Negative\""));
					br.write("}");
					}
				

			}

		} catch (IOException e) {
			System.out.println("error");
			System.out.println(e.getMessage());
		}
	}

	private static BufferedWriter createFileHeader(String fileName) {
		File file = new File(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(file));

			if (fileName.equals("dob.arff")) {
				br.write("@relation DateOfBirth");

			} else {
				br.write("@relation CountryOfBirth");

			}
			br.newLine();
			for (String attribute : FeatureFactory.set) {
				br.newLine();
				br.write("@attribute " + attribute + "  {'0','1'}");
			}
			
			br.newLine();
			br.write("@attribute isLocationBetween {'yes','no'}");
			br.newLine();
			br.write("@attribute isDateBetween {'yes','no'}");
			br.newLine();
			br.write("@attribute isEntityMentionedFirst {'yes','no'}");
			br.newLine();
			br.write("@attribute numberOfWordsBetween numeric");
			br.newLine();
			br.write("@attribute isPositiveSample {'Positive','Negative'}");
			br.newLine();
			br.newLine();
			br.write("@data");
			br.newLine();

			return br;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return null;
		}

	}
}
