# Seismic Monitor
 Graphical user interface in Java to display earthquakes recordings from around the globe.
 
# Installation

Import this folder in Eclipse ('File' -> 'Import' -> 'Existing Projects into
Workspace', Select this folder, 'Finish')


### Manual Installation

If the import does not work follow the steps below.

- Create new Java project
- Copy+Paste all files into project
- Add all lib/*.jars to build path
- Set native library location for jogl.jar. Choose appropriate folder for your OS.
- Add data/ as src


### Trouble Shooting

Switch Java Compiler to 1.6 if you get VM problems. (Processing should work with Java 1.6, and 1.7)

# UML Diagram
![](UML.png)

# Functionality
1. The program parses live data from the USGS, displaying geospatial information using the Unfolding Maps library.
2. Displays a choropleth map, distinct markers to differentiate between earthquake types, along with a chart of the top seismic activities of the past day sorted by their magnitude.
3. Implements appropriate event handling methods to provide basic user interaction such as hover over marker to peek, click marker to focus on it, and zoom in and out functionality.

![](result.png)
