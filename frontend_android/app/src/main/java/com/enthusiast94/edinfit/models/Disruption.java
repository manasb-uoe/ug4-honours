package com.enthusiast94.edinfit.models;

import java.util.List;

/**
 * Created by manas on 06-02-2016.
 */
public class Disruption {

    private int id;
    private String type;
    private String category;
    private String summary;
    private List<String> servicesAffected;
    private String webLink;
    private long updatedAt;

    public Disruption(int id, String type, String category, String summary, List<String> servicesAffected,
                      String webLink, long updatedAt) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.summary = summary;
        this.servicesAffected = servicesAffected;
        this.webLink = webLink;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getServicesAffected() {
        return servicesAffected;
    }

    public String getWebLink() {
        return webLink;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
