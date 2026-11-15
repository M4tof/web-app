package com.example.app;

import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    private final ItemRepository itemRepository;

    public ApiController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public record ItemRequest(String name, String description) {}

    @PostMapping("/api/submit")
    public String handleSubmission(@RequestBody ItemRequest request) {
        // 1. Create and save the entity into the DB
        Item item = new Item(request.name(), request.description());
        Item savedItem = itemRepository.save(item);

        // 2. Log confirmation to terminal
        System.out.println("LOG: Saved to DB with ID #" + savedItem.getId() + " -> " + savedItem.getName());

        // 3. Return message to the frontend
        return "Saved item to database! (Assigned ID: " + savedItem.getId() + ")";
    }
}