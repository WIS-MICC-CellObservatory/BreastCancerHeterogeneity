/* 
 * Analysis workflow of Set2 slides, part of Breast Cancer Hetrogeneity project
 * Immune and Tumor Cells distribution 
 * by: Vishnu Mohan, Ofra Golani, Tamar Geiger 
 *
 * Pre-request prior to running this script
 * 1. Align the *Set1* immunofluorescence slide against the matching H-DAB slide, following the Warpy protocol[1] 
 * 2. Transfer the proteomics regions from the H-DAB slides to the immuno flourescence slides using transform_objects.groovy 
 * 3. Manually draw regions arround the whole tissue and classified it as "WholeTissue" 
 * 
 * The script implement the following steps
 * 1. Segment cells based on *StarDist* nuclei segmentation followed by expansion (CellExpansion)
 * 2. Discard cell-candidates based on minimal and maximal size ([MinNucArea-MaxNucArea])
 * 3. Discard Red-blood-cells: RBC appears very similar to nuclei in the DAPI channel. However, they show high autoflourescence in the pan-CK channel. 
      We trained an object classifier to Cell/RBC/Ignore and used it to discard RBC. (RBCClassifierName, should be placed under classifiers\object_classifiers)
 * 4. Classify cells as either pan-CK positive, CD68 positive, CD3 positive cells or Other: (CellClassifierName, should be placed under classifiers\object_classifiers) 
 *    RBC are automatically discarded, as they are assigned to Ignored class 
 * 5. Expand the proteomic regions by  expand_radius_um 
 * 
 * You can either Run it slide by slide or for multiple slides using *Run For batch* 
 * When done extract the cell type distribution for each region and its related surrounding ring from the annotation table 
 * 
 * script originaly named: RunStarDistOpenCV_And_CellClassification_ForProjects.groovy
 * 
 */

import qupath.ext.stardist.StarDist2D

// Specify the model .pb file (you will need to change this!)
def pathModel = 'A:/shared/QuPathScriptsAndProtocols/QuPath_StarDistModels/stardist_for_vishnu_v5.pb'

def RBCClassifierName = 'RBC-Classifier-FromStarDist-WithExpansion'
def CellClassifierName = 'Tumor_Mac_TC_Classifier_v5'

//def SelectedObjectClass = 'WholeTissue'  // Class of Annotation for Analysis

def expand_radius_um = 500.0

def NucChannel = 'Nuclei'
def ProbabilityThreshold = 0.5
def PixelSize = 0.3257  // Resolution for detection
def CellExpansion = 5.0

// Further object filtering parameters
def MaxNucArea= 350
def MinNucArea= 5

def MinNucIntensity=20 //remove any detections with an intensity less than or equal to this value

def runAnnotationExpansion = 1
def runStarDist = 1
def deletePreviousDetections = 0
def deleteRBC = 1
def classifyCells = 1

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
//def imageData = entry.readImageData()
// Get name of current image    
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

if (imageData.ImageType == ImageData.ImageType.FLUORESCENCE) 
{
    if (deletePreviousDetections == 1) 
    {
        print("Image: " + name + " - Deleting previous objects")
        removeObjects(getDetectionObjects(), false);
    }
    
    // Select WholeTissue Annotation
    //selectObjectsByClassification(SelectedObjectClass);
    resetSelection()
    selectObjectsByClassification("WholeTissue");
    
    def pathObjects = getSelectedObjects()
    //pathObjects = getSelectedObjects();
    //def pathObjects = getAnnotationObjects().findAll{it.getPathClass() != getPathClass("WholeTissue")}
    
    if (pathObjects.isEmpty()) 
    {
    //if (pathObjects == null) {
        //print("Image: " + name + " - No Annotation of Class " + SelectedObjectClass + " found");
        print("Image: " + name + " - No Annotation of Class WholeTissue found");
        //continue;
        //Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
        //return
    } else 
    {
        print("Processing Image: " + name );
        //if (deletePreviousDetections)
        //{
        //    removeObjects(getDetectionObjects(), false)     
        //    print("Deleting previous objects");
        //}
        
        if (runStarDist == 1) 
        {
            println '============== Running StarDist... =================='
            stardist.detectObjects(imageData, pathObjects)
            
            // Filter Nuc by size and Intensity
            //def NucAreaMeasurement='Nucleus: Area µm^2' //Name of the measurement you want to perform filtering on
            NucAreaMeasurement='Nucleus: Area µm^2' //Name of the measurement you want to perform filtering on
            if (CellExpansion == 0) 
            {
                NucAreaMeasurement='Area µm^2' //Name of the measurement you want to perform filtering on
            }
            //def toDelete =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) > MaxNucArea}
            toDelete =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) > MaxNucArea}
            removeObjects(toDelete, true)
            //def toDelete1 =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) < MinNucArea}
            toDelete1 =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) < MinNucArea}
            removeObjects(toDelete1, true)
            
            //def NucIntensityMeasurement='Nuclei: Nucleus: Mean' //Name of the measurement you want to perform filtering on
            NucIntensityMeasurement='Nuclei: Nucleus: Mean' //Name of the measurement you want to perform filtering on
            if (CellExpansion == 0)
            {
                NucIntensityMeasurement='Nuclei: Mean' //Name of the measurement you want to perform filtering on
            }
            //def toDelete2 = getDetectionObjects().findAll {measurement(it, NucIntensityMeasurement) <= MinNucIntensity}
            toDelete2 = getDetectionObjects().findAll {measurement(it, NucIntensityMeasurement) <= MinNucIntensity}
            removeObjects(toDelete2, true)
            
            // Use Object Classifier to detect RBC, and delete them
            if (deleteRBC == 1)
            {
                rbcCl = getPathClass("RBC")
                //def cells = getCellObjects()
                cells = getCellObjects()
                //def classifier = loadObjectClassifier(RBCClassifierName)
                classifier = loadObjectClassifier(RBCClassifierName)
                classifier.classifyObjects(imageData, cells, false)
                rbcCells = cells.findAll{it.getPathClass() == rbcCl}
                removeObjects(rbcCells, true)
                fireHierarchyUpdate()
            }
            
            println '============== Classfying Cells ... =================='            
            // Classify Cells 
            if (classifyCells == 1)
            {
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
            runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+expand_radius_um+',  "lineCap": "Round",  "removeInterior": true,  "constrainToParent": true}');
            
        }
        fireHierarchyUpdate()

        
        
    } // if not empty list of selectedClass            
} // if FLUORESCENCE
else
{
    print("Image: " + name + " - Not FLUORESCENCE ");
}

print("Image: " + name + " Done");
//println '====================== Done! ======================='

