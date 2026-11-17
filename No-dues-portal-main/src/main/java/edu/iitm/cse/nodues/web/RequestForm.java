package edu.iitm.cse.nodues.web;

import jakarta.validation.constraints.NotBlank;

public class RequestForm {

    @NotBlank
    private String facultyAdvisorId;

    @NotBlank
    private String details;

    public String getFacultyAdvisorId() {
        return facultyAdvisorId;
    }

    public void setFacultyAdvisorId(String facultyAdvisorId) {
        this.facultyAdvisorId = facultyAdvisorId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
