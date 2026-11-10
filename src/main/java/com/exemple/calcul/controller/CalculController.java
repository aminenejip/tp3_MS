package com.exemple.calcul.controller;
import com.exemple.calcul.model.Resultat;
import com.exemple.calcul.service.Calculatrice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/calcul")
public class CalculController {
    private final Calculatrice calculService;
    // Injection par constructeur : Spring fournit l'objet Calculatrice
    public CalculController(Calculatrice calculService) {
        this.calculService = calculService;
    }
    @GetMapping(value = "/somme", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> somme(@RequestParam("a") int a,
                                        @RequestParam("b") int b) {
        int result = calculService.somme(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }
    @GetMapping(value = "/soustraction", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> soustraction(@RequestParam("a") int a,
                                        @RequestParam("b") int b) {
        int result = calculService.soustraction(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }
    @GetMapping(value = "/produit", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> produit(@RequestParam("a") int a,
                                        @RequestParam("b") int b) {
        int result = calculService.produit(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }
    @GetMapping(value = "/produit/{a}/{b}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> produitParChemin(@PathVariable int a,
                                                    @PathVariable int b) {
        int result = calculService.produit(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }
    @GetMapping(value = "/division", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> division(@RequestParam("a") int a,
                                           @RequestParam("b") int b) {
        if (b == 0) {
            return ResponseEntity.badRequest()
                    .body("Erreur : division par 0 impossible");
        }
        double result = calculService.division(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }
    @GetMapping(value = "/somme/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public Resultat sommeDetail(@RequestParam("a") int a,
                                @RequestParam("b") int b) {
        return new Resultat("somme", a, b, calculService.somme(a, b));
    }

    @GetMapping(value = "/division/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resultat> divisionDetail(@RequestParam("a") int a,
                                                    @RequestParam("b") int b) {
        if (b == 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                new Resultat("division", a, b, calculService.division(a, b)));
    }
}
