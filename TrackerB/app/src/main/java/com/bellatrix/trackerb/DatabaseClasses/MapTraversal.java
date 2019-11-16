package com.bellatrix.trackerb.DatabaseClasses;

import java.util.HashMap;
import java.util.List;

public class MapTraversal {

    public String distance, duration;
    public List<List<HashMap<String, String>>> routes;

    public MapTraversal() {
        this.distance = "";
        this.duration = "";
        this.routes = null;
    }

    public MapTraversal(String distance, String duration, List<List<HashMap<String, String>>> routes) {
        this.distance = distance;
        this.duration = duration;
        this.routes = routes;
    }

}
