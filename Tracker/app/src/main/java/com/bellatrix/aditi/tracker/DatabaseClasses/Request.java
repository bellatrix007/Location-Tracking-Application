package com.bellatrix.aditi.tracker.DatabaseClasses;

public class Request {
    private String user, name;

    public Request() {
        user = "";
        name = "";
    }

    public Request(String user, String name) {
        this.user = user;
        this.name = name;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return this.user;
    }

    public String getName() {
        return this.name;
    }
}
