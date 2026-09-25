package com.diyebure.golia.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

/**
 * Temporizador periódico ligado al ciclo de vida, usado por el ViewModel para el
 * polling en vivo (Requisitos 7.5, 7.6, 7B.1, 7B.2).
 *
 * <p><b>Elección de implementación:</b> se usa {@link Handler} sobre
 * {@link Looper#getMainLooper()} en lugar de un {@code ScheduledExecutorService}.
 * De esta forma, cada tick {@code onTick} se ejecuta en el hilo principal, lo que
 * simplifica el contrato con el {@code ViewModel}: éste recibe el tick en el hilo
 * de UI y delega el trabajo de red real a su {@code @IoExecutor}. Al vivir en el
 * hilo principal, {@link #start(long, Runnable)}, {@link #stop()} e
 * {@link #isRunning()} no necesitan sincronización adicional siempre que se
 * invoquen desde el hilo principal (como hace el {@code ViewModel}).</p>
 *
 * <p>El intervalo (por defecto 60 s compartido) lo controla el llamador; esta
 * clase sólo provee el mecanismo de temporización de repetición fija.</p>
 */
public class LiveRefreshScheduler {

    private final Handler handler;

    /** Tarea de tick activa, o {@code null} cuando el scheduler está detenido. */
    private Runnable tickRunnable;

    private long intervalMs;

    private boolean running;

    public LiveRefreshScheduler() {
        this(new Handler(Looper.getMainLooper()));
    }

    /**
     * Constructor visible para pruebas que permite inyectar un {@link Handler}
     * (por ejemplo, uno respaldado por un {@code Looper} controlable).
     */
    LiveRefreshScheduler(@NonNull Handler handler) {
        this.handler = handler;
    }

    /**
     * Inicia un temporizador de repetición que invoca {@code onTick} cada
     * {@code intervalMs} milisegundos. El primer tick se dispara tras el primer
     * intervalo (no de forma inmediata).
     *
     * <p>Si el scheduler ya está en ejecución, se detiene la programación previa
     * antes de arrancar la nueva, de modo que llamar a {@code start()} de forma
     * repetida es seguro.</p>
     *
     * @param intervalMs intervalo entre ticks en milisegundos; debe ser positivo
     * @param onTick     acción a ejecutar en cada tick (en el hilo principal)
     * @throws IllegalArgumentException si {@code intervalMs <= 0}
     * @throws NullPointerException     si {@code onTick} es {@code null}
     */
    @MainThread
    public void start(long intervalMs, @NonNull final Runnable onTick) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be positive: " + intervalMs);
        }
        if (onTick == null) {
            throw new NullPointerException("onTick must not be null");
        }

        // Detener cualquier programación previa para que start() sea idempotente.
        stop();

        this.intervalMs = intervalMs;
        this.running = true;
        this.tickRunnable = new Runnable() {
            @Override
            public void run() {
                // Guardas contra ticks tardíos tras un stop().
                if (!running || tickRunnable != this) {
                    return;
                }
                onTick.run();
                // Reprogramar sólo si seguimos activos y no se ha reemplazado.
                if (running && tickRunnable == this) {
                    handler.postDelayed(this, LiveRefreshScheduler.this.intervalMs);
                }
            }
        };
        handler.postDelayed(tickRunnable, intervalMs);
    }

    /**
     * Detiene el temporizador. Si el scheduler no está en ejecución, es un no-op.
     */
    @MainThread
    public void stop() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
            tickRunnable = null;
        }
        running = false;
    }

    /**
     * @return {@code true} si el scheduler tiene un temporizador activo.
     */
    @MainThread
    public boolean isRunning() {
        return running;
    }
}
