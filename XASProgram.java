/******************************************************************************
This is the converter to change SigScan files into arrays used for java use and then exports individual txt files with the formatted data, tiny jpeg files with plots of the data, and a final txt file showing peak ratios.
This program assumes:
  -Skip the first 16 rows
  -The only txt files in the folder are formatted as usual from ALS (program lazily checks for this by seeing if the first word in the txt file is "Date:")
  -There is one peak that represents the high spin state, and one that represents the low spin state.

There is a memory leak, probably in the StdDraw, so if you do too many files at once it will stop at a certain point or use a lot of memory. Use x64 version of Java to allocate more memory to program.

Path in line 28 (output) must be different than path in line 27 (input), or it will overwrite original data.
For plotting, this relies on the StdDraw library, which may be downloaded from here: https://goo.gl/m4U7gt (save as java file and put in same directory as this file).

Written in Java 8 on Atom with script and Atom Material, soft wrap at preferred line.

Paulo S. Costa
*******************************************************************************/

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintWriter;

public class XASProgram {
  public static void main (String[] args) throws IOException {

    File folder = new File("C:/Users/Paulo Costa/Desktop/10-15-17/"); // Here is the input folder, where all your data is kept. Type in what folder your data is stored in.
    String exportDirectory = "C:/Users/Paulo Costa/Desktop/Output/";  // Here is your export folder, where all the formatted data and plots will be written.

    // The following variables affect the spin ratio data. It is represented as the the faction of the peak that represents the high spin in comparison to the peak that represents the low spin. So 0.60 would mean the data consists a high spin peak that is 60% of the intensity, and the remaining 40% is the low spin peak.
    Double HSPeakMax = 707.5; // The peak that represents the high spin state. Your high spin peak should fall between these min and max values.
    Double HSPeakMin = 707.1;
    Double LSPeakMax = 708.9; // The peak that repersents the low spin state. Your low spin peak should fall between these min and max values.
    Double LSPeakMin = 708.5;
    Double FullHSRatio = 0.70;  // What percentage of your amplitude is the high spin peak when the sample is found to be in a high spin state?
    Double FullLSRatio = 0.19345; // What percentage of your amplitude is the high spin peak when the smaple is found to be ina low spin state?

    int numer = 6; // This is the column number from your data that containts the numberator you want for your plot. Start couting from 0 (the first column is 0, secon one is 1, etc). Usually EY here.
    int denom = 9; // This is the column number for your denominator. Ususally I0 ES, could be Clock if there are issues.

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
    spinWriter.println("Sample\tHSRatio\tTime");

    for (int k = 0; k < subFolderCount + 1; k++) {  // First run of loop will be through files in input folder, then subsequent runs will go through all the folders found inside the input folder.
      if (k > 0) {  // After first run will search through other sub folders.
        folder = new File(subFolders[k - 1]);
      }

      File[] listOfFiles = folder.listFiles();

      String firstWord = "firstWord";
      for (File file : listOfFiles) {
        String fileName = file.getName(); // Grabs name of input file.
        String filePath = file.getPath(); // Grabs full path of input file.
        if (file.isFile()) {
          Scanner dataChecker = new Scanner(new FileReader(filePath));
          firstWord = dataChecker.next();  // Read off first word of txt file here, to make sure it says "Date:", as a lazy check to make sure this is ALS formatted data.
          dataChecker.close();
        }

        if (file.isFile() && file.getName().endsWith(".txt") && firstWord.equals("Date:")) { // Only looking at txt files who's first word is "Date:", ignore others in directory, if ALS format changes this check needs to be changed as well.


          Scanner scannerRow = new Scanner (new FileReader(filePath));

          int rowCount = 0;  // I will count the total number of rows in the text file to find out how large my array should be.
          int colNum = 0; // I will count the total number or columns in the text file to find out how large my array should be.
          while (scannerRow.hasNextLine()) {
            rowCount++;
            if (rowCount != 17) {
              scannerRow.nextLine();
            }
            else {  // Once I hit the first row of real data (the 17th row), I count the number of columns in that row for my array.
              String line = scannerRow.nextLine();

              Scanner scannerLine = new Scanner(line);
              while (scannerLine.hasNext()) {
                scannerLine.next();
                colNum++;
              }
              scannerLine.close();
            }
          }


          int rowNum = rowCount - 16; // I am not counting the other text that is not useful. BullshitRowCounter.java told me I could skip the first 16 rows.
          if (rowNum > 0) {  // To avoid errors I will ignore txt files that have no numbers.
            double[] [] data = new double[rowNum] [colNum]; // Assuming 44 columns of data here.

            Scanner scannerRead = new Scanner (new FileReader(filePath));  // Recall the file here to re-read it for array conversion.

            for (int i = 0; i < 16; i++) {  // Burn off text i don't care about. BullshitRowCounter.java told me I can skip the first 16 rows.
              scannerRead.nextLine();
            }
            for (int i = 0; i < rowNum; i++) {
              for (int j = 0; j < colNum; j++) {
                if (j == 0) { // Need to skip first column since it list time in a format that can't be a double. There is probably a nicer way to deal with this, but don't care.
                  scannerRead.next();
                }
                else {
                  data[i] [j] = scannerRead.nextDouble();
                }
              }
            }

            double[] [] XAS = new double[rowNum] [2]; // Now I make an array that keeps only the values I want to plot: EY/I0 ES vs Energy (or EY/Clock when there are problems).
            for (int i = 0; i < rowNum; i++) {
              XAS[i] [0] = data[i] [2]; // Energy here
              XAS[i] [1] = (data[i] [numer])/(data[i] [denom]); // Ratio here
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
            double time = data[0] [1];

            if (rowNum > 4) { // Make sure that data set is long enough so it does not trigger errors.
              for (int i = 0; i < rowNum; i++) {  // Determines min and max for both x and y for plotting.
                if (XAS[i] [1] < YMin) {
                  YMin = XAS[i] [1];
                }
                if (XAS[i] [1] > YMax) {
                  YMax = XAS[i] [1];
                }
                if (XAS[i] [0] < XAS[3] [0]) { // This needs to be adjusted depending on limits of your scan. Right now I have the first three data points that appear.
                  iniZeroSum += XAS[i] [1];
                  iniEnergySum += XAS[i] [0];
                  iniCount++;
                }
                if (XAS[i] [0] > XAS[rowNum - 4] [0]) { // This needs to be adjusted depending on limits of your scan. Right now I have the last three data points that appear.
                  finalZeroSum += XAS[i] [1];
                  finalEnergySum += XAS[i] [0];
                  finalCount++;
                }
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
              if (HSPeakMin < XAS[i] [0] && XAS[i] [0] < HSPeakMax) { // Adjust these as necessary, should encapsulate the range that your HS peak fits in.
                HSSum += XAS[i] [1] - (m*XAS[i] [0] + b);
                HSCount++;
              }
              if (LSPeakMin < XAS[i] [0] && XAS[i] [0] < LSPeakMax) { // Adjust these as necessary, should encapsulate the range that your LS peak fits in.
                LSSum += XAS[i] [1] - (m*XAS[i] [0] + b);
                LSCount++;
              }
            }

            // The values I found for HS peak and LS peak after subtracting background.
            double HSValue = HSSum/HSCount;
            double LSValue = LSSum/LSCount;

            // Ratios of HS to LS as percentages.
            double t2gRatio = HSValue/(HSValue + LSValue);
            double egRatio = LSValue/(HSValue + LSValue);

            double HSRatio = (t2gRatio - FullLSRatio)/(FullHSRatio - FullLSRatio);

            String strippedName = fileName.substring(0, fileName.lastIndexOf('.'));
            spinWriter.println(strippedName  + "\t" + HSRatio + "\t" + time);

            if (YMin != YMax && XMin != XMax) { // This is to prevent some errors where there is no data, and the maxes equal the mins, which StdDraw does not like.
              StdDraw.setCanvasSize(250, 250);  // This determines picture size (in px). It's low now to keep sizes small, but increase if you want higher quality.
              StdDraw.setYscale(YMin, YMax);  // Min and max y values for the plot.
              StdDraw.setXscale(XMin, XMax);  // Min and max x values for the plot.
            }

            if (filePath.equals(exportPath)) {  // Check if import and export paths are the same.
              System.out.println("Import and export path must be different.");
            }
            else {
              PrintWriter dataWriter = new PrintWriter(exportPath, "UTF-8"); // Exports a txt file with two columns that are tab seperated. First one is energy, second one is EY/I0 ES. In addition exports jpeg of plot.
              dataWriter.println("Energy\tEY/Clock");
              for (int i = 0; i < rowNum; i++) {
                dataWriter.println(XAS[i] [0] + "\t" + XAS[i] [1]);
                if (i > 0) {
                  StdDraw.line(prevX, prevY, XAS[i] [0], XAS[i] [1]);
                  prevX = XAS[i] [0];
                  prevY = XAS[i] [1];
                }
              }
              dataWriter.close();
              StdDraw.save(exportDirectory + strippedName + ".jpg"); // Export path for plots is the same as for the txt files.
            }
          }

        }
      }

    }
    spinWriter.close();

  }
}
