package uj.wmii.pwj.exec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class MyExecService implements ExecutorService {

    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final Worker worker = new Worker();
    private volatile boolean isShutDown = false;
    MyExecService(){
        this.worker.start();
    }
    static MyExecService newInstance() {
        return new MyExecService();
    }

    @Override
    public void shutdown() {
        isShutDown = true;
        worker.interrupt();
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutDown = true;
        worker.interrupt();
        return tasks.stream().toList();
    }

    @Override
    public boolean isShutdown() {
        return isShutDown;
    }

    @Override
    public boolean isTerminated() {
        return isShutDown && tasks.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeMillis = unit.toMillis(timeout);
        long endTime = System.currentTimeMillis() + timeMillis;
        while (!isTerminated() && System.currentTimeMillis() < endTime) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        return isTerminated();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        execute(future);
        return future;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        FutureTask<T> future = new FutureTask<>(task, result);
        execute(future);
        return future;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> futureList = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futureList.add(submit(task));
        }
        return futureList;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<Future<T>> futureList = invokeAll(tasks);

        long timeMillis = unit.toMillis(timeout);

        for(Future<T> future : futureList){
            try {
                future.get(timeMillis, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | TimeoutException e) {}
        }

        return futureList;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Future<T>> futureList = invokeAll(tasks);
        return futureList.stream().
                filter(Future::isDone).
                findAny().
                orElseThrow(() ->
                        new ExecutionException( new RuntimeException("no task completed"))).
                get();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<Future<T>> futureList = invokeAll(tasks, timeout, unit);
        return futureList.stream().
                filter(Future::isDone).
                findAny().
                orElseThrow(() ->
                        new TimeoutException("time out")).
                get();
    }

    @Override
    public void execute(Runnable command) {
        if(!isShutDown)
            tasks.add(command);
        else System.err.println("executeService is shut down");
    }

    private class Worker extends Thread{
        @Override
        public void run(){
            while (!isShutDown){
                try{
                    Runnable task = tasks.take();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
