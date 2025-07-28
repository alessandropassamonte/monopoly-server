package com.monopoly.server.monopoly.controllers;

import com.monopoly.server.monopoly.classes.dto.PropertyDto;
import com.monopoly.server.monopoly.classes.dto.PropertyOwnershipDto;
import com.monopoly.server.monopoly.classes.dto.TransactionDto;
import com.monopoly.server.monopoly.classes.request.PayRentRequest;
import com.monopoly.server.monopoly.classes.request.TransferPropertyRequest;
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
        try {
            System.out.println("=== GET ALL PROPERTIES REQUEST ===");
            List<PropertyDto> properties = propertyService.getAllProperties();
            System.out.println("Properties found: " + properties.size());
            properties.forEach(p -> System.out.println("- " + p.getName() + " (" + p.getType() + "): " + p.getPrice()));
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            System.err.println("Error getting all properties: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{propertyId}/purchase")
    public ResponseEntity<PropertyOwnershipDto> purchaseProperty(
            @PathVariable Long propertyId,
            @RequestParam Long playerId) {
        try {
            System.out.println("=== PURCHASE PROPERTY REQUEST ===");
            System.out.println("Property ID: " + propertyId + ", Player ID: " + playerId);

            PropertyOwnershipDto ownership = propertyService.purchaseProperty(playerId, propertyId);
            System.out.println("Property purchased successfully: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PlayerNotFoundException | PropertyNotFoundException e) {
            System.err.println("Not found error: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (PropertyAlreadyOwnedException | InsufficientFundsException e) {
            System.err.println("Bad request error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Unexpected error in purchase: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUOVO: Pagamento affitto
     */
    @PostMapping("/{propertyId}/pay-rent")
    public ResponseEntity<TransactionDto> payRent(
            @PathVariable Long propertyId,
            @RequestBody PayRentRequest request) {
        try {
            System.out.println("=== PAY RENT REQUEST ===");
            System.out.println("Property ID: " + propertyId + ", Tenant ID: " + request.getTenantPlayerId() + ", Dice: " + request.getDiceRoll());

            TransactionDto transaction = propertyService.payRent(
                    propertyId,
                    request.getTenantPlayerId(),
                    request.getDiceRoll()
            );
            System.out.println("Rent paid successfully: " + transaction.getAmount());
            return ResponseEntity.ok(transaction);
        } catch (PlayerNotFoundException | PropertyNotFoundException e) {
            System.err.println("Not found error: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidTransactionException | InsufficientFundsException e) {
            System.err.println("Transaction error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Unexpected error in rent payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUOVO: Trasferimento proprietà tra giocatori
     */
    @PostMapping("/ownership/{ownershipId}/transfer")
    public ResponseEntity<PropertyOwnershipDto> transferProperty(
            @PathVariable Long ownershipId,
            @RequestBody TransferPropertyRequest request) {
        try {
            System.out.println("=== TRANSFER PROPERTY REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId + ", New Owner ID: " + request.getNewOwnerId());

            PropertyOwnershipDto ownership = propertyService.transferProperty(
                    ownershipId,
                    request.getNewOwnerId(),
                    request.getPrice()
            );
            System.out.println("Property transferred successfully: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PlayerNotFoundException | PropertyNotFoundException e) {
            System.err.println("Not found error: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidTransactionException | InsufficientFundsException e) {
            System.err.println("Transfer error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Unexpected error in property transfer: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<PropertyOwnershipDto>> getPlayerProperties(@PathVariable Long playerId) {
        try {
            System.out.println("=== GET PLAYER PROPERTIES REQUEST ===");
            System.out.println("Player ID: " + playerId);

            List<PropertyOwnershipDto> properties = propertyService.getPlayerProperties(playerId);
            System.out.println("Properties found for player: " + properties.size());
            properties.forEach(p -> System.out.println("- " + p.getPropertyName() +
                    " (Houses: " + p.getHouses() + ", Hotel: " + p.isHasHotel() + ", Mortgaged: " + p.isMortgaged() + ")"));

            return ResponseEntity.ok(properties);
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error getting player properties: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/mortgage")
    public ResponseEntity<PropertyOwnershipDto> mortgageProperty(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== MORTGAGE PROPERTY REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.mortgageProperty(ownershipId);
            System.out.println("Property mortgaged: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException e) {
            System.err.println("Invalid action: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error mortgaging property: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/redeem")
    public ResponseEntity<PropertyOwnershipDto> redeemProperty(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== REDEEM PROPERTY REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.redeemProperty(ownershipId);
            System.out.println("Property redeemed: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            System.err.println("Action error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error redeeming property: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/build-house")
    public ResponseEntity<PropertyOwnershipDto> buildHouse(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== BUILD HOUSE REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.buildHouse(ownershipId);
            System.out.println("House built on: " + ownership.getPropertyName() +
                    " (Total houses: " + ownership.getHouses() + ")");
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            System.err.println("Build error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error building house: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/ownership/{ownershipId}/build-hotel")
    public ResponseEntity<PropertyOwnershipDto> buildHotel(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== BUILD HOTEL REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.buildHotel(ownershipId);
            System.out.println("Hotel built on: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException | InsufficientFundsException e) {
            System.err.println("Build error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error building hotel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUOVO: Vendita casa
     */
    @PostMapping("/ownership/{ownershipId}/sell-house")
    public ResponseEntity<PropertyOwnershipDto> sellHouse(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== SELL HOUSE REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.sellHouse(ownershipId);
            System.out.println("House sold from: " + ownership.getPropertyName() +
                    " (Remaining houses: " + ownership.getHouses() + ")");
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException e) {
            System.err.println("Sell error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error selling house: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUOVO: Vendita hotel
     */
    @PostMapping("/ownership/{ownershipId}/sell-hotel")
    public ResponseEntity<PropertyOwnershipDto> sellHotel(@PathVariable Long ownershipId) {
        try {
            System.out.println("=== SELL HOTEL REQUEST ===");
            System.out.println("Ownership ID: " + ownershipId);

            PropertyOwnershipDto ownership = propertyService.sellHotel(ownershipId);
            System.out.println("Hotel sold from: " + ownership.getPropertyName());
            return ResponseEntity.ok(ownership);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (InvalidPropertyActionException e) {
            System.err.println("Sell error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error selling hotel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{propertyId}/rent")
    public ResponseEntity<BigDecimal> calculateRent(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "7") int diceRoll) {
        try {
            System.out.println("=== CALCULATE RENT REQUEST ===");
            System.out.println("Property ID: " + propertyId + ", Dice roll: " + diceRoll);

            BigDecimal rent = propertyService.calculateRent(propertyId, diceRoll);
            System.out.println("Calculated rent: " + rent);
            return ResponseEntity.ok(rent);
        } catch (PropertyNotFoundException e) {
            System.err.println("Property not found for rent calculation: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error calculating rent: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * NUOVO: Ottieni tutte le proprietà possedute in una sessione
     */
    @GetMapping("/session/{sessionCode}")
    public ResponseEntity<List<PropertyOwnershipDto>> getSessionProperties(@PathVariable String sessionCode) {
        try {
            System.out.println("=== GET SESSION PROPERTIES REQUEST ===");
            System.out.println("Session Code: " + sessionCode);

            List<PropertyOwnershipDto> properties = propertyService.getSessionProperties(sessionCode);
            System.out.println("Properties found in session: " + properties.size());
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            System.err.println("Error getting session properties: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}