/*
 * Copyright (c) 2026 Wellshh
 *
 * SPDX-License-Identifier: ISC
 */

package pathfinder.util.data_structure;

import java.util.HashMap;
import java.util.Map;
import pathfinder.util.HasPriorityKey;

/**
 * Indexed binary heap: wraps {@link BinaryHeap} and maintains a companion {@code HashMap} that maps
 * each item to its current array index, enabling O(1) lookup by item and O(log n) arbitrary removal
 * / priority update.
 *
 * <p>
 */
public class IndexedBH<T extends Comparable<? super T> & HasPriorityKey<H>, H> {
  private final BinaryHeap<T> bh;
  private final Map<T, Integer> indexMap;

  public IndexedBH() {
    this(new BinaryHeap<T>());
  }

  public IndexedBH(boolean isMaxHeap) {
    this(new BinaryHeap<T>(isMaxHeap));
  }

  public IndexedBH(BinaryHeap<T> bh) {
    this.bh = bh;
    this.indexMap = new HashMap<>();
    // Sync with any pre-existing items already in the heap
    for (int i = 0; i < bh.heap.size(); i++) {
      indexMap.put(bh.heap.get(i), i);
    }
    // Core trick: every swap inside BinaryHeap automatically
    // refreshes both moved items' indices in the map.
    bh.swapListener =
        (i, j) -> {
          indexMap.put(bh.heap.get(i), i);
          indexMap.put(bh.heap.get(j), j);
        };
  }

  // -------- Public interface ----------

  /** Insert a new item into the heap. */
  public void insert(T item) {
    indexMap.put(item, bh.heap.size());
    bh.insert(item);
  }

  /** Remove a specific item from the heap in O(log n). */
  public void remove(T item) {
    int index = indexMap.get(item);
    int lastIndex = bh.heap.size() - 1;
    indexMap.remove(item);

    if (index == lastIndex) {
      bh.heap.remove(lastIndex);
      return;
    }

    // Move the last item into the vacated slot
    T lastItem = bh.heap.get(lastIndex);
    bh.heap.set(index, lastItem);
    bh.heap.remove(lastIndex);
    indexMap.put(lastItem, index);

    // Rebalance: direction depends on the replacement value,
    // so try up first; if it didn't move, try down.
    int settled = bh.percolateUp(index);
    if (settled == index) {
      bh.percolateDown(index);
    }
  }

  /** Remove and return the top-priority item. */
  public T poll() {
    if (bh.isEmpty()) {
      return null;
    }
    T result = bh.poll();
    indexMap.remove(result);
    return result;
  }

  /**
   * Change the priority key of an existing item and restore heap order. Handles both increase and
   * decrease of priority.
   */
  public void update(T item, H key) {
    int index = indexMap.get(item);
    item.setPriorityKey(key);
    int settled = bh.percolateUp(index);
    if (settled == index) {
      bh.percolateDown(index);
    }
  }

  public boolean contains(T item) {
    return indexMap.containsKey(item);
  }

  public T peek() {
    return bh.peek();
  }

  public boolean isEmpty() {
    return bh.isEmpty();
  }

  public int size() {
    return bh.size();
  }

  public void clear() {
    bh.clear();
    indexMap.clear();
  }

  @Override
  public String toString() {
    return bh.toString();
  }

  // -------- End public interface ----------
}
