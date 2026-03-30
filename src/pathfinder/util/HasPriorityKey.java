package pathfinder.util;

public interface HasPriorityKey<H> {
  H getPriorityKey();

  void setPriorityKey(H key);
}
