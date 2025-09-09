// src/main/java/com/example/demoapp/dto/PromoteRequest.java
package com.example.demoapp.dto;

public class PromoteRequest {
    private boolean makeAdmin;

    // Default constructor
    public PromoteRequest() {}

    // Constructor
    public PromoteRequest(boolean makeAdmin) {
        this.makeAdmin = makeAdmin;
    }

    // Getters and setters
    public boolean isMakeAdmin() {
        return makeAdmin;
    }

    public void setMakeAdmin(boolean makeAdmin) {
        this.makeAdmin = makeAdmin;
    }
}
