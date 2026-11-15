package com.example.app;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // You don't need to write any methods!
    // JpaRepository gives you .save(), .findAll(), .deleteById(), etc. for free.
}