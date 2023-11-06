/* 
 * Analysis workflow of Set1 slides, part of Breast Cancer Hetrogeneity project
 * Blood vessels area coverage and alpha-sma positive cells cells distribution 
 * by: Vishnu Mohan, Ofra Golani, Tamar Geiger 
 *
 * Pre-request prior to running this script
 * 1. Align the *Set1* immunofluorescence slide against the matching H-DAB slide, following the Warpy protocol[1] 
 * 2. Transfer the proteomics regions from the H-DAB slides to the immuno flourescence slides using transform_objects.groovy 
 * 
 * The script implement the following steps
 * 1. Automatically detect the whole tissue using trained Pixel classifier (WholeTissue_PixelClassifier, should be placed under classifiers\pixel_classifiers) 
 * 2. Segment cells based on *StarDist* nuclei segmentation followed by expansion (CellExpansion)
 * 3. Discard cell-candidates based on minimal and maximal size ([MinNucArea-MaxNucArea])
 * 4. Classify cells as either alpha-SMA positive, RBC or Other: (CellClassifierName, should be placed under classifiers\object_classifiers) 
 *    RBC are automatically discarded, as they are assigned to Ignored class 
 * 5. Segment blood vessels based on pixel classifier followed by connected component analysis (BV_PixelClassifier, should be placed under classifiers\pixel_classifiers)
 * 6. Expand the proteomic regions by expand_radius1_um  and expand_radius2_um  
 * 7. Find intersection of blood vessels and proteomics regions and surrounding rings 
 * 
 * You can either Run it slide by slide or for multiple slides using *Run For batch* 
 * When done extract the cell type distribution and blood vessels area coverage for each region and its related surrounding ring from the annotation table 
 * 
 * script originaly named: SET1-FullAnalysis_ForProjects_final-MeasureBVInWholeTissue.groovy
 * 
 * /

import qupath.ext.stardist.StarDist2D

// Specify the StarDist model .pb file 
def pathModel = 'A:/shared/QuPathScriptsAndProtocols/QuPath_StarDistModels/stardist_for_vishnu_v5.pb'

// Pixel Classifier Parameters 
def WholeTissue_PixelClassifier = "ExactTissue_v1"
def Minimum_WT_ObjectSize = 2000000
def Minimum_WT_LumenSize = 150000

def BV_PixelClassifier = "BV-Classifier_v7"
def Minimum_BV_ObjectSize = 225
def Minimum_BV_LumenSize = 75

def expand_radius1_um = 500.0 
def expand_radius2_um = 100.0

def RBCClassifierName = 'RBC-Classifier-FromStarDist-WithExpansion'
def CellClassifierName = 'alphaSMA-calssifier-4'

//def SelectedObjectClass = 'WholeTissue'  // Class of Annotation for Analysis

def NucChannel = 'Nuclei'
def ProbabilityThreshold = 0.5
def PixelSize = 0.3257    
def CellExpansion = 5.0

// Further object filtering parameters
def MaxNucArea= 300 // µm^2
def MinNucArea= 10  // µm^2

def MinNucIntensity=20 //remove any detections with an intensity less than or equal to this value

// Control parameters , to enable modular execution of the script
def RunPixelClassifier = 1
def runStarDist = 1
def filterSomeCells = 1
def deletePreviousDetections = 0 // not used for SET1, keep it 0
def deleteRBC = 0                // not used for SET1, keep it 0
def classifyCells = 1
def runAnnotationExpansion = 1
def runBVIntersectionMeasurement = 1
def XClassToMeasure = 'BV'           
def removeTmpIntersectionObjects = 1 // keep it 1 (0 is used for debugging only)


def stardist = StarDist2D.builder(pathModel)
        .threshold(ProbabilityThreshold) // Probability (detection) threshold
        .channels(NucChannel)            // Select detection channel
        .normalizePercentiles(1, 99)     // Percentile normalization
        .pixelSize(PixelSize)            // Resolution for detection
        .cellExpansion(CellExpansion)    // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5)         // Constrain cell expansion using nucleus size
        .measureShape()                  // Add shape measurements
        .measureIntensity()              // Add cell measurements (in all compartments)
        .includeProbability(true)        // Add probability as a measurement (enables later filtering)
        .build()

// ============= MAIN CODE =======================

println '==========================================================='
// Run detection for the selected objects
def imageData = getCurrentImageData()
// Get name of current image    
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

if (imageData.ImageType == ImageData.ImageType.FLUORESCENCE) 
{
    // Automatically detect the whole tissue using trained Pixel classifier (WholeTissue_PixelClassifier) 
    createAnnotationsFromPixelClassifier(WholeTissue_PixelClassifier, Minimum_WT_ObjectSize, Minimum_WT_LumenSize, "SELECT_NEW")

    if (deletePreviousDetections == 1) 
    {
        print("Image: " + name + " - Deleting previous objects")
        removeObjects(getDetectionObjects(), false);
    }
    
    // Select WholeTissue Annotation
    resetSelection()
    //selectObjectsByClassification(SelectedObjectClass);
    selectObjectsByClassification("ExactTissue");
    
    def pathObjects = getSelectedObjects()
    
    if (pathObjects.isEmpty()) 
    {
        //print("Image: " + name + " - No Annotation of Class " + SelectedObjectClass + " found");
        print("Image: " + name + " - No Annotation of Class WholeTissue found");
        //continue;
        //Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
        //return
    } else 
    {
        print("Processing Image: " + name );
        
        if (runStarDist == 1) 
        {
            println '============== Running StarDist... =================='
            stardist.detectObjects(imageData, pathObjects)
            
            if (filterSomeCells == 1)
            {
                println '============== Filter Out small / big / deem Cells ... =================='
                // Filter Nuc by size and Intensity
                NucAreaMeasurement='Nucleus: Area µm^2' //Name of the measurement you want to perform filtering on
                if (CellExpansion == 0) 
                {
                    NucAreaMeasurement='Area µm^2' //Name of the measurement you want to perform filtering on
                }
                toDelete =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) > MaxNucArea}
                removeObjects(toDelete, true)
                toDelete1 =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) < MinNucArea}
                removeObjects(toDelete1, true)
                
                NucIntensityMeasurement='Nuclei: Nucleus: Mean' //Name of the measurement you want to perform filtering on
                if (CellExpansion == 0)
                {
                    NucIntensityMeasurement='Nuclei: Mean' //Name of the measurement you want to perform filtering on
                }
                toDelete2 = getDetectionObjects().findAll {measurement(it, NucIntensityMeasurement) <= MinNucIntensity}
                removeObjects(toDelete2, true)
            }                
            
            // Use Object Classifier to detect RBC, and delete them - not used here 
            if (deleteRBC == 1)
            {
                rbcCl = getPathClass("RBC")
                cells = getCellObjects()
                classifier = loadObjectClassifier(RBCClassifierName)
                classifier.classifyObjects(imageData, cells, false)
                rbcCells = cells.findAll{it.getPathClass() == rbcCl}
                removeObjects(rbcCells, true)
                fireHierarchyUpdate()
            }
            
            // Classify Cells 
            if (classifyCells == 1)
            {
                println '============== Classfying Cells ... =================='            
                resetDetectionClassifications();
                cells = getCellObjects()
                classifier = loadObjectClassifier(CellClassifierName)
                classifier.classifyObjects(imageData, cells, false)
                fireHierarchyUpdate()
            }
        } // if runStarDist
        
        if (runAnnotationExpansion == 1)
        {
            println '============== Expand Annotation ... =================='                   
            //clearSelectedObjects();
            //selectAnnotations();
            NotBVAnnotations = getAnnotationObjects().findAll{(it.getPathClass() != getPathClass("BV")) && (it.getPathClass() != getPathClass("WholeTissue"))}
            
            // Create a Ring around each annotation 
            selectObjects(NotBVAnnotations)
            runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+expand_radius1_um+',  "lineCap": "Round",  "removeInterior": true,  "constrainToParent": true}');
            runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+expand_radius2_um+',  "lineCap": "Round",  "removeInterior": true,  "constrainToParent": true}');                                
        }
        fireHierarchyUpdate()

        if (RunPixelClassifier == 1)
        {
            // Create BV objects
            //print("Image: " + name + " - Run Pixel Classifier");
            println '============== Run Pixel Classifier ... =================='            
            resetSelection()
            selectObjectsByClassification("WholeTissue");
            createAnnotationsFromPixelClassifier(BV_PixelClassifier, Minimum_BV_ObjectSize, Minimum_BV_LumenSize, "SPLIT")
            selectAnnotations();
            //addPixelClassifierMeasurements(BV_PixelClassifier, BV_PixelClassifier)
        }            
        
        // Measure area of intersecting BV objects
        if (runBVIntersectionMeasurement)
        {

            resetSelection()
            def NotX_Annotations = getAnnotationObjects().findAll{(it.getPathClass() != getPathClass(XClassToMeasure)) }
            //def NotX_Annotations = getAnnotationObjects().findAll{(it.getPathClass() != getPathClass(XClassToMeasure)) && (it.getPathClass() != getPathClass("WholeTissue"))}            
            def X_Annotations = getAnnotationObjects().findAll {it.getPathClass() == getPathClass(XClassToMeasure)}
            
            println '=========== Measure Intersecting area ==================='
            for (it_NotX in NotX_Annotations) {
                //pathClass = it.getPathClass()
                selectObjects(it_NotX)
                def g1 = it_NotX.getROI().getGeometry()
                def plane = it_NotX.getROI().getImagePlane()
                def total_area = 0;
                def n_intersecting_obj = 0
                for (it_X in X_Annotations) {
                    def g2 = it_X.getROI().getGeometry()
                    def intersection = g1.intersection(g2)
                    if ( !intersection.isEmpty())
                    {
                        def roi = GeometryTools.geometryToROI(intersection, plane)
                        def annotation = PathObjects.createAnnotationObject(roi, getPathClass('Intersection'))
                        addObject(annotation)
                        //selectObjects(annotation)
                        //println "Annotated created for subtraction"
                        //total_area += annotation.getROI().getArea()
                        area = annotation.getROI().getArea() * PixelSize * PixelSize     // area in um
                        print ("Area = "+area);
                        total_area += area        
                        n_intersecting_obj++
                     }
                }
                it_NotX_area = it_NotX.getROI().getArea() * PixelSize * PixelSize
                print("============ Total Area = " + total_area + " =================")
                it_NotX.getMeasurementList().putMeasurement(XClassToMeasure+" Area", total_area)
                it_NotX.getMeasurementList().putMeasurement(XClassToMeasure+" %", total_area / it_NotX_area * 100)
                it_NotX.getMeasurementList().putMeasurement(XClassToMeasure+" N Intersecting", n_intersecting_obj)
            }
            
            fireHierarchyUpdate()
            if (removeTmpIntersectionObjects)
            {
                Intersection_Annotations = getAnnotationObjects().findAll {it.getPathClass() == getPathClass('Intersection')}
                removeObjects(Intersection_Annotations, true)
                println '=========== Done measure Intersection ! ==================='
            }
            else {
                Intersection_Annotations = getAnnotationObjects().findAll {it.getPathClass() == getPathClass('Intersection')}
                selectObjects(Intersection_Annotations)
                println '=========== Done measure Intersection !             You can remove selected objects ==================='
            }
        
        }
    } // if not empty list of selectedClass            
} // if FLUORESCENCE
else
{
    print("Image: " + name + " - Not FLUORESCENCE ");
}

print("Image: " + name + " Done ");
//println '====================== Done! ======================='

