setImageType('FLUORESCENCE');
// convert original channel names from DAPI-Q, Cy5-Q, FITC, TRITC
setChannelNames('Nuclei','CD15','CD31','alphaSMA');
// Set Colors to gray, cyn, green, magenta
setChannelColors(
    getColorRGB(255, 255, 255),
    getColorRGB(100, 255, 255),
    getColorRGB(0, 255, 0),
    getColorRGB(255, 100, 255)
)

