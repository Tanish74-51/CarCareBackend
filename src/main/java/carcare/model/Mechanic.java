package CarCareBackend.src.main.java.carcare.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Mechanic {

    private UUID id;
    private String name;
    private String phoneNumber;
    private String email;
    private int yearsOfExperience;
    private UUID serviceCentreId;
    private boolean available;
    private boolean acceptingRequests;
    private LocalDateTime busyUntil;      // set by the server when a job is assigned; null = not busy
    private String address;
    private String city;
    private String state;
    private String postalCode;

    public Mechanic() {
        this.id = UUID.randomUUID();
        this.available = true;
        this.acceptingRequests = true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public UUID getServiceCentreId() {
        return serviceCentreId;
    }

    public void setServiceCentreId(UUID serviceCentreId) {
        this.serviceCentreId = serviceCentreId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAcceptingRequests() {
        return acceptingRequests;
    }

    public void setAcceptingRequests(boolean acceptingRequests) {
        this.acceptingRequests = acceptingRequests;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDateTime getBusyUntil() {
        return busyUntil;
    }

    public void setBusyUntil(LocalDateTime busyUntil) {
        this.busyUntil = busyUntil;
    }
}
