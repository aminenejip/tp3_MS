package com.exemple.historique.service;

import com.exemple.historique.model.Operation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OperationService {
    private final ConcurrentHashMap<Integer, Operation> operations = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(0);

    public Operation ajouter(Operation operation) {
        int id = sequence.incrementAndGet();
        operation.setId(id);
        operations.put(id, operation);
        return operation;
    }

    public List<Operation> lister() {
        return new ArrayList<>(operations.values());
    }

    public Operation lire(int id) {
        return operations.get(id);
    }

    public boolean existe(int id) {
        return operations.containsKey(id);
    }

    public Operation remplacer(int id, Operation operation) {
        operation.setId(id);
        operations.put(id, operation);
        return operation;
    }

    public void supprimer(int id) {
        operations.remove(id);
    }
}
