package com.monopoly.server.monopoly.controllers;

import com.monopoly.server.monopoly.classes.dto.PropertyDto;
import com.monopoly.server.monopoly.classes.dto.PropertyOwnershipDto;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.services.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "http://localhost:4200")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @GetMapping
    public ResponseEntity<List<PropertyDto>> getAllProperties() {
        List<PropertyDto> properties = propertyService.getAllProperties();
        return ResponseEntity.ok(properties);
    }

    @PostMapping("/{propertyId}/purchase")
    public ResponseEntity<PropertyOwnershipDto> purchaseProperty(
            @PathVariable Long propertyId,
            @RequestParam Long playerId) {
        try {
            PropertyOwnershipDto ownership = propertyService.purchaseProperty(playerId, propertyId);
            return ResponseEntity.ok(ownership);
        } catch (PlayerNotFoundException | PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (PropertyAlreadyOwnedException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<PropertyOwnershipDto>> getPlayerProperties(@PathVariable Long playerId) {
        try {
            List<PropertyOwnershipDto> properties = propertyService.getPlayerProperties(playerId);
            return ResponseEntity.ok(properties);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/mortgage")
    public ResponseEntity<PropertyOwnershipDto> mortgageProperty(@PathVariable Long ownershipId) {
        try {
            PropertyOwnershipDto ownership = propertyService.mortgageProperty(ownershipId);
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/redeem")
    public ResponseEntity<PropertyOwnershipDto> redeemProperty(@PathVariable Long ownershipId) {
        try {
            PropertyOwnershipDto ownership = propertyService.redeemProperty(ownershipId);
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/build-house")
    public ResponseEntity<PropertyOwnershipDto> buildHouse(@PathVariable Long ownershipId) {
        try {
            PropertyOwnershipDto ownership = propertyService.buildHouse(ownershipId);
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/build-hotel")
    public ResponseEntity<PropertyOwnershipDto> buildHotel(@PathVariable Long ownershipId) {
        try {
            PropertyOwnershipDto ownership = propertyService.buildHotel(ownershipId);
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{propertyId}/rent")
    public ResponseEntity<BigDecimal> calculateRent(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "7") int diceRoll) {
        try {
            BigDecimal rent = propertyService.calculateRent(propertyId, diceRoll);
            return ResponseEntity.ok(rent);
        } catch (PropertyNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}