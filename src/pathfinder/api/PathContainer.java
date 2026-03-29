package pathfinder.api;

import java.util.List;
import pathfinder.model.Point;

/* Interface for the container of the path */
public interface PathContainer<P extends Point> {
  /** Add a point to the path */
  void add(P point);

  /** Get a point from the path */
  P get(int index);

  /** Get the next point from the path */
  P next(P current);

  /** Get the size of the path */
  int size();

  /** Check if the path is empty */
  boolean isEmpty();

  /** Clear the path */
  void clear();

  /** Get the path as a list */
  List<P> toList();
}
