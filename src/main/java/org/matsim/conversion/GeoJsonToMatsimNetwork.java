package org.matsim.conversion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GeoJsonToMatsimNetwork {

    // Haversine distance formula for more accurate geographic distance
    public static double haversineDistance(Coord from, Coord to) {
        final int R = 6371; // Radius of Earth in km
        double lat1 = Math.toRadians(from.getY());
        double lat2 = Math.toRadians(to.getY());
        double dlat = lat2 - lat1;
        double dlon = Math.toRadians(to.getX() - from.getX());

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    // formulas taken from: https://www.fhwa.dot.gov/policyinformation/pubs/pl18003/hpms_cap.pdf
    // assuming speed is passed as km/h but necessary to be mph
    public static double calculateCapacity(double speedKmh, int lanes) {
        // Convert speed from km/h to mph
        double speedMph = speedKmh * 0.621371;

        // Calculate BaseCapacity based on the free flow speed (FFS) in mph
        double baseCapacity;
        if (speedMph <= 60) {
            baseCapacity = 1000 + 20 * speedMph;
        } else {
            baseCapacity = 2200;
        }

        // Since we are assuming no heavy vehicles, f_HV is 1
        double fHV = 1;

        // Calculate the total capacity
        double capacity = baseCapacity * fHV * lanes;

        return capacity;
    }


    public static void main(String[] args) throws Exception {
        String geoJsonFilePath = "/Users/kimberly/Documents/Perimeter/matsim/matsim-example-project/src/main/java/org/matsim/conversion/perimeter-example-export-data.geojson";
        String matsimXmlFilePath = "/Users/kimberly/Documents/Perimeter/matsim/matsim-example-project/src/main/java/org/matsim/conversion/perimeter-example-export-network.xml";

        double speed = 35.15; // km/h
        int numberOfLanes = 1;
        double capacity = calculateCapacity(speed, numberOfLanes);

        // Initialize MATSim network
        Network network = NetworkUtils.createNetwork();
        NetworkFactory factory = network.getFactory();

        // Parse GeoJSON file
        JsonParser parser = new JsonParser();
        JsonObject geoJson = parser.parse(new FileReader(geoJsonFilePath)).getAsJsonObject();

        // Map to store the nodes (MATSim nodes need to have unique IDs)
        Map<String, Node> nodeMap = new HashMap<>();

        // Extract features array from GeoJSON
        JsonArray features = geoJson.getAsJsonArray("features");

        int linkIdCounter = 0;

        for (JsonElement featureElement : features) {
            JsonObject feature = featureElement.getAsJsonObject();
            String geometryType = feature.getAsJsonObject("geometry").get("type").getAsString();

            if (geometryType.equals("LineString")) {
                // Get the coordinates for the LineString
                JsonArray coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates");

                for (int i = 0; i < coordinates.size() - 1; i++) {
                    JsonArray fromCoordArray = coordinates.get(i).getAsJsonArray();
                    JsonArray toCoordArray = coordinates.get(i + 1).getAsJsonArray();

                    Coord fromCoord = new Coord(fromCoordArray.get(0).getAsDouble(), fromCoordArray.get(1).getAsDouble());
                    Coord toCoord = new Coord(toCoordArray.get(0).getAsDouble(), toCoordArray.get(1).getAsDouble());

                    // Create or fetch 'from' node, rounding to avoid precision issues
                    String fromNodeId = String.format("node_%.5f_%.5f", fromCoord.getX(), fromCoord.getY());
                    if (!nodeMap.containsKey(fromNodeId)) {
                        Node fromNode = factory.createNode(Id.createNodeId(fromNodeId), fromCoord);
                        network.addNode(fromNode);
                        nodeMap.put(fromNodeId, fromNode);
                    }

                    // Create or fetch 'to' node, rounding to avoid precision issues
                    String toNodeId = String.format("node_%.5f_%.5f", toCoord.getX(), toCoord.getY());
                    if (!nodeMap.containsKey(toNodeId)) {
                        Node toNode = factory.createNode(Id.createNodeId(toNodeId), toCoord);
                        network.addNode(toNode);
                        nodeMap.put(toNodeId, toNode);
                    }

                    // Create a forward link from 'from' to 'to'
                    String forwardLinkId = "link_" + linkIdCounter++;
                    Link forwardLink = factory.createLink(Id.createLinkId(forwardLinkId), nodeMap.get(fromNodeId), nodeMap.get(toNodeId));
                    forwardLink.setLength(haversineDistance(fromCoord, toCoord));
                    forwardLink.setFreespeed(speed / 3.6); // convert to m/s
                    forwardLink.setCapacity(capacity);
                    forwardLink.setNumberOfLanes(numberOfLanes);
                    forwardLink.setAllowedModes(Set.of(TransportMode.car)); // Allow cars
                    network.addLink(forwardLink);

                    // Create a reverse link from 'to' to 'from' for bidirectional traffic
                    String reverseLinkId = "link_" + linkIdCounter++;
                    Link reverseLink = factory.createLink(Id.createLinkId(reverseLinkId), nodeMap.get(toNodeId), nodeMap.get(fromNodeId));
                    reverseLink.setLength(haversineDistance(toCoord, fromCoord));
                    reverseLink.setFreespeed(speed / 3.6); // convert to m/s
                    reverseLink.setCapacity(capacity);
                    reverseLink.setNumberOfLanes(numberOfLanes);
                    reverseLink.setAllowedModes(Set.of(TransportMode.car)); // Allow cars
                    network.addLink(reverseLink);
                }

            }
        }

        // Write the MATSim network to XML
        new NetworkWriter(network).write(matsimXmlFilePath);
        System.out.println("MATSim network saved to: " + matsimXmlFilePath);
    }
}
