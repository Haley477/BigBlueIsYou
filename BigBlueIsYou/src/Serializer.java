

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import com.google.gson.Gson;
import java.io.FileWriter;
import java.io.FileReader;

public class Serializer implements Runnable{
    private enum Activity {
        Nothing,
        Load,
        Save
    }

    private boolean done = false;
    private final Lock lockSignal = new ReentrantLock();
    private final Condition doSomething = lockSignal.newCondition();
    private Activity doThis = Activity.Nothing;

    private KeyboardState keyboardState;

    private final Thread tInternal;

    public Serializer() {
        this.tInternal = new Thread(this);
        this.tInternal.start();
    }

    @Override
    public void run() {
        try {
            while (!done) {
                // Wait for a signal to do something
                lockSignal.lock();
                doSomething.await();
                lockSignal.unlock();

                // Based on what was requested, do something
                switch (doThis) {
                    case Nothing -> {}
                    case Save -> saveSomething();
                    case Load -> loadSomething();
                }
            }
        } catch (Exception ex) {
            System.out.printf("Something bad happened: %s\n", ex.getMessage());
        }
    }

    /// Public method used by client code to request the game state is saved
    /// NOTE: This does not prevent against race conditions if the keyboardState object
    ///       is modified while the saving is taking place.  A production level
    ///       approach would have an event held by the client signaled when
    ///       the saving is complete.
    public void saveGameState(KeyboardState state) {
        lockSignal.lock();

        this.keyboardState = state;
        doThis = Activity.Save;
        doSomething.signal();

        lockSignal.unlock();
    }

    /// Public method used the client code to request the game state is loaded.
    /// NOTE: Same comment about race conditions as above.
    public void loadGameState(KeyboardState state) {
        lockSignal.lock();

        this.keyboardState = state;
        doThis = Activity.Load;
        doSomething.signal();

        lockSignal.unlock();
    }

    /// Public method used to signal this code to perform a graceful shutdown
    public void shutdown() {
        try {
            lockSignal.lock();

            doThis = Activity.Nothing;
            done = true;
            doSomething.signal();

            lockSignal.unlock();

            tInternal.join();
        } catch (Exception ex) {
            System.out.printf("Failure to gracefully shutdown thread: %s\n", ex.getMessage());
        }
    }

    /// This is where the actual serialization of the game state is performed.  Have
    /// chosen to save in JSON format for readability for the demo, but the state
    /// could have been stored using a binary serializer for more efficient storage
    private synchronized void saveSomething() {
        System.out.println("saving something...");
        try (FileWriter writer = new FileWriter("keyboardstate.json")) {
            Gson gson = new Gson();
            gson.toJson(this.keyboardState, writer);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /// This is where the actual deserialization of the game state is performed.
    /// Same note as above regarding the choice to use JSON formatting.
    private synchronized void loadSomething() {
        System.out.println("loading something...");
        try (FileReader reader = new FileReader("keyboardstate.json")) {
            KeyboardState state = (new Gson()).fromJson(reader, KeyboardState.class);
            this.keyboardState.moveUpKey = state.moveUpKey;
            this.keyboardState.moveDownKey = state.moveDownKey;
            this.keyboardState.moveLeftKey = state.moveLeftKey;
            this.keyboardState.moveRightKey = state.moveRightKey;
            this.keyboardState.resetLevel = state.resetLevel;
            this.keyboardState.undo = state.undo;
            this.keyboardState.initialized = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
