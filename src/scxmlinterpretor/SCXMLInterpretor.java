package scxmlinterpretor; 

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.FinalState;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.PseudostateKind;
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
    private List<Vertex> states = new ArrayList<Vertex>();
    private List<Pseudostate> initialStates = new ArrayList<Pseudostate>();
    private XPath path;
    private UMLFactory umlFactory;
    private Model model;
    private StateMachine stateMachine;
    private Region mainRegion;
    private Element root;
    private Activity activity;   

    /**
     * 
     * @param name of the state
     * @return the state or null if not found
     */
    public Vertex getStateByName(String name) {
    	for(int i = 0; i < this.states.size(); i++) {
    		if(this.states.get(i).getName().equals(name)) {
    			return this.states.get(i);
    		}
    	}
    	return null;
    }
    
    /**
     * 
     * @param name of the parameter
     * @param actualStateMachine the state machine is parameter is into
     * @return the parameter or null if not found
     */
    public Parameter getParameterByName(String name, StateMachine actualStateMachine) {
    	for(int i = 0; i < actualStateMachine.getOwnedParameters().size(); i++) {
    		if(actualStateMachine.getOwnedParameters().get(i).getName().equals(name)) {
    			return actualStateMachine.getOwnedParameters().get(i);
    		}
    	}
    	return null;
    }
    
    /**
     * 
     * @param list of nodes
     * @return true if every node of the list if a Text node, return false otherwise
     */
    public boolean hasOnlyTextChildren(NodeList list) {
    	boolean textFounded = true;
    	for(int i = 0; i < list.getLength(); i++) {
    		if(!list.item(i).getNodeName().equals("#text")) {
    			textFounded = false;
    		}
    	}
    	return textFounded;
    }
    
    
    public void getInitialStates(Node n, Region mainRegion) {
    	if(n instanceof Element) {
    		Element element = (Element)n;

    		if(element.getTagName().equals("scxml") || (element.getTagName().equals("state") && !element.getAttribute("initial").equals(""))) {
    			Pseudostate newPseudostate = this.umlFactory.createPseudostate();
    			newPseudostate.setName(element.getAttribute("initial"));
    			this.initialStates.add(newPseudostate);
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

    /**
     * 
     * @param input the .xml or .scxml file name
     * @param output the .xmi file name
     */
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
	        this.model = this.umlFactory.createModel();
	        this.stateMachine = this.umlFactory.createStateMachine();
	        this.mainRegion = this.umlFactory.createRegion();
	        this.mainRegion.setName("Main");
	        
	        
	        	        
	        //Get Initial States Names
	        this.getInitialStates(this.root, this.mainRegion);
	        this.createStates();

	        // Create the body of the main container
	        System.out.println("Generating body...");
	        this.generateBody(this.root, this.mainRegion, this.stateMachine);
	        System.out.println("Generation of body has successfully ended.\n");

	        this.stateMachine.getRegions().add(this.mainRegion);
	        this.model.getPackagedElements().add(this.stateMachine);
	        
	        
	        
	        System.out.println("Starting writing to " + output + "...");
	        if(saveXMI(this.model, output)) {
	        	System.out.println("Successfully exported to .xmi (Saved to " + output + ") !");
	        }

	    } catch ( ParserConfigurationException | SAXException | IOException | XPathExpressionException e)  {
	    	System.out.println(e.getMessage() + "\nClosing program...");
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
     * Populate the states List with all the states from the scxml file
     */
    public void createStates() throws XPathExpressionException {
    	
    	System.out.println(this.initialStates);
    	System.out.println("Generating all states...");
    	// |// FINAL !!
    	String expression = "//state | //parallel | //final";
    	NodeList list = (NodeList)this.path.evaluate(expression, this.root, XPathConstants.NODESET);
    	for(int i = 0; i < list.getLength(); i++) {
    		if(list.item(i) instanceof Element) {
    			Element el = (Element)list.item(i);
    			Vertex state;
    			
    			System.out.println("\tGenerating " + el.getAttribute("id"));
    			
    			
    			if(el.getTagName().equals("final")) {
    				state = this.umlFactory.createFinalState();
    			} else {
    				state = this.umlFactory.createState();
    			}

    			// Name attribute
    			String name = el.getAttribute("id");
    			if(!name.equals("")) {
    				state.setName(name);
    			}
    			
    			for(int j = 0; j < this.initialStates.size(); j++) {
    				if(this.initialStates.get(j).getName().equals(el.getAttribute("id")) && state instanceof State) {
    					this.initialStates.get(j).setState((State)state);
    				}
    			}
    			
    			this.states.add(state);
    		}
    	}
    	System.out.println("Generation of all states has successfully ended.\n");
    }


    /**
     * 
     * @param n the actual node
     * @param mainRegion the containing region
     * @param actualStateMachine the containing state machine
     */
    public void generateBody(Node n, Region mainRegion, StateMachine actualStateMachine){
    	if(n instanceof Element){
    		Element element = (Element)n;
    		
    		// STATE
    		if(element.getTagName().equals("state")) {	
    			State newState = null;
    			Vertex resultState = this.getStateByName(element.getAttribute("id"));
    			newState = (State)resultState;
    			
    			System.out.println("\tGenerating state " + element.getAttribute("id"));
    			
            	this.getStateByName(element.getAttribute("id")).setContainer(mainRegion);
            	
            	// Look if there is any child
            	int nbChild = n.getChildNodes().getLength();
            	NodeList list = n.getChildNodes();
            	boolean areChildrenOnlyText = this.hasOnlyTextChildren(list);
            	if(nbChild > 0 && !areChildrenOnlyText) {
            		Region localRegion = this.umlFactory.createRegion();
                	localRegion.setState(newState);
                	for(int i = 0; i < nbChild; i++){
                		Node n2 = list.item(i);
                		
                		if (n2 instanceof Element && !n2.getNodeName().equals("#text")){
                			generateBody(n2, localRegion, actualStateMachine);
                		}
                	}	
    			}
            	
            // SCXML 
    		} else if(element.getTagName().equals("scxml")) {
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++){
    					Node n2 = list.item(i);
    					if (n2 instanceof Element){
    						generateBody(n2, mainRegion, actualStateMachine);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("final")) {
    			FinalState finalState = (FinalState)this.getStateByName(element.getAttribute("id"));
    			finalState.setName(element.getAttribute("id"));
    			finalState.setContainer(mainRegion);
    			System.out.println("\tGenerating final state " + element.getAttribute("id"));
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0 ) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, mainRegion, actualStateMachine);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("transition")) {
    			Transition tr = this.umlFactory.createTransition();
    			tr.setContainer(mainRegion);
    			
    			tr.setName(element.getAttribute("event"));
    			
    			
    			    			    			
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
    			
    			// Attribute cond
    			String cond = element.getAttribute("cond");
    			if(!cond.equals("")) {
    				Constraint c = this.umlFactory.createConstraint();
        			LiteralString s = this.umlFactory.createLiteralString();
        			
        			s.setValue(cond);
        			c.setSpecification(s);
        			System.out.println(c.getSpecification().stringValue());
        			tr.setGuard(c);     			
    			}
    			
    			System.out.println("\tGenerating transition from " + source + " to " + target);
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, mainRegion, actualStateMachine);
    					}
    				}
    			}
    		} else if(element.getTagName().equals("parallel")) {
    			Vertex vt = this.getStateByName(element.getAttribute("id"));
    			State state = null;
    			Pseudostate pseudostate = null;
    			Region localRegion = this.umlFactory.createRegion();
    			StateMachine subStateMachine = this.umlFactory.createStateMachine();
    			subStateMachine.getRegions().add(localRegion);
    			subStateMachine.setName(element.getAttribute("id"));
    			
    			this.model.getPackagedElements().add(subStateMachine);
    			
    			if(vt instanceof State) {
    				state = (State)vt;
    				state.setContainer(mainRegion);
    				state.setSubmachine(subStateMachine);
    			} else {
    				pseudostate = (Pseudostate)vt;
    				pseudostate.setContainer(mainRegion);
    			}
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, localRegion, subStateMachine);
    					}
    				}
    			}
    		
    		} else if(element.getTagName().equals("history")) {
    			String kind = element.getAttribute("type");
    			Pseudostate history = this.umlFactory.createPseudostate();
    			
    			// By default, the kind is "shallow"
    			if(kind.equals("")) {
    				kind = "shallow";
    			}
    			
    			if(kind.equals("shallow")) {
    				history.setKind(PseudostateKind.SHALLOW_HISTORY_LITERAL);
    			} else {
    				history.setKind(PseudostateKind.DEEP_HISTORY_LITERAL);
    			}
    			
    			history.setContainer(mainRegion);
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				Region localRegion = this.umlFactory.createRegion();
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, localRegion, actualStateMachine);
    					}
    				}
    			}
    		} else if(element.getNodeName().equals("datamodel")) {
    			this.activity = this.umlFactory.createActivity();
    			this.activity.setPackage(mainRegion.getNearestPackage());
    			
    			NodeList list = n.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					if(n2 instanceof Element) {
    						generateBody(n2, mainRegion, actualStateMachine);
    					}
    				}
    			}
    			
    		} else if(element.getNodeName().equals("data")) {
    			if(this.activity != null) {
    				Parameter param = this.umlFactory.createParameter();
    				param.setName(element.getAttribute("id"));
    				String value = element.getAttribute("expr");
    				System.out.println("\tGenerating parameter " + param.getName());
    				if(value != "") {
    					if(value.charAt(0) == '\'' && value.charAt(value.length()-1) == '\'') {
    						// String case
    						value = value.substring(1, value.length()-1);
    						param.setDefault(value);

    					} else if(value.equals("null")) {
    						// null case
    						param.setNullDefaultValue();
    					} else if(value.equals("false") || value.equals("true")) {
    						// boolean case
    						param.setBooleanDefaultValue(Boolean.valueOf(value));
    					} else if(Integer.valueOf(value) != null) {
    						// integer case
    						param.setIntegerDefaultValue(Integer.valueOf(value));
    					} 
    				} else {
						param.setDefault("");
					}
    				actualStateMachine.getOwnedParameters().add(param);
    			}
    		} else if(element.getNodeName().equals("assign")) {
    			   			
    		} else if(element.getNodeName().equals("onentry")) {
    			NodeList list = element.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					generateBody(n2, mainRegion, actualStateMachine);
    				}
    			}
    		} else if(element.getTagName().equals("onexit")) {
    			//State parent = (State)this.getStateByName(element.getParentNode().getAttributes().getNamedItem("id").getNodeValue());
    			NodeList list = element.getChildNodes();
    			if(list.getLength() > 0) {
    				for(int i = 0; i < list.getLength(); i++) {
    					Node n2 = list.item(i);
    					generateBody(n2, mainRegion, actualStateMachine);
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

            if(outputFilePath == null && inputFilePath.indexOf(".xml") != -1) {
            	outputFilePath = inputFilePath.substring(0, inputFilePath.indexOf(".xml")) + ".xmi";
            }
            if(outputFilePath.indexOf(".xmi") == -1) {
            	System.out.println("Missing .xmi extension for the output file.\nClosing program...");
            	System.exit(1);
            }
            if(inputFilePath.indexOf(".xml") == -1 && inputFilePath.indexOf(".scxml") == -1) {
            	System.out.println("Missing .xml or .scxml extension for the input file.\nClosing program...");
            	System.exit(1);
            }
            
            new SCXMLInterpretor(inputFilePath, outputFilePath);
            
    	} catch(ParseException e) {
    		System.out.println(e.getMessage());
    		formatter.printHelp("SCXMLInterpretor", options);
    		System.exit(1);
    	}
    }
}
