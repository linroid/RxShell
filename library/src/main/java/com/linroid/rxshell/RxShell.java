package com.linroid.rxshell;

import android.os.HandlerThread;

import com.linroid.rxshell.exception.ShellExecuteErrorException;
import com.linroid.rxshell.exception.ShellTerminateException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * @author linroid <linroid@gmail.com>
 * @since 17/01/2017
 */
public class RxShell {
    private final HandlerThread workerThread = new HandlerThread("RxShell-Worker");

    private final Shell shell;

    public RxShell(String customShell) {
        this.shell = new Shell(customShell);
        init();
    }

    public RxShell(boolean requireRoot) {
        this.shell = new Shell(requireRoot);
        init();
    }

    private void init() {
        workerThread.start();
    }

    public Observable<Boolean> destory() {
        return create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                emitter.onNext(shell.destroy());
                emitter.onComplete();
            }
        });
    }

    public Observable<String> exec(final String binary) {
        return exec(binary, "");
    }

    public Observable<String> exec(final String binary, final String arguments) {
        return create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) {
                shell.exec(binary, arguments, new Shell.Callback() {
                    @Override
                    public void onOutput(String line) {
                        emitter.onNext(line);
                    }

                    @Override
                    public void onTerminate() {
                        emitter.onError(new ShellTerminateException());
                    }

                    @Override
                    public void onFinished() {
                        emitter.onComplete();
                    }

                    @Override
                    public void onError(String output) {
                        emitter.onError(new ShellExecuteErrorException(output));
                    }
                });
            }
        });
    }

    public <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return Observable.create(source).subscribeOn(AndroidSchedulers.from(workerThread.getLooper()));
    }

}
