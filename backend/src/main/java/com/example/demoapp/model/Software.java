// src/main/java/com/example/demoapp/model/Software.java
package com.example.demoapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "software")
public class Software {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String videoUrl;
    private String zipUrl;
    private double price;
    private String uploadedBy; // Add this field


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getZipUrl() { return zipUrl; }
    public void setZipUrl(String zipUrl) { this.zipUrl = zipUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

   public String getUploadedBy() { return uploadedBy; }
   public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

}
