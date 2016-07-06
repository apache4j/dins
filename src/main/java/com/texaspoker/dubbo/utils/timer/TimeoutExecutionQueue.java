package com.texaspoker.dubbo.utils.timer;

import java.util.LinkedList;

public class TimeoutExecutionQueue {

  // 单例
  private static TimeoutExecutionQueue instance = new TimeoutExecutionQueue();

  public static TimeoutExecutionQueue getInstance() {
    return instance;
  }

  // 初始化
  private TimeoutExecutionQueue() {}

  // 处理线程
  private QueueWorker[] threads = null;

  // 队列处理线程
  private class QueueWorker extends Thread {
    // 请求队列信息
    private final LinkedList<Runnable> queue = new LinkedList<Runnable>();

    public void run() {
      Runnable r;
      while (true) {
        synchronized (queue) {
          while (queue.isEmpty()) {
            if (stopflag)
              return;
            try {
              queue.wait(TimeoutConfig.TIMEOUT_ORDER_REQUIRED_THREAD_WAIT_MAX_TIME);
            } catch (InterruptedException ignored) {

            }
          }
          r = (Runnable) queue.removeFirst();
        }
        // If we don't catch RuntimeException,
        // the pool could leak threads
        try {
          r.run();
        } catch (RuntimeException e) {
          // You might want to log something here
        }
        if (stopflag)
          return;
      }
    }

    private boolean stopflag = false;

    public void setStop() {
      stopflag = true;
    }

    public void execute(Runnable r) {
      synchronized (queue) {
        queue.addLast(r);
        queue.notify();
      }
    }

  }

  /*
   * 启动线程处理队列，方便自定义线程数
   */
  public void startQueue() {
    synchronized (this) {
      // 确保只启动一次
      if (!started) {
        started=true;
        // 启动线程
        threads = new QueueWorker[TimeoutConfig.TIMEOUT_ORDER_REQUIRED_THREAD_MAX_NUM];
        for (int i = 0; i < TimeoutConfig.TIMEOUT_ORDER_REQUIRED_THREAD_MAX_NUM; i++) {
          threads[i] = new QueueWorker();
          threads[i].start();
        }
      }
    }
  }

  /**
   * 判断线程池是否已经启动
   */
  private boolean started = false;

  /**
   * 执行一项任务,根据特征值分配到对应的线程中,保证相同特征值的任务按顺序执行
   * (这里描述这个方法适用条件 – 可选)
   * @param r
   * @param index
   * void
   * @exception
  */
  public void execute(Runnable r, long index) {
    int target = (int) (index % TimeoutConfig.TIMEOUT_ORDER_REQUIRED_THREAD_MAX_NUM);
    if (target < 0)
      target = -target;
    threads[target].execute(r);
  }

  /*
   * 清理队列和处理线程
   */
  public void releaseQueue() {
    synchronized (this) {
      for (int i = 0; i < TimeoutConfig.TIMEOUT_ORDER_REQUIRED_THREAD_MAX_NUM; i++) {
        threads[i].setStop();
      }
    }
  }

  /**
   * @return  the started to get
   */
  
  public boolean isStarted() {
    return started;
  }

}
