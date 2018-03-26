# XASProgram
Simple program to organize and plot out XAS data for analysis

This is the converter to change SigScan files into arrays used for java use and then exports individual txt files with the formatted data, tiny jpeg files with plots of the data, and a final txt file showing peak ratios.
This program assumes:
  –Skip the first 16 rows
  –The only txt files in the folder are formatted as usual from ALS (program lazily checks for this by seeing if the first word in the txt file is "Date:")
  –There is one peak that represents the high spin state, and one that represents the low spin state.

Path in line 28 (output) must be different than path in line 27 (input), or it will overwrite original data (there is a safety check for this).
For plotting, this relies on the StdDraw library, which may be downloaded from here: https://goo.gl/m4U7gt (save as java file and put in same directory as this file).
