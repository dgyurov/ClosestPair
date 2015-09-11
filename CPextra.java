import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CPextra{
	
     /* 
     *
     * by Denitsa Mlechkova, Andreas Rangholm, Dimitar Gyurov and Lars Ommen - September 2015
	 *
	 */

	public static List<Point> parsefile(String fileName) {

		//Create a new File object using the passed filename
		File file = new File(fileName);
		
		//Initialize an arraylist of points
		ArrayList<Point> points = new ArrayList<Point>();
		Scanner scanner = null;

		//Initialize the scanner
		try {
			scanner = new Scanner(file);
		} catch (Exception e) {
			System.exit(0);
		}
		
		
		try {
			//Skip until we reach the coordinates section
			while (true) {
				String l = scanner.nextLine();
				if (l.trim().equals("NODE_COORD_SECTION")) {
					break;
				}
			}
			
			//Scan each line until the EOF mark and add each point to the arraylist of points
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				if (!line.equals("EOF")) {
					
					//Split by any kind of white space
					String[] splitLine = line.split("\\s+");
					Point point = null;
					point = new Point(splitLine[0], splitLine[1], splitLine[2]);
					points.add(point);
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error at file: " + fileName);
		}
		return points;
	}
	
	public static String algorithm(String file){
		//Parse the current file and populate the List of points
		List<Point> points = parsefile(file);

		//This list will hold all points sorted by X
		List<Point> pointsSortedByX = new ArrayList<Point>(points);
		Mergesort.sort(pointsSortedByX, true);

		//This list will hold all points sorted by Y
		List<Point> pointsSortedByY = new ArrayList<Point>(points);
		Mergesort.sort(pointsSortedByY, false);

		//Trim the unnecessary file paths for better output
		String trimedFileNameForOutput = file.substring(file.lastIndexOf("/") + 1).trim();
		
		//Run the actual algorithm on the points and put the result into delta object
		Delta delta = closestPair(pointsSortedByX, pointsSortedByY);
		
		//return a String with the results
		return "../data-cp/" + trimedFileNameForOutput + ": " + points.size()+ " " + delta.getPair()[0].getName() + " " + delta.getPair()[1].getName() + " " + tidyDouble(delta.getDelta());
	}

	public static Delta closestPair(List<Point> pointsByX, List<Point> pointsByY) {	

		// If there are less than 4 points it will be much faster to just compare them directly
		if (pointsByX.size() <= 4) {
			Delta delta = new Delta();
			for (int i = 0; i < pointsByX.size(); i++) {
				// Get first point
				Point point1 = pointsByX.get(i);
				
				for (int j = 0; j < pointsByX.size(); j++) {
					// Get second point
					Point point2 = pointsByX.get(j);
					if (!point1.equals(point2)) {
						
						// Get the distance between each point and if its lower than current delta, replace it.
						double distance = euclideanDistance(point1, point2);
						if (distance < delta.getDelta() || delta.getDelta() == -1) {
							replaceDelta(delta, point1, point2, distance);
						}
						
					}

				}

			}
			return delta;
		}
		
		//Find the number of points devided by 2
		int splitX = pointsByX.size() / 2;
		
		//Put half of the points in the left list
		List<Point> leftHalf = pointsByX.subList(0, splitX);
		
		//The left half sorted by X
		List<Point> leftHalfX = new ArrayList<Point>(leftHalf);
		List<Point> leftHalfY = new ArrayList<Point>();
		
		//Get the X coordinate of the right most point in the left plane
		double lastXinLeft = leftHalfX.get(leftHalfX.size() - 1).getX();

		//Populate the list with all the points in left plane sorted by Y
		for (int i = 0; i < pointsByY.size(); i++) {
			if (pointsByY.get(i).getX() <= lastXinLeft) {
				leftHalfY.add(new Point(pointsByY.get(i)));
			}
		}

		//Put the other half in the right list
		List<Point> rightHalf = pointsByX.subList(splitX, pointsByX.size());
		
		//The right half sorted by X
		List<Point> rightHalfX = new ArrayList<Point>(rightHalf);
		List<Point> rightHalfY = new ArrayList<Point>();

		//Get the X coordinate of the left most point in the right plane
		double firstXinRight = rightHalfX.get(0).getX();

		//Populate the list with all the points in right plane sorted by Y
		for (int i = 0; i < pointsByY.size(); i++) {
			if (pointsByY.get(i).getX() >= firstXinRight) {
				rightHalfY.add(new Point(pointsByY.get(i)));
			}
		}
	        
		// The resulting Delta from the right plane
		Delta deltaLeft = closestPair(leftHalfX, leftHalfY);

		// The resulting Delta from the right plane
		Delta deltaRight = closestPair(rightHalfX, rightHalfY);

		// Get the minimum from the two
		Delta delta = deltaLeft.isLowerThan(deltaRight) ? deltaLeft : deltaRight;

		// Get the s line X value
		double lineX = (lastXinLeft + (firstXinRight - lastXinLeft) / 2);
		
		//Get the limits of the S strip
		double lowerBound = lineX - delta.getDelta();
		double upperBound = lineX + delta.getDelta();

		// Build an arraylist with only the points inside the margins of the S strip
		ArrayList<Point> pointsInStrip = new ArrayList<Point>();
		for (int i = 0; i < pointsByY.size(); i++) {
			Point p = new Point(pointsByY.get(i));
			if (p.getX() > lowerBound && p.getX() < upperBound) {
				pointsInStrip.add(p);
			}
		}

		// If the S strip is not empty
		if (pointsInStrip.size() > 1) {
			
			double currentDelta = delta.getDelta();
			
			//Run for all the points but the last one
			final int TO = pointsInStrip.size() - 1;
			for (int i = 0; i < TO; i++) {
				//Get first point
				Point pointOne = pointsInStrip.get(i);
				
				//Calculate next 7 points or, remaining points in case of less than 7
				int jEnd = i + 7 < pointsInStrip.size() ? i + 7 : pointsInStrip.size();
				for (int j = i + 1; j < jEnd; j++) {
					//Get the second points
					Point pointTwo = pointsInStrip.get(j);

					//If their distance is lower than current delta, replace the current delta with the new one
					if (Math.abs(pointTwo.getY() - pointOne.getY()) < currentDelta) {
						double distance = euclideanDistance(pointOne, pointTwo);
						if (distance < currentDelta) {
							replaceDelta(delta, pointOne, pointTwo, distance);
						}
					} else {
						break;
					}
				}
			}
		}
		
		//Return the result
		return delta;

	}

	//Function used to update the current delta
	private static void replaceDelta(Delta delta, Point point1, Point point2, double distance) {
        delta.setDelta(distance);
        delta.setPair(point1, point2);
	}
	
	//Calculate the Euclidean distance between two points
	public static double euclideanDistance(Point point1, Point point2) {
		double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	public static void main(String[] args) {
		//Set the data folder
		Path dir = Paths.get("data-cp");
        
		//Initialize a list of strings
		List<String> files = new ArrayList<String>();
        
		//Get all file paths inside the directory and populate the list
        files = getAllFileNames(files, dir);
        
        for (String file : files) {
        	System.out.println(algorithm(file));
		}
	}

    public static List<String> getAllFileNames(List<String> fileNames, Path dir) {
        try {
            //Create a new dir stream
        	DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            
            for (Path path : stream) {
            	//If its a sub dir, call the method recursively 
                if (path.toFile().isDirectory()) {
                    getAllFileNames(fileNames, path);
                } else {
                	//Otherwise add the file path to the list if it's a tsp one
                	String fileName = path.toAbsolutePath().toString();
                	if(fileName.contains("tsp")){
                        fileNames.add(fileName);
                	}
                }
            }
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileNames;
    }
    
    //Make the double better readable and match the output requirements
    public static String tidyDouble(double d)
    {
    	DecimalFormat decimalFormat = new DecimalFormat("#0.0000000000000000");
        if(d == (long) d)
            return String.format("%d",(long)d);
        else
            return String.format("%s",decimalFormat.format(d));
    }
    
}

class Point {

	// Once a point is created we don't have to set it again, so we make it final
	private final double x, y;
	private final String name;

	public Point(String name, String x, String y) {
		this.name = name;
		this.x = Double.parseDouble(x);
		this.y = Double.parseDouble(y);
	}

	public Point(Point oldpoint) {
		this.name = oldpoint.name;
		this.x = oldpoint.x;
		this.y = oldpoint.y;
	}

	public String toString() {
		return this.name + ":" + this.x + ", " + this.y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public String getName() {
		return name;
	}
}

class Delta {

	// A delta class, which holds the current smallest distance and also the pair of points involved
	private double delta = -1;
	private Point[] pair = new Point[2];

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	public void setPair(Point point1, Point point2) {
		pair[0] = point1;
		pair[1] = point2;
	}

	public Point[] getPair() {
		return pair;
	}

	public boolean isLowerThan(Delta d) {
		// If sets are empty
		if (delta == -1 || d.getDelta() == -1) {
			return true;
		}
		return delta < d.getDelta();
	}

}

class Mergesort {

	//Depends on either we sort by X or by Y
    private static boolean sortByX = true;
    private static Point[] numbers;
    private static Point[] addition;

    public static void sort(List<Point> values, boolean sortingByX) {
        
    	sortByX = sortingByX;

    	//Pass the size
        int capacity = values.size();
        
        //Initialize the numbers array
        numbers = new Point[capacity];
        
        //Populate the numbers array
        for (int i = 0; i < values.size(); i++) {
            numbers[i] = values.get(i);
        }
        
        //Initialize the addition array
        addition = new Point[capacity];
        mergesort(0, capacity - 1);
        
        //Update the source
        for (int i = 0; i < numbers.length; i++) {
            values.set(i, numbers[i]);
        }
        
    }

	private static void mergesort(int low, int high) {
		
		// Check if low is smaller then high, if not then the array is sorted
		if (low < high) {
			
			// Get the index of the element which is in the middle
			int middle = low + (high - low) / 2;
			
			// Sort the left side of the array
			mergesort(low, middle);
			
			// Sort the right side of the array
			mergesort(middle + 1, high);
			
			// Combine them both
			merge(low, middle, high);
		
		}
		
	}

    private static void merge(int low, int middle, int high) {
        // Copy both parts into the addition array
        for (int i = low; i <= high; i++) {
            addition[i] = numbers[i];
        }
        
        //Initialize pointers
        int i = low;
        int j = middle + 1;
        int k = low;
        
        // Copy the smallest values to the original array
        while (i <= middle && j <= high) {
            double first = addition[i].getX();
            double second = addition[j].getX();

            //If we sort by Y instead
            if (!sortByX) {
                first = addition[i].getY();
                second = addition[j].getY();
            }
            if (first <= second) {
                numbers[k] = addition[i];
                i++;
            } else {
                numbers[k] = addition[j];
                j++;
            }
            k++;
        }
        
        // Copy the rest of the left side of the array into the target array
        while (i <= middle) {
            numbers[k] = addition[i];
            k++;
            i++;
        }
        
    }
}
