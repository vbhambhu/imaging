package uk.ac.ox.kir.imaging.repositories;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ox.kir.imaging.models.Entry;
import uk.ac.ox.kir.imaging.models.User;

import java.util.List;

@Repository
public interface EntryRepository extends CrudRepository<Entry, Integer> {

    Entry findByCategoryIdAndUsername(int category_id, String username);

    List<Entry> findAllByUsername(String username);

    List<Entry> findAllByStatus(int status);

}