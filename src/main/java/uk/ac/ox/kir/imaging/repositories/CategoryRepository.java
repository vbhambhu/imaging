package uk.ac.ox.kir.imaging.repositories;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.kir.imaging.models.Category;

import java.util.List;

public interface CategoryRepository extends CrudRepository<Category, Integer> {

    List<Category> findAll();
}
