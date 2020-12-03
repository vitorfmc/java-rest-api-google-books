package com.vitor.cordeiro.teste.quarkus.service;

import com.vitor.cordeiro.teste.quarkus.entity.Book;
import com.vitor.cordeiro.teste.quarkus.entity.Image;
import com.vitor.cordeiro.teste.quarkus.exception.DataValidationException;
import com.vitor.cordeiro.teste.quarkus.exception.GoogleApiGenericException;
import com.vitor.cordeiro.teste.quarkus.util.ImageUtil;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookServiceImpl implements BookService {

    private static final Logger LOG = Logger.getLogger(BookServiceImpl.class);

    @Inject
    GoogleBooksService googleService;

    @Override
    public List<Book> getBook(String q, Integer limit, Integer offset)
            throws GoogleApiGenericException, DataValidationException {

        var googleBook = googleService.getBookByTitle(q, limit, offset);
        List<Book> response = new ArrayList<>();

        if(googleBook != null && googleBook.getItems() != null && !googleBook.getItems().isEmpty()){
            response = googleBook.getItems().stream().map( x ->
                {
                    try {
                        if(x.getVolumeInfo() != null){
                            var published =  x.getVolumeInfo().getPublishedDate() != null ? (new SimpleDateFormat("yyyy-MM-dd"))
                                    .parse(x.getVolumeInfo().getPublishedDate()) : null;

                            var image = x.getVolumeInfo().getImageLinks() != null ? new Image(
                                    x.getVolumeInfo().getImageLinks().getThumbnail(),
                                    "data:image/jpeg;base64," +
                                            ImageUtil.urlToBase64(x.getVolumeInfo().getImageLinks().getThumbnail() + ".jpeg")
                            ) : null;

                            return new Book( x.getVolumeInfo().getTitle(),
                                    x.getVolumeInfo().getAuthors(),
                                    x.getVolumeInfo().getCategories(),
                                    x.getVolumeInfo().getPublisher(),
                                    published,
                                    x.getVolumeInfo().getDescription(),
                                    x.getVolumeInfo().getPageCount(),
                                    image);
                        }

                        return null;

                    } catch (Exception e) {
                        LOG.error("Error converting book: " + x.getId(), e);
                        return null;
                    }
                }
            ).collect(Collectors.toList());
        }

        return response;
    }
}
