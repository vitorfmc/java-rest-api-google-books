package com.rovitapps.google.books.integration.api.service;

import com.rovitapps.google.books.integration.api.exception.DataNotFoundException;
import com.rovitapps.google.books.integration.api.exception.DataValidationException;
import com.rovitapps.google.books.integration.api.model.Book;
import com.rovitapps.google.books.integration.api.model.dto.BookCreateDTO;
import com.rovitapps.google.books.integration.api.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BookService {

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    private BookRepository repository;
    
    public Book save(@NotNull(message = "Request body not informed") @Valid BookCreateDTO dto)
            throws DataValidationException, DataNotFoundException {
        try{
            Book oldBook = findByTitle(dto.getTitle());
            if(oldBook != null)
                throw new DataValidationException(messageSource.getMessage("book.error.already.exists", null,
                        null, Locale.getDefault()));

            Book book = new Book(dto.getLibraryCode(), dto.getTitle(),
                    (new SimpleDateFormat("dd/MM/yyyy")).parse(dto.getCatalogingDate()));
            validate(book);

            return repository.save(book);

        }catch (ParseException e){
            throw new DataValidationException(messageSource.getMessage("book.error.invalid.date", null,
                    null, Locale.getDefault()));
        }
    }

    public Book update(@NotNull(message = "The book ID is mandatory") String id,
                       @NotNull(message = "Request body not informed") @Valid BookCreateDTO dto)
            throws DataValidationException, DataNotFoundException {

        try{
            Book oldBook = findById(id);
            if(oldBook == null)
                throw new DataValidationException(messageSource.getMessage("book.error.not.exist", null,
                        null, Locale.getDefault()));

            Book book = findByTitle(dto.getTitle());
            if(book !=null && !book.getId().equals(id))
                throw new DataValidationException(messageSource.getMessage("book.error.already.exists", null,
                        null, Locale.getDefault()));

            oldBook.setLibraryCode(dto.getLibraryCode());
            oldBook.setTitle(dto.getTitle());
            oldBook.setCatalogingDate((new SimpleDateFormat("dd/MM/yyyy")).parse(dto.getCatalogingDate()));

            validate(oldBook);

            return repository.save(oldBook);

        }catch (ParseException e){
            throw new DataValidationException(messageSource.getMessage("book.error.invalid.date", null,
                    null, Locale.getDefault()));
        }
    }

    public void delete(String id) throws DataValidationException, DataNotFoundException {
        Book book = findById(id);
        if(book == null)
            throw new DataValidationException(messageSource.getMessage("book.error.not.exist",
                    null, null, Locale.getDefault()));

        repository.delete(book);
    }

    public Book findByTitle(String title) throws DataValidationException {
        if(title == null){
            String message = messageSource.getMessage("book.error.not.informed",
                    null, null, Locale.getDefault());
            throw new DataValidationException(message);
        }

        return repository.findByTitle(title);
    }

    public Book findById(String id) throws DataNotFoundException {
        if(id == null){
            String message = messageSource.getMessage("book.error.not.informed",
                    null, null, Locale.getDefault());
            throw new DataNotFoundException(message);
        }

        return repository.findById(id, Book.class);
    }

    public Page<Book> findAll(int limit, int offset, String q) throws DataValidationException {

        validateLimitAndOffset(limit, offset);

        Page<Book> data;
        if(q != null && !q.isEmpty())
            data = repository.findAllByCriteria(PageRequest.of(offset, limit), q);
        else
            data = repository.findAll(PageRequest.of(offset, limit));

        return data;
    }

    private void validateLimitAndOffset(Integer limit, Integer offset) throws DataValidationException {

        List<String> errors = new ArrayList<>();

        if(limit == null || limit <= 0){
            errors.add(messageSource.getMessage("generic.error.limit",
                    null, Locale.getDefault()));
        }

        if(offset == null || offset < 0){
            errors.add(messageSource.getMessage("generic.error.offset",
                    null, Locale.getDefault()));
        }

        if(!errors.isEmpty())
            throw new DataValidationException(errors);
    }

    private void validate(Object arg) throws DataValidationException {
        List<String> errors = new ArrayList<>();

        ValidatorFactory factory= Validation.buildDefaultValidatorFactory();
        Validator validator=factory.getValidator();
        List<ConstraintViolation<Object>> violations = new ArrayList<>(validator.validate(arg));

        if(!violations.isEmpty()){
            for(ConstraintViolation<Object> currentError : violations){
                errors.add(currentError.getMessage());
            }

            throw new DataValidationException(errors);
        }
    }
}
