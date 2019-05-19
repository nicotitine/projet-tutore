package scxmlinterpretor; 

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.Region;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.Vertex;
import org.eclipse.uml2.uml.internal.impl.UMLFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SCXMLInterpretor {
    private Set<Vertex> states = new HashSet<Vertex>();
    private List<String> nameOfInitialStates = new ArrayList<String>();
    private XPath path;
    private UMLFactory umlFactory;
    private Model model;
    private StateMachine stateMachine;
    private Region mainRegion;
    private Element root;

    public Vertex getStateByName(String name) {
    	Iterator<Vertex> it = this.states.iterator();
    	while(it.hasNext()) {
    		Vertex vt = it.next();
    		if(vt.getName() != null && vt.getName().equals(name)) {
    			return vt;
    		}
    	}
    	return null;
    }
    
    public void getInitialStates(Node n, Region mainRegion) {
    	if(n instanceof Element) {
    		Element element = (Element)n;

    		if(element.getTagName().equals("scxml")) {
    			this.nameOfInitialStates.add(element.getAttribute("initial"));
    		} else if(element.getTagName().equals("state")){
    			if(!element.getAttribute("initial").equals("")) {
    				this.nameOfInitialStates.add(element.getAttribute("initial"));
    			}
    		}

    		NodeList list = n.getChildNodes();
			if(list.getLength() > 0 ) {
				for(int i = 0; i < list.getLength(); i++) {
					Node n2 = list.item(i);
					if(n2 instanceof Element) {
						getInitialStates(n2, mainRegion);
					}
				}
			}
    	}
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
	        this.mainRegion.setName("Main");
	        
	        //Get Initial States Names
	        this.getInitialStates(this.root, this.mainRegion);
	        this.createStates();

	        // Create the body of the main container
	        System.out.println("Generating body...");
	        this.generateBody(this.root, this.mainRegion);
	        System.out.println("Generation of body has successfully ended.\n");

	        this.stateMachine.getRegions().add(this.mainRegion);
	        this.model.getPackagedElements().add(this.stateMachine);

	        
	        System.out.println("Starting writing to " + output + "...");
	        if(saveXMI(this.model, output)) {
	        	System.out.println("Successfully exported to .xmi (Saved to " + output + ") !");
	        }

	    } catch ( ParserConfigurationException | SAXException | IOException | XPathExpressionException e)  {
	    	e.printStackTrace();
	    }
	}

    /**
     * 
     * @param root
     * @param uri
     * @return true if successfully saved, false if an exception is throwed
     */
    public Boolean saveXMI(EObject root, String uri) {
    	Resource resource = null;
		try {
			URI uriUri = URI.createURI(uri);
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			resource = (new ResourceSetImpl()).createResource(uriUri);
			resource.getContents().add(root);
			resource.save(null);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
    
    /**
     * 
     * Populate the states Set with all the states of the scxml file
     */
    public void createStates() throws XPathExpressionException {
    	
    	System.out.println("Generating all states...");
    	String expression = "//state | //parallel";
    	NodeList list = (NodeList)this.path.evaluate(expression, this.root, XPathConstants.NODESET);
    	for(int i = 0; i < list.getLength(); i++) {
    		if(list.item(i) instanceof Element) {
    			Element el = (Element)list.item(i);
    			Vertex state;
    			
    			System.out.println("\tGenerating " + el.getAttribute("id"));
    			
    			//Initial state
    			if(this.nameOfInitialStates.contains(el.getAttribute("id"))) {
    				state = this.umlFactory.createPseudostate();
    			} else {
    				state = this.umlFactory.createState();
    			}

    			// Name attribute
    			String name = el.getAttribute("id");
    			if(!name.equals("")) {
    				state.setName(name);
    			}
    			this.states.add(state);
    		}
    	}
    	System.out.println("Generation of all states has successfully ended.\n");
    }



    public void generateBody(Node n, Region mainRegion){
    	if(n instanceof Element){
    		Element element = (Element)n;
    		if(element.getTagName().equals("state") || element.getTagName().equals("parallel")) {	
    			State newState = null;
    			Pseudostate newPseudostate = null;
    			Vertex resultState = this.getStateByName(element.getAttribute("id"));
    			if(resultState instanceof State) {
    				newState = (State)resultState;
    				System.out.println("\tGenerating state " + element.getAttribute("id"));
    			} else if(resultState instanceof Pseudostate) {
    				System.out.println("\tGenerating initial state " + element.getAttribute("id"));
    				newPseudostate = (Pseudostate)resultState;
    			}
            	this.getStateByName(element.getAttribute("id")).setContainer(mainRegion);

            	// Look if there is any child
            	int nbChild = n.getChildNodes().getLength();
            	NodeList list = n.getChildNodes();
            	if(nbChild > 0) {
            		Region localRegion = this.umlFactory.createRegion();
            		if(resultState instanceof State) {
        				localRegion.setState(newState);
        			} else if(resultState instanceof Pseudostate) {
        				localRegion.setState(newPseudostate.getState());
        			}
            		for(int i = 0; i < nbChild; i++){
            			Node n2 = list.item(i);
            			if (n2 instanceof Element){
            				generateBody(n2, localRegion);
            			}
            		}	
    			}
    		} else if(element.getTagName().equals("scxml")) {
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++){
    					Node n2 = list.item(i);
    					if (n2 instanceof Element){
    						generateBody(n2, mainRegion);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("final")) {
    			FinalState finalState = this.umlFactory.createFinalState();
    			finalState.setName(element.getAttribute("id"));
    			finalState.setContainer(mainRegion);
    			System.out.println("\tGenerating final state " + element.getAttribute("id"));
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0 ) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, mainRegion);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("transition")) {
    			Transition tr = this.umlFactory.createTransition();
    			tr.setContainer(mainRegion);
    			
    			tr.setName(element.getAttribute("event"));
    			tr.createTrigger(element.getAttribute("event"));
    			    			    			
    			// Attribute source
    			String source = element.getAttribute("source");
    			if(!source.equals("")) {
    				tr.setSource(this.getStateByName(source));
    			} else {
    				source = element.getParentNode().getAttributes().getNamedItem("id").getNodeValue();
    				tr.setSource(this.getStateByName(source));
    			}
    			
    			// Attribute target
    			String target = element.getAttribute("target");
    			if(!target.equals("")) {
    				tr.setTarget(this.getStateByName(target));
    			} else {
    				target = element.getParentNode().getAttributes().getNamedItem("id").getNodeValue();
    				tr.setTarget(this.getStateByName(target));
    			}
    			
    			System.out.println("\tGenerating transition from " + source + " to " + target);
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, mainRegion);
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

            //TO DO : Gérer le cas ou l'utilisateur donne un output autre que .xmi
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
