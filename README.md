# Breast Cancer Heterogeneity

## Study Overview

To study breast cancer microenvironment and immune-cancer crosstalk at the proteomic level, we integrated the multi-regional proteomic data with 
immune-fluorescence imaging to unravel the TME impact on the cancer proteome. 
The research is composed of five layers: i) Multi region MS-based proteomic analysis; ii) Bioinformatic analysis of proteomics data 
iii) Multiplexed opal imaging; iv) Image processing ; v) Integration of proteomics and image analysis.

For the proteomics-imaging integration, 11 tumors, 47 tissue sections including 121 regions were stained. 

This repository includes the workflow and code used for the image analysis part of the study.

For each tissue 3 consecutive slices were used:
1. Stained with H-DAB (for either ER or PR or HER2 or CK), imaged by Aperio slide scanner and 
   used for manual region selection with the help of the pathologist. 
   The selected regions were saved in Aperio .xml annotation format, 
   and were manually marked on the LCM scan image and used for laser capture.  
2. Stained for CD31, alpha-SMA and DAPI ("Set1")
3. Stained for Pan cytokeratin (Pan-CK), CD68, CD3 and DAPI ("Set2")

We aligned the H-DAB and immunofluorescence slides, and transferred selected region annotations to the immunofluorescence slides using *Warpy*[1] 
protocol which combines *QuPath*[2], *Fiji*[3] and *elstix*[4],[5] software.   
We used the Set1 slides to measure blood vessels area coverage and spatial distribution of alpha-SMA positive cells within the regions and their surroundings.  
We further used Set2 slides to measure tumor and immune cells distribution within the regions and their surroundings.    
This analysis was done using *QuPath*. Cells were segmented by nuclei segmentation using *StarDist*[6] model followed by expansion as implemented in QuPath StarDist extension.  
We trained our own model using *ZeroCostDL4Mic*[7] StarDist notebook.

The QuPath scripts, StarDist model and the data used for training are provided here 

Software package: QuPath, StarDist, ZeroCostDL4Mic, Fiji (ImageJ), Warpy, elastix

Workflow language: groovy

<p align="center">
<img src="PNG/Spatial Proteomics Workflow.png" alt=""Image Analysis Workflow" width="750" title="Image Analysis Workflow">
	<br/> <br/> </p>

## QuPath Workflow

### import aperio slides annotations 
The H-DAB slides were imaged by aperio slide scanner.
We used the script **xml_to_QuPath_v3.groovy** to import the aperio-format region annotations into QuPath (either polygonal and ellipsoidal). 

### Immune and Tumor Cells distribution 
1. Align the *Set2* immunofluorescence slide against the matching H-DAB slide, following the Warpy protocol[1] as detailed also in [Warpy documentation](https://imagej.net/plugins/bdv/warpy/warpy)
2. Transfer the proteomics regions from the H-DAB slides to the immuno fluorescence slides using *transform_objects.groovy* 
3. Manually draw regions around the whole tissue and classified it as "WholeTissue" 
4. Segment cells based on *StarDist* nuclei segmentation followed by expansion as implemented in the *StarDist* extension of *QuPath*
5. Discard cell-candidates based on minimal and maximal size 
6. Discard Red-blood-cells: RBC appears very similar to nuclei in the DAPI channel. However, they show high autoflourescence in the pan-CK channel. 
   We trained an object classifier to diffrentiate between Cell/RBC/Ignore and used it to discard RBC.
7. Classify cells as either pan-CK positive, CD68 positive, CD3 positive cells or Other: We trained an object classifier on multiple representative regions from multiple slides and used it for classification. 
8. Expand the proteomic regions by 500um 
9. Extract cell type distribution for each region and its related surrounding ring from the annotation table 
Steps 4-8 of the Analysis workflow are implemented in the script **SET2-FullAnalysis.groovy** 
The warpy transformations for the sample data are saved within the QuPath project

### blood vessels area coverage and alpha-sma positive cells cells distribution 
1. Align the *Set1* immunofluorescence slide against the matching H-DAB slide, following the Warpy protocol
2. Transfer the proteomics regions from the H-DAB slides to the immuno fluorescence slides using *transform_objects.groovy* 
3. Automatically detect the whole tissue using trained Pixel classifier 
4. Segment cells based on *StarDist* nuclei segmentation followed by expansion as implemented in the *StarDist* extension of *QuPath*
5. Discard cell-candidates based on minimal and maximal size 
6. Classify cells as either alpha-SMA positive, RBC or Other: We trained an object classifier on multiple representative regions from multiple slides and used it for classification. 
   RBC are automatically discarded, as they are assigned to Ignored class 
7. Segment blood vessels based on pixel classifier followed by connected component analysis (*Create Objects* operation within QuPath)
8. Expand the proteomic regions by 100um and 500um 
9. Find intersection of blood vessels and proteomics regions and surrounding rings 
10. Extract cell type distribution and blood vessels area coverage for each region and its related surrounding ring from the annotation table 
Steps 3-9 of the Analysis workflow are implemented in the script **SET1-FullAnalysis.groovy** 

Data used for StarDist model training + Model: <a href="https://doi.org/10.5281/zenodo.11235393"><img src="https://zenodo.org/badge/DOI/10.5281/zenodo.11235393.svg" alt="DOI"></a> <br/>
QuPath Classifiers:<br/>
SET2: <br/>
Stardist: stardist_for_vishnu_v5<br/>
RBC classifier: RBC-Classifier-FromStarDist-WithExpansion (should be placed in the project folder under classifiers\object_classifiers)<br/>
Cell classifier: Tumor_Mac_TC_Classifier_v5               (should be placed in the project folder under classifiers\object_classifiers)<br/>

SET1: <br/>
Stardist: stardist_for_vishnu_v5
The whole tissue pixel classifier for SET1: ExactTissue_v1 (should be placed in the project folder under classifiers\pixel_classifiers)
BV Pixel classifier: BV-Classifier_v7					   (should be placed in the project folder under classifiers\pixel_classifiers)
Cell classifier: alphaSMA-calssifier-4					   (should be placed in the project folder under classifiers\object_classifiers)
</p>

## Dependencies
QuPath version: mostly 0.3.2. and 0.4.2 for few slides where we encountered errors 
QuPath StarDist extension: 0.3.2 (0.4.0) 
QuPath Warpy extension: 0.2.0 

Fiji Warpy plugin:

## References 
[1] *Warpy* : Chiaruttini N, Burri O, Haub P, Guiet R, Sordet-Dessimoz J and Seitz A (2022) An Open-Source Whole Slide Image Registration Workflow at Cellular Precision Using Fiji, QuPath and Elastix. Front. Comput. Sci. 3:780026. doi: 10.3389/fcomp.2021.780026 <br>
[2] *QuPath* : Bankhead, P. et al. QuPath: Open source software for digital pathology image analysis. Scientific Reports (2017).
https://doi.org/10.1038/s41598-017-17204-5 <br>
[3] *Fiji* : Schindelin, J., Arganda-Carreras, I., Frise, E., Kaynig, V., Longair, M., Pietzsch, T., et al. (2012). Fiji: an Open-Source Platform for Biological-Image Analysis. Nat. Methods 9, 676–682. doi:10.1038/nmeth.2019 <br>
[4] *elstix* : Klein, S., Staring, M., Murphy, K., Viergever, M. A., and Pluim, J. (2010). Elastix: A Toolbox for Intensity-Based Medical Image Registration. IEEE Trans. Med. Imaging 29, 196–205. doi:10.1109/TMI.2009.2035616 <br>
[5] *elstix* : Shamonin, D. (2013). Fast Parallel Image Registration on CPU and GPU for Diagnostic Classification of Alzheimer's Disease. Front. Neuroinform. 7. doi:10.3389/fninf.2013.00050 <br>
[6] *StarDist* : Uwe Schmidt, Martin Weigert, Coleman Broaddus, and Gene Myers. Cell Detection with Star-convex Polygons. International Conference on Medical Image Computing and Computer-Assisted Intervention (MICCAI), Granada, Spain, September 2018. <br>
[7] *ZeroCostDL4Mic* : Lucas von Chamier*, Romain F. Laine*, Johanna Jukkala, Christoph Spahn, Daniel Krentzel, Elias Nehme, Martina Lerche, Sara Hernández-pérez, Pieta Mattila, Eleni Karinou, Séamus Holden, Ahmet Can Solak, Alexander Krull, Tim-Oliver Buchholz, Martin L Jones, Loic Alain Royer, Christophe Leterrier, Yoav Shechtman, Florian Jug, Mike Heilemann, Guillaume Jacquemet, Ricardo Henriques. Democratising deep learning for microscopy with ZeroCostDL4Mic. Nature Communications, 2021. DOI: https://doi.org/10.1038/s41467-021-22518-0 <br>

## Data availability
We intend to upload the image data to public repository

