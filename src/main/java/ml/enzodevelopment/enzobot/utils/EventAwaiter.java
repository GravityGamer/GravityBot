/*
 * Enzo Bot, a multipurpose discord bot
 *
 * Copyright (c) 2018 William "Enzo" Johnstone
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package ml.enzodevelopment.enzobot.utils;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.hooks.EventListener;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class EventAwaiter implements EventListener  {
    private final HashMap<Class<?>, Set<AwaitingTask>> events;
    private final ScheduledExecutorService pool;
    public EventAwaiter() {
        this.events = new HashMap<>();
        this.pool = Executors.newSingleThreadScheduledExecutor();
    }
    public <T extends Event> void awaitEvent(JDA jda, Class<T> classType, Predicate<T> condition, Consumer<T> action) {
        awaitEvent(jda, classType, condition, action, -1L, null, null);
    }
    public <T extends Event> void awaitEvent(JDA jda, Class<T> type, Predicate<T> check, Consumer<T> action, long timeout, TimeUnit unit, Runnable result) {
        if (!Objects.isNull(type) && !Objects.isNull(check) && !Objects.isNull(action)) {
            jda.addEventListener(this);
            AwaitingTask event = new AwaitingTask<>(check, action);
            Set<AwaitingTask> set = events.computeIfAbsent(type, c -> new HashSet<>());
            set.add(event);
            if (timeout > 0 && !Objects.isNull(unit)) {
                pool.schedule(() -> {
                    if (set.remove(event) && result != null) {
                        result.run();
                        jda.removeEventListener(this);
                    }
                }, timeout, unit);
            }
            return;
        }
        throw new IllegalArgumentException("Something went wrong!");
    }
    @Override
    @SuppressWarnings("unchecked")
    public final void onEvent(Event event) {
        Class c = event.getClass();
        while (!Objects.isNull(c)) {
            if (events.containsKey(c)) {
                Set<AwaitingTask> set = events.get(c);
                AwaitingTask[] removable = set.toArray(new AwaitingTask[set.size()]);
                set.removeAll(Stream.of(removable).filter(i -> i.check(event)).collect(Collectors.toSet()));
            }
            c = c.getSuperclass();
        }
    }
    private class AwaitingTask<T extends Event> {
        private final Predicate<T> check;
        private final Consumer<T> action;
        AwaitingTask(Predicate<T> check, Consumer<T> action) {
            this.check = check;
            this.action = action;
        }
        private boolean check(T event) {
            if (check.test(event)) {
                action.accept(event);
                return true;
            }
            return false;
        }
    }
}
