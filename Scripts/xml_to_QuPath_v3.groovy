// script taken from https://forum.image.sc/t/labelled-annotation-import/37787/7 
// https://raw.githubusercontent.com/pjl54/qupath_scripts/d07056fc975c56c138e15bce964f145b5e1c08f3/xml_to_QuPath.groovy

// Changes by OG: 
// - switch between X,Y and flip the Y axis- assumed that the image is rotated to be placed as in AperioImageScope,
// - implement either polygon or ellipsoid ROI
//
// Usage: 
// Update "xmlDirectory" 
// Set use_EllipseROI - by default assume the ROI is given as polygon, if use_EllipseROI==true  assume the ROI is given as Ellipsoid 
// Run for each slide independently or Run For Project for all slides whose data (scn + xml files) reside within the same folder n the disk 

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import qupath.lib.roi.PathROIToolsAwt
import qupath.lib.objects.*
import qupath.lib.roi.*
import qupath.lib.regions.*


import qupath.lib.objects.classes.*

String xmlDirectory = 'A:/vishnum/protemics_region_annotation/BC7'
String xmlFileName = 'A:/vishnum/protemics_region_annotation/BC7/mmbc7_1-4_Her2_005138f0-24cc-432b-b7b9-0cb62095e5f7.xml'

boolean use_xmlDir = true
boolean use_xmlFileName = false
boolean use_EllipseROI = false  // by default assume the ROI is given as polygon, if use_EllipseROI==true  assume the ROI is given as Ellipsoid 

def server = getCurrentImageData().getServer()

String imgPath = server.getPath()

String path2 = server.getPath()
int ind1 = path2.lastIndexOf("/") + 1;
int ind2 = path2.lastIndexOf(".") - 1;
name = path2[ind1..ind2]

path = path2[path2.indexOf('/')+1..path2.lastIndexOf("/")-1]

if (use_xmlFileName)
    maskFilename = xmlFileName
else
    maskFilename = path + File.separator + name + '.xml'
File fileMask = new File(maskFilename)
if(!fileMask.exists() || use_xmlDir) {
    print(maskFilename + ' does not exist or use_xmlDir is set')
	maskFilename = xmlDirectory + File.separator + File.separator + name + '.xml'
	//maskFilename = maskFilename.replaceFirst('[\\.].*$',customSuffix)
	fileMask = new File(maskFilename)
}
if(!fileMask.exists()) {
	print(maskFilename + ' does not exist')
	return
}
else {
	print('Loading mask file ' + fileMask)
}

File xmlFile = fileMask

DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance()
DocumentBuilder dBuilder

dBuilder = dbFactory.newDocumentBuilder()
Document doc = dBuilder.parse(xmlFile)


doc.getDocumentElement().normalize();

NodeList Annotation = doc.getElementsByTagName('Annotations');
NodeList Annotations = Annotation.item(0).getElementsByTagName('Annotation');

for (A = 0; A < Annotations.getLength(); A++) {

    Integer linecolor = Integer.parseInt(Annotations.item(A).getAttribute('LineColor'));
    String name = Annotations.item(A).getAttribute('Name')
    
    NodeList Regions = Annotations.item(A).getElementsByTagName('Regions');
    NodeList Region = Regions.item(0).getElementsByTagName('Region');
    
    // Loop through the annotations
    
    for (R = 0; R < Region.getLength(); R++) {
        NodeList Vertices = Region.item(R).getElementsByTagName('Vertices')
        NodeList Vertex = Vertices.item(0).getElementsByTagName('Vertex')
        RegionLengthUm = Region.item(R).getAttribute('LengthMicrons') // added by OG
        RegionLength = Region.item(R).getAttribute('Length') // added by OG
        
        def coordinatesX = new float[Vertex.getLength()]
        def coordinatesY = new float[Vertex.getLength()]
        for (V = 0; V < Vertex.getLength(); V++) {
            //coordinatesX[V] = Float.parseFloat(Vertex.item(V).getAttribute('X')) // original lines
            //coordinatesY[V] = Float.parseFloat(Vertex.item(V).getAttribute('Y')) // original lines
            coordinatesX[V] = Float.parseFloat(Vertex.item(V).getAttribute('Y'))    // changed by OG
            coordinatesY[V] = server.getHeight() - Float.parseFloat(Vertex.item(V).getAttribute('X'))  // changed by OG
        }
        
        //print(coordinatesX, coordinatesY, RegionLength, RegionLengthUm);
        if (use_EllipseROI) {
            //def roi = new EllipseROI(coordinatesX,coordinatesY,RegionLength, RegionLength, ImagePlane.getDefaultPlane())
            //def roi = ROIs.createEllipseROI(coordinatesX[0], coordinatesY[0], width, height, ImagePlane.getDefaultPlane())
            roi = ROIs.createEllipseROI(coordinatesX[0], coordinatesY[0], width, height, ImagePlane.getDefaultPlane())
        } else { // Polygon
            //def roi = new PolygonROI(coordinatesX,coordinatesY,-1,0,0)
            //def roi = new PolygonROI(coordinatesX,coordinatesY,ImagePlane.getDefaultPlane())
            roi = new PolygonROI(coordinatesX,coordinatesY,ImagePlane.getDefaultPlane())
        }
        
        //pathclass = PathClassFactory.getPathClass('ru',linecolor)
        
        // decode linecolor
        Integer[] gbr = [linecolor>>16,linecolor>>8&255,linecolor&255]
        
        if(linecolor == 16711680) {
            pathclass = null
        }
        else {
            pathclassLocal = PathClassFactory.getPathClass(name)
            //pathClasses = getQuPath().getAvailablePathClasses()
            if(pathclassLocal.getColor() - (255<<24) == linecolor) {
                pathclass = PathClassFactory.getPathClass(name,ColorTools.makeRGB(gbr[0],gbr[1],gbr[2]))
            }
            else {
                pathclass = PathClassFactory.getPathClass(String.valueOf(linecolor),ColorTools.makeRGB(gbr[0],gbr[1],gbr[2]))
            }
        }
    
        def pathObject = new PathAnnotationObject(roi, pathclass)
        // Add object to hierarchy
        addObject(pathObject)
    }
}
// lock all annotations
getAnnotationObjects().each {it.setLocked(true)}

//print("Image: " + name + " Done");
println '====================== Done! ======================='
