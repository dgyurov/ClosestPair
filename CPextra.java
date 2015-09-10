import java.io.File;
import java.io.FileNotFoundException;
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
	
	String line;
    static int countme = 1;
    
	public static List<Point> parsefile(String fileName){
	       File file = new File(fileName);
	        ArrayList<Point> points = new ArrayList<Point>();
	        Scanner scanner = null;
	        int id = 0;

	        try {
	            scanner = new Scanner(file);
	        } catch (Exception e) {
	            System.out.println("Please add the filename to the arguments.");
	            System.exit(0);
	        }
	        try {
	            while (true) {
	                String l = scanner.nextLine();
	                if (l.trim().equals("NODE_COORD_SECTION")) {
	                    break;
	                }
	            }
	            while (scanner.hasNext()) {
	                String line = scanner.nextLine().trim();
	                if (!line.equals("EOF")) {

	                    String[] splitLine = line.split("\\s+");
	                    Point point = null;
	                    point = new Point(splitLine[0], splitLine[1], splitLine[2],id++);
	                    points.add(point);
	                }
	            }
	        } catch (Exception e) {
	            System.out.println("file error: " + fileName);
	            e.printStackTrace();
	        }
	        return points;
	}
	
	public static String algorithm(String file){
		List<Point> points = parsefile(file);

		List<Point> pointsSortedByX = new ArrayList<Point>(points);
		Mergesort.sort(pointsSortedByX, true);

		List<Point> pointsSortedByY = new ArrayList<Point>(points);
		Mergesort.sort(pointsSortedByY, false);

		String trimedFileNameForOutput = file.substring(file.lastIndexOf("/") + 1).trim();
		Delta delta = closestPair(pointsSortedByX, pointsSortedByY);

		return "../data-cp/" + trimedFileNameForOutput + ": " + points.size()+ " " + delta.getPair()[0].getName() + " " + delta.getPair()[1].getName() + " " + tidyDouble(delta.getDelta());
	}

	public static Delta closestPair(List<Point> pointsByX, List<Point> pointsByY) {	
//TODO
	       if (pointsByX.size() < 4) {
	    	   
	           //If there are less than 4 points it will be much faster to just compare them directly
	           Delta delta = new Delta();
	           for (int i = 0; i < pointsByX.size(); i++) {
	               Point point1 = pointsByX.get(i);
	               for (int j = 0; j < pointsByX.size(); j++) {
	                   Point point2 = pointsByX.get(j);
	                   if (!point1.equals(point2)) {
	                       double distance = euclideanDistance(point1, point2);
	                       if (distance < delta.getDelta() || delta.getDelta() == -1) {
	                           replaceDelta(delta, point1, point2, distance);
	                       }
	                   }
	               }
	           }
	           return delta;
	        }

	        int splitX = pointsByX.size() / 2;
	        List<Point> leftHalf = pointsByX.subList(0, splitX);
	        List<Point> leftHalfX = new ArrayList<Point>(leftHalf);
	        List<Point> leftHalfY = new ArrayList<Point>();
	        
	        double lastXinLeft = leftHalfX.get(leftHalfX.size()-1).getX();
	        
	        for (int i = 0; i < pointsByY.size(); i++) {
				if (pointsByY.get(i).getX()<=lastXinLeft) {
					leftHalfY.add(new Point(pointsByY.get(i)));
				}
			}
	        
	        List<Point> rightHalf = pointsByX.subList(splitX, pointsByX.size());
	        List<Point> rightHalfX = new ArrayList<Point>(rightHalf);
	        List<Point> rightHalfY = new ArrayList<Point>();
	        
	        double firstXinRight = rightHalfX.get(0).getX();
	        
	        for (int i = 0; i < pointsByY.size(); i++) {
				if (pointsByY.get(i).getX()>=firstXinRight) {
					rightHalfY.add(new Point(pointsByY.get(i)));
				}
			}
	        

	        
	        // delta from left half
	        Delta deltaLeft = closestPair(leftHalfX, leftHalfY); // split left
	        
	        
	        // delta from right half
	        Delta deltaRight = closestPair(rightHalfX, rightHalfY); // split right

	        // find minimum delta
	        Delta delta = deltaLeft.isLowerThan(deltaRight) ? deltaLeft : deltaRight;
	        
//	        // we don't have a line... by x coordinate
	        double leftX = lastXinLeft;
	        double rightX = firstXinRight;
	        double lineX = (leftX + (rightX - leftX) / 2);
	        double lowerBound = lineX - delta.getDelta();
	        double upperBound = lineX + delta.getDelta();

	        
//	        System.out.println(countme++);
//	        System.out.println("Left half is that big: "+leftHalf.size());
//	        for (Point point : leftHalf) {
//				System.out.println(point.toString());
//			}
//	        System.out.println("Right half is that big: "+rightHalf.size());
//	        for (Point point : rightHalf) {
//				System.out.println(point.toString());
//			}
//	        System.out.println("The X of the last point in the left half is: "+leftX);
//	        System.out.println("The X of the first point in the right half is: "+rightX);
//	        System.out.println("So the split is at:"+lineX);
//	        System.out.println("============================================================");
	        // remove all elements further than delta from lineX
	        ArrayList<Point> pointsByLine = new ArrayList<Point>();
	        for (int i = 0; i < pointsByY.size(); i++) {
	            Point p = new Point(pointsByY.get(i));
	            if (p.getX() > lowerBound && p.getX() < upperBound) {
	            	pointsByLine.add(p);
	            }   
	        }
	        
	        //if there are points to be processed
	        if (pointsByLine.size() > 1) {

	            double twoDeltaHalf = delta.getDelta();

	            final int TO = pointsByLine.size() - 1;
	            for (int i = 0; i < TO; i++) {
	                Point pointOne = pointsByLine.get(i);
	                int jTO = i + 7 < pointsByLine.size() ? i + 7 : pointsByLine.size();
	                for (int j = i + 1; j < jTO; j++) {
	                    Point pointTwo = pointsByLine.get(j);
	                    if (Math.abs(pointTwo.getY() - pointOne.getY()) < twoDeltaHalf) {
	                        double distance = euclideanDistance(pointOne, pointTwo);
	                        if (distance < twoDeltaHalf) {
	                            replaceDelta(delta, pointOne, pointTwo, distance);
	                        }
	                    } else {
	                        break;
	                    }
	                }
	            }
	        }
	        return delta;
	        
	}

	private static void replaceDelta(Delta delta, Point point1, Point point2, double distance) {
        delta.setDelta(distance);
        delta.setPair(point1, point2);
	}

	public static double euclideanDistance(Point point1, Point point2) {
		double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	public static void main(String[] args) {
		Path dir = Paths.get("data-cp");
        List<String> files = new ArrayList<String>();
        
        files = getAllFileNames(files, dir);
        
        for (String file : files) {
        	System.out.println(algorithm(file));
		}
	}

    public static List<String> getAllFileNames(List<String> fileNames, Path dir) {
        try {
            
        	DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            
            for (Path path : stream) {
            	
                if (path.toFile().isDirectory()) {
                    getAllFileNames(fileNames, path);
                } else {
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
	
	//Once a point is created we don't have to set it again, so we make it final
    private final double x, y;
    private final String name;
    
    public Point(String name, String x, String y, int id) {
        this.name = name;
        this.x = Double.parseDouble(x);
        this.y = Double.parseDouble(y);
    }
    
    public Point(Point oldpoint){
    		this.name = oldpoint.name;
    		this.x = oldpoint.x;
    		this.y = oldpoint.y;
    }
    
    public String toString(){
    		return this.name +":"+ this.x +", "+ this.y;
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

    	// in case of empty sets
        if (delta == -1 || d.getDelta() == -1) {
            return true;
        }
        return delta < d.getDelta();
    }

}

class Mergesort {

    private static boolean sortByX = true;
    private static Point[] numbers;
    private static Point[] helper;

    public static void sort(List<Point> values, boolean shouldSortByX) {
        sortByX = shouldSortByX;
        int capacity = values.size();
        numbers = new Point[capacity];
        for (int i = 0; i < values.size(); i++) {
            numbers[i] = values.get(i);
        }
        helper = new Point[capacity];
        mergesort(0, capacity - 1);

        for (int i = 0; i < numbers.length; i++) {
            values.set(i, numbers[i]);
        }
    }

    private static void mergesort(int low, int high) {
        // check if low is smaller then high, if not then the array is sorted
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
        // Copy both parts into the helper array
        for (int i = low; i <= high; i++) {
            helper[i] = numbers[i];
        }
        int i = low;
        int j = middle + 1;
        int k = low;
        // Copy the smallest values from either the left or the right side back
        // to the original array
        while (i <= middle && j <= high) {
            double first = helper[i].getX();
            double second = helper[j].getX();
            if (!sortByX) {
                first = helper[i].getY();
                second = helper[j].getY();
            }
            if (first <= second) {
                // if (helper[i].getX() <= helper[j].getX()) {
                numbers[k] = helper[i];
                i++;
            } else {
                numbers[k] = helper[j];
                j++;
            }
            k++;
        }
        // Copy the rest of the left side of the array into the target array
        while (i <= middle) {
            numbers[k] = helper[i];
            k++;
            i++;
        }
    }
}
