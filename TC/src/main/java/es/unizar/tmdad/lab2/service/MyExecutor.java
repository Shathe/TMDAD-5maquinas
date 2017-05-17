package es.unizar.tmdad.lab2.service;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;
@Component
public class MyExecutor implements Executor {
	
    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }

}
