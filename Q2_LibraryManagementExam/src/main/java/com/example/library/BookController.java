package com.example.library;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookRepository repository;
    public BookController(BookRepository repository){this.repository=repository;}

    // POST /books — 201 Created
    @PostMapping
    public ResponseEntity<Book> addBook(@RequestBody Book book){
        book.setId(null);
        if (!valid(book)) return ResponseEntity.badRequest().build();
        Book saved=repository.save(book);
        return ResponseEntity.created(URI.create("/books/"+saved.getId())).body(saved);
    }

    // GET /books — 200 OK
    @GetMapping
    public List<Book> getAllBooks(){return repository.findAll();}

    // GET /books/{id} — 200 OK or 404 Not Found
    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Integer id){
        Optional<Book> book=repository.findById(id);
        if(book.isEmpty())return ResponseEntity.notFound().build();
        return ResponseEntity.ok(book.get());
    }

    // PUT /books/{id} — 200 OK / 400 Bad Request / 404 Not Found
    @PutMapping("/{id}")
    public ResponseEntity<Book> update(@PathVariable Integer id,@RequestBody Book details){
        if(!valid(details))return ResponseEntity.badRequest().build();
        Optional<Book> result=repository.findById(id);
        if(result.isEmpty())return ResponseEntity.notFound().build();
        Book book=result.get();
        book.setTitle(details.getTitle());
        book.setAuthor(details.getAuthor());
        book.setCategory(details.getCategory());
        book.setAvailableCopies(details.getAvailableCopies());
        return ResponseEntity.ok(repository.save(book));
    }

    // DELETE /books/{id} — 204 No Content or 404 Not Found
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        Optional<Book> book=repository.findById(id);
        if(book.isEmpty())return ResponseEntity.notFound().build();
        repository.delete(book.get());
        return ResponseEntity.noContent().build();
    }

    private boolean valid(Book book){
        return book!=null && book.getTitle()!=null && !book.getTitle().isBlank()
            && book.getAuthor()!=null && !book.getAuthor().isBlank()
            && book.getCategory()!=null && !book.getCategory().isBlank()
            && book.getAvailableCopies()!=null && book.getAvailableCopies()>=0;
    }
}
