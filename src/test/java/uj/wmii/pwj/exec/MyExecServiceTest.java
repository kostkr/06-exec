package uj.wmii.pwj.exec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyExecServiceTest {
    @Test
    void testShutdown(){
        MyExecService s = new MyExecService();
        TestRunnable r = new TestRunnable();
        s.execute(r);
        doSleep(10);
        s.shutdown();
        assertTrue(s.isShutdown() && r.wasRun);
    }

    @Test
    void testShutdownNow(){
        MyExecService s = new MyExecService();
        TestRunnable r = new TestRunnable();
        s.execute(r);
        doSleep(10);
        s.shutdownNow();
        assertTrue(s.isShutdown() && r.wasRun );
    }

    @Test
    void testIsTerminated() throws InterruptedException {
        MyExecService execService = MyExecService.newInstance();

        Future<String> future = execService.submit(() -> {
            Thread.sleep(100);
            return "Task completed";
        });

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(500);
                execService.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        assertFalse(execService.isTerminated());

        assertTrue(execService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS));

        assertTrue(execService.isTerminated());

        assertTrue(future.isDone());
    }

    @Test
    void testInvokeAll() throws InterruptedException, ExecutionException, TimeoutException {
        MyExecService execService = MyExecService.newInstance();

        List<Callable<String>> tasks = new ArrayList<>();
        tasks.add(() -> {
            Thread.sleep(200);
            return "Task 1 completed";
        });
        tasks.add(() -> {
            Thread.sleep(300);
            return "Task 2 completed";
        });

        List<Future<String>> futures = execService.invokeAll(tasks);

        for (Future<String> future : futures) {
            assertNotNull(future.get());
        }
    }

    @Test
    void testInvokeAnyException() throws InterruptedException{
        MyExecService execService = MyExecService.newInstance();

        List<Callable<String>> tasks = new ArrayList<>();
        tasks.add(() -> {
            Thread.sleep(200);
            return "Task 1 completed";
        });
        tasks.add(() -> {
            Thread.sleep(300);
            throw new RuntimeException("Task 2 failed");
        });

        try {
            String result = execService.invokeAny(tasks);

            assertNotNull(result);

            assertTrue(result.equals("Task 1 completed") || result.equals("Task 2 failed"));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof RuntimeException);
            assertEquals("no task completed", cause.getMessage());
        }
    }

    @Test
    void testInvokeAny() throws InterruptedException, ExecutionException, TimeoutException {
        MyExecService execService = MyExecService.newInstance();

        List<Callable<String>> tasks = new ArrayList<>();
        tasks.add(() -> {
            return "Task 1 completed";
        });

        String result = execService.invokeAny(tasks, 50, TimeUnit.MILLISECONDS);
        assertEquals("Task 1 completed", result);
    }

    class TestRunnable implements Runnable {

        boolean wasRun;
        @Override
        public void run() {
            wasRun = true;
        }
    }

    static void doSleep(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}


