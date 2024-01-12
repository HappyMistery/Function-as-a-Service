package models;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

import java.util.concurrent.atomic.AtomicReference;

public class TimerDecorator extends Controller {

    private Controller controller;

    /**
     * Constructor for TimerDecorator
     * @param controller controller to decorate
     */
    public TimerDecorator(Controller controller) {
        super(controller.getNInvokers(), controller.getTotalSizeMB());
        this.controller = controller;
    }

    /**
     * Invokes an action with a given parameter and a policy and uses a timer to measure the time
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action to invoke
     * @param actionParam parameter of the action to invoke
     * @param policy policy to apply
     * @return result of the action
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
    @Override
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        AtomicReference<R> resultContainer = new AtomicReference<>();

        Thread threadInvoke = new Thread(() -> {
            try {
                R result = controller.invoke(actionName, actionParam, controller.getNInvokers());
                resultContainer.set(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread threadTimer = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long endTime;
            while (resultContainer.get() == null) {
                try {
                    Thread.sleep(1);  // Ajusta el tiempo de espera según sea necesario
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                endTime = System.currentTimeMillis();
                System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
            }
        });

        threadInvoke.start();
        threadTimer.start();

        threadInvoke.join();
        threadTimer.join();

        return (R) resultContainer.get();
    }
}