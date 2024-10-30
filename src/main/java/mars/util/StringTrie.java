package mars.util;

import java.util.*;

public class StringTrie<V> extends AbstractMap<String, V> {
    private final String key;
    private V terminalValue;
    private final Map<Character, StringTrie<V>> children;

    public StringTrie() {
        this("");
    }

    public StringTrie(String key) {
        this.key = key;
        this.terminalValue = null;
        this.children = new HashMap<>();
    }

    public String getKey() {
        return this.key;
    }

    public V getTerminalValue() {
        return this.terminalValue;
    }

    public void setTerminalValue(V value) {
        this.terminalValue = value;
    }

    public StringTrie<V> getSubTrie(String key) {
        Objects.requireNonNull(key);

        StringTrie<V> current = this;
        for (int index = 0; index < key.length(); index++) {
            String currentKey = current.key;
            current = current.children.computeIfAbsent(key.charAt(index), character -> new StringTrie<>(currentKey + character));
        }

        return current;
    }

    public StringTrie<V> getSubTrieIfPresent(String key) {
        Objects.requireNonNull(key);

        StringTrie<V> current = this;
        for (int index = 0; current != null && index < key.length(); index++) {
            current = current.children.get(key.charAt(index));
        }

        return current;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key. More formally, returns {@code true}
     * if and only if this map contains a mapping for a key {@code k} such that {@code Objects.equals(key, k)}. (There
     * can be at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @throws ClassCastException   if the key is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.  More formally, returns
     * {@code true} if and only if this map contains at least one mapping to a value {@code v} such that
     * {@code Objects.equals(value, v)}.  This operation will probably require time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     * @throws ClassCastException   if the value is of an inappropriate type for this map
     * @throws NullPointerException if the specified value is null
     */
    @Override
    public boolean containsValue(Object value) {
        Objects.requireNonNull(value);

        if (value.equals(this.terminalValue)) {
            return true;
        }

        for (StringTrie<V> child : this.children.values()) {
            if (child.containsValue(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     * key.
     * <p>
     * More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code Objects.equals(key, k)}, then this method returns {@code v};
     * otherwise it returns {@code null}.  (There can be at most one such mapping.)
     * <p>
     * If this map permits null values, then a return value of
     * {@code null} does not <i>necessarily</i> indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}. The {@link #containsKey containsKey} operation
     * may be used to distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     *     key
     * @throws ClassCastException   if the key is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V get(Object key) {
        Objects.requireNonNull(key);

        StringTrie<V> subTrie = this.getSubTrieIfPresent((String) key);
        if (subTrie == null) {
            return null;
        }

        return subTrie.terminalValue;
    }

    /**
     * Associates the specified value with the specified key in this map (optional operation).  If the map previously
     * contained a mapping for the key, the old value is replaced by the specified value.  (A map {@code m} is said to
     * contain a mapping for a key {@code k} if and only if {@link #containsKey(Object) m.containsKey(k)} would return
     * {@code true}.)
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     * @throws NullPointerException if the specified key or value is null
     */
    @Override
    public V put(String key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        StringTrie<V> subTrie = this.getSubTrie(key);

        V previousValue = subTrie.terminalValue;
        subTrie.terminalValue = value;

        return previousValue;
    }

    /**
     * Removes the mapping for a key from this map if it is present (optional operation). More formally, if this map
     * contains a mapping from key {@code k} to value {@code v} such that {@code Objects.equals(key, k)}, that mapping
     * is removed. (The map can contain at most one such mapping.)
     * <p>
     * Returns the value to which this map previously associated the key, or {@code null} if the map contained no
     * mapping for the key.
     * <p>
     * The map will not contain a mapping for the specified key once the call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     * @throws ClassCastException            if the key is of an inappropriate type for this map
     * @throws NullPointerException          if the specified key is null
     */
    @Override
    public V remove(Object key) {
        Objects.requireNonNull(key);

        StringTrie<V> subTrie = this.getSubTrieIfPresent((String) key);
        if (subTrie == null) {
            return null;
        }

        V previousValue = subTrie.terminalValue;
        subTrie.terminalValue = null;

        return previousValue;
    }

    /**
     * Removes all of the mappings from this map (optional operation). The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        this.children.clear();
        this.terminalValue = null;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map. The set is backed by the map, so changes to the
     * map are reflected in the set, and vice-versa.  If the map is modified while an iteration over the set is in
     * progress (except through the iterator's own {@code remove} operation, or through the {@code setValue} operation
     * on a map entry returned by the iterator) the results of the iteration are undefined.  The set supports element
     * removal, which removes the corresponding mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<String, V>> entrySet() {
        return new EntrySet();
    }

    private class EntrySet extends AbstractSet<Entry<String, V>> {
        /**
         * Returns an iterator over the elements contained in this collection.
         *
         * @return an iterator over the elements contained in this collection
         */
        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new EntryIterator<>(StringTrie.this, "");
        }

        @Override
        public int size() {
            // Eh, just iterate over the elements
            int size = 0;
            for (Entry<String, V> ignored : this) {
                size++;
            }
            return size;
        }
    }

    private static class EntryIterator<V> implements Iterator<Entry<String, V>> {
        private final StringTrie<V> trie;
        private final String relativeKey;
        private Iterator<Entry<Character, StringTrie<V>>> children;
        private EntryIterator<V> currentChild;
        private Entry<String, V> nextEntry;
        private boolean canRemove;

        public EntryIterator(StringTrie<V> trie, String relativeKey) {
            this.trie = trie;
            this.relativeKey = relativeKey;
            this.children = null;
            this.currentChild = null;
            this.nextEntry = this.findNext();
            this.canRemove = false;
        }

        /**
         * Returns {@code true} if the iteration has more elements. (In other words, returns {@code true} if
         * {@link #next} would return an element rather than throwing an exception.)
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            return this.nextEntry != null;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public Entry<String, V> next() {
            if (this.nextEntry == null) {
                throw new NoSuchElementException();
            }

            // If no child has started iteration at this point, the entry corresponding to this.trie is being returned,
            // and thus can be removed following this call
            this.canRemove = this.currentChild == null;

            Entry<String, V> next = this.nextEntry;
            this.nextEntry = this.findNext();

            return next;
        }

        private Entry<String, V> findNext() {
            if (this.children == null) {
                this.children = this.trie.children.entrySet().iterator();

                if (this.trie.terminalValue != null) {
                    return Map.entry(this.relativeKey, this.trie.terminalValue);
                }
            }

            while (this.currentChild == null || !this.currentChild.hasNext()) {
                if (!this.children.hasNext()) {
                    return null;
                }
                Entry<Character, StringTrie<V>> childEntry = this.children.next();
                this.currentChild = new EntryIterator<>(childEntry.getValue(), this.relativeKey + childEntry.getKey());
            }

            return this.currentChild.next();
        }

        /**
         * Removes from the underlying collection the last element returned by this iterator (optional operation).  This
         * method can be called only once per call to {@link #next}.
         * <p>
         * The behavior of an iterator is unspecified if the underlying collection is modified while the iteration is in
         * progress in any way other than by calling this method, unless an overriding class has specified a concurrent
         * modification policy.
         * <p>
         * The behavior of an iterator is unspecified if this method is called after a call to the
         * {@link #forEachRemaining forEachRemaining} method.
         *
         * @throws IllegalStateException if the {@code next} method has not yet been called, or the
         *                               {@code remove} method has already been called after the last call to
         *                               the {@code next} method
         */
        @Override
        public void remove() {
            if (!this.canRemove) {
                throw new IllegalStateException();
            }

            this.trie.terminalValue = null;
            this.canRemove = false;
        }
    }
}
