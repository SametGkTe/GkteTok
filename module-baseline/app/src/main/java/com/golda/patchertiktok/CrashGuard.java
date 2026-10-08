package com.golda.patchertiktok;

/**
 * GkteTok — çökme koruması.
 *
 * Modülde ya da modülün taktığı bir kancada beklenmedik bir hata oluşursa, uygulama
 * düşmeden hemen önce hata **tam yığın iziyle** günlüğe yazılır. Böylece cihazda
 * "çöktü" denen her durum tani-log.txt içinde sebebiyle birlikte görünür olur.
 *
 * Hata yutulmaz: Android'in kendi işleyicisi yine çağrılır, davranış değişmez.
 */
final class CrashGuard {
    private static volatile boolean armed;

    private CrashGuard() { }

    static boolean armed() {
        return armed;
    }

    static void install() {
        if (armed) return;
        try {
            Thread.UncaughtExceptionHandler next = Thread.getDefaultUncaughtExceptionHandler();
            if (next instanceof Guard) return;
            Thread.setDefaultUncaughtExceptionHandler(new Guard(next));
            armed = true;
            RuntimeLog.log("crash guard: armed");
        } catch (Throwable error) {
            RuntimeLog.log("crash guard: kurulamadi: " + error.getClass().getSimpleName());
        }
    }

    /** Saf metin: çökme özeti (bkz. CrashGuardTest). */
    static String describe(Throwable error) {
        return describe(error, 0);
    }

    private static String describe(Throwable error, int depth) {
        if (error == null) return "-";
        StringBuilder out = new StringBuilder(error.getClass().getName());
        String message = error.getMessage();
        if (message != null && !message.isEmpty()) out.append(": ").append(message);
        StackTraceElement[] trace = error.getStackTrace();
        int limit = Math.min(trace.length, 12);
        for (int index = 0; index < limit; index++) out.append(" | ").append(trace[index]);
        if (trace.length > limit) out.append(" | ... (").append(trace.length).append(" satir)");
        Throwable cause = error.getCause();
        if (cause != null && cause != error && depth < 3) {
            out.append(" || nedeni: ").append(describe(cause, depth + 1));
        }
        return out.toString();
    }

    private static final class Guard implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler next;

        Guard(Thread.UncaughtExceptionHandler next) {
            this.next = next;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable error) {
            try {
                RuntimeLog.log("CRASH [" + thread.getName() + "] " + describe(error));
            } catch (Throwable ignored) { }
            if (next != null) next.uncaughtException(thread, error);
        }
    }
}
