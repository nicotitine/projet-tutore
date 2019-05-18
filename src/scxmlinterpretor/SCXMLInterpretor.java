package scxmlinterpretor; 

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.FinalState;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Region;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.TransitionKind;
import org.eclipse.uml2.uml.internal.impl.UMLFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SCXMLInterpretor {
	private Set<State> states = new HashSet<State>();
    private XPath path;
    private UMLFactory umlFactory;
    private Model model;
    private StateMachine stateMachine;
    private Region mainRegion;
    private Element root;

    public State getStateByName(String name) {
    	Iterator<State> it = this.states.iterator();
    	while(it.hasNext()) {
    		State state = (State) it.next();
    		if(state.getName().equals(name)) {
    			return state;
    		}
    	}
    	return null;
    }

    public SCXMLInterpretor(String input, String output) {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	factory.setIgnoringComments(true);
    	factory.setIgnoringElementContentWhitespace(true);

	    try {
	    	// SCXML Loading
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	        File fileXML = new File(input);
	        Document xml = builder.parse(fileXML);
	        this.root = xml.getDocumentElement();
	        XPathFactory xpf = XPathFactory.newInstance();
	        this.path = xpf.newXPath();

	        // UML Init
	        this.umlFactory = new UMLFactoryImpl();
	        this.model = umlFactory.createModel();
	        this.stateMachine = umlFactory.createStateMachine();
	        
	        this.mainRegion = umlFactory.createRegion();

	        // Create all states
	        this.createStates(this.root, this.mainRegion);

	        this.stateMachine.getRegions().add(this.mainRegion);
	        this.model.getPackagedElements().add(this.stateMachine);

	        // Create all transitions
	        this.createTransitions(this.mainRegion);
	        
	        System.out.println("Starting writing to " + output + "...");
	        if(saveXMI((EObject) this.model, output)) {
	        	System.out.println("Successfully exported to .xmi (Saved to " + output + ") !");
	        }

	    } catch ( ParserConfigurationException | SAXException | IOException | XPathExpressionException e)  {
	    	e.printStackTrace();
	    }
	}

    public Boolean saveXMI(EObject root, String uri) {
    	Resource resource = null;
		try {
			URI uriUri = URI.createURI(uri);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			resource = (new ResourceSetImpl()).createResource(uriUri);
			resource.getContents().add((EObject)root);
			resource.save(null);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

    public void createTransitions(Region umlRegion) throws XPathExpressionException{

    	String expression = "//transition";
    	NodeList list = (NodeList)this.path.evaluate(expression, this.root, XPathConstants.NODESET);

    	for(int i = 0; i < list.getLength(); i++) {

    		if(list.item(i) instanceof Element) {
    			Element el = (Element)list.item(i);
    			Transition tr = this.umlFactory.createTransition();

    			// Target attribute
    			String target = el.getAttribute("target");
    			if(!target.equals("")) {
    				tr.setTarget(this.getStateByName(target));
    			} else {
    				target = el.getParentNode().getAttributes().getNamedItem("id").getNodeValue();
    				tr.setTarget(this.getStateByName(target));
    				tr.setKind(TransitionKind.INTERNAL_LITERAL);
    			}

    			// Source attribute
    			String source = el.getAttribute("source");
    			if(!source.equals("")) {
    				tr.setSource(this.getStateByName(source));
    			} else {
    				source = el.getParentNode().getAttributes().getNamedItem("id").getNodeValue();
    				tr.setSource(this.getStateByName(source));
    			}

    			umlRegion.getTransitions().add(tr);
    		}
    	}
    }

    public void createStates(Node n, Region mainRegion){
    	if(n instanceof Element){
    		Element element = (Element)n;
    		if(element.getTagName().equals("state") || element.getTagName().equals("parallel")) {

    			State newState = umlFactory.createState();
    			newState.setName(element.getAttribute("id"));
    			newState.setContainer(mainRegion); 
    			this.states.add(newState);

    			// Look if there is any child
    			int nbChild = n.getChildNodes().getLength();
    			NodeList list = n.getChildNodes();
    			if(nbChild > 0) {
    				Region localRegion = this.umlFactory.createRegion();
    				localRegion.setState(newState);
    				for(int i = 0; i < nbChild; i++){
    					Node n2 = list.item(i);
    					if (n2 instanceof Element){
    						createStates(n2, localRegion);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("scxml")) {
    			//String idInitialState = element.getAttribute("initial");
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++){
    					Node n2 = list.item(i);
    					if (n2 instanceof Element){
    						createStates(n2, mainRegion);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("final")) {
    			FinalState finalState = this.umlFactory.createFinalState();
    			finalState.setName(element.getAttribute("id"));
    			finalState.setContainer(mainRegion);
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0 ) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						createStates(n2, mainRegion);
    					}
    				}
    			}
    		}
    	}
    }

    public static void main(String[] args) {
    	// Handle CLI arguments
    	Options options = new Options();
    	Option input = new Option("p", "path", true, "input SCXML file path");
    	input.setRequired(true);
    	options.addOption(input);

    	Option output = new Option("o", "output", true, "output XMI file path");
    	output.setRequired(false);
    	options.addOption(output);

    	CommandLineParser parser = new DefaultParser();
    	HelpFormatter formatter = new HelpFormatter();
    	CommandLine cmd;

    	try {
    		cmd = parser.parse(options, args);
    		String inputFilePath = cmd.getOptionValue("path");
            String outputFilePath = cmd.getOptionValue("output");

            //TO DO : G�rer le cas ou l'utilisateur donne un output autre que .xmi
            if(outputFilePath == null) {
            	outputFilePath = inputFilePath.substring(0, inputFilePath.indexOf(".xml")) + ".xmi";
            }
            new SCXMLInterpretor(inputFilePath, outputFilePath);
    	} catch(ParseException e) {
    		System.out.println(e.getMessage());
    		formatter.printHelp("SCXMLInterpretor", options);
    		System.exit(1);
    	}
    }
}