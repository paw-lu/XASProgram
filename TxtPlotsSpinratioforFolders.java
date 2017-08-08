/******************************************************************************
This is the converter to change SigScan files into arrays used for java use and then exports individual txt files with the formatted data, tiny jpeg files with plots of the data, and a final txt file showing peak ratios.
This program assumes:
  -Skip the first 129 words
  -Skip the first 16 rows
  -That the data is divided into 44 columns
  -The only things in the folder are data txt files formatted as usual from   ALS, and other folders containing more data txt files, nothing else.

Path in line 27 (output) must be different than path in line 26 (input), or it will overwrite original data.
For plotting, this relies on the StdDraw library, which may be downloaded from here: https://goo.gl/m4U7gt (save as java file and put in same directory as this file).

Written in Java 8 with Atom with script and Atom Material, soft wrap at preferred line.

Paulo S. Costa
*******************************************************************************/

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintWriter;

public class TxtPlotsSpinratioforFolders {
  public static void main (String[] args) throws IOException {

    File folder = new File("C:/Users/Paulo/Desktop/Test/"); // Here is the input folder, where all your data is kept. Type in what folder your data is stored in.
    String exportDirectory = "C:/Users/Paulo/Desktop/Output/";  // Here is your export folder, where all the formatted data and pltos will be written.

    int subFolderCount = 0;
    int folderArrayNum = 0;
    for (File names : folder.listFiles()) { // Counting how many folders are in folder specified as input to search through those as well.
      if (names.isDirectory()) {
        subFolderCount++;
      }
    }
    String[] subFolders = new String[subFolderCount]; // Make array consisting of all folders found in input folder.
    for (File names : folder.listFiles()) {
      if (names.isDirectory()) {
        subFolders[folderArrayNum] = names.getPath();
        folderArrayNum++;
      }
    }

    PrintWriter spinWriter = new PrintWriter(exportDirectory + "SpinRatios.txt", "UTF-8");
    spinWriter.println("Sample\tHS\tLS\tHSRatio\tLSRatio");

    for (int k = 0; k < subFolderCount + 1; k++) {  // First run of loop will be through files in input folder, then subsequent runs will go through all the folders found inside the input folder.
      if (k > 0) {  // After first run will search through other sub folders.
        folder = new File(subFolders[k - 1]);
      }

      File[] listOfFiles = folder.listFiles();


      for (File file : listOfFiles) {
        if (file.isFile()) {
          String fileName = file.getName(); // Grabs name of input file.
          String filePath = file.getPath(); // Grabs full path of input file.

          Scanner scannerRow = new Scanner (new FileReader(filePath));

          int rowCount = 0;  // I will count the total number of rows in the text file to find out how large my array should be.
          while (scannerRow.hasNextLine()) {
            rowCount++;
            scannerRow.nextLine();
          }
          int rowNum = rowCount - 16; // I am not counting the other text that is not useful. BullshitRowCounter.java told me I could skip the first 16 rows.
          if (rowNum != 0) {  // To avoid errors I will ignore txt files that have no numbers.
            double[] [] data = new double[rowNum] [44]; // Assuming 44 columns of data here.

            Scanner scannerRead = new Scanner (new FileReader(filePath));  // Recall the file here to re-read it for array conversion.

            for (int i = 0; i < 16; i++) {  // Burn off text i don't care about. BullshitRowCounter.java told me I can skip the first 16 rows.
              scannerRow.next();
            }

            for (int i = 0; i < rowNum; i++) {
              for (int j = 0; j < 44; j++) {
                if (j == 0) { // Need to skip first column since it list time in a format that can't be a double. There is probably a nicer way to deal with this, but don't care.
                  scannerRead.next();
                }
                else {
                  data[i] [j] = scannerRead.nextDouble();
                }
              }
            }

            double[] [] XAS = new double[rowNum] [2]; // Now I make an array that keeps only the values I want to plot: EY/I0 ES vs Energy.
            for (int i = 0; i < rowNum; i++) {
              XAS[i] [0] = data[i] [2];
              XAS[i] [1] = (data[i] [5])/(data[i] [4]);
            }

            String exportPath = exportDirectory + fileName; // Type in prefered export path here, must be different than import path.
            double prevX = XAS[0] [0];
            double prevY = XAS[0] [1];
            double YMax = Double.NEGATIVE_INFINITY;
            double YMin = Double.POSITIVE_INFINITY;
            double XMax = XAS[rowNum - 1] [0];
            double XMin = XAS[0] [0];
            double HSSum = 0.0;
            int HSCount = 0;
            double LSSum = 0.0;
            int LSCount = 0;
            double iniZeroSum = 0.0;
            double iniEnergySum = 0.0;
            double finalZeroSum = 0.0;
            double finalEnergySum = 0.0;
            int iniCount = 0;
            int finalCount = 0;

            for (int i = 0; i < rowNum; i++) {  // Determines min and max for both x and y for plotting.
              if (XAS[i] [1] < YMin) {
                YMin = XAS[i] [1];
              }
              if (XAS[i] [1] > YMax) {
                YMax = XAS[i] [1];
              }
              if (XAS[i] [0] < 701) {
                iniZeroSum += XAS[i] [1];
                iniEnergySum += XAS[i] [0];
                iniCount++;
              }
              if (XAS[i] [0] > 714) {
                finalZeroSum += XAS[i] [1];
                finalEnergySum += XAS[i] [0];
                finalCount++;
              }
            }

            // These will be used to find m and b below to find linear line to substract as background.
            double iniZero = iniZeroSum/iniCount;
            double iniEnergy = iniEnergySum/iniCount;
            double finalZero = finalZeroSum/finalCount;
            double finalEnergy = finalEnergySum/finalCount;

            // y = mx + b line for subtracting backgound.
            double m = (finalZero - iniZero)/(finalEnergy - iniEnergy);
            double b = iniZero - m*iniEnergy;

            for (int i = 0; i < rowNum; i++) {
              if (707.9 < XAS[i] [0] && XAS[i] [0] < 708.2) { // Adjust these as necessary, should encapsulate the range that your HS peak fits in.
                HSSum += XAS[i] [1] - (m*XAS[i] [0] + b);
                HSCount++;
              }
              if (708.5 < XAS[i] [0] && XAS[i] [0] < 709.1) { //Adjust these as necessary, should encapsulate the range that your LS peak fits in.
                LSSum += XAS[i] [1] - (m*XAS[i] [0] + b);
                LSCount++;
              }
            }

            // The values I found hor HS peak and LS peak after subtracting background.
            double HSValue = HSSum/HSCount;
            double LSValue = LSSum/LSCount;

            // Ratios of HS to LS as percentages.
            double HSRatio = HSValue/(HSValue + LSValue);
            double LSRatio = LSValue/(HSValue + LSValue);

            String strippedName = fileName.substring(0, fileName.lastIndexOf('.'));
            spinWriter.println(strippedName + "\t" + HSValue + "\t" + LSValue + "\t" + HSRatio + "\t" + LSRatio);

            StdDraw.setCanvasSize(250, 250);  // This determines picture size (in px). It's low now to keep sizes small, but increase if you want higher quality.
            StdDraw.setYscale(YMin, YMax);  // Min and max y values for the plot.
            StdDraw.setXscale(XMin, XMax);  // Min and max x values for the plot.

            if (filePath.equals(exportPath)) {  // Check if import and export paths are the same.
              System.out.println("Import and export path must be different.");
            }
            else {
              PrintWriter dataWriter = new PrintWriter(exportPath, "UTF-8"); // Exports a txt file with two columns, that are tab seperated. First one is energy, second one is EY/I0 ES. In addition exports jpeg of plot.
              dataWriter.println("Energy\tEY/I0 ES");
              for (int i = 0; i < rowNum; i++) {
                dataWriter.println(XAS[i] [0] + "\t" + XAS[i] [1]);
                if (i > 0) {
                  StdDraw.line(prevX, prevY, XAS[i] [0], XAS[i] [1]);
                  prevX = XAS[i] [0];
                  prevY = XAS[i] [1];
                }
              }
              dataWriter.close();
              StdDraw.save(exportPath + strippedName + ".jpg"); // Export path for plots is the same as for the txt files.
            }
          }

        }
      }

    }
    spinWriter.close();

  }
}
