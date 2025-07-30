package com.monopoly.server.monopoly.controllers;

import com.monopoly.server.monopoly.classes.dto.GameSessionDto;
import com.monopoly.server.monopoly.classes.request.CreateSessionRequest;
import com.monopoly.server.monopoly.classes.request.JoinSessionRequest;
import com.monopoly.server.monopoly.classes.request.StartGameRequest;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.services.GameSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:4200")
public class GameSessionController {

    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping
    public ResponseEntity<GameSessionDto> createSession(@RequestBody CreateSessionRequest request) {
        try {
            GameSessionDto session = gameSessionService.createSession(request.getHostName());
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{sessionCode}")
    public ResponseEntity<GameSessionDto> getSession(@PathVariable String sessionCode) {
        try {
            GameSessionDto session = gameSessionService.getSession(sessionCode);
            return ResponseEntity.ok(session);
        } catch (SessionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionCode}/join")
    public ResponseEntity<GameSessionDto> joinSession(
            @PathVariable String sessionCode,
            @RequestBody JoinSessionRequest request) {
        try {
            GameSessionDto session = gameSessionService.joinSession(
                    sessionCode,
                    request.getPlayerName(),
                    request.getColor()
            );
            return ResponseEntity.ok(session);
        } catch (SessionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SessionNotJoinableException | SessionFullException | ColorTakenException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{sessionCode}/start")
    public ResponseEntity<Void> startGame(
            @PathVariable String sessionCode,
            @RequestBody StartGameRequest request) {
        try {
            gameSessionService.startGame(sessionCode, request.getHostPlayerId());
            return ResponseEntity.ok().build();
        } catch (SessionNotFoundException | PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException | NotEnoughPlayersException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{sessionCode}")
    public ResponseEntity<Void> endSession(
            @PathVariable String sessionCode,
            @RequestParam Long hostPlayerId) {
        try {
            gameSessionService.endSession(sessionCode, hostPlayerId);
            return ResponseEntity.ok().build();
        } catch (SessionNotFoundException | UnauthorizedException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{sessionCode}/delete")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionCode,
            @RequestParam Long hostPlayerId) {
        try {
            System.out.println("=== DELETE SESSION REQUEST ===");
            System.out.println("Session Code: " + sessionCode);
            System.out.println("Host Player ID: " + hostPlayerId);

            gameSessionService.deleteSession(sessionCode, hostPlayerId);

            System.out.println("Session deleted successfully");
            return ResponseEntity.ok().build();
        } catch (SessionNotFoundException e) {
            System.err.println("Session not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            System.err.println("Error deleting session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}