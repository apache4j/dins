/**
 * 包名:com.texaspoker.timer
 * 文件名:TimeoutMap.java
 * 创建人: chenxi
 * 创建日期:2015年10月15日-下午2:58:06
 * Copyright (c) 2015 illuminate 公司-版权所有
 * 备注: 向 Jonathan Halterman 致敬
 */
package com.texaspoker.dubbo.utils.timer;

import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 类名称:TimeoutMap 类描述:超时处理map
 */
public class TimeoutMap<K, V> implements ConcurrentMap<K, V> {

  private static AtomicLong timeoutEventIndex = new AtomicLong();

  static final ScheduledExecutorService expirer =
      Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("TimeoutMap-Expirer"));
  static final ThreadPoolExecutor listenerService =
      (ThreadPoolExecutor) Executors.newCachedThreadPool(new NamedThreadFactory("TimeoutMap-Listener-%s"));

  /** 超时触发执行同步或异步调用的阈值 */
  private static final long LISTENER_EXECUTION_THRESHOLD = TimeUnit.MILLISECONDS.toNanos(100);

  /** 超时触发列表 */
  List<TimeoutListenerConfig<K, V>> timeoutListeners;
  private AtomicLong timeoutNanos;

  /** 超时的触发策略 */
  private final AtomicReference<TimeoutPolicy> timeoutPolicy;

  /** map的锁操作 */
  // private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  // private final Lock readLock = readWriteLock.readLock();
  // private final Lock writeLock = readWriteLock.writeLock();
  private final Lock mapLock = new ReentrantLock();

  /** 数据存储map */
  private final EntryMap<K, V> entries;

  /** 超时时间是否可变 */
  private final boolean variableTimeout;

  /**
   * 创建一个新超时map，使用builer模式创建
   */
  private TimeoutMap(final Builder<K, V> builder) {
    variableTimeout = builder.variableTimeout;
    entries = variableTimeout ? new EntryTreeHashMap<K, V>() : new EntryLinkedHashMap<K, V>();
    if (builder.timeoutListeners != null)
      timeoutListeners = new CopyOnWriteArrayList<TimeoutListenerConfig<K, V>>(builder.timeoutListeners);
    timeoutPolicy = new AtomicReference<TimeoutPolicy>(builder.timeoutPolicy);
    timeoutNanos = new AtomicLong(TimeUnit.NANOSECONDS.convert(builder.duration, builder.timeUnit));
    // 如果定时器执行线程池没有启动,则启动之
    if (!TimeoutExecutionQueue.getInstance().isStarted()) {
      TimeoutExecutionQueue.getInstance().startQueue();
    }
  }

  /**
   * 新建一个实例，默认设置 创建触发策略，60秒超时
   */
  public static final class Builder<K, V> {
    private TimeoutPolicy timeoutPolicy = TimeoutPolicy.CREATED;
    private List<TimeoutListenerConfig<K, V>> timeoutListeners;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private boolean variableTimeout = false;
    private long duration = 60;

    /**
     * Creates a new Builder object.
     */
    private Builder() {}

    /**
     * Builds and returns an expiring map.
     * 
     * @param <K1>
     *        Key type
     * @param <V1>
     *        Value type
     */
    @SuppressWarnings("unchecked")
    public <K1 extends K, V1 extends V> TimeoutMap<K1, V1> build() {
      return new TimeoutMap<K1, V1>((Builder<K1, V1>) this);
    }

    /**
     * Sets the default map entry expiration.
     * 
     * @param duration
     *        the length of time after an entry is created that it should be removed
     * @param timeUnit
     *        the unit that {@code duration} is expressed in
     * @throws NullPointerException
     *         if {@code timeUnit} is null
     */
    public Builder<K, V> timeout(long duration, TimeUnit timeUnit) {
      this.duration = duration;
      this.timeUnit = Assert.notNull(timeUnit, "timeUnit");
      return this;
    }

    /**
     * Sets the expiration listener which will receive notifications upon each map entry's
     * expiration.
     * 
     * @param listener
     *        to set
     * @throws NullPointerException
     *         if {@code listener} is null
     */
    @SuppressWarnings("unchecked")
    public <K1 extends K, V1 extends V> Builder<K1, V1> timeoutListener(
        TimeoutListener<? super K1, ? super V1> listener) {
      Assert.notNull(listener, "listener");
      if (timeoutListeners == null)
        timeoutListeners = new ArrayList<TimeoutListenerConfig<K, V>>();
      timeoutListeners.add(new TimeoutListenerConfig<K, V>((TimeoutListener<K, V>) listener));
      return (Builder<K1, V1>) this;
    }

    /**
     * Sets expiration listeners which will receive notifications upon each map entry's expiration.
     * 
     * @param listeners
     *        to set
     * @throws NullPointerException
     *         if {@code listener} is null
     */
    @SuppressWarnings("unchecked")
    public <K1 extends K, V1 extends V> Builder<K1, V1> timeoutListeners(
        List<TimeoutListener<? super K1, ? super V1>> listeners) {
      Assert.notNull(listeners, "listeners");
      if (timeoutListeners == null)
        timeoutListeners = new ArrayList<TimeoutListenerConfig<K, V>>(listeners.size());
      for (TimeoutListener<? super K1, ? super V1> listener : listeners)
        timeoutListeners.add(new TimeoutListenerConfig<K, V>((TimeoutListener<K, V>) listener));
      return (Builder<K1, V1>) this;
    }

    /**
     * Sets the map entry expiration policy.
     * 
     * @param timeoutPolicy
     * @throws NullPointerException
     *         if {@code timeoutPolicy} is null
     */
    public Builder<K, V> timeoutPolicy(TimeoutPolicy timeoutPolicy) {
      this.timeoutPolicy = Assert.notNull(timeoutPolicy, "timeoutPolicy");
      return this;
    }

    /**
     * Allows for map entries to have individual expirations and for expirations to be changed.
     */
    public Builder<K, V> variableTimeout() {
      variableTimeout = true;
      return this;
    }
  }

  /** 超时触发的配置项 */
  static class TimeoutListenerConfig<K, V> {
    final TimeoutListener<K, V> timeoutListener;
    /** -1 = initial, 0 = synchronous, 1 = asynchronous */
    int executionPolicy = -1;

    /** Constructs a new TimeoutListenerConfig. */
    TimeoutListenerConfig(TimeoutListener<K, V> timeoutListener) {
      this.timeoutListener = timeoutListener;
    }
  }

  /** timeout map entry 实现. */
  static class TimeoutEntry<K, V> implements Comparable<TimeoutEntry<K, V>> {
    final AtomicLong timeoutNanos;
    /** Epoch time at which the entry is expected to expire */
    final AtomicLong expectedTimeout;
    final AtomicReference<TimeoutPolicy> timeoutPolicy;
    final K key;
    /** Guarded by "this" */
    volatile Future<?> entryFuture;
    /** Guarded by "this" */
    V value;
    /** Guarded by "this" */
    volatile boolean scheduled;

    /**
     * Creates a new TimeoutEntry object.
     * 
     * @param key
     *        for the entry
     * @param value
     *        for the entry
     * @param timeoutPolicy
     *        for the entry
     * @param timeoutNanos
     *        for the entry
     */
    TimeoutEntry(K key, V value, AtomicReference<TimeoutPolicy> timeoutPolicy, AtomicLong timeoutNanos) {
      this.key = key;
      this.value = value;
      this.timeoutPolicy = timeoutPolicy;
      this.timeoutNanos = timeoutNanos;
      this.expectedTimeout = new AtomicLong();
      resetTimeout();
    }

    @Override
    public int compareTo(TimeoutEntry<K, V> other) {
      if (key.equals(other.key))
        return 0;
      return expectedTimeout.get() < other.expectedTimeout.get() ? -1 : 1;
    }

    @Override
    public boolean equals(Object pOther) {
      return key.equals(((TimeoutEntry<?, ?>) pOther).key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }

    /**
     * Marks the entry as canceled and resets the expiration if {@code resetTimeout} is true.
     * 
     * @param resetTimeout
     *        whether the entry's expiration should be reset
     * @return true if the entry was scheduled
     */
    synchronized boolean cancel(boolean resetTimeout) {
      boolean result = scheduled;
      if (entryFuture != null)
        entryFuture.cancel(false);

      entryFuture = null;
      scheduled = false;

      if (resetTimeout)
        resetTimeout();
      return result;
    }

    /** Gets the entry value. */
    synchronized V getValue() {
      return value;
    }

    /** Resets the entry's expected expiration. */
    void resetTimeout() {
      expectedTimeout.set(timeoutNanos.get() + System.nanoTime());
    }

    /** Marks the entry as scheduled. */
    synchronized void schedule(Future<?> entryFuture) {
      this.entryFuture = entryFuture;
      scheduled = true;
    }

    /** Sets the entry value. */
    synchronized void setValue(V value) {
      this.value = value;
    }
  }

  /** Map Entry的转化 */
  private static <K, V> Map.Entry<K, V> mapEntryFor(final TimeoutEntry<K, V> entry) {
    return new Map.Entry<K, V>() {
      @Override
      public K getKey() {
        return entry.key;
      }

      @Override
      public V getValue() {
        return entry.value;
      }

      @Override
      public V setValue(V value) {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** Entry map 接口定义. */
  private interface EntryMap<K, V> extends Map<K, TimeoutEntry<K, V>> {
    /** Returns the first entry in the map or null if the map is empty. */
    TimeoutEntry<K, V> first();

    /**
     * Reorders the given entry in the map.
     * 
     * @param entry
     *        to reorder
     */
    void reorder(TimeoutEntry<K, V> entry);

    /** Returns a values iterator. */
    Iterator<TimeoutEntry<K, V>> valuesIterator();
  }

  /** Entry LinkedHashMap implementation. */
  private static class EntryLinkedHashMap<K, V> extends LinkedHashMap<K, TimeoutEntry<K, V>>
      implements EntryMap<K, V> {
    private static final long serialVersionUID = 1L;

    @Override
    public TimeoutEntry<K, V> first() {
      return isEmpty() ? null : values().iterator().next();
    }

    @Override
    public void reorder(TimeoutEntry<K, V> value) {
      remove(value.key);
      put(value.key, value);
    }

    @Override
    public Iterator<TimeoutEntry<K, V>> valuesIterator() {
      return values().iterator();
    }

    abstract class AbstractHashIterator {
      private final Iterator<Map.Entry<K, TimeoutEntry<K, V>>> iterator = entrySet().iterator();
      private TimeoutEntry<K, V> next;

      public boolean hasNext() {
        return iterator.hasNext();
      }

      public TimeoutEntry<K, V> getNext() {
        next = iterator.next().getValue();
        return next;
      }

      public void remove() {
        iterator.remove();
      }
    }

    final class KeyIterator extends AbstractHashIterator implements Iterator<K> {
      public final K next() {
        return getNext().key;
      }
    }

    final class ValueIterator extends AbstractHashIterator implements Iterator<V> {
      public final V next() {
        return getNext().value;
      }
    }

    public final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>> {
      public final Map.Entry<K, V> next() {
        return mapEntryFor(getNext());
      }
    }
  }

  /** Entry TreeHashMap implementation. */
  private static class EntryTreeHashMap<K, V> extends HashMap<K, TimeoutEntry<K, V>>
      implements EntryMap<K, V> {
    private static final long serialVersionUID = 1L;
    SortedSet<TimeoutEntry<K, V>> sortedSet = new TreeSet<TimeoutEntry<K, V>>();

    @Override
    public void clear() {
      super.clear();
      sortedSet.clear();
    }

    @Override
    public TimeoutEntry<K, V> first() {
      return sortedSet.isEmpty() ? null : sortedSet.first();
    }

    @Override
    public TimeoutEntry<K, V> put(K key, TimeoutEntry<K, V> value) {
      sortedSet.add(value);
      return super.put(key, value);
    }

    @Override
    public TimeoutEntry<K, V> remove(Object key) {
      TimeoutEntry<K, V> entry = super.remove(key);
      if (entry != null)
        sortedSet.remove(entry);
      return entry;
    }

    @Override
    public void reorder(TimeoutEntry<K, V> value) {
      sortedSet.remove(value);
      sortedSet.add(value);
    }

    @Override
    public Iterator<TimeoutEntry<K, V>> valuesIterator() {
      return new TimeoutEntryIterator();
    }

    abstract class AbstractHashIterator {
      private final Iterator<TimeoutEntry<K, V>> iterator = sortedSet.iterator();
      protected TimeoutEntry<K, V> next;

      public boolean hasNext() {
        return iterator.hasNext();
      }

      public TimeoutEntry<K, V> getNext() {
        next = iterator.next();
        return next;
      }

      public void remove() {
        EntryTreeHashMap.super.remove(next.key);
        iterator.remove();
      }
    }

    final class TimeoutEntryIterator extends AbstractHashIterator implements Iterator<TimeoutEntry<K, V>> {
      public final TimeoutEntry<K, V> next() {
        return getNext();
      }
    }

    final class KeyIterator extends AbstractHashIterator implements Iterator<K> {
      public final K next() {
        return getNext().key;
      }
    }

    final class ValueIterator extends AbstractHashIterator implements Iterator<V> {
      public final V next() {
        return getNext().value;
      }
    }

    final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>> {
      public final Map.Entry<K, V> next() {
        return mapEntryFor(getNext());
      }
    }
  }

  /**
   * 构建一个timeout map 创建者.
   * 
   * @return New TimeoutMap builder
   */
  public static Builder<Object, Object> builder() {
    return new Builder<Object, Object>();
  }

  /**
   * 新建一个实例，默认设置 创建触发策略，60秒超时
   */
  @SuppressWarnings("unchecked")
  public static <K, V> TimeoutMap<K, V> create() {
    return new TimeoutMap<K, V>((Builder<K, V>) TimeoutMap.builder());
  }

  /**
   * 增加一个超时触发器
   * 
   * @param listener
   *        to add
   * @throws NullPointerException
   *         if {@code listener} is null
   */
  public void addTimeoutListener(TimeoutListener<K, V> listener) {
    Assert.notNull(listener, "listener");
    if (timeoutListeners == null)
      timeoutListeners = new CopyOnWriteArrayList<TimeoutListenerConfig<K, V>>();
    timeoutListeners.add(new TimeoutListenerConfig<K, V>(listener));
  }

  /**
   * 获取超时策略
   */
  TimeoutPolicy getTimeoutPolicy(K key) {
    TimeoutEntry<K, V> entry = readEntry(key);
    return entry == null ? null : entry.timeoutPolicy.get();
  }

  @Override
  public void clear() {
    mapLock.lock();
    try {
      for (TimeoutEntry<K, V> entry : entries.values())
        entry.cancel(false);
      entries.clear();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    mapLock.lock();
    try {
      return entries.containsKey(key);
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    mapLock.lock();
    try {
      return entries.containsValue(value);
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public boolean equals(Object obj) {
    mapLock.lock();
    try {
      return entries.equals(obj);
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public V get(Object key) {
    TimeoutEntry<K, V> entry = readEntry(key);

    if (entry == null) {
      return null;
    } else if (TimeoutPolicy.ACCESSED.equals(entry.timeoutPolicy.get()))
      resetEntry(entry, false);

    return entry.getValue();
  }


  /**
   * Returns the map's default expiration duration in milliseconds.
   * 
   * @return The expiration duration (milliseconds)
   */
  public long getTimeout() {
    return TimeUnit.NANOSECONDS.toMillis(timeoutNanos.get());
  }

  /**
   * Gets the expiration duration in milliseconds for the entry corresponding to the given key.
   * 
   * @param key
   * @return The expiration duration in milliseconds
   * @throws NullPointerException
   *         if {@code key} is null
   * @throws NoSuchElementException
   *         If no entry exists for the given key
   */
  public long getTimeout(K key) {
    Assert.notNull(key, "key");
    TimeoutEntry<K, V> entry = readEntry(key);
    if (entry == null)
      throw new NoSuchElementException();
    return TimeUnit.NANOSECONDS.toMillis(entry.timeoutNanos.get());
  }

  /**
   * Gets the expected expiration in milliseconds for the entry corresponding to the given key.
   * 
   * @param key
   * @return The expiration duration in milliseconds
   * @throws NullPointerException
   *         if {@code key} is null
   * @throws NoSuchElementException
   *         If no entry exists for the given key
   */
  public long getExpectedTimeout(K key) {
    Assert.notNull(key, "key");
    TimeoutEntry<K, V> entry = readEntry(key);
    if (entry == null)
      throw new NoSuchElementException();
    return TimeUnit.NANOSECONDS.toMillis(entry.expectedTimeout.get() - System.nanoTime());
  }

  @Override
  public int hashCode() {
    mapLock.lock();
    try {
      return entries.hashCode();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    mapLock.lock();
    try {
      return entries.isEmpty();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public int size() {
    mapLock.lock();
    try {
      return entries.size();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public String toString() {
    mapLock.lock();
    try {
      return entries.toString();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public void clear() {
        TimeoutMap.this.clear();
      }

      @Override
      public boolean contains(Object key) {
        return containsKey(key);
      }

      @Override
      public Iterator<K> iterator() {
        return (entries instanceof EntryLinkedHashMap)
            ? ((EntryLinkedHashMap<K, V>) entries).new KeyIterator()
            : ((EntryTreeHashMap<K, V>) entries).new KeyIterator();
      }

      @Override
      public boolean remove(Object value) {
        return TimeoutMap.this.remove(value) != null;
      }

      @Override
      public int size() {
        return TimeoutMap.this.size();
      }
    };
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new AbstractSet<Map.Entry<K, V>>() {
      @Override
      public void clear() {
        TimeoutMap.this.clear();
      }

      @Override
      public boolean contains(Object entry) {
        if (!(entry instanceof Map.Entry))
          return false;
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
        return containsKey(e.getKey());
      }

      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return (entries instanceof EntryLinkedHashMap)
            ? ((EntryLinkedHashMap<K, V>) entries).new EntryIterator()
            : ((EntryTreeHashMap<K, V>) entries).new EntryIterator();
      }

      @Override
      public boolean remove(Object entry) {
        if (entry instanceof Map.Entry) {
          Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
          return TimeoutMap.this.remove(e.getKey()) != null;
        }
        return false;
      }

      @Override
      public int size() {
        return TimeoutMap.this.size();
      }
    };
  }

  @Override
  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public void clear() {
        TimeoutMap.this.clear();
      }

      @Override
      public boolean contains(Object value) {
        return containsValue(value);
      }

      @Override
      public Iterator<V> iterator() {
        return (entries instanceof EntryLinkedHashMap)
            ? ((EntryLinkedHashMap<K, V>) entries).new ValueIterator()
            : ((EntryTreeHashMap<K, V>) entries).new ValueIterator();
      }

      @Override
      public int size() {
        return TimeoutMap.this.size();
      }
    };
  }

  /**
   * Puts {@code value} in the map for {@code key}. Resets the entry's expiration unless an entry
   * already exists for the same {@code key} and {@code value}.
   * 
   * @param key
   *        to put value for
   * @param value
   *        to put for key
   * @return the old value
   * @throws NullPointerException
   *         if {@code key} is null
   */
  @Override
  public V put(K key, V value) {
    Assert.notNull(key, "key");
    return putInternal(key, value, timeoutPolicy.get(), timeoutNanos.get());
  }

  /**
   * @see #put(Object, Object, TimeoutPolicy, long, TimeUnit)
   */
  public V put(K key, V value, TimeoutPolicy timeoutPolicy) {
    return put(key, value, timeoutPolicy, timeoutNanos.get(), TimeUnit.NANOSECONDS);
  }

  /**
   * @see #put(Object, Object, TimeoutPolicy, long, TimeUnit)
   */
  public V put(K key, V value, long duration, TimeUnit timeUnit) {
    return put(key, value, timeoutPolicy.get(), duration, timeUnit);
  }

  /**
   * Puts {@code value} in the map for {@code key}. Resets the entry's expiration unless an entry
   * already exists for the same {@code key} and {@code value}. Requires that variable expiration be
   * enabled.
   * 
   * @param key
   *        Key to put value for
   * @param value
   *        Value to put for key
   * @param duration
   *        the length of time after an entry is created that it should be removed
   * @param timeUnit
   *        the unit that {@code duration} is expressed in
   * @return the old value
   * @throws UnsupportedOperationException
   *         If variable expiration is not enabled
   * @throws NullPointerException
   *         if {@code key}, {@code timeoutPolicy} or {@code timeUnit} are null
   */
  public V put(K key, V value, TimeoutPolicy timeoutPolicy, long duration, TimeUnit timeUnit) {
    Assert.notNull(key, "key");
    Assert.notNull(timeoutPolicy, "timeoutPolicy");
    Assert.notNull(timeUnit, "timeUnit");
    Assert.operation(variableTimeout, "Variable expiration is not enabled");
    return putInternal(key, value, timeoutPolicy, TimeUnit.NANOSECONDS.convert(duration, timeUnit));
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    Assert.notNull(map, "map");
    long expiration = timeoutNanos.get();
    TimeoutPolicy timeoutPolicy = this.timeoutPolicy.get();
    mapLock.lock();
    try {
      for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
        putInternal(entry.getKey(), entry.getValue(), timeoutPolicy, expiration);
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    Assert.notNull(key, "key");
    mapLock.lock();
    try {
      if (!entries.containsKey(key))
        return putInternal(key, value, timeoutPolicy.get(), timeoutNanos.get());
      else
        return entries.get(key).getValue();
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public V remove(Object key) {
    Assert.notNull(key, "key");
    TimeoutEntry<K, V> entry = null;
    mapLock.lock();
    try {
      entry = entries.remove(key);
    } finally {
      mapLock.unlock();
    }

    if (entry == null)
      return null;
    if (entry.cancel(false))
      scheduleEntry(entries.first());

    return entry.getValue();
  }

  @Override
  public boolean remove(Object key, Object value) {
    Assert.notNull(key, "key");
    mapLock.lock();
    try {
      TimeoutEntry<K, V> entry = entries.get(key);
      if (entry != null && entry.getValue().equals(value)) {
        entries.remove(key);
        if (entry.cancel(false))
          scheduleEntry(entries.first());
        return true;
      } else
        return false;
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public V replace(K key, V value) {
    Assert.notNull(key, "key");
    mapLock.lock();
    try {
      if (entries.containsKey(key)) {
        return putInternal(key, value, timeoutPolicy.get(), timeoutNanos.get());
      } else
        return null;
    } finally {
      mapLock.unlock();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    Assert.notNull(key, "key");
    mapLock.lock();
    try {
      TimeoutEntry<K, V> entry = entries.get(key);
      if (entry != null && entry.getValue().equals(oldValue)) {
        putInternal(key, newValue, timeoutPolicy.get(), timeoutNanos.get());
        return true;
      } else
        return false;
    } finally {
      mapLock.unlock();
    }
  }


  /**
   * Removes an expiration listener.
   * 
   * @param listener
   * @throws NullPointerException
   *         if {@code listener} is null
   */
  public void removeTimeoutListener(TimeoutListener<K, V> listener) {
    Assert.notNull(listener, "listener");
    for (int i = 0; i < timeoutListeners.size(); i++) {
      if (timeoutListeners.get(i).timeoutListener.equals(listener)) {
        timeoutListeners.remove(i);
        return;
      }
    }
  }

  /**
   * Resets expiration for the entry corresponding to {@code key}.
   * 
   * @param key
   *        to reset expiration for
   * @throws NullPointerException
   *         if {@code key} is null
   */
  public void resetTimeout(K key) {
    Assert.notNull(key, "key");
    TimeoutEntry<K, V> entry = readEntry(key);
    if (entry != null)
      resetEntry(entry, false);
  }

  /**
   * Sets the expiration duration for the entry corresponding to the given key. Supported only if
   * variable expiration is enabled.
   * 
   * @param key
   *        Key to set expiration for
   * @param duration
   *        the length of time after an entry is created that it should be removed
   * @param timeUnit
   *        the unit that {@code duration} is expressed in
   * @throws NullPointerException
   *         if {@code key} or {@code timeUnit} are null
   * @throws UnsupportedOperationException
   *         If variable expiration is not enabled
   */
  public void setTimeout(K key, long duration, TimeUnit timeUnit) {
    Assert.notNull(key, "key");
    Assert.notNull(timeUnit, "timeUnit");
    Assert.operation(variableTimeout, "Variable expiration is not enabled");
    mapLock.lock();
    try {
      TimeoutEntry<K, V> entry = entries.get(key);
      entry.timeoutNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
      resetEntry(entry, true);
    } finally {
      mapLock.unlock();
    }
  }

  /**
   * Updates the default map entry expiration. Supported only if variable expiration is enabled.
   * 
   * @param duration
   *        the length of time after an entry is created that it should be removed
   * @param timeUnit
   *        the unit that {@code duration} is expressed in
   * @throws NullPointerException
   *         {@code timeUnit} is null
   * @throws UnsupportedOperationException
   *         If variable expiration is not enabled
   */
  public void setTimeout(long duration, TimeUnit timeUnit) {
    Assert.notNull(timeUnit, "timeUnit");
    Assert.operation(variableTimeout, "Variable expiration is not enabled");
    timeoutNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
  }

  /**
   * Sets the global expiration policy for the map.
   * 
   * @param timeoutPolicy
   * @throws NullPointerException
   *         {@code timeoutPolicy} is null
   */
  public void setTimeoutPolicy(TimeoutPolicy timeoutPolicy) {
    Assert.notNull(timeoutPolicy, "timeoutPolicy");
    this.timeoutPolicy.set(timeoutPolicy);
  }

  /**
   * Sets the expiration policy for the entry corresponding to the given key.
   * 
   * @param key
   *        to set policy for
   * @param timeoutPolicy
   *        to set
   * @throws NullPointerException
   *         if {@code key} or {@code timeoutPolicy} are null
   * @throws UnsupportedOperationException
   *         If variable expiration is not enabled
   */
  public void setTimeoutPolicy(K key, TimeoutPolicy timeoutPolicy) {
    Assert.notNull(key, "key");
    Assert.notNull(timeoutPolicy, "timeoutPolicy");
    Assert.operation(variableTimeout, "Variable expiration is not enabled");
    TimeoutEntry<K, V> entry = readEntry(key);
    if (entry != null)
      entry.timeoutPolicy.set(timeoutPolicy);
  }

  private TimeoutEntry<K, V> readEntry(Object key) {
    mapLock.lock();
    try {
      return entries.get(key);
    } finally {
      mapLock.unlock();
    }
  }

  /**
   * Notifies expiration listeners that the given entry expired. Utilizes an expiration policy to
   * invoke the listener. If the listener's initial execution exceeds LISTENER_EXECUTION_THRESHOLD
   * then the listener will be invoked within the context of {@code listenerService}, else it will
   * be invoked within the context of {@code timer}. Must not be called from within a locked
   * context.
   * 
   * @param entry
   *        Entry to expire
   */
  void notifyListeners(final TimeoutEntry<K, V> entry) {
    if (timeoutListeners == null)
      return;

    for (final TimeoutListenerConfig<K, V> listener : timeoutListeners) {
      if (listener.executionPolicy == -1) {
        long startTime = System.nanoTime();
        try {
          listener.timeoutListener.expired(entry.key, entry.getValue());
        } catch (Exception ignoreUserExceptions) {
        }
        long endTime = System.nanoTime();
        listener.executionPolicy = startTime + LISTENER_EXECUTION_THRESHOLD > endTime ? 0 : 1;
      } else if (listener.executionPolicy == 0)
        try {
          listener.timeoutListener.expired(entry.key, entry.getValue());
        } catch (Exception ignoreUserExceptions) {
        }
      else if (listener.executionPolicy == 1) {
        listenerService.execute(new Runnable() {
          public void run() {
            try {
              listener.timeoutListener.expired(entry.key, entry.getValue());
            } catch (Exception ignoreUserExceptions) {
            }
          }
        });
      }
    }
  }

  /**
   * 满足<相同特征值的事件按顺序执行,不同特征值的事件并发执行>的listener调用方式 (这里描述这个方法适用条件 – 可选) @param entry void @exception
   */
  void notifyOrderRequiredListeners(final TimeoutEntry<K, V> entry) {
    if (timeoutListeners == null)
      return;

    for (final TimeoutListenerConfig<K, V> listener : timeoutListeners) {
      long index = 0L;
      // 如果已经指定了该事件的特征值,则以该指定的特征值作为依据分配到对应的线程执行
      if (entry.getValue() instanceof IOrderRequiredListener) {
        index = ((IOrderRequiredListener) entry.getValue()).getOrderIndex();
      } else {
        // 如果未指定事件特征值,则取一个递增的index来作为分配依据,以rounding的方式,将每个事件依次分配给每个线程
        index = timeoutEventIndex.getAndIncrement();
      }
      TimeoutExecutionQueue.getInstance().execute(new Runnable() {
        public void run() {
          try {
            listener.timeoutListener.expired(entry.key, entry.getValue());
          } catch (Exception ignoreUserExceptions) {
          }
        }
      }, index);
    }
  }

  /**
   * Resets the given entry's schedule canceling any existing scheduled expiration and reordering
   * the entry in the internal map. Schedules the next entry in the map if the given {@code entry}
   * was scheduled or if {@code scheduleNext} is true.
   * 
   * @param entry
   *        to reset
   * @param scheduleFirstEntry
   *        whether the first entry should be automatically scheduled
   */
  void resetEntry(TimeoutEntry<K, V> entry, boolean scheduleFirstEntry) {
    mapLock.lock();
    try {
      boolean scheduled = entry.cancel(true);
      entries.reorder(entry);

      if (scheduled || scheduleFirstEntry)
        scheduleEntry(entries.first());
    } finally {
      mapLock.unlock();
    }
  }

  /**
   * Schedules an entry for expiration. Guards against concurrent schedule/schedule, cancel/schedule
   * and schedule/cancel calls.
   * 
   * @param entry
   *        Entry to schedule
   */
  void scheduleEntry(TimeoutEntry<K, V> entry) {
    if (entry == null || entry.scheduled)
      return;

    Runnable runnable = null;
    synchronized (entry) {
      if (entry.scheduled)
        return;

      final WeakReference<TimeoutEntry<K, V>> entryReference = new WeakReference<TimeoutEntry<K, V>>(entry);
      runnable = new Runnable() {
        @Override
        public void run() {
          TimeoutEntry<K, V> entry = entryReference.get();

          mapLock.lock();
          try {
            if (entry != null && entry.scheduled) {
              entries.remove(entry.key);
              notifyOrderRequiredListeners(entry);
              // notifyListeners(entry);
            }

            try {
              // Expires entries and schedules the next entry
              Iterator<TimeoutEntry<K, V>> iterator = entries.valuesIterator();
              boolean schedulePending = true;

              while (iterator.hasNext() && schedulePending) {
                TimeoutEntry<K, V> nextEntry = iterator.next();
                if (nextEntry.expectedTimeout.get() <= System.nanoTime()) {
                  iterator.remove();
                  notifyOrderRequiredListeners(nextEntry);
                  // notifyListeners(nextEntry);
                } else {
                  scheduleEntry(nextEntry);
                  schedulePending = false;
                }
              }
            } catch (NoSuchElementException ignored) {
            }
          } finally {
            mapLock.unlock();
          }
        }
      };

      Future<?> entryFuture =
          expirer.schedule(runnable, entry.expectedTimeout.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
      entry.schedule(entryFuture);
    }
  }

  /**
   * Puts the given key/value in storage, scheduling the new entry for expiration if needed. If a
   * previous value existed for the given key, it is first cancelled and the entries reordered to
   * reflect the new expiration.
   */
  V putInternal(K key, V value, TimeoutPolicy timeoutPolicy, long timeoutNanos) {
    mapLock.lock();
    try {
      TimeoutEntry<K, V> entry = entries.get(key);
      V oldValue = null;

      if (entry == null) {
        entry = new TimeoutEntry<K, V>(key, value,
            variableTimeout ? new AtomicReference<TimeoutPolicy>(timeoutPolicy) : this.timeoutPolicy,
            variableTimeout ? new AtomicLong(timeoutNanos) : this.timeoutNanos);
        entries.put(key, entry);
        if (entries.size() == 1 || entries.first().equals(entry))
          scheduleEntry(entry);
      } else {
        oldValue = entry.getValue();
        if (!TimeoutPolicy.ACCESSED.equals(timeoutPolicy)
            && ((oldValue == null && value == null) || (oldValue != null && oldValue.equals(value))))
          return value;

        entry.setValue(value);
        resetEntry(entry, false);
      }

      return oldValue;
    } finally {
      mapLock.unlock();
    }
  }

}
