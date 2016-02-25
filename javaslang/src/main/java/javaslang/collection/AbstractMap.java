package javaslang.collection;

import javaslang.Match;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Option;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

abstract class AbstractMap<K, V, M extends Map<K, V>> implements Map<K, V> {

    abstract M createFromEntries(Iterable<? extends Tuple2<? extends K, ? extends V>> entries);
    abstract M getEmpty();

    @Override
    public boolean containsValue(V value) {
        return iterator().map(Tuple2::_2).contains(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public M distinct() {
        return (M) this;
    }

    @Override
    public M distinctBy(Comparator<? super Tuple2<K, V>> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return createFromEntries(iterator().distinctBy(comparator));
    }

    @Override
    public <U> M distinctBy(Function<? super Tuple2<K, V>, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return createFromEntries(iterator().distinctBy(keyExtractor));
    }

    @SuppressWarnings("unchecked")
    @Override
    public M drop(long n) {
        if (n <= 0) {
            return (M) this;
        }
        if (n >= length()) {
            return getEmpty();
        }
        return createFromEntries(iterator().drop(n));
    }

    @SuppressWarnings("unchecked")
    @Override
    public M dropRight(long n) {
        if (n <= 0) {
            return (M) this;
        }
        if (n >= length()) {
            return getEmpty();
        }
        return createFromEntries(iterator().dropRight(n));
    }

    @Override
    public M dropUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    @Override
    public M dropWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return createFromEntries(iterator().dropWhile(predicate));
    }

    @Override
    public M filter(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return createFromEntries(iterator().filter(predicate));
    }

    @Override
    public <U> U foldRight(U zero, BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return iterator().foldRight(zero, f);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> Map<C, M> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return foldLeft(HashMap.empty(), (map, entry) -> {
            final C key = classifier.apply(entry);
            final Map<K, V> values = map.get(key)
                    .map(entries -> entries.put(entry._1, entry._2))
                    .getOrElse(createFromEntries(Iterator.of(entry)));
            return map.put(key, (M) values);
        });
    }

    @Override
    public Iterator<M> grouped(long size) {
        return sliding(size, size);
    }

    @Override
    public boolean hasDefiniteSize() {
        return true;
    }

    @Override
    public Option<Tuple2<K, V>> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<M> initOption() {
        return isEmpty() ? Option.none() : Option.some((M) init());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Option<M> tailOption() {
        if (isEmpty()) {
            return Option.none();
        } else {
            return Option.some((M) tail());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public M take(long n) {
        if (size() <= n) {
            return (M) this;
        } else {
            return createFromEntries(iterator().take(n));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public M takeRight(long n) {
        if (size() <= n) {
            return (M) this;
        } else {
            return createFromEntries(iterator().takeRight(n));
        }
    }

    @Override
    public M takeUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    @SuppressWarnings("unchecked")
    @Override
    public M takeWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final M taken = createFromEntries(iterator().takeWhile(predicate));
        return taken.length() == length() ? (M) this : taken;
    }

    @Override
    public boolean isTraversableAgain() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Match<M> match() {
        return Match.Match((M) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public M merge(Map<? extends K, ? extends V> that) {
        Objects.requireNonNull(that, "that is null");
        if (isEmpty()) {
            return createFromEntries(that);
        } else if (that.isEmpty()) {
            return (M) this;
        } else {
            return that.foldLeft((M) this, (map, entry) -> !map.containsKey(entry._1) ? (M) map.put(entry) : map);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends V> M merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(collisionResolution, "collisionResolution is null");
        if (isEmpty()) {
            return createFromEntries(that);
        } else if (that.isEmpty()) {
            return (M) this;
        } else {
            return that.foldLeft((M) this, (map, entry) -> {
                final K key = entry._1;
                final U value = entry._2;
                final V newValue = map.get(key).map(v -> (V) collisionResolution.apply(v, value)).getOrElse(value);
                return (M) map.put(key, newValue);
            });
        }
    }

    @Override
    public Tuple2<M, M> partition(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Tuple2<Iterator<Tuple2<K, V>>, Iterator<Tuple2<K, V>>> p = iterator().partition(predicate);
        return Tuple.of(createFromEntries(p._1), createFromEntries(p._2));
    }

    @SuppressWarnings("unchecked")
    @Override
    public M peek(Consumer<? super Tuple2<K, V>> action) {
        Objects.requireNonNull(action, "action is null");
        if (!isEmpty()) {
            action.accept(head());
        }
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public M replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        Objects.requireNonNull(currentElement, "currentElement is null");
        Objects.requireNonNull(newElement, "newElement is null");
        return containsKey(currentElement._1) ? (M) remove(currentElement._1).put(newElement) : (M) this;
    }

    @Override
    public M replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return replace(currentElement, newElement);
    }

    @SuppressWarnings("unchecked")
    @Override
    public M scan(Tuple2<K, V> zero, BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> operation) {
        Objects.requireNonNull(operation, "operation is null");
        return Collections.scanLeft(this, zero, operation, getEmpty(), (m, e) -> (M) m.put(e), Function.identity());
    }

    @Override
    public <U> Seq<U> scanLeft(U zero, BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> operation) {
        Objects.requireNonNull(operation, "operation is null");
        return Collections.scanLeft(this, zero, operation, List.empty(), List::prepend, List::reverse);
    }

    @Override
    public <U> Seq<U> scanRight(U zero, BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> operation) {
        Objects.requireNonNull(operation, "operation is null");
        return Collections.scanRight(this, zero, operation, List.empty(), List::prepend, Function.identity());
    }

    @Override
    public Iterator<M> sliding(long size) {
        return sliding(size, 1);
    }

    @Override
    public Iterator<M> sliding(long size, long step) {
        return iterator().sliding(size, step).map(this::createFromEntries);
    }

    @Override
    public Tuple2<M, M> span(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Tuple2<Iterator<Tuple2<K, V>>, Iterator<Tuple2<K, V>>> t = iterator().span(predicate);
        return Tuple.of(createFromEntries(t._1), createFromEntries(t._2));
    }
}
