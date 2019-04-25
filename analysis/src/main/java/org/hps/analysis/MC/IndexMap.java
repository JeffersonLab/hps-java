package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexMap<T> implements Map<int[], T> {
    private final Map<Integer, Map<Integer, Map<Integer, T>>> xMap = new HashMap<Integer, Map<Integer, Map<Integer, T>>>();
    
    @Override
    public void clear() {
        xMap.clear();
    }
    
    @Override
    public boolean containsKey(Object key) {
        if(key instanceof int[]) {
            int[] indices = (int[]) key;
            if(indices.length != 3) {
                throw new IllegalArgumentException("IndexMap requires an array of three int primitives as a key.");
            }
            
            Map<Integer, T> zMap = null;
            Map<Integer, Map<Integer, T>> yMap = null;
            
            if(xMap.containsKey(Integer.valueOf(indices[0]))) {
                yMap = xMap.get(Integer.valueOf(indices[0]));
            } else { return false; }
            
            if(yMap.containsKey(Integer.valueOf(indices[1]))) {
                zMap = yMap.get(Integer.valueOf(indices[1]));
            } else { return false; }
            
            return zMap.containsKey(indices[2]);
        }
        
        return false;
    }
    
    @Override
    public boolean containsValue(Object value) {
        for(Map<Integer, Map<Integer, T>> yMap : xMap.values()) {
            for(Map<Integer, T> zMap : yMap.values()) {
                if(zMap.containsValue(value)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public Set<Entry<int[], T>> entrySet() {
        Set<Entry<int[], T>> entries = new HashSet<Entry<int[], T>>();
        
        for(Entry<Integer, Map<Integer, Map<Integer, T>>> xEntry : xMap.entrySet()) {
            for(Entry<Integer, Map<Integer, T>> yEntry : xEntry.getValue().entrySet()) {
                for(Entry<Integer, T> zEntry : yEntry.getValue().entrySet()) {
                    int[] key = new int[] { xEntry.getKey().intValue(), yEntry.getKey().intValue(), zEntry.getKey().intValue() };
                    T value = zEntry.getValue();
                    
                    Entry<int[], T> entry = new MapEntry<int[], T>(key, value);
                    entries.add(entry);
                }
            }
        }
        
        return Collections.unmodifiableSet(entries);
    }
    
    @Override
    public T get(Object key) {
        if(key instanceof int[]) {
            int[] indices = (int[]) key;
            if(indices.length != 3) {
                throw new IllegalArgumentException("IndexMap requires an array of three int primitives as a key.");
            }
            
            Map<Integer, T> zMap = null;
            Map<Integer, Map<Integer, T>> yMap = null;
            
            if(xMap.containsKey(Integer.valueOf(indices[0]))) {
                yMap = xMap.get(Integer.valueOf(indices[0]));
            } else { return null; }
            
            if(yMap.containsKey(Integer.valueOf(indices[1]))) {
                zMap = yMap.get(Integer.valueOf(indices[1]));
            } else { return null; }
            
            return zMap.get(indices[2]);
        }
        
        return null;
    }
    
    @Override
    public boolean isEmpty() {
        return xMap.isEmpty();
    }
    
    @Override
    public Set<int[]> keySet() {
        Set<int[]> keys = new HashSet<int[]>();
        
        for(Entry<Integer, Map<Integer, Map<Integer, T>>> xEntry : xMap.entrySet()) {
            for(Entry<Integer, Map<Integer, T>> yEntry : xEntry.getValue().entrySet()) {
                for(Entry<Integer, T> zEntry : yEntry.getValue().entrySet()) {
                    keys.add(new int[] { xEntry.getKey().intValue(), yEntry.getKey().intValue(), zEntry.getKey().intValue() });
                }
            }
        }
        
        return Collections.unmodifiableSet(keys);
    }
    
    @Override
    public T put(int[] key, T value) {
        if(key instanceof int[]) {
            int[] indices = (int[]) key;
            if(indices.length != 3) {
                throw new IllegalArgumentException("IndexMap requires an array of three int primitives as a key.");
            }
            
            Map<Integer, T> zMap = null;
            Map<Integer, Map<Integer, T>> yMap = null;
            
            if(xMap.containsKey(Integer.valueOf(indices[0]))) {
                yMap = xMap.get(Integer.valueOf(indices[0]));
            } else {
                yMap = new HashMap<Integer, Map<Integer, T>>();
                xMap.put(Integer.valueOf(indices[0]), yMap);
            }
            
            if(yMap.containsKey(Integer.valueOf(indices[1]))) {
                zMap = yMap.get(Integer.valueOf(indices[1]));
            } else {
                zMap = new HashMap<Integer, T>();
                yMap.put(Integer.valueOf(indices[1]), zMap);
            }
            
            return zMap.put(Integer.valueOf(indices[2]), value);
        }
        
        return null;
    }
    
    @Override
    public void putAll(Map<? extends int[], ? extends T> m) {
        for(Map.Entry<? extends int[], ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public T remove(Object key) {
        if(key instanceof int[]) {
            int[] indices = (int[]) key;
            if(indices.length != 3) {
                throw new IllegalArgumentException("IndexMap requires an array of three int primitives as a key.");
            }
            
            Map<Integer, T> zMap = null;
            Map<Integer, Map<Integer, T>> yMap = null;
            
            if(xMap.containsKey(Integer.valueOf(indices[0]))) {
                yMap = xMap.get(Integer.valueOf(indices[0]));
            } else { return null; }
            
            if(yMap.containsKey(Integer.valueOf(indices[1]))) {
                zMap = yMap.get(Integer.valueOf(indices[1]));
            } else { return null; }
            
            return zMap.remove(indices[2]);
        }
        
        return null;
    }
    
    @Override
    public int size() {
        if(xMap.isEmpty()) { return 0; }
        
        int totalEntries = 0;
        for(Map<Integer, Map<Integer, T>> yMap : xMap.values()) {
            for(Map<Integer, T> zMap : yMap.values()) {
                totalEntries += zMap.size();
            }
        }
        
        return totalEntries;
    }
    
    @Override
    public Collection<T> values() {
        List<T> values = new ArrayList<T>();
        for(Map<Integer, Map<Integer, T>> yMap : xMap.values()) {
            for(Map<Integer, T> zMap : yMap.values()) {
                values.addAll(zMap.values());
            }
        }
        
        return Collections.unmodifiableList(values);
    }
    
    private class MapEntry<K, V> implements Entry<K, V> {
        private final K key;
        private final V value;
        
        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public K getKey() {
            return key;
        }
        
        @Override
        public V getValue() {
            return value;
        }
        
        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }
}