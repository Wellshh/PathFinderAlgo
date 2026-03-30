package pathfinder.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  /** Path container with O(1) next lookup by point. */
  class O1PathContainer<P extends Point> implements PathContainer<P> {
    private final ArrayList<P> path = new ArrayList<>();
    private final Map<P, Integer> firstIndexByPoint = new HashMap<>();

    @Override
    public void add(P point) {
      firstIndexByPoint.putIfAbsent(point, path.size());
      path.add(point);
    }

    @Override
    public P get(int index) {
      return path.get(index);
    }

    @Override
    public P next(P current) {
      Integer firstIndex = firstIndexByPoint.get(current);
      if (firstIndex == null) {
        return null;
      }
      int nextIndex = firstIndex + 1;
      if (nextIndex >= path.size()) {
        return null;
      }
      return path.get(nextIndex);
    }

    @Override
    public int size() {
      return path.size();
    }

    @Override
    public boolean isEmpty() {
      return path.isEmpty();
    }

    @Override
    public void clear() {
      path.clear();
      firstIndexByPoint.clear();
    }

    @Override
    public List<P> toList() {
      return new ArrayList<>(path);
    }
  }
}
