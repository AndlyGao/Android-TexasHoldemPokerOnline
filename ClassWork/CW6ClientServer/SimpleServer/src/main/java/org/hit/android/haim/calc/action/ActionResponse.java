package org.hit.android.haim.calc.action;

public class ActionResponse<T> {
    private final T response;

    public ActionResponse(T response) {
        this.response = response;
    }

    public T getResponse() {
        return response;
    }
}
