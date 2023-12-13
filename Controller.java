import java.util.function.Function;


public class Controller {
    private int nInvokers;
    private int totalSizeMB;

    public Controller(int nInv, int tSizeMB){
        nInvokers = nInv;
        totalSizeMB = tSizeMB;
    }

    public int getNInvokers() {
        return nInvokers;
    }

    public int getTotalSizeMB() {
        return totalSizeMB;
    }

    public void registerAction(String actionName, Function<T, R> f, int actionSizeMB) {

    }

    public R invoke(String actionName, T actionParam) {
        return actionName.apply(actionParam);
    }
}
