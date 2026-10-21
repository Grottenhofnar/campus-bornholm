package dk.campusbornholm.daos;

import java.util.List;

public interface IDAO<T, ID> {
    T create(T dto);

    T getById(ID id);

    List<T> getAll();

    T update(ID id, T dto);

    void delete(ID id);
}
