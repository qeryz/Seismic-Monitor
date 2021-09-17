package SeismicMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * Parses live data from the USGS, displaying geospatial information using the Unfolding Maps library.
 * @author Marcos Padilla
 * Date: August 1, 2021
 * */
public class EarthquakeCityMap extends PApplet {
	
	// You can ignore this.  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;
	
	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";
	
	

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	HashMap<String, DataEntry> dataEntriesMap;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// A list of sorted quakes
	private List<EarthquakeMarker> sortedQuakes = new ArrayList<EarthquakeMarker>();
	
	// For markers that are hovered over or clicked.
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	public void setup() {		
		// (1) Initializing canvas and map tiles
		size(1100, 700, OPENGL);
		if (offline) {
		    map = new UnfoldingMap(this, 433, 50, 650, 600);
		    map.setBackgroundColor(240);
		    earthquakesURL = "2.5_week.atom";  // Local earthquake feed
		}
		else {
			map = new UnfoldingMap(this, 433, 50, 650, 600, new Google.GoogleMapProvider());
			// to test local file, uncomment the next line
		    //earthquakesURL = "2.5_week.atom";
		}
		MapUtils.createDefaultEventDispatcher(this, map);
		
		//testing
		//earthquakesURL = "test1.atom";
		//earthquakesURL = "test2.atom";
		
		
		
		// (2) Reading in earthquake data
		
	    	//Step 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
			// Load population data
		dataEntriesMap = loadPopulationDensityFromCSV("countries-population-density.csv");
		//println("Loaded " + dataEntriesMap.size() + " data entries");
		
			// Country markers are shaded according to its population density (only once)
		shadeCountries();
		
			//Step 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
			//Step 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }

	    // could be used for debugging
	    //printQuakes();
	    sortQuakes();
	 		
	    // (3) Add markers to map
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    map.addMarkers(countryMarkers);
	    
	    
	}  // End setup
	
	
	public void draw() {
		background(85);
		map.draw();
		addKey();
		printQuakes(19);
	}
	
	// Used to sort earthquakes in descending order of magnitude
	private void sortQuakes() {	
		EarthquakeMarker eM;
			
		for(Marker m : quakeMarkers) {
			eM = (EarthquakeMarker)(m);
			sortedQuakes.add(eM);
		}
		
		Collections.sort(sortedQuakes);
		
		
	}
	
	// Prints sorted earthquakes
	private void printQuakes(int numToPrint) {
		
		String toPrint;
		EarthquakeMarker m;
		int counter = 0;
		int actualNumToPrint = (numToPrint >= sortedQuakes.size() ? sortedQuakes.size() : numToPrint);
		
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 320;
		
		rect(xbase, ybase, 390, 330);
		fill(0);
		textSize(13);
		text("Top Seismic Activity In The Past 24 hours", 90, 335);
		
		for (int index = 0; index < actualNumToPrint; index++ ) {
			m = sortedQuakes.get(index);
			toPrint = m.toString();
			fill(0);
			textSize(12);
			text(toPrint, 30, 360 + counter);
			counter += 15;
		}
	}
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m : markers) 
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else if (lastClicked == null) 
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}
	
	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) 
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}		
	}
	
	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker) m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) 
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() {	
		
		// Earthquake map key
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 50;
		
		rect(xbase, ybase, 150, 250);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);
		
		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);
		
		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);
		
		fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);
		
		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);
		
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);
		
		
		// Population density bar and text key
		fill(255, 250, 240);
		rect(xbase + 240, ybase, 150, 250);
		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("Population density", xbase + 260, ybase + 30);
		textSize(10);
		text("(per square km)", xbase + 273, ybase + 41);
		fill(color(255, 0, 0, 400));
		rect(xbase+260, ybase+60, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("400+", xbase + 280, ybase + 63);
		fill(color(255, 0, 0, 190));
		rect(xbase+260, ybase+70, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("300 - 400", xbase + 280, ybase + 74);
		fill(color(255, 0, 0, 150));
		rect(xbase+260, ybase+80, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("200 - 300", xbase + 280, ybase + 84);
		fill(color(255, 0, 0, 100));
		rect(xbase+260, ybase+90, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("100 - 200", xbase + 280, ybase + 94);
		fill(color(255, 0, 0, 50));
		rect(xbase+260, ybase+100, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("30 - 100", xbase + 280, ybase + 104);
		fill(color(255, 0, 0, 15));
		rect(xbase+260, ybase+110, 10, 10);
		fill(0, 0, 0);
		textSize(11);
		text("10 - 30", xbase + 280, ybase + 114);
		
		
		// Past hour marker
		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);
		
		
	}

	// Used to shade countries based on population density
	public void shadeCountries() {
		for (Marker marker : countryMarkers) {
			// Find data for country of the current marker
			String countryId = marker.getId();
			DataEntry dataEntry = dataEntriesMap.get(countryId);

			if (dataEntry != null && dataEntry.value != null) {
				// Encode value as brightness (values range: 0-1000)
				float transparency = map(dataEntry.value, 0, 700, 10, 255);
				marker.setColor(color(255, 0, 0, transparency));
			} else {
				// No value available
				marker.setColor(color(100, 120));
			}
		}
	}
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {
		
		// loop over all countries to check if location is in any of them
		// If it is, add one to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	// loop through the country markers or country features
	// and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property, 
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers)
			{
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}
	
	
	
	// Helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}
	
	// Parser for population density
	public HashMap<String, DataEntry> loadPopulationDensityFromCSV(String fileName) {
		HashMap<String, DataEntry> dataEntriesMap = new HashMap<String, DataEntry>();
	
		String[] rows = loadStrings(fileName);
		for (String row : rows) {
			// Reads country name and population density value from CSV row
			// split row by commas not in quotations
			String[] columns = row.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			if (columns.length >= 3) {
				DataEntry dataEntry = new DataEntry();
				dataEntry.countryName = columns[0];
				dataEntry.id = columns[1];
				dataEntry.value = Float.parseFloat(columns[2]);
				dataEntriesMap.put(dataEntry.id, dataEntry);
			}
		}
	
		return dataEntriesMap;
	}
	
	// class object for population density
	class DataEntry {
		String countryName;
		String id;
		Integer year;
		Float value;
	}

}
