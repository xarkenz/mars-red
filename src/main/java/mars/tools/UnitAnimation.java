package mars.tools;

import mars.Application;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;


class UnitAnimation extends JPanel {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2681757800180958534L;

    //config variables
    private static final int PERIOD = 8;    // velocity of frames in ms
    private static final int PWIDTH = 1000;     // size of this panel
    private static final int PHEIGHT = 574;
    private final GraphicsConfiguration gc;

    private int counter;            //verify then remove.

    private Vector<Vector<MipsXray.Vertex>> outputGraph;
    private final ArrayList<MipsXray.Vertex> vertexList;
    private ArrayList<MipsXray.Vertex> traversedVertices;
    //Screen Label variables

    private final HashMap<String, String> registerEquivalenceTable;

    private String instructionCode;


    private final MipsXray.DatapathUnit datapathTypeUsed;

    private Graphics2D g2d;

    private BufferedImage datapath;

    public UnitAnimation(String instructionBinary, MipsXray.DatapathUnit datapathType) {
        datapathTypeUsed = datapathType;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

        setBackground(Color.white);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

        // load and initialise the images
        initImages();

        vertexList = new ArrayList<>();
        counter = 0;
        instructionCode = instructionBinary;

        //declaration of labels definition.
        registerEquivalenceTable = new HashMap<>();

        loadHashMapValues();


    } // end of ImagesTests()

    //set the binnary opcode value of the basic instructions of MIPS instruction set
    public void loadHashMapValues() {
        if (datapathTypeUsed == MipsXray.DatapathUnit.REGISTER) {
            importXmlStringData("/registerDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
            importXmlDatapathMap("/registerDatapath.xml", "datapath_map");
        } else if (datapathTypeUsed == MipsXray.DatapathUnit.CONTROL) {
            importXmlStringData("/controlDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
            importXmlDatapathMap("/controlDatapath.xml", "datapath_map");
        } else if (datapathTypeUsed == MipsXray.DatapathUnit.ALU_CONTROL) {
            importXmlStringData("/ALUcontrolDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
            importXmlDatapathMapAluControl("/ALUcontrolDatapath.xml", "datapath_map");
        }
    }

    //import the list of opcodes of mips set of instructions
    public void importXmlStringData(String xmlName, HashMap<String, String> table, String elementTree, String tagId, String tagData) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder;
        try {
            //System.out.println();
            docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(Objects.requireNonNull(getClass().getResource(xmlName)).toString());
            Element root = doc.getDocumentElement();
            Element equivalenceItem;
            NodeList bitsList, mnemonic;
            NodeList equivalenceList = root.getElementsByTagName(elementTree);
            for (int i = 0; i < equivalenceList.getLength(); i++) {
                equivalenceItem = (Element) equivalenceList.item(i);
                bitsList = equivalenceItem.getElementsByTagName(tagId);
                mnemonic = equivalenceItem.getElementsByTagName(tagData);
                for (int j = 0; j < bitsList.getLength(); j++) {
                    table.put(bitsList.item(j).getTextContent(), mnemonic.item(j).getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    //import the parameters of the animation on datapath
    public void importXmlDatapathMap(String xmlName, String elementTree) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(Objects.requireNonNull(getClass().getResource(xmlName)).toString());
            Element root = doc.getDocumentElement();
            Element datapath_mapItem;
            NodeList index_vertex, name, init, end, color, other_axis, isMovingXaxis, targetVertex, isText;
            NodeList datapath_mapList = root.getElementsByTagName(elementTree);
            for (int i = 0; i < datapath_mapList.getLength(); i++) { //extract the vertex of the xml input and encapsulate into the vertex object
                datapath_mapItem = (Element) datapath_mapList.item(i);
                index_vertex = datapath_mapItem.getElementsByTagName("num_vertex");
                name = datapath_mapItem.getElementsByTagName("name");
                init = datapath_mapItem.getElementsByTagName("init");
                end = datapath_mapItem.getElementsByTagName("end");
                //definition of colors line 

                String opcode = instructionCode.substring(0, 6);
                if (opcode.equals("000000")) {//R-type instructions
                    color = datapath_mapItem.getElementsByTagName("color_Rtype");
                    //System.out.println("rtype");
                } else if (opcode.matches("00001[0-1]")) { //J-type instructions
                    color = datapath_mapItem.getElementsByTagName("color_Jtype");
                    //System.out.println("jtype");
                } else if (opcode.matches("100[0-1][0-1][0-1]")) { //LOAD type instructions
                    color = datapath_mapItem.getElementsByTagName("color_LOADtype");
                    //System.out.println("load type");
                } else if (opcode.matches("101[0-1][0-1][0-1]")) { //LOAD type instructions
                    color = datapath_mapItem.getElementsByTagName("color_STOREtype");
                    //System.out.println("store type");
                } else if (opcode.matches("0001[0-1][0-1]")) { //BRANCH type instructions
                    color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
                    //System.out.println("branch type");
                } else { //BRANCH type instructions
                    color = datapath_mapItem.getElementsByTagName("color_Itype");
                    //System.out.println("immediate type");
                }


                other_axis = datapath_mapItem.getElementsByTagName("other_axis");
                isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
                targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
                isText = datapath_mapItem.getElementsByTagName("is_text");

                for (int j = 0; j < index_vertex.getLength(); j++) {
                    MipsXray.Vertex vert = new MipsXray.Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer.parseInt(init.item(j).getTextContent()),
                            Integer.parseInt(end.item(j).getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()),
                            Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j).getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(isText.item(j).getTextContent()));
                    vertexList.add(vert);
                }
            }
            //loading matrix of control of vertex.
            outputGraph = new Vector<>();
            traversedVertices = new ArrayList<>();
            MipsXray.Vertex vertex;
            ArrayList<Integer> targetList;
            for (MipsXray.Vertex value : vertexList) {
                vertex = value;
                targetList = vertex.targetVertices;
                Vector<MipsXray.Vertex> vertexOfTargets = new Vector<>();
                for (Integer integer : targetList) {
                    vertexOfTargets.add(vertexList.get(integer));
                }
                outputGraph.add(vertexOfTargets);
            }

            vertexList.get(0).isActive = true;
            traversedVertices.add(vertexList.get(0));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }


    public void importXmlDatapathMapAluControl(String xmlName, String elementTree) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(Objects.requireNonNull(getClass().getResource(xmlName)).toString());
            Element root = doc.getDocumentElement();
            Element datapath_mapItem;
            NodeList index_vertex, name, init, end, color, other_axis, isMovingXaxis, targetVertex, isText;
            NodeList datapath_mapList = root.getElementsByTagName(elementTree);
            for (int i = 0; i < datapath_mapList.getLength(); i++) { //extract the vertex of the xml input and encapsulate into the vertex object
                datapath_mapItem = (Element) datapath_mapList.item(i);
                index_vertex = datapath_mapItem.getElementsByTagName("num_vertex");
                name = datapath_mapItem.getElementsByTagName("name");
                init = datapath_mapItem.getElementsByTagName("init");
                end = datapath_mapItem.getElementsByTagName("end");
                //definition of colors line 

                String opcode = instructionCode.substring(0, 6);
                if (opcode.equals("000000")) {//R-type instructions
                    String func = instructionCode.substring(28, 32);
                    if (func.matches("0000")) { //BRANCH type instructions
                        color = datapath_mapItem.getElementsByTagName("ALU_out010");
                        System.out.println("ALU_out010 type " + func);
                    } else if (func.matches("0010")) { //BRANCH type instructions
                        color = datapath_mapItem.getElementsByTagName("ALU_out110");
                        System.out.println("ALU_out110 type " + func);
                    } else if (func.matches("0100")) { //BRANCH type instructions
                        color = datapath_mapItem.getElementsByTagName("ALU_out000");
                        System.out.println("ALU_out000 type " + func);
                    } else if (func.matches("0101")) { //BRANCH type instructions
                        color = datapath_mapItem.getElementsByTagName("ALU_out001");
                        System.out.println("ALU_out001 type " + func);
                    } else { //BRANCH type instructions
                        color = datapath_mapItem.getElementsByTagName("ALU_out111");
                        System.out.println("ALU_out111 type " + func);
                    }
                } else if (opcode.matches("00001[0-1]")) { //J-type instructions
                    color = datapath_mapItem.getElementsByTagName("color_Jtype");
                    System.out.println("jtype");
                } else if (opcode.matches("100[0-1][0-1][0-1]")) { //LOAD type instructions
                    color = datapath_mapItem.getElementsByTagName("color_LOADtype");
                    System.out.println("load type");
                } else if (opcode.matches("101[0-1][0-1][0-1]")) { //LOAD type instructions
                    color = datapath_mapItem.getElementsByTagName("color_STOREtype");
                    System.out.println("store type");
                } else if (opcode.matches("0001[0-1][0-1]")) { //BRANCH type instructions
                    color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
                    System.out.println("branch type");
                } else {
                    color = datapath_mapItem.getElementsByTagName("color_Itype");
                    System.out.println("immediate type");
                }


                other_axis = datapath_mapItem.getElementsByTagName("other_axis");
                isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
                targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
                isText = datapath_mapItem.getElementsByTagName("is_text");

                for (int j = 0; j < index_vertex.getLength(); j++) {
                    MipsXray.Vertex vert = new MipsXray.Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer.parseInt(init.item(j).getTextContent()),
                            Integer.parseInt(end.item(j).getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()),
                            Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j).getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(isText.item(j).getTextContent()));
                    vertexList.add(vert);
                }
            }
            //loading matrix of control of vertex.
            outputGraph = new Vector<>();
            traversedVertices = new ArrayList<>();
            MipsXray.Vertex vertex;
            ArrayList<Integer> targetList;
            for (MipsXray.Vertex value : vertexList) {
                vertex = value;
                targetList = vertex.targetVertices;
                Vector<MipsXray.Vertex> vertexOfTargets = new Vector<>();
                for (Integer integer : targetList) {
                    vertexOfTargets.add(vertexList.get(integer));
                }
                outputGraph.add(vertexOfTargets);
            }

            vertexList.get(0).isActive = true;
            traversedVertices.add(vertexList.get(0));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

    //set the initial state of the variables that controls the animation, and start the timer that triggers the animation. 
    public void startAnimation(String codeInstruction) {
        instructionCode = codeInstruction;
        new Timer(PERIOD, event -> repaint()).start();    // start timer
        this.repaint();
    }

    //initialize the image of datapath.
    private void initImages() {
        try {
            BufferedImage im;
            if (datapathTypeUsed == MipsXray.DatapathUnit.REGISTER) {
                im = ImageIO.read(
                        Objects.requireNonNull(getClass().getResource(Application.IMAGES_PATH + "register.png")));
            } else if (datapathTypeUsed == MipsXray.DatapathUnit.CONTROL) {
                im = ImageIO.read(
                        Objects.requireNonNull(getClass().getResource(Application.IMAGES_PATH + "control.png")));
            } else if (datapathTypeUsed == MipsXray.DatapathUnit.ALU_CONTROL) {
                im = ImageIO.read(
                        Objects.requireNonNull(getClass().getResource(Application.IMAGES_PATH + "ALUcontrol.png")));
            } else {
                im = ImageIO.read(
                        Objects.requireNonNull(getClass().getResource(Application.IMAGES_PATH + "alu.png")));
            }

            int transparency = im.getColorModel().getTransparency();
            datapath = gc.createCompatibleImage(
                    im.getWidth(), im.getHeight(),
                    transparency);
            g2d = datapath.createGraphics();
            g2d.drawImage(im, 0, 0, null);
            g2d.dispose();
        } catch (IOException e) {
            System.out.println("Load Image error for " +
                    getClass().getResource(Application.IMAGES_PATH + "register.png") + ":\n" + e);
        }
    }

    public void updateDisplay() {
        this.repaint();
    }


    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g2d = (Graphics2D) g;
        // use antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // smoother (and slower) image transformations  (e.g. for resizing)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        drawImage(g2d, datapath, 0, 0, null);
        executeAnimation(g2d);
        counter = (counter + 1) % 100;
        g2d.dispose();
    }

    private void drawImage(Graphics2D g2d, BufferedImage im, int x, int y, Color c) {
        MipsXray.DatapathAnimation.drawImage(g2d, im, this, x, y, c);
    }

    private void executeAnimation(Graphics2D g) {
        MipsXray.DatapathAnimation.executeAnimation(g, traversedVertices, outputGraph, (a, b) ->{});
    }

}
