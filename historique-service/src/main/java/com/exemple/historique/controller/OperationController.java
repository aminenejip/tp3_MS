package com.exemple.historique.controller;

import com.exemple.historique.client.CalculClient;
import com.exemple.historique.model.Operation;
import com.exemple.historique.service.OperationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/operations")
public class OperationController {
    private static final Set<String> OPERATEURS =
            Set.of("somme", "soustraction", "produit", "division");

    private final CalculClient calculClient;
    private final OperationService operationService;

    public OperationController(CalculClient calculClient,
                                OperationService operationService) {
        this.calculClient = calculClient;
        this.operationService = operationService;
    }

    private Operation calculer(Operation operation) {
        if (operation == null || !OPERATEURS.contains(operation.getOperateur())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Opérateur invalide");
        }
        try {
            operation.setResultat(calculClient.calculer(
                    operation.getOperateur(), operation.getA(), operation.getB()));
            return operation;
        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getResponseBodyAsString(), exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "calcul-service est indisponible", exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors du calcul", exception);
        }
    }

    @PostMapping
    public ResponseEntity<Operation> creer(@RequestBody Operation operation) {
        Operation operationCalculee = calculer(operation);
        Operation sauvegardee = operationService.ajouter(operationCalculee);
        URI location = URI.create("/operations/" + sauvegardee.getId());
        return ResponseEntity.created(location).body(sauvegardee);
    }

    @GetMapping
    public ResponseEntity<java.util.List<Operation>> lister() {
        return ResponseEntity.ok(operationService.lister());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> lire(@PathVariable int id) {
        Operation operation = operationService.lire(id);
        return operation == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(operation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Operation> modifier(@PathVariable int id,
                                               @RequestBody Operation operation) {
        if (!operationService.existe(id)) {
            return ResponseEntity.notFound().build();
        }
        Operation operationCalculee = calculer(operation);
        return ResponseEntity.ok(operationService.remplacer(id, operationCalculee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable int id) {
        if (!operationService.existe(id)) {
            return ResponseEntity.notFound().build();
        }
        operationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
