package pathfinder.util.data_structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiConsumer;

/**
 * Binary heap (priority queue) backed by a dynamic array, following the textbook heap layout
 * (default to min-heap) (complete binary tree in level order).
 *
 * <p>Adapted from Runestone Academy, <em>Problem Solving with Algorithms and Data Structures using
 * Java</em>, section &quot;Binary Heap Implementation&quot; ({@linkplain
 * https://runestone.academy/ns/books/published/javads/trees_binary-heap-implementation.html Binary
 * Heap Implementation}).
 *
 * <p>The original book material is licensed under the terms stated on Runestone; this file may
 * contain project-specific changes (API, generics, package, or behavior). See commit history for
 * exact modifications.
 *
 * @author Junyan Bai
 * @see <a href=
 *     "https://runestone.academy/ns/books/published/javads/trees_binary-heap-implementation.html">Runestone:
 *     Binary Heap Implementation</a>
 */
public class BinaryHeap<T extends Comparable<? super T>> {
  ArrayList<T> heap;
  private final Comparator<? super T> comparator;
  private final boolean isMaxHeap;
  BiConsumer<Integer, Integer> swapListener;

  public boolean isMaxHeap() {
    return isMaxHeap;
  }

  public BinaryHeap() {
    this.heap = new ArrayList<T>();
    this.comparator = null;
    this.isMaxHeap = false;
  }

  public BinaryHeap(boolean isMaxHeap) {
    this.heap = new ArrayList<T>();
    this.comparator = null;
    this.isMaxHeap = isMaxHeap;
  }

  public BinaryHeap(Comparator<? super T> comparator) {
    this.heap = new ArrayList<T>();
    this.comparator = comparator;
    this.isMaxHeap = false;
  }

  public BinaryHeap(Comparator<? super T> comparator, boolean isMaxHeap) {
    this.heap = new ArrayList<T>();
    this.comparator = comparator;
    this.isMaxHeap = isMaxHeap;
  }

  private int compareItemsAt(int index1, int index2) {
    T a = heap.get(index1);
    T b = heap.get(index2);
    if (comparator != null) {
      return comparator.compare(a, b);
    }
    return a.compareTo(b);
  }

  private void swapItemsAt(int index1, int index2) {
    T temporary = heap.get(index1);
    heap.set(index1, heap.get(index2));
    heap.set(index2, temporary);
    if (swapListener != null) {
      swapListener.accept(index1, index2);
    }
  }

  protected int percolateUp(int index) {
    while ((index > 0) && (index - 1) / 2 >= 0) {
      int parentIndex = (index - 1) / 2;
      // For min-heap: swap when child < parent (higher priority is smaller).
      // For max-heap: swap when child > parent (higher priority is larger).
      int cmp = compareItemsAt(index, parentIndex);
      if ((!isMaxHeap && cmp < 0) || (isMaxHeap && cmp > 0)) {
        swapItemsAt(index, parentIndex);
      }
      index = parentIndex;
    }

    return index;
  }

  public void insert(T item) {
    heap.add(item);
    percolateUp(heap.size() - 1);
  }

  public int insertAndGetIndex(T item) {
    heap.add(item);
    return percolateUp(heap.size() - 1);
  }

  protected int percolateDown(int index) {
    while (2 * index + 1 < heap.size()) {
      int preferredChild = getPreferredChild(index);
      // For min-heap: swap when parent > child.
      // For max-heap: swap when parent < child.
      int cmp = compareItemsAt(index, preferredChild);
      if ((!isMaxHeap && cmp > 0) || (isMaxHeap && cmp < 0)) {
        swapItemsAt(index, preferredChild);
      } else {
        break;
      }
      index = preferredChild;
    }

    return index;
  }

  private int getPreferredChild(int index) {
    // Pick the child with higher priority:
    // - min-heap: smaller child
    // - max-heap: larger child
    if (2 * index + 2 > heap.size() - 1) {
      return 2 * index + 1;
    }

    int left = 2 * index + 1;
    int right = 2 * index + 2;
    int cmp = compareItemsAt(left, right);
    if (!isMaxHeap) {
      // min-heap: choose smaller (left < right)
      return (cmp <= 0) ? left : right;
    }
    // max-heap: choose larger (left > right)
    return (cmp >= 0) ? left : right;
  }

  /** Remove and return the item on top of the heap */
  public T poll() {
    T result = heap.get(0);
    swapItemsAt(0, heap.size() - 1);
    heap.remove(heap.size() - 1);
    percolateDown(0);
    return result;
  }

  /* Build heap from an array-like structure */
  public void heapify(T[] nonHeap) {
    heap = new ArrayList<T>(); // eliminate old data

    if (nonHeap.length != 0) {
      // copy non-heap into the new heap
      for (int i = 0; i < nonHeap.length; i++) {
        heap.add(nonHeap[i]);
      }

      int currIndex = heap.size() / 2 - 1;
      while (currIndex >= 0) {
        percolateDown(currIndex);
        currIndex = currIndex - 1;
      }
    }
  }

  public T peek() {
    return heap.get(0);
  }

  public boolean isEmpty() {
    return heap.isEmpty();
  }

  public int size() {
    return heap.size();
  }

  public String toString() {
    return heap.toString();
  }

  public void clear() {
    this.heap.clear();
  }
}
