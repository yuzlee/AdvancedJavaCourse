import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.*;

/**
 * Created by Rolrence on 2017/5/25.
 *
 * A simple wrapper for HashMap<?, HashMap<?, ?>
 */


public class GenericMatrix<K1, K2, V> {
    public class DataObject {
        public K1 k1;
        public K2 k2;
        public V v;

        public DataObject(K1 k1, K2 k2, V v) {
            this.k1 = k1;
            this.k2 = k2;
            this.v = v;
        }
    }
    public class Keys<_K1, _K2> {
        public _K1 key1;
        public _K2 key2;

        Keys(_K1 k1, _K2 k2) {
            this.key1 = k1;
            this.key2 = k2;
        }
    }

    private HashMap<K1, HashMap<K2, V>> data;

    GenericMatrix() {
        this.data = new HashMap<>();
    }

    public boolean has(K1 k1, K2 k2) {
        return this.data.containsKey(k1) && this.data.get(k1).containsKey(k2);
    }

    public void set(K1 k1, K2 k2, V v) {
        this.data.putIfAbsent(k1, new HashMap<>());
        this.data.get(k1).putIfAbsent(k2, v);
    }

    public void reset(K1 k1, K2 k2, V v) {
        this.data.putIfAbsent(k1, new HashMap<>());
        this.data.get(k1).put(k2, v);
    }

    public V get(K1 k1, K2 k2) {
        return this.data.get(k1).get(k2);
    }

    public void opt(K1 k1, K2 k2, Function<V, V> callback) {
        V value = callback.apply(this.get(k1, k2));
        this.reset(k1, k2, value);
    }

    public void map(BiConsumer<Keys<K1, K2>, V> callback) {
        this.data.forEach((k1, _v) -> {
            _v.forEach((k2, v) -> {
                callback.accept(new Keys<>(k1, k2), this.get(k1, k2));
            });
        });
    }

    public <T> void mapTo(GenericMatrix<K1, K2, T> m, BiFunction<Keys<K1, K2>, V, T> callback) {
        this.data.forEach((k1, _v) -> {
            _v.forEach((k2, v) -> {
                m.reset(k1, k2, callback.apply(new Keys<>(k1, k2), this.get(k1, k2)));
            });
        });
    }

    public K2 max(K1 k1, BiFunction<V, V, Boolean> comp) {
        HashMap<K2, V> temp = this.data.get(k1);
        V max_val = null;
        K2 max_key = null;
        for (K2 k2: temp.keySet()) {
            if (max_val == null) {
                max_val = temp.get(k2);
                max_key = k2;
                continue;
            }
            // if temp.get(k2) > max_val
            V temp_val = temp.get(k2);
            if (comp.apply(temp.get(k2), max_val)) {
                max_val = temp_val;
                max_key = k2;
            }
        }
        return max_key;
    }

    public ArrayList<DataObject> asList() {
        ArrayList<DataObject> list = new ArrayList<>();
        for (K1 k1: this.data.keySet()) {
            for (K2 k2: this.data.get(k1).keySet()) {
                list.add(new DataObject(k1, k2, this.get(k1, k2)));
            }
        }
        return list;
    }
}
